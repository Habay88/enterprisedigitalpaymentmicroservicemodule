package com.edpp.ledger.enums;

/**
 * Account Type Enum - Classification of GL Accounts
 * 
 * The five main account types in double-entry accounting:
 * - ASSET: Resources owned (Cash, Accounts Receivable)
 * - LIABILITY: Obligations owed (Customer Deposits, Loans)
 * - EQUITY: Owner's interest (Retained Earnings)
 * - REVENUE: Income earned (Fees, Interest)
 * - EXPENSE: Costs incurred (Processing Fees, Salaries)
 */
public enum AccountType {
    ASSET("Asset", "DEBIT"),
    LIABILITY("Liability", "CREDIT"),
    EQUITY("Equity", "CREDIT"),
    REVENUE("Revenue", "CREDIT"),
    EXPENSE("Expense", "DEBIT");

    private final String displayName;
    private final String normalBalance;

    AccountType(String displayName, String normalBalance) {
        this.displayName = displayName;
        this.normalBalance = normalBalance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNormalBalance() {
        return normalBalance;
    }
}