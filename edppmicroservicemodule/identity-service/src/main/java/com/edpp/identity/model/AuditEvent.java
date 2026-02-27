package com.edpp.identity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String userId;
    private String userEmail;
    private String action;
    private String resource;
    private String resourceId;
    private String status;
    private Map<String, Object> details;
    private String ipAddress;
    private String userAgent;
}
