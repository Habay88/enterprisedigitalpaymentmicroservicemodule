package com.edpp.wallet.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends WalletException {

    public InsufficientBalanceException(String walletNumber, BigDecimal required, 
                                        BigDecimal available, String currency) {
        super("INSUFFICIENT_BALANCE", 
              String.format("Insufficient balance in wallet %s. Required: %s %s, Available: %s %s",
                      walletNumber, required, currency, available, currency));
    }
}