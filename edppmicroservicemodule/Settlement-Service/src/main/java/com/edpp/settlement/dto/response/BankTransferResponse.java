package com.edpp.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bank Transfer Response - DTO for bank transfer operations
 */
public record BankTransferResponse(
    String transferReference,
    String settlementId,
    BigDecimal amount,
    String status,
    String responseCode,
    String responseMessage,
    LocalDateTime initiatedAt
) {
    public boolean isSuccessful() {
        return "SUCCESS".equals(status) || "COMPLETED".equals(status);
    }
    
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}