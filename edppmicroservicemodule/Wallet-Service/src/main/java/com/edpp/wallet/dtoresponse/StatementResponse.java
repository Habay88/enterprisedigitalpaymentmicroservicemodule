package com.edpp.wallet.dtoresponse;

import java.math.BigDecimal;
import java.util.List;

public record StatementResponse(
     String walletNumber,
    String customerId,
    String period,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal totalCredit,
    BigDecimal totalDebit,
    List<StatementEntry> transactions
) {

}
