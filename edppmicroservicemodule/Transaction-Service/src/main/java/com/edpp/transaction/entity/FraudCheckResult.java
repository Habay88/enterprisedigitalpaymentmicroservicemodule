package com.edpp.transaction.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResult {
    private boolean allowed;
    private int riskScore;
    private List<String> flags;
    private String reason;
    private boolean requiresAdditionalAuth;
}
