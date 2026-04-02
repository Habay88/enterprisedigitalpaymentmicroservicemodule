package com.edpp.wallet.exception;

import java.math.BigDecimal;

public class LimitExceededException extends WalletException {

    public LimitExceededException(String message, BigDecimal limit, BigDecimal attempted) {
        super("LIMIT_EXCEEDED", 
              String.format("%s. Limit: %s, Attempted: %s", message, limit, attempted));
    }
}