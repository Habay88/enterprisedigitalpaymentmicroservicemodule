package com.edpp.transaction.dtoresponse;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLimitResponse {
    private String customerId;
    private BigDecimal dailyLimit;
    private BigDecimal dailyLimitRemaining;
    private BigDecimal monthlyLimit;
    private BigDecimal monthlyLimitRemaining;
    private BigDecimal perTransactionLimit;
}