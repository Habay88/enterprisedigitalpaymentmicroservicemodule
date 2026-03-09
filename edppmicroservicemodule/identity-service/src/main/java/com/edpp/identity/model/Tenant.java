package com.edpp.identity.model;

import com.edpp.identity.config.TenantConfiguration;
import com.edpp.identity.enums.TenantStatus;
import com.edpp.identity.enums.TenantType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String tenantId; // Business identifier for the tenant

    @Column(nullable = false)
    private String name; // Company/Bank name

    @Column(unique = true)
    private String schemaName; // Database schema name

    @Column(unique = true)
    private String domain; // Tenant domain (e.g., banka.example.com)

    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    @Enumerated(EnumType.STRING)
    private TenantType tenantType; // BANK, FINTECH, MICROFINANCE

    private String databaseUrl;
    private String databaseUsername;
    private String databasePassword; // Encrypted

    @Embedded
    private TenantConfiguration configuration;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

