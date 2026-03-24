package com.edpp.transaction.service;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edpp.transaction.dtorequest.PaymentRequest;
import com.edpp.transaction.dtoresponse.TransactionResponse;
import com.edpp.transaction.entity.Transaction;
import com.edpp.transaction.enums.TransactionStatus;
import com.edpp.transaction.enums.TransactionType;
import com.edpp.transaction.exception.InsufficientBalanceException;
import com.edpp.transaction.exception.TransactionException;
import com.edpp.transaction.mapper.TransactionMapper;
import com.edpp.transaction.processor.PaymentProcessor;
import com.edpp.transaction.repository.TransactionRepository;
import com.edpp.transaction.util.RequestContext;
import com.edpp.transaction.validator.TransactionIdGenerator;
import com.edpp.transaction.validator.TransactionValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionLogService transactionLogService;
    private final PaymentProcessorService paymentProcessorService;
    private final WalletServiceClient walletServiceClient;
    private final IdentityServiceClient identityServiceClient;
    private final FraudDetectionService fraudDetectionService;
    private final KafkaProducerService kafkaProducerService;
    private final TransactionMapper transactionMapper;
    private final TransactionValidator validator;
    private final TransactionIdGenerator idGenerator;
    private final RequestContext requestContext;

    /**
     * Initialize a payment transaction
     */
    @Transactional
    public TransactionResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating payment: {} for tenant: {}", 
                request.getMerchantTransactionId(), requestContext.getTenantId());

        // Validate request
        validator.validatePaymentRequest(request);

        // Check fraud
        var fraudCheck = fraudDetectionService.checkTransaction(request);
        if (!fraudCheck.isAllowed()) {
            throw new TransactionException("Transaction blocked by fraud detection: " + fraudCheck.getReason());
        }

        // Validate customer and wallet
        validateCustomerAndWallet(request);

        // Create transaction
        Transaction transaction = buildTransaction(request);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setFraudCheckResult(fraudCheck);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Publish event
        kafkaProducerService.publishTransactionCreated(savedTransaction);

        // Process asynchronously
        processPaymentAsync(savedTransaction.getId());

        return transactionMapper.toResponse(savedTransaction);
    }

    /**
     * Process payment asynchronously
     */
    @Async
    @Transactional
    @CircuitBreaker(name = "paymentProcessor", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentProcessor")
    @TimeLimiter(name = "paymentProcessor")
    public CompletableFuture<Transaction> processPaymentAsync(String transactionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processPayment(transactionId);
            } catch (Exception e) {
                log.error("Error processing payment: {}", transactionId, e);
                throw new TransactionException("Payment processing failed", e);
            }
        });
    }

    /**
     * Process payment synchronously
     */
    @Transactional
    public Transaction processPayment(String transactionId) {
        Transaction transaction = transactionRepository.findByIdWithLock(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found: " + transactionId));

        log.info("Processing payment: {}", transaction.getTransactionReference());

        try {
            // Update status
            transaction.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(transaction);
            transactionLogService.log(transaction, "Started processing");

            // Select processor
            PaymentProcessor processor = paymentProcessorService.selectProcessor(transaction);
            transaction.setProcessorName(processor.getProcessorName());

            // Debit source wallet
            walletServiceClient.debitWallet(
                    transaction.getSourceWalletId(),
                    transaction.getTotalAmount(),
                    transaction.getTransactionReference()
            );

            // Process through payment gateway
            var processorResponse = processor.processPayment(transaction);

            if (processorResponse.isSuccessful()) {
                // Credit destination wallet if internal transfer
                if (transaction.getDestinationWalletId() != null) {
                    walletServiceClient.creditWallet(
                            transaction.getDestinationWalletId(),
                            transaction.getAmount(),
                            transaction.getTransactionReference()
                    );
                }

                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setProcessorTransactionId(processorResponse.getTransactionId());
                transaction.setProcessorResponseCode(processorResponse.getResponseCode());
                
                transactionLogService.log(transaction, "Payment completed successfully");
                kafkaProducerService.publishTransactionCompleted(transaction);
                
            } else {
                // Refund if debit already happened
                if (transaction.getStatus() == TransactionStatus.PROCESSING) {
                    walletServiceClient.creditWallet(
                            transaction.getSourceWalletId(),
                            transaction.getTotalAmount(),
                            transaction.getTransactionReference() + "-REFUND"
                    );
                }

                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setProcessorResponseMessage(processorResponse.getMessage());
                transaction.setFailedAt(LocalDateTime.now());
                
                transactionLogService.log(transaction, "Payment failed: " + processorResponse.getMessage());
                kafkaProducerService.publishTransactionFailed(transaction);
            }

        } catch (InsufficientBalanceException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setProcessorResponseMessage("Insufficient balance");
            transaction.setFailedAt(LocalDateTime.now());
            transactionLogService.log(transaction, "Insufficient balance");
            kafkaProducerService.publishTransactionFailed(transaction);
            
        } catch (Exception e) {
            log.error("Unexpected error processing transaction: {}", transactionId, e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setProcessorResponseMessage(e.getMessage());
            transaction.setFailedAt(LocalDateTime.now());
            transactionLogService.log(transaction, "System error: " + e.getMessage());
            kafkaProducerService.publishTransactionFailed(transaction);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * Fallback method for circuit breaker
     */
    public CompletableFuture<Transaction> processPaymentFallback(String transactionId, Exception e) {
        log.error("Circuit breaker triggered for transaction: {}", transactionId, e);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found"));
        
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setProcessorResponseMessage("Service temporarily unavailable");
        transaction.setFailedAt(LocalDateTime.now());
        
        transactionRepository.save(transaction);
        transactionLogService.log(transaction, "Circuit breaker fallback: " + e.getMessage());
        kafkaProducerService.publishTransactionFailed(transaction);
        
        return CompletableFuture.completedFuture(transaction);
    }

    /**
     * Get transaction by reference
     */
    @Cacheable(value = "transactions", key = "#reference + '_' + #tenantId")
    public TransactionResponse getTransactionByReference(String reference, String tenantId) {
        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new TransactionException("Transaction not found: " + reference));
        
        // Validate tenant access
        if (!transaction.getTenantId().equals(tenantId)) {
            throw new TransactionException("Access denied to transaction");
        }
        
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Search transactions
     */
    public Page<TransactionResponse> searchTransactions(String searchTerm, Pageable pageable) {
        String tenantId = requestContext.getTenantId();
        Page<Transaction> transactions = transactionRepository.searchTransactions(tenantId, searchTerm, pageable);
        return transactions.map(transactionMapper::toResponse);
    }

    /**
     * Get transaction statistics
     */
    public Map<String, Object> getTransactionStatistics(LocalDateTime start, LocalDateTime end) {
        String tenantId = requestContext.getTenantId();
        
        BigDecimal totalAmount = transactionRepository.getTotalTransactionAmount(tenantId, start, end);
        Long count = transactionRepository.getTransactionCount(tenantId, start, end);
        List<Object[]> statusStats = transactionRepository.getTransactionStatusStats(tenantId);
        
        return Map.of(
                "totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO,
                "transactionCount", count != null ? count : 0L,
                "statusBreakdown", statusStats,
                "periodStart", start,
                "periodEnd", end
        );
    }

    /**
     * Process refund
     */
    @Transactional
    public TransactionResponse processRefund(String originalTransactionReference, BigDecimal amount, String reason) {
        String tenantId = requestContext.getTenantId();
        
        Transaction originalTransaction = transactionRepository.findByTransactionReference(originalTransactionReference)
                .orElseThrow(() -> new TransactionException("Original transaction not found"));
        
        if (!originalTransaction.getTenantId().equals(tenantId)) {
            throw new TransactionException("Access denied");
        }
        
        // Create refund transaction
        Transaction refund = Transaction.builder()
                .transactionReference(idGenerator.generateRefundReference(originalTransactionReference))
                .merchantTransactionId(idGenerator.generateMerchantRefundId())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.PENDING)
                .sourceWalletId(originalTransaction.getDestinationWalletId())
                .destinationWalletId(originalTransaction.getSourceWalletId())
                .amount(amount)
                .currency(originalTransaction.getCurrency())
                .tenantId(tenantId)
                .description("Refund for " + originalTransactionReference + ": " + reason)
                .customerId(originalTransaction.getCustomerId())
                .customerEmail(originalTransaction.getCustomerEmail())
                .build();
        
        Transaction savedRefund = transactionRepository.save(refund);
        
        // Process refund asynchronously
        processPaymentAsync(savedRefund.getId());
        
        return transactionMapper.toResponse(savedRefund);
    }

    private void validateCustomerAndWallet(PaymentRequest request) {
        // Validate wallet exists and has sufficient balance
        var walletValidation = walletServiceClient.validateWallet(request.getSourceWalletId());
        
        if (!walletValidation.isValid()) {
            throw new TransactionException("Invalid source wallet: " + walletValidation.getMessage());
        }
        
        // Validate customer is active
        var customerValidation = identityServiceClient.validateCustomer(
                walletValidation.getCustomerId(), 
                requestContext.getTenantId()
        );
        
        if (!customerValidation.isValid()) {
            throw new TransactionException("Customer not eligible for transaction: " + customerValidation.getMessage());
        }
    }

    private Transaction buildTransaction(PaymentRequest request) {
        return Transaction.builder()
                .transactionReference(idGenerator.generateTransactionReference())
                .merchantTransactionId(request.getMerchantTransactionId())
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PENDING)
                .sourceWalletId(request.getSourceWalletId())
                .destinationWalletId(request.getDestinationWalletId())
                .amount(request.getAmount())
                .fee(calculateFee(request.getAmount(), request.getPaymentMethod()))
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .description(request.getDescription())
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .ipAddress(requestContext.getClientIp())
                .userAgent(requestContext.getUserAgent())
                .tenantId(requestContext.getTenantId())
                .cardDetails(request.getCardDetails())
                .bankDetails(request.getBankDetails())
                .createdBy(requestContext.getUserId())
                .build();
    }

    private BigDecimal calculateFee(BigDecimal amount, String paymentMethod) {
        // Complex fee calculation logic based on payment method, amount, merchant agreement
        BigDecimal feePercentage = switch (paymentMethod) {
            case "CARD" -> new BigDecimal("0.015"); // 1.5%
            case "BANK_TRANSFER" -> new BigDecimal("0.005"); // 0.5%
            case "WALLET" -> new BigDecimal("0.001"); // 0.1%
            default -> new BigDecimal("0.02"); // 2% default
        };
        
        return amount.multiply(feePercentage).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
