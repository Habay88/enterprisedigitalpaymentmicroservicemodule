package com.edpp.iso8583.dto;

import com.edpp.iso8583.enums.ProcessingCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Authorization Request DTO - Internal representation of ISO 8583 authorization
 */
public record AuthorizationRequest(
        String pan,                    // Primary Account Number (masked in logs)
        ProcessingCode processingCode, // Transaction type (purchase, withdrawal, etc.)
        BigDecimal amount,             // Transaction amount
        String stan,                   // System Trace Audit Number
        LocalDateTime transactionTime, // Transaction date/time
        String posEntryMode,           // POS entry mode (swipe, chip, contactless)
        String terminalId,             // Terminal identifier
        String merchantId,             // Merchant identifier
        String pinData,                // Encrypted PIN block (if present)
        String emvData                 // EMV chip data (if present)
) {
    public String getMaskedPan() {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}