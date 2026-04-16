package com.edpp.ledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * Journal Line Request - DTO for individual debit/credit line
 * 
 * Each journal line represents either a DEBIT or CREDIT to a specific GL account.
 * The direction must be either "DEBIT" or "CREDIT".
 */
public record JournalLineRequest(
    
    @NotBlank(message = "Account ID is required")
    String accountId,
    
    @NotBlank(message = "Direction is required")
    @Pattern(regexp = "^(DEBIT|CREDIT)$", message = "Direction must be either DEBIT or CREDIT")
    String direction,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
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
     * Validate that amount is positive
     */
    public boolean isValidAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}