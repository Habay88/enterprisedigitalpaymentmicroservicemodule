package com.edpp.iso8583.enums;

/**
 * ISO 8583 Response Codes
 *
 * Common codes:
 * 00 = Approved
 * 05 = Do not honor
 * 12 = Invalid transaction
 * 14 = Invalid card number
 * 51 = Insufficient funds
 * 54 = Expired card
 * 55 = Invalid PIN
 * 91 = Issuer unavailable
 * 96 = System malfunction
 */
public enum ResponseCode {
    APPROVED("00", "Approved"),
    DO_NOT_HONOR("05", "Do not honor"),
    INVALID_TRANSACTION("12", "Invalid transaction"),
    INVALID_CARD("14", "Invalid card number"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    EXPIRED_CARD("54", "Expired card"),
    INVALID_PIN("55", "Invalid PIN"),
    ISSUER_UNAVAILABLE("91", "Issuer unavailable"),
    SYSTEM_MALFUNCTION("96", "System malfunction");

    private final String code;
    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}