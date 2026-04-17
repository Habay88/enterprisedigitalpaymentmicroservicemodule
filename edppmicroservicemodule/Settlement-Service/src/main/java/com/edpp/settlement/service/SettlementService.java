package com.edpp.settlement.service;

import com.edpp.settlement.dto.request.CreateBatchRequest;
import com.edpp.settlement.dto.response.SettlementResponse;
import com.edpp.settlement.entity.*;
import com.edpp.settlement.enums.SettlementStatus;
import com.edpp.settlement.mapper.SettlementMapper;
import com.edpp.settlement.repository.SettlementBatchRepository;
import com.edpp.settlement.repository.SettlementRepository;
import com.edpp.settlement.repository.SettlementTransactionRepository;
import com.edpp.settlement.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementBatchRepository batchRepository;
    private final SettlementTransactionRepository transactionRepository;
    private final SettlementCalculationService calculationService;
    private final FeeCalculationService feeService;
    private final BankTransferService bankTransferService;
    private final SettlementMapper mapper;
    private final RequestContext requestContext;

    /**
     * Create settlement batch for a specific date
     */
    @Transactional
    public SettlementBatch createBatch(CreateBatchRequest request) {
        String tenantId = requestContext.getTenantId();
        log.info("Creating settlement batch for date: {} in tenant: {}", request.batchDate(), tenantId);

        String batchReference = generateBatchReference(request.batchDate());
        
        SettlementBatch batch = SettlementBatch.builder()
                .batchReference(batchReference)
                .batchDate(request.batchDate())
                .frequency(request.frequency())
                .status(BatchStatus.CREATED)
                .tenantId(tenantId)
                .build();

        return batchRepository.save(batch);
    }

    /**
     * Process settlements for a batch
     */
    @Transactional
    public void processBatch(String batchId) {
        log.info("Processing settlement batch: {}", batchId);

        SettlementBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        batch.setStatus(BatchStatus.PROCESSING);
        batch.setProcessedAt(LocalDateTime.now());
        batch.setProcessedBy(requestContext.getUserId());
        batchRepository.save(batch);

        // Get all merchants with transactions in this batch period
        List<String> merchantIds = getMerchantsWithTransactions(batch.getBatchDate());
        
        int totalMerchants = 0;
        int totalTransactions = 0;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (String merchantId : merchantIds) {
            try {
                Settlement settlement = createSettlementForMerchant(merchantId, batch);
                totalMerchants++;
                totalTransactions += settlement.getTransactionCount();
                totalGross = totalGross.add(settlement.getGrossAmount());
                totalNet = totalNet.add(settlement.getNetAmount());
            } catch (Exception e) {
                log.error("Failed to create settlement for merchant: {}", merchantId, e);
            }
        }

        batch.setTotalMerchants(totalMerchants);
        batch.setTotalTransactions(totalTransactions);
        batch.setTotalGrossAmount(totalGross);
        batch.setTotalNetAmount(totalNet);
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setCompletedAt(LocalDateTime.now());
        batchRepository.save(batch);

        log.info("Batch processing completed: {} merchants, {} transactions, Net: {}", 
                totalMerchants, totalTransactions, totalNet);
    }

    /**
     * Create settlement for a specific merchant
     */
    @Transactional
    public Settlement createSettlementForMerchant(String merchantId, SettlementBatch batch) {
        String tenantId = requestContext.getTenantId();
        log.info("Creating settlement for merchant: {} in batch: {}", merchantId, batch.getBatchReference());

        // Get merchant details (from Merchant Service via Feign)
        MerchantDetails merchant = getMerchantDetails(merchantId, tenantId);
        
        // Get merchant settlement configuration
        MerchantSettlementConfig config = getMerchantConfig(merchantId, tenantId);
        
        // Get all transactions for this merchant in the batch period
        List<TransactionRecord> transactions = getMerchantTransactions(merchantId, batch.getBatchDate());
        
        if (transactions.isEmpty()) {
            log.info("No transactions found for merchant: {} in batch: {}", merchantId, batch.getBatchReference());
            return null;
        }

        // Calculate settlement amounts
        BigDecimal grossAmount = calculationService.calculateGrossAmount(transactions);
        BigDecimal fees = feeService.calculateTotalFees(transactions, config);
        BigDecimal netAmount = grossAmount.subtract(fees);
        
        // Apply reserve if configured
        if (config.getReservePercentage() != null && config.getReservePercentage().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal reserve = grossAmount.multiply(config.getReservePercentage())
                    .divide(BigDecimal.valueOf(100));
            netAmount = netAmount.subtract(reserve);
        }

        // Create settlement record
        String settlementReference = generateSettlementReference(merchantId, batch.getBatchDate());
        
        Settlement settlement = Settlement.builder()
                .settlementReference(settlementReference)
                .batchId(batch.getId())
                .merchantId(merchantId)
                .merchantName(merchant.name())
                .merchantEmail(merchant.email())
                .settlementDate(batch.getBatchDate())
                .cutoffDate(batch.getBatchDate())
                .status(SettlementStatus.PENDING)
                .grossAmount(grossAmount)
                .totalFees(fees)
                .netAmount(netAmount)
                .transactionCount(transactions.size())
                .bankAccountNumber(merchant.bankAccountNumber())
                .bankAccountName(merchant.bankAccountName())
                .bankCode(merchant.bankCode())
                .bankName(merchant.bankName())
                .tenantId(tenantId)
                .build();

        Settlement savedSettlement = settlementRepository.save(settlement);

        // Link transactions to settlement
        for (TransactionRecord transaction : transactions) {
            SettlementTransaction settlementTransaction = SettlementTransaction.builder()
                    .settlementId(savedSettlement.getId())
                    .transactionId(transaction.id())
                    .transactionReference(transaction.reference())
                    .merchantId(merchantId)
                    .transactionDate(transaction.createdAt())
                    .transactionAmount(transaction.amount())
                    .fee(transaction.fee())
                    .settlementAmount(transaction.amount().subtract(transaction.fee()))
                    .currency(transaction.currency())
                    .paymentMethod(transaction.paymentMethod())
                    .build();
            transactionRepository.save(settlementTransaction);
        }

        // Initiate bank transfer if auto-settlement is enabled
        if (config.isAutoSettlement() && netAmount.compareTo(BigDecimal.ZERO) > 0) {
            bankTransferService.initiateTransfer(savedSettlement);
        }

        log.info("Settlement created: {} - Gross: {}, Net: {}", settlementReference, grossAmount, netAmount);
        return savedSettlement;
    }

    /**
     * Get settlement by reference
     */
    public SettlementResponse getSettlementByReference(String reference) {
        Settlement settlement = settlementRepository.findBySettlementReference(reference)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + reference));
        return mapper.toResponse(settlement);
    }

    /**
     * Get settlements for a merchant
     */
    public List<SettlementResponse> getMerchantSettlements(String merchantId) {
        String tenantId = requestContext.getTenantId();
        return settlementRepository.findByMerchantIdAndTenantId(merchantId, tenantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private String generateBatchReference(LocalDate date) {
        return "BATCH_" + date.toString().replace("-", "") + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateSettlementReference(String merchantId, LocalDate date) {
        return "STL_" + merchantId + "_" + date.toString().replace("-", "") + "_" + System.currentTimeMillis();
    }

    private List<String> getMerchantsWithTransactions(LocalDate date) {
        // Implementation to get unique merchants with transactions on that date
        // This would query the Transaction Service via Feign
        return List.of();
    }

    private MerchantDetails getMerchantDetails(String merchantId, String tenantId) {
        // Implementation to fetch merchant details from Merchant Service
        return new MerchantDetails(merchantId, "Merchant Name", "merchant@email.com",
                "1234567890", "Account Name", "BANK001", "Bank Name");
    }

    private MerchantSettlementConfig getMerchantConfig(String merchantId, String tenantId) {
        // Implementation to fetch merchant settlement config
        return MerchantSettlementConfig.builder()
                .frequency(SettlementFrequency.DAILY)
                .mdrRate(new BigDecimal("1.5"))
                .fixedFeePerTransaction(new BigDecimal("100"))
                .autoSettlement(true)
                .build();
    }

    private List<TransactionRecord> getMerchantTransactions(String merchantId, LocalDate date) {
        // Implementation to fetch transactions from Transaction Service
        return List.of();
    }

    private record MerchantDetails(String id, String name, String email, String bankAccountNumber,
                                   String bankAccountName, String bankCode, String bankName) {}
    
    private record TransactionRecord(String id, String reference, BigDecimal amount, BigDecimal fee,
                                     String currency, String paymentMethod, LocalDateTime createdAt) {}
}