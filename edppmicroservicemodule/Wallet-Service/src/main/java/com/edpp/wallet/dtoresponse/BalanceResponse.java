package com.edpp.wallet.dtoresponse;

import java.math.BigDecimal;

public record BalanceResponse(
       String walletNumber,
    BigDecimal balance,
    BigDecimal availableBalance,
    BigDecimal ledgerBalance,
    String currency
) {

}
