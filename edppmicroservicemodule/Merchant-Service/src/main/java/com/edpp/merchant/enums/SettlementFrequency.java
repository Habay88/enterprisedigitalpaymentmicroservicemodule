package com.edpp.merchant.enums;

public enum SettlementFrequency {
    DAILY,      // Every day (T+1)
    WEEKLY,     // Weekly (every Monday)
    MONTHLY,    // Monthly (1st of month)
    THRESHOLD   // When balance reaches threshold
}