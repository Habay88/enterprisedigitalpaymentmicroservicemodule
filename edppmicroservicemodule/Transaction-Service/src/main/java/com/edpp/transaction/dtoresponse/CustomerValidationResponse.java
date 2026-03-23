package com.edpp.transaction.dtoresponse;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerValidationResponse {
    private boolean valid;
    private String customerId;
    private String status;
    private String message;
    private String riskRating;
    private boolean kycCompleted;
    private BigDecimal dailyLimitRemaining;
    private BigDecimal monthlyLimitRemaining;
}


