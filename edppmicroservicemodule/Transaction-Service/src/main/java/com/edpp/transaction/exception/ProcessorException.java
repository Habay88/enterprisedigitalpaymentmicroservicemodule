package com.edpp.transaction.exception;


import lombok.Getter;

@Getter
public class ProcessorException extends RuntimeException {

    private final String processorName;
    private final String errorCode;
    private final String transactionId;

    public ProcessorException(String message) {
        super(message);
        this.processorName = null;
        this.errorCode = null;
        this.transactionId = null;
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
        this.processorName = null;
        this.errorCode = null;
        this.transactionId = null;
    }

    public ProcessorException(String processorName, String message) {
        super(String.format("Processor [%s] error: %s", processorName, message));
        this.processorName = processorName;
        this.errorCode = null;
        this.transactionId = null;
    }

    public ProcessorException(String processorName, String errorCode, String message) {
        super(String.format("Processor [%s] error [%s]: %s", processorName, errorCode, message));
        this.processorName = processorName;
        this.errorCode = errorCode;
        this.transactionId = null;
    }

    public ProcessorException(String processorName, String errorCode, String message, String transactionId) {
        super(String.format("Processor [%s] error [%s]: %s (Transaction: %s)", 
              processorName, errorCode, message, transactionId));
        this.processorName = processorName;
        this.errorCode = errorCode;
        this.transactionId = transactionId;
    }

    public static ProcessorException connectionError(String processorName, Exception e) {
        return new ProcessorException(processorName, "CONNECTION_ERROR", 
            "Failed to connect to processor: " + e.getMessage());
    }

    public static ProcessorException timeoutError(String processorName) {
        return new ProcessorException(processorName, "TIMEOUT_ERROR", 
            "Processor request timed out");
    }

    public static ProcessorException authenticationError(String processorName) {
        return new ProcessorException(processorName, "AUTH_ERROR", 
            "Authentication with processor failed");
    }

    public static ProcessorException validationError(String processorName, String message) {
        return new ProcessorException(processorName, "VALIDATION_ERROR", message);
    }
}
