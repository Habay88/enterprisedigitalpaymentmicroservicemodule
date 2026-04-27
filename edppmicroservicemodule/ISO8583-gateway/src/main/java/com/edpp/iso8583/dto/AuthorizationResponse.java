package com.edpp.iso8583.dto;

/**
 * Authorization Response DTO - Response to authorization request
 */
public record AuthorizationResponse(
        String responseCode,    // 00 = Approved, 05 = Declined, etc.
        String responseMessage, // Human-readable message
        String authorizationCode, // 6-digit approval code
        String settlementDate   // Settlement date (T+1)
) {
    public static AuthorizationResponse approved(String authCode) {
        return new AuthorizationResponse("00", "Approved", authCode, null);
    }

    public static AuthorizationResponse declined(String reason) {
        return new AuthorizationResponse("05", reason, null, null);
    }

    public static AuthorizationResponse insufficientFunds() {
        return new AuthorizationResponse("51", "Insufficient funds", null, null);
    }
}