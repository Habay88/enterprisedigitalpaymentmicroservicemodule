package com.edpp.identity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public
class AuditLog {
    @Id
    private String id;

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String userId;
    private String userEmail;
    private String action;
    private String resource;
    private String resourceId;
    private String status;

    @Column(length = 4000)
    private String details; // JSON string

    private String ipAddress;
    private String userAgent;

    @CreationTimestamp
    private LocalDateTime createdAt;
}