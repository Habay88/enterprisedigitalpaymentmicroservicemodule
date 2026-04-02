package com.edpp.wallet.exception;

public class WalletNotFoundException extends WalletException {

    public WalletNotFoundException(String walletNumber) {
        super("NOT_FOUND", "Wallet not found: " + walletNumber);
    }
}