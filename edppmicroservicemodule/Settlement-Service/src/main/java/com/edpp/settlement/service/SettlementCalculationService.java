package com.edpp.settlement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class SettlementCalculationService {

    /**
     * Calculate gross amount from list of transactions
     */
    public BigDecimal calculateGrossAmount(List<TransactionRecord> transactions) {
        return transactions.stream()
                .map(TransactionRecord::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate net amount after fees
     */
    public BigDecimal calculateNetAmount(BigDecimal grossAmount, BigDecimal totalFees) {
        return grossAmount.subtract(totalFees);
    }

    private record TransactionRecord(BigDecimal amount) {}
}