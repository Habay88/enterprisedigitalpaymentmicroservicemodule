package com.edpp.transaction.service;

import com.edpp.transaction.dto.response.ProcessorResponse;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ObjectMapper objectMapper;

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
        
        try {
            transaction.setFraudCheckResult(objectMapper.writeValueAsString(fraudCheck));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize fraud check result", e);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        transactionLogService.logTransactionInitiated(savedTransaction);

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
            ProcessorResponse processorResponse = processor.processPayment(transaction);

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
                transaction.setProcessorResponseMessage(processorResponse.getResponseMessage());
                
                transactionLogService.logTransactionCompleted(transaction, processorResponse.getTransactionId());
                kafkaProducerService.publishTransactionCompleted(transaction);
                
            } else {
                handleFailedTransaction(transaction, processorResponse.getResponseMessage());
            }

        } catch (InsufficientBalanceException e) {
            handleFailedTransaction(transaction, "Insufficient balance");
            
        } catch (Exception e) {
            log.error("Unexpected error processing transaction: {}", transactionId, e);
            handleFailedTransaction(transaction, e.getMessage());
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
     * Reverse a transaction (full reversal)
     */
    @Transactional
    public TransactionResponse reverseTransaction(String transactionReference, String reason) {
        log.info("Reversing transaction: {} for tenant: {}", transactionReference, requestContext.getTenantId());
        
        Transaction originalTransaction = transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new TransactionException("Original transaction not found: " + transactionReference));

        // Validate tenant access
        if (!originalTransaction.getTenantId().equals(requestContext.getTenantId())) {
            throw new TransactionException("Access denied to transaction");
        }

        // Check if reversal is eligible 
        validateReversalEligibility(originalTransaction);

        // Create reversal transaction 
        Transaction reversalTransaction = Transaction.builder()
                .transactionReference(idGenerator.generateReversalReference(transactionReference))
                .merchantTransactionId(idGenerator.generateMerchantTransactionId())
                .type(TransactionType.REVERSAL)
                .status(TransactionStatus.PENDING)
                .sourceWalletId(originalTransaction.getDestinationWalletId())
                .destinationWalletId(originalTransaction.getSourceWalletId())
                .amount(originalTransaction.getAmount())
                .fee(BigDecimal.ZERO)
                .totalAmount(originalTransaction.getTotalAmount())
                .currency(originalTransaction.getCurrency())
                .paymentMethod(originalTransaction.getPaymentMethod())
                .tenantId(requestContext.getTenantId())
                .description("Reversal for " + transactionReference + ": " + reason)
                .customerId(originalTransaction.getCustomerId())
                .customerEmail(originalTransaction.getCustomerEmail())
                .originalTransactionId(originalTransaction.getId())
                .build();

        Transaction savedReversal = transactionRepository.save(reversalTransaction);
        transactionLogService.log(savedReversal, "Created reversal transaction for " + transactionReference);
        
        // Process reversal
        processReversal(savedReversal, originalTransaction, reason);
        
        // Update original transaction
        originalTransaction.setStatus(TransactionStatus.REVERSED);
        originalTransaction.setReversalReference(savedReversal.getTransactionReference());
        originalTransaction.setReversedAt(LocalDateTime.now());
        transactionRepository.save(originalTransaction);
        
        transactionLogService.log(originalTransaction, "Transaction reversed: " + reason);
        kafkaProducerService.publishTransactionReversed(originalTransaction, reason);
        
        return transactionMapper.toResponse(savedReversal);
    }

    /**
     * Process refund (full or partial)
     */
    @Transactional
    public TransactionResponse processRefund(String originalTransactionReference, BigDecimal amount, String reason) {
        log.info("Processing refund for transaction: {} amount: {} reason: {}", 
                originalTransactionReference, amount, reason);
        
        Transaction originalTransaction = transactionRepository.findByTransactionReference(originalTransactionReference)
                .orElseThrow(() -> new TransactionException("Original transaction not found: " + originalTransactionReference));

        // Validate tenant access
        if (!originalTransaction.getTenantId().equals(requestContext.getTenantId())) {
            throw new TransactionException("Access denied to transaction");
        }

        // Validate refund eligibility
        validateRefundEligibility(originalTransaction, amount);

        // Check if refund amount is valid 
        if (amount.compareTo(originalTransaction.getAmount()) > 0) {
            throw new TransactionException("Refund amount exceeds original transaction amount");
        }
        
        // Check if already refunded 
        if (originalTransaction.getRefundedAmount() != null && 
            originalTransaction.getRefundedAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remaining = originalTransaction.getAmount().subtract(originalTransaction.getRefundedAmount());
            if (amount.compareTo(remaining) > 0) {
                throw new TransactionException("Refund amount exceeds remaining balance: " + remaining);
            }
        }
        
        // Create refund transaction
        Transaction refundTransaction = Transaction.builder()
                .transactionReference(idGenerator.generateRefundReference(originalTransactionReference))
                .merchantTransactionId(idGenerator.generateMerchantTransactionId())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.PENDING)
                .sourceWalletId(originalTransaction.getDestinationWalletId())
                .destinationWalletId(originalTransaction.getSourceWalletId())
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .totalAmount(amount)
                .currency(originalTransaction.getCurrency())
                .paymentMethod(originalTransaction.getPaymentMethod())
                .tenantId(requestContext.getTenantId())
                .description("Refund for " + originalTransactionReference + ": " + reason)
                .customerId(originalTransaction.getCustomerId())
                .customerEmail(originalTransaction.getCustomerEmail())
                .customerPhone(originalTransaction.getCustomerPhone())
                .originalTransactionId(originalTransaction.getId())
                .build();

        Transaction savedRefund = transactionRepository.save(refundTransaction);
        transactionLogService.log(savedRefund, "Created refund transaction for " + originalTransactionReference);

        // Process refund
        processRefundTransaction(savedRefund, originalTransaction);

        // Update original transaction's refunded amount
        BigDecimal newRefundedAmount = originalTransaction.getRefundedAmount() != null ? 
                originalTransaction.getRefundedAmount().add(amount) : amount;
        originalTransaction.setRefundedAmount(newRefundedAmount);
        
        if (newRefundedAmount.compareTo(originalTransaction.getAmount()) >= 0) {
            originalTransaction.setStatus(TransactionStatus.REFUNDED);
        } else {
            originalTransaction.setStatus(TransactionStatus.PARTIALLY_REFUNDED);
        }
        
        transactionRepository.save(originalTransaction);
        
        transactionLogService.logRefundProcessed(originalTransaction, savedRefund, reason);
        kafkaProducerService.publishRefundProcessed(originalTransaction, savedRefund, reason);
        
        return transactionMapper.toResponse(savedRefund);
    }

    /**
     * Partial refund (convenience method)
     */
    @Transactional
    public TransactionResponse partialRefund(String originalTransactionReference, 
                                             BigDecimal amount, 
                                             String reason) {
        return processRefund(originalTransactionReference, amount, reason);
    }

    /**
     * Capture an authorized transaction
     */
    @Transactional
    public TransactionResponse captureTransaction(String authorizationReference, BigDecimal amount) {
        log.info("Capturing transaction: {} amount: {}", authorizationReference, amount);

        Transaction authorizedTransaction = transactionRepository.findByTransactionReference(authorizationReference)
                .orElseThrow(() -> new TransactionException("Authorization not found: " + authorizationReference));

        // Validate tenant access
        if (!authorizedTransaction.getTenantId().equals(requestContext.getTenantId())) {
            throw new TransactionException("Access denied to transaction");
        }

        // Validate that transaction is in authorized state
        if (authorizedTransaction.getStatus() != TransactionStatus.AUTHORIZED) {
            throw new TransactionException("Transaction not in AUTHORIZED state. Current status: " + 
                    authorizedTransaction.getStatus());
        }

        // Validate capture amount
        if (amount.compareTo(authorizedTransaction.getAmount()) > 0) {
            throw new TransactionException("Capture amount cannot exceed authorized amount");
        }

        // Process capture through payment processor
        PaymentProcessor processor = paymentProcessorService.getProcessor(authorizedTransaction.getProcessorName());
        ProcessorResponse captureResponse = processor.capturePayment(authorizedTransaction.getProcessorTransactionId(), amount);

        if (!captureResponse.isSuccessful()) {
            throw new TransactionException("Capture failed: " + captureResponse.getMessage());
        }

        // Update transaction
        authorizedTransaction.setStatus(TransactionStatus.CAPTURED);
        authorizedTransaction.setCapturedAmount(amount);
        authorizedTransaction.setCapturedAt(LocalDateTime.now());
        
        if (amount.compareTo(authorizedTransaction.getAmount()) < 0) {
            authorizedTransaction.setStatus(TransactionStatus.PARTIALLY_CAPTURED);
        }

        transactionRepository.save(authorizedTransaction);
        transactionLogService.log(authorizedTransaction, "Transaction captured: " + amount);
        kafkaProducerService.publishPaymentCaptured(authorizedTransaction);

        return transactionMapper.toResponse(authorizedTransaction);
    }

    /**
     * Void an authorized transaction before capture
     */
    @Transactional
    public TransactionResponse voidTransaction(String authorizationReference, String reason) {
        log.info("Voiding transaction: {} reason: {}", authorizationReference, reason);

        Transaction authorizedTransaction = transactionRepository.findByTransactionReference(authorizationReference)
                .orElseThrow(() -> new TransactionException("Authorization not found: " + authorizationReference));

        // Validate tenant access
        if (!authorizedTransaction.getTenantId().equals(requestContext.getTenantId())) {
            throw new TransactionException("Access denied to transaction");
        }

        // Validate that transaction can be voided
        if (authorizedTransaction.getStatus() != TransactionStatus.AUTHORIZED) {
            throw new TransactionException("Cannot void transaction. Current status: " + 
                    authorizedTransaction.getStatus());
        }

        // Process void through payment processor
        PaymentProcessor processor = paymentProcessorService.getProcessor(authorizedTransaction.getProcessorName());
        ProcessorResponse voidResponse = processor.voidPayment(authorizedTransaction.getProcessorTransactionId());

        if (!voidResponse.isSuccessful()) {
            throw new TransactionException("Void failed: " + voidResponse.getMessage());
        }

        // Update transaction
        authorizedTransaction.setStatus(TransactionStatus.VOIDED);
        authorizedTransaction.setVoidedAt(LocalDateTime.now());
        authorizedTransaction.setVoidReason(reason);

        transactionRepository.save(authorizedTransaction);
        transactionLogService.log(authorizedTransaction, "Transaction voided: " + reason);
        kafkaProducerService.publishTransactionReversed(authorizedTransaction, reason);

        return transactionMapper.toResponse(authorizedTransaction);
    }

    /**
     * Retry a failed transaction
     */
    @Transactional
    public TransactionResponse retryFailedTransaction(String transactionReference, String reason) {
        log.info("Retrying failed transaction: {} reason: {}", transactionReference, reason);

        Transaction failedTransaction = transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new TransactionException("Transaction not found: " + transactionReference));

        // Validate tenant access
        if (!failedTransaction.getTenantId().equals(requestContext.getTenantId())) {
            throw new TransactionException("Access denied to transaction");
        }

        // Validate that transaction is in failed state
        if (failedTransaction.getStatus() != TransactionStatus.FAILED) {
            throw new TransactionException("Only failed transactions can be retried. Current status: " + 
                    failedTransaction.getStatus());
        }

        // Check retry count
        int retryCount = failedTransaction.getRetryCount() != null ? failedTransaction.getRetryCount() : 0;
        if (retryCount >= 3) {
            throw new TransactionException("Maximum retry attempts (3) reached for this transaction");
        }

        // Create retry transaction
        Transaction retryTransaction = Transaction.builder()
                .transactionReference(idGenerator.generateRetryReference(transactionReference))
                .merchantTransactionId(idGenerator.generateMerchantTransactionId())
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PENDING)
                .sourceWalletId(failedTransaction.getSourceWalletId())
                .destinationWalletId(failedTransaction.getDestinationWalletId())
                .amount(failedTransaction.getAmount())
                .fee(failedTransaction.getFee())
                .totalAmount(failedTransaction.getTotalAmount())
                .currency(failedTransaction.getCurrency())
                .paymentMethod(failedTransaction.getPaymentMethod())
                .description("Retry for " + transactionReference + ": " + reason)
                .customerId(failedTransaction.getCustomerId())
                .customerEmail(failedTransaction.getCustomerEmail())
                .customerPhone(failedTransaction.getCustomerPhone())
                .tenantId(failedTransaction.getTenantId())
                .originalTransactionId(failedTransaction.getId())
                .retryCount(retryCount + 1)
                .build();

        Transaction savedRetry = transactionRepository.save(retryTransaction);
        
        // Update original transaction
        failedTransaction.setRetryReference(savedRetry.getTransactionReference());
        failedTransaction.setRetryCount(retryCount + 1);
        transactionRepository.save(failedTransaction);
        
        transactionLogService.log(savedRetry, "Retry initiated for: " + transactionReference);
        
        // Process the retry
        processPaymentAsync(savedRetry.getId());
        
        return transactionMapper.toResponse(savedRetry);
    }

    // =================== PRIVATE HELPER METHODS ===================

    /**
     * Validate reversal eligibility
     */
    private void validateReversalEligibility(Transaction transaction) {
        if (transaction.getStatus() == TransactionStatus.REVERSED) {
            throw new TransactionException("Transaction has already been reversed");
        }
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new TransactionException("Only completed transactions can be reversed. Current status: " + 
                    transaction.getStatus());
        }
        // Check reversal window (e.g., 24 hours)
        LocalDateTime reversalDeadline = transaction.getSettledAt().plusHours(24);
        if (LocalDateTime.now().isAfter(reversalDeadline)) {
            throw new TransactionException("Reversal window has expired (24 hours from settlement)");
        }
    }

    /**
     * Validate refund eligibility
     */
    private void validateRefundEligibility(Transaction transaction, BigDecimal amount) {
        if (transaction.getStatus() == TransactionStatus.REFUNDED) {
            throw new TransactionException("Transaction already fully refunded");
        }
        
        if (transaction.getStatus() != TransactionStatus.COMPLETED && 
            transaction.getStatus() != TransactionStatus.PARTIALLY_REFUNDED) {
            throw new TransactionException("Only completed transactions can be refunded");
        }
        
        // Check refund window (e.g., within 30 days)
        LocalDateTime refundDeadline = transaction.getTransactionDate().plusDays(30);
        if (LocalDateTime.now().isAfter(refundDeadline)) {
            throw new TransactionException("Refund window has expired (30 days from transaction date)");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Refund amount must be greater than zero");
        }
    }

    /**
     * Process reversal transaction
     */
    private void processReversal(Transaction reversal, Transaction original, String reason) {
        try {
            reversal.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(reversal);
            
            // Credit source wallet (return funds)
            walletServiceClient.creditWallet(
                    reversal.getSourceWalletId(),
                    reversal.getAmount(),
                    reversal.getTransactionReference()
            );
            
            // If original had fees, reverse them
            if (original.getFee() != null && original.getFee().compareTo(BigDecimal.ZERO) > 0) {
                walletServiceClient.creditWallet(
                        original.getSourceWalletId(),
                        original.getFee(),
                        reversal.getTransactionReference() + "-FEE"
                );
            }
            
            reversal.setStatus(TransactionStatus.COMPLETED);
            reversal.setSettledAt(LocalDateTime.now());
            transactionRepository.save(reversal);
            
            transactionLogService.log(reversal, "Reversal completed successfully");
            
        } catch (Exception e) {
            reversal.setStatus(TransactionStatus.FAILED);
            reversal.setProcessorResponseMessage(e.getMessage());
            transactionRepository.save(reversal);
            transactionLogService.log(reversal, "Reversal failed: " + e.getMessage());
            throw new TransactionException("Reversal processing failed", e);
        }
    }

    /**
     * Process refund transaction
     */
    private void processRefundTransaction(Transaction refund, Transaction original) {
        try {
            refund.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(refund);
            
            // Process refund through payment processor
            PaymentProcessor processor = paymentProcessorService.getProcessor(original.getProcessorName());
            ProcessorResponse refundResponse = processor.processRefund(original.getProcessorTransactionId(), refund.getAmount());
            
            if (!refundResponse.isSuccessful()) {
                throw new TransactionException("Refund failed: " + refundResponse.getMessage());
            }
            
            // Credit source wallet
            walletServiceClient.creditWallet(
                    refund.getDestinationWalletId(),
                    refund.getAmount(),
                    refund.getTransactionReference()
            );
            
            refund.setStatus(TransactionStatus.COMPLETED);
            refund.setProcessorTransactionId(refundResponse.getTransactionId());
            refund.setSettledAt(LocalDateTime.now());
            transactionRepository.save(refund);
            
            transactionLogService.log(refund, "Refund completed successfully");
            
        } catch (Exception e) {
            refund.setStatus(TransactionStatus.FAILED);
            refund.setProcessorResponseMessage(e.getMessage());
            transactionRepository.save(refund);
            transactionLogService.log(refund, "Refund failed: " + e.getMessage());
            throw new TransactionException("Refund processing failed", e);
        }
    }

    /**
     * Handle failed transaction
     */
    private void handleFailedTransaction(Transaction transaction, String errorMessage) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setProcessorResponseMessage(errorMessage);
        transaction.setFailedAt(LocalDateTime.now());
        
        transactionLogService.logTransactionFailed(transaction, errorMessage, null);
        kafkaProducerService.publishTransactionFailed(transaction);
    }

    /**
     * Build transaction from request
     */
    private Transaction buildTransaction(PaymentRequest request) {
        BigDecimal fee = calculateFee(request.getAmount(), request.getPaymentMethod());
        return Transaction.builder()
                .transactionReference(idGenerator.generateTransactionReference())
                .merchantTransactionId(request.getMerchantTransactionId())
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PENDING)
                .sourceWalletId(request.getSourceWalletId())
                .destinationWalletId(request.getDestinationWalletId())
                .amount(request.getAmount())
                .fee(fee)
                .totalAmount(request.getAmount().add(fee))
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
                .createdBy(requestContext.getUserId() != null ? requestContext.getUserId() : "SYSTEM")
                .transactionDate(LocalDateTime.now())
                .build();
    }

    /**
     * Calculate transaction fee
     */
    private BigDecimal calculateFee(BigDecimal amount, String paymentMethod) {
        BigDecimal feePercentage = switch (paymentMethod.toUpperCase()) {
            case "CARD" -> new BigDecimal("0.015");
            case "BANK_TRANSFER" -> new BigDecimal("0.005");
            case "WALLET" -> new BigDecimal("0.001");
            case "QR_CODE" -> new BigDecimal("0.002");
            case "USSD" -> new BigDecimal("0.003");
            default -> new BigDecimal("0.02");
        };
        BigDecimal fee = amount.multiply(feePercentage);
        BigDecimal minimumFee = new BigDecimal("50");
        return fee.compareTo(minimumFee) < 0 ? minimumFee : fee.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Validate customer and wallet
     */
    private void validateCustomerAndWallet(PaymentRequest request) {
        try {
            var walletValidation = walletServiceClient.validateWallet(request.getSourceWalletId());
            if (!walletValidation.isValid()) {
                throw new TransactionException("Invalid source wallet: " + walletValidation.getMessage());
            }
            
            var customerValidation = identityServiceClient.validateCustomer(
                    walletValidation.getCustomerId(),
                    requestContext.getTenantId()
            );
            
            if (!customerValidation.isValid()) {
                throw new TransactionException("Customer not eligible: " + customerValidation.getMessage());
            }
        } catch (Exception e) {
            throw new TransactionException("Validation failed: " + e.getMessage());
        }
    }
}