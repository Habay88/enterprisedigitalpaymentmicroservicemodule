package com.edpp.identity.model;

import java.time.LocalDateTime;

public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    // Additional fields for specific exceptions
    private String field;
    private String value;
    private String identifier;
    private String reason;
    private String customerId;
    private String blockReason;
    private Integer retryAfterSeconds;
}
