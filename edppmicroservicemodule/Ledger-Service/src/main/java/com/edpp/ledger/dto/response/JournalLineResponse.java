package com.edpp.ledger.dto.response;

import java.math.BigDecimal;

/**
 * Journal Line Response - DTO for returning journal line data
 * 
 * This record represents a single line in a journal entry,
 * showing the account affected and the amount.
 */
public record JournalLineResponse(
    String id,
    String accountCode,
    String accountName,
    String direction,
    BigDecimal amount,
    String description
) {
    /**
     * Check if this is a debit line
     */
    public boolean isDebit() {
        return "DEBIT".equalsIgnoreCase(direction);
    }
    
    /**
     * Check if this is a credit line
     */
    public boolean isCredit() {
        return "CREDIT".equalsIgnoreCase(direction);
    }
    
    /**
     * Get formatted amount with direction
     */
    public String getFormattedAmount() {
        return String.format("%s %s", direction, amount);
    }
}