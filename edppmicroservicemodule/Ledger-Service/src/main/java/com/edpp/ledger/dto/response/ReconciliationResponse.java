package com.edpp.ledger.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reconciliation Response - DTO for bank reconciliation results
 */
public record ReconciliationResponse(
    String bankAccountId,
    LocalDate asOfDate,
    BigDecimal statementBalance,
    BigDecimal ledgerBalance,
    BigDecimal difference,
    boolean isReconciled,
    List<String> unmatchedTransactions
) {
    /**
     * Create a reconciled response (no difference)
     */
    public static ReconciliationResponse reconciled(String bankAccountId, LocalDate asOfDate,
                                                     BigDecimal statementBalance, BigDecimal ledgerBalance) {
        return new ReconciliationResponse(
            bankAccountId, asOfDate, statementBalance, ledgerBalance,
            BigDecimal.ZERO, true, List.of()
        );
    }
    
    /**
     * Create an unreconciled response with difference
     */
    public static ReconciliationResponse unreconciled(String bankAccountId, LocalDate asOfDate,
                                                       BigDecimal statementBalance, BigDecimal ledgerBalance,
                                                       List<String> unmatchedTransactions) {
        BigDecimal difference = ledgerBalance.subtract(statementBalance);
        return new ReconciliationResponse(
            bankAccountId, asOfDate, statementBalance, ledgerBalance,
            difference, false, unmatchedTransactions
        );
    }
}