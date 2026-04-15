package com.edpp.ledger.dto.response;

import com.edpp.ledger.enums.AccountType;
import java.math.BigDecimal;

/**
 * GL Account Response - DTO for returning GL account data
 */
public record GLAccountResponse(
    String id,
    String accountCode,
    String accountName,
    AccountType accountType,
    String normalBalance,
    BigDecimal balance,
    String parentAccountCode,
    boolean active,
    String description
) {
    /**
     * Check if this is an asset account
     */
    public boolean isAsset() {
        return accountType == AccountType.ASSET;
    }
    
    /**
     * Check if this is a liability account
     */
    public boolean isLiability() {
        return accountType == AccountType.LIABILITY;
    }
    
    /**
     * Check if this is an equity account
     */
    public boolean isEquity() {
        return accountType == AccountType.EQUITY;
    }
    
    /**
     * Check if this is a revenue account
     */
    public boolean isRevenue() {
        return accountType == AccountType.REVENUE;
    }
    
    /**
     * Check if this is an expense account
     */
    public boolean isExpense() {
        return accountType == AccountType.EXPENSE;
    }
    
    /**
     * Get the formatted balance with normal balance indicator
     */
    public String getFormattedBalance() {
        if (balance == null) return "0.00";
        return String.format("%s %s", normalBalance.equals("DEBIT") ? "Dr" : "Cr", balance);
    }
}