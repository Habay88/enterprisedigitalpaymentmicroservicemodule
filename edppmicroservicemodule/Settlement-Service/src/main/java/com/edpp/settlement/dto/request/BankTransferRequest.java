package com.edpp.settlement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BankTransferRequest(
    @NotBlank(message = "Settlement ID is required")
    String settlementId,

    @NotBlank(message = "Bank account number is required")
    String bankAccountNumber,

    @NotBlank(message = "Bank code is required")
    String bankCode,

    @NotBlank(message = "Bank name is required")
    String bankName,

    @NotNull(message = "Amount is required")
    BigDecimal amount
) {}