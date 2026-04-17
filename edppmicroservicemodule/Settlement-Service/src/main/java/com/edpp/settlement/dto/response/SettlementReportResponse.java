package com.edpp.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SettlementReportResponse(
    String merchantId,
    String merchantName,
    LocalDate periodStart,
    LocalDate periodEnd,
    Integer totalTransactions,
    BigDecimal totalGrossAmount,
    BigDecimal totalFees,
    BigDecimal totalNetAmount,
    List<SettlementSummary> settlements
) {}
