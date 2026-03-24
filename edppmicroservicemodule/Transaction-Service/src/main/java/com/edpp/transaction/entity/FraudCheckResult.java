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
    private String additionalAuthToken;
    private String challengeUrl;
    
    public static FraudCheckResult allow() {
        return FraudCheckResult.builder()
                .allowed(true)
                .riskScore(0)
                .flags(List.of())
                .reason("Transaction allowed")
                .requiresAdditionalAuth(false)
                .build();
    }
    
    public static FraudCheckResult block(String reason, int riskScore, List<String> flags) {
        return FraudCheckResult.builder()
                .allowed(false)
                .riskScore(riskScore)
                .flags(flags)
                .reason(reason)
                .requiresAdditionalAuth(false)
                .build();
    }
    
    public static FraudCheckResult requireAdditionalAuth(String reason, int riskScore, 
                                                         List<String> flags, String token) {
        return FraudCheckResult.builder()
                .allowed(true)
                .riskScore(riskScore)
                .flags(flags)
                .reason(reason)
                .requiresAdditionalAuth(true)
                .additionalAuthToken(token)
                .build();
    }
}
