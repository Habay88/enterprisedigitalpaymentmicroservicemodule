package com.edpp.settlement.enums;

public enum SettlementStatus {
    PENDING,        // Awaiting processing
    PROCESSING,     // Currently being processed
    COMPLETED,      // Successfully settled
    FAILED,         // Settlement failed
    REVERSED,       // Settlement reversed
    ON_HOLD         // Held for review
}