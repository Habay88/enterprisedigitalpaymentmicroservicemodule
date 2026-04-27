package com.edpp.iso8583.enums;

/**
 * Message Type Indicator (MTI) - Identifies the message type
 *
 * Format: xyyz
 * x = Version (0=ISO 8583:1987, 1=ISO 8583:1993, 2=ISO 8583:2003)
 * yy = Message Class (10=Authorization, 20=Financial, 40=Reversal, 80=Network)
 * z = Message Function (0=Request, 1=Response, 2=Advice)
 */
public enum MtiType {
    AUTHORIZATION_REQUEST("0100"),
    AUTHORIZATION_RESPONSE("0110"),
    FINANCIAL_REQUEST("0200"),
    FINANCIAL_RESPONSE("0210"),
    REVERSAL_REQUEST("0400"),
    REVERSAL_RESPONSE("0410"),
    NETWORK_REQUEST("0800"),
    NETWORK_RESPONSE("0810");

    private final String code;

    MtiType(String code) {
        this.code = code;
    }

    public String getCode() { return code; }

    public static MtiType fromCode(String code) {
        for (MtiType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MTI: " + code);
    }
}