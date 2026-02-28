package com.edpp.identity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
