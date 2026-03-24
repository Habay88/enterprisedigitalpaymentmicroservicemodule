package com.edpp.transaction.exception;

import lombok.Getter;

@Getter
public class TransactionException extends RuntimeException {

    private final String code;
    private final String transactionReference;
    private final String transactionId;

    public TransactionException(String message) {
        super(message);
        this.code = "TRANSACTION_ERROR";
        this.transactionReference = null;
        this.transactionId = null;
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
        this.code = "TRANSACTION_ERROR";
        this.transactionReference = null;
        this.transactionId = null;
    }

    public TransactionException(String code, String message) {
        super(message);
        this.code = code;
        this.transactionReference = null;
        this.transactionId = null;
    }

    public TransactionException(String code, String message, String transactionReference) {
        super(message);
        this.code = code;
        this.transactionReference = transactionReference;
        this.transactionId = null;
    }

    public TransactionException(String code, String message, String transactionReference, String transactionId) {
        super(message);
        this.code = code;
        this.transactionReference = transactionReference;
        this.transactionId = transactionId;
    }

    public static TransactionException notFound(String transactionReference) {
        return new TransactionException("NOT_FOUND", 
            String.format("Transaction not found with reference: %s", transactionReference), 
            transactionReference);
    }

    public static TransactionException invalidStatus(String transactionReference, String currentStatus, String expectedStatus) {
        return new TransactionException("INVALID_STATUS",
            String.format("Transaction %s has invalid status. Current: %s, Expected: %s", 
                transactionReference, currentStatus, expectedStatus),
            transactionReference);
    }

    public static TransactionException processingFailed(String transactionReference, String reason) {
        return new TransactionException("PROCESSING_FAILED",
            String.format("Transaction %s processing failed: %s", transactionReference, reason),
            transactionReference);
    }

    public static TransactionException validationFailed(String message) {
        return new TransactionException("VALIDATION_FAILED", message);
    }

    public static TransactionException duplicateTransaction(String transactionReference) {
        return new TransactionException("DUPLICATE",
            String.format("Duplicate transaction detected: %s", transactionReference),
            transactionReference);
    }
}