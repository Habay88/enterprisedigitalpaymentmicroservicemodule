package com.edpp.ledger.dto.request;

import com.edpp.ledger.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Create GL Account Request - DTO for creating a new GL account
 */
public record CreateGLAccountRequest(
    
    @NotBlank(message = "Account code is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "Account code must be 4 digits")
    String accountCode,

    @NotBlank(message = "Account name is required")
    String accountName,

    @NotNull(message = "Account type is required")
    AccountType accountType,

    String parentAccountCode,

    String description
) {
    /**
     * Validate that parent account code is different from account code
     */
    public boolean isValidParent() {
        if (parentAccountCode == null) return true;
        return !parentAccountCode.equals(accountCode);
    }
}