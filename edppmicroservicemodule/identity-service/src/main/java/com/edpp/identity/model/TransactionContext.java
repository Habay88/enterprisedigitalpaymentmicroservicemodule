package com.edpp.identity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Transaction Context for Fraud Check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionContext {
    private String transactionId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String ipAddress;
    private String deviceId;
    private String location;
    private String paymentMethod;
}
