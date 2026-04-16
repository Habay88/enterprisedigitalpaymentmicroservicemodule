package com.edpp.ledger.exception;

import java.math.BigDecimal;

public class UnbalancedJournalException extends LedgerException {

    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;

    public UnbalancedJournalException(String message, BigDecimal totalDebit, BigDecimal totalCredit) {
        super("UNBALANCED_JOURNAL", message);
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
    }

    public BigDecimal getDifference() {
        return totalDebit.subtract(totalCredit).abs();
    }
}