package com.edpp.settlement.service;

import com.edpp.settlement.entity.MerchantSettlementConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
public class FeeCalculationService {

    /**
     * Calculate total fees for a list of transactions
     */
    public BigDecimal calculateTotalFees(List<TransactionRecord> transactions, MerchantSettlementConfig config) {
        BigDecimal totalMdr = calculateMdrFees(transactions, config);
        BigDecimal totalFixedFees = calculateFixedFees(transactions, config);
        
        return totalMdr.add(totalFixedFees);
    }

    /**
     * Calculate MDR (Merchant Discount Rate) fees
     * MDR is a percentage of the transaction amount
     */
    private BigDecimal calculateMdrFees(List<TransactionRecord> transactions, MerchantSettlementConfig config) {
        BigDecimal mdrRate = config.getMdrRate();
        if (mdrRate == null) return BigDecimal.ZERO;
        
        BigDecimal rate = mdrRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        return transactions.stream()
                .map(t -> t.amount().multiply(rate))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate fixed per-transaction fees
     */
    private BigDecimal calculateFixedFees(List<TransactionRecord> transactions, MerchantSettlementConfig config) {
        BigDecimal fixedFee = config.getFixedFeePerTransaction();
        if (fixedFee == null) return BigDecimal.ZERO;
        
        return fixedFee.multiply(BigDecimal.valueOf(transactions.size()));
    }

    private record TransactionRecord(BigDecimal amount) {}
}