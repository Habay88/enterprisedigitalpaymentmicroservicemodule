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
public class WalletValidationResponse {
    private boolean valid;
    private String walletNumber;
    private String customerId;
    private String walletType;
    private String status;
    private BigDecimal balance;
    private String message;
}


