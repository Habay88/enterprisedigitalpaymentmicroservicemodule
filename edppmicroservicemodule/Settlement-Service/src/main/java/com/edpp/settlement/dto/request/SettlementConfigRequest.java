package com.edpp.settlement.dto.request;

import com.edpp.settlement.enums.SettlementFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SettlementConfigRequest(
    @NotBlank(message = "Merchant ID is required")
    String merchantId,

    @NotNull(message = "Frequency is required")
    SettlementFrequency frequency,

    BigDecimal mdrRate,
    BigDecimal fixedFeePerTransaction,
    BigDecimal minimumSettlementAmount,
    BigDecimal reservePercentage,
    boolean autoSettlement,
    String webhookUrl
) {}