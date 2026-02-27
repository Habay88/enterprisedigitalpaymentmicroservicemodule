package com.edpp.identity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
// fraud check result model
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResult {
    private String transactionId;
    private LocalDateTime timestamp;
    private int riskScore;
    private List<String> flags;
    private boolean requiresAdditionalAuth;
    private boolean isAllowed;
}
