package com.edpp.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public record SettlementResponse(
	    String id,
	    String settlementReference,
	    String batchId,
	    String merchantId,
	    String merchantName,
	    LocalDate settlementDate,
	    String status,
	    BigDecimal grossAmount,
	    BigDecimal totalFees,
	    BigDecimal netAmount,
	    Integer transactionCount,
	    String transferReference,
	    String transferStatus,
	    LocalDateTime createdAt
	) {}
