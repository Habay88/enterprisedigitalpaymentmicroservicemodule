package com.edpp.iso8583.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "terminal_sessions", indexes = {
        @Index(name = "idx_terminal_id", columnList = "terminalId"),
        @Index(name = "idx_merchant_id", columnList = "merchantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String terminalId;

    private String merchantId;

    private String ipAddress;

    private String serialNumber;

    private String model;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    private LocalDateTime connectedAt;

    private LocalDateTime lastHeartbeat;

    private boolean isActive;

    private String tenantId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}

enum SessionStatus {
    CONNECTED, DISCONNECTED, SUSPENDED
}