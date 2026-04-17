package com.edpp.merchant.enums;

public enum MerchantStatus {
    PENDING_VERIFICATION,  // Awaiting KYC/document verification
    ACTIVE,                // Fully active, can process payments
    SUSPENDED,             // Temporarily suspended
    BLOCKED,               // Permanently blocked
    CLOSED                 // Account closed
}