package com.edpp.wallet.dtorequest;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DebitRequest(

  @NotBlank(message = "Wallet number is required")
    String walletNumber,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "Reference is required")
    String reference,

    String description,

    String relatedTransactionId
) {

}
