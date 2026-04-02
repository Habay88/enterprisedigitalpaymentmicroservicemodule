package com.edpp.wallet.dtoresponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatementEntry(
    String transactionId,
    String reference,
    String type,
    BigDecimal amount,
    BigDecimal balanceAfter,
    String description,
    LocalDateTime createdAt
) {

}
