package com.edpp.ledger.exception;

public class DuplicateEntryException extends LedgerException {

    public DuplicateEntryException(String message) {
        super("DUPLICATE_ENTRY", message);
    }
}