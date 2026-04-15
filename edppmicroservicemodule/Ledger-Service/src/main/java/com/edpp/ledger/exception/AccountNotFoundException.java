package com.edpp.ledger.exception;

public class AccountNotFoundException extends LedgerException {

    public AccountNotFoundException(String accountId) {
        super("ACCOUNT_NOT_FOUND", "GL Account not found: " + accountId);
    }

    public AccountNotFoundException(String accountCode, String tenantId) {
        super("ACCOUNT_NOT_FOUND", String.format("GL Account not found: %s for tenant: %s", accountCode, tenantId));
    }
}