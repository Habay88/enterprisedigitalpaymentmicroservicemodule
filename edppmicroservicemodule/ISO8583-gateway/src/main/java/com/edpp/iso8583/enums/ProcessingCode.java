package com.edpp.iso8583.enums;

/**
 * Processing Code - Identifies the transaction type
 *
 * Format: XXYZZZ
 * XX = Transaction type (00=Purchase, 01=Withdrawal, 20=Refund)
 * Y = Account type (0=Default, 1=Savings, 2=Current)
 * ZZZ = Additional processing options
 */
public enum ProcessingCode {
    PURCHASE("000000", "Goods purchase"),
    CASH_WITHDRAWAL("010000", "ATM withdrawal"),
    REFUND("200000", "Refund"),
    PRE_AUTHORIZATION("020000", "Pre-authorization");

    private final String code;
    private final String description;

    ProcessingCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ProcessingCode fromCode(String code) {
        for (ProcessingCode pc : values()) {
            if (pc.code.equals(code)) {
                return pc;
            }
        }
        return PURCHASE; // Default
    }
}