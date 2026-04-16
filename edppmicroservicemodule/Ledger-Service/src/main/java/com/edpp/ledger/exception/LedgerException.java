package com.edpp.ledger.exception;

import lombok.Getter;

@Getter
public class LedgerException extends RuntimeException {

    private final String code;

    public LedgerException(String message) {
        super(message);
        this.code = "LEDGER_ERROR";
    }

    public LedgerException(String code, String message) {
        super(message);
        this.code = code;
    }

    public LedgerException(String message, Throwable cause) {
        super(message, cause);
        this.code = "LEDGER_ERROR";
    }
}