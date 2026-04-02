package com.edpp.wallet.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.edpp.wallet.enums.TransactionType;

public record WalletStatement(
    String transactionId,
    String reference,
    TransactionType type,
    BigDecimal amount,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    String description,
    LocalDateTime createdAt
) {

}
