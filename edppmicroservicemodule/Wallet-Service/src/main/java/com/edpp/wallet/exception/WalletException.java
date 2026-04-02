package com.edpp.wallet.exception;

import lombok.Getter;

@Getter
public class WalletException extends RuntimeException {

    private final String code;

    public WalletException(String message) {
        super(message);
        this.code = "WALLET_ERROR";
    }

    public WalletException(String code, String message) {
        super(message);
        this.code = code;
    }

    public WalletException(String message, Throwable cause) {
        super(message, cause);
        this.code = "WALLET_ERROR";
    }
}