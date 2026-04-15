package com.edpp.ledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReconcileRequest(
    @NotBlank(message = "Bank account ID is required")
    String bankAccountId,

    @NotNull(message = "Statement date is required")
    LocalDate statementDate,

    @NotNull(message = "Statement balance is required")
    @DecimalMin(value = "0.00", message = "Statement balance must be positive")
    BigDecimal statementBalance
) {}