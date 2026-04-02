package com.edpp.wallet.dtoresponse;

import java.math.BigDecimal;

import com.edpp.wallet.enums.WalletStatus;
import com.edpp.wallet.enums.WalletType;

public record WalletResponse(
      String id,
    String walletNumber,
    String customerId,
    WalletType walletType,
    WalletStatus status,
    BigDecimal balance,
    BigDecimal availableBalance,
    String currency,
    BigDecimal dailyTransactionLimit,
    BigDecimal monthlyTransactionLimit,
    BigDecimal perTransactionLimit,
    String tenantId
) {

}
