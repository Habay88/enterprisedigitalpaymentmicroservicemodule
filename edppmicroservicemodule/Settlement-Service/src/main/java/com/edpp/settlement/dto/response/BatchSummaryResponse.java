package com.edpp.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchSummaryResponse(
    String batchReference,
    LocalDate batchDate,
    String frequency,
    String status,
    Integer totalMerchants,
    Integer totalTransactions,
    Integer totalSettlements,
    BigDecimal totalGrossAmount,
    BigDecimal totalNetAmount
) {}