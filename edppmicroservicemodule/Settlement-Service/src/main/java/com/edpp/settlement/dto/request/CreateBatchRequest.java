package com.edpp.settlement.dto.request;

import com.edpp.settlement.enums.SettlementFrequency;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateBatchRequest(
    @NotNull(message = "Batch date is required")
    LocalDate batchDate,

    @NotNull(message = "Frequency is required")
    SettlementFrequency frequency
) {}
