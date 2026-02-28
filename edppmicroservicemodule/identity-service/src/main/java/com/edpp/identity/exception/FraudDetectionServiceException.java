package com.edpp.identity.exception;

import lombok.Getter;

public class FraudDetectionServiceException extends RuntimeException{
    @Getter
    private final String serviceName;

    public FraudDetectionServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }

    public FraudDetectionServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }
}
