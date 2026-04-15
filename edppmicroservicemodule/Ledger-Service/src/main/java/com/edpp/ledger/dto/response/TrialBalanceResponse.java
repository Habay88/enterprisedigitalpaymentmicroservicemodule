package com.edpp.ledger.dto.response;

import com.edpp.ledger.enums.AccountType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Trial Balance Response - DTO for trial balance report
 */
public record TrialBalanceResponse(
    String asOfDate,
    List<TrialBalanceLine> lines,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    boolean balanced
) {
    /**
     * Get the difference between debits and credits
     */
    public BigDecimal getDifference() {
        if (totalDebit == null || totalCredit == null) {
            return BigDecimal.ZERO;
        }
        return totalDebit.subtract(totalCredit).abs();
    }
}

/**
 * Trial Balance Line - Individual line in trial balance
 */
public record TrialBalanceLine(
    String accountCode,
    String accountName,
    AccountType accountType,
    BigDecimal debitBalance,
    BigDecimal creditBalance
) {
    /**
     * Get the net balance (debit - credit)
     */
    public BigDecimal getNetBalance() {
        return debitBalance.subtract(creditBalance);
    }
    
    /**
     * Get the balance type (Debit or Credit)
     */
    public String getBalanceType() {
        if (debitBalance.compareTo(BigDecimal.ZERO) > 0) {
            return "DEBIT";
        } else if (creditBalance.compareTo(BigDecimal.ZERO) > 0) {
            return "CREDIT";
        }
        return "ZERO";
    }
}