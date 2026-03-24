package com.edpp.transaction.exception;



import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class InsufficientBalanceException extends RuntimeException {

    private final String walletId;
    private final BigDecimal requiredAmount;
    private final BigDecimal availableBalance;
    private final String currency;

    public InsufficientBalanceException(String message) {
        super(message);
        this.walletId = null;
        this.requiredAmount = null;
        this.availableBalance = null;
        this.currency = null;
    }

    public InsufficientBalanceException(String walletId, BigDecimal requiredAmount, BigDecimal availableBalance) {
        super(String.format("Insufficient balance in wallet %s. Required: %s, Available: %s", 
              walletId, requiredAmount, availableBalance));
        this.walletId = walletId;
        this.requiredAmount = requiredAmount;
        this.availableBalance = availableBalance;
        this.currency = null;
    }

    public InsufficientBalanceException(String walletId, BigDecimal requiredAmount, 
                                        BigDecimal availableBalance, String currency) {
        super(String.format("Insufficient balance in wallet %s. Required: %s %s, Available: %s %s", 
              walletId, requiredAmount, currency, availableBalance, currency));
        this.walletId = walletId;
        this.requiredAmount = requiredAmount;
        this.availableBalance = availableBalance;
        this.currency = currency;
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
        this.walletId = null;
        this.requiredAmount = null;
        this.availableBalance = null;
        this.currency = null;
    }

    public static InsufficientBalanceException forWallet(String walletId, BigDecimal required, BigDecimal available) {
        return new InsufficientBalanceException(walletId, required, available);
    }

    public static InsufficientBalanceException forTransaction(String transactionRef, String walletId, 
                                                               BigDecimal required, BigDecimal available) {
        return new InsufficientBalanceException(
            String.format("Transaction %s failed: Insufficient balance in wallet %s", transactionRef, walletId),
            required, available
        );
    }

    public BigDecimal getShortfall() {
        if (requiredAmount != null && availableBalance != null) {
            return requiredAmount.subtract(availableBalance);
        }
        return BigDecimal.ZERO;
    }
}
