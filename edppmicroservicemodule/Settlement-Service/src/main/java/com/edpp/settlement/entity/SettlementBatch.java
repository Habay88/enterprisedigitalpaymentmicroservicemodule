package com.edpp.settlement.entity;

import com.edpp.settlement.enums.BatchStatus;
import com.edpp.settlement.enums.SettlementFrequency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Settlement Batch Entity - Groups settlements by processing period
 * 
 * A batch represents a group of settlements processed together.
 * This allows for:
 * - Daily settlement runs
 * - Weekly settlement cycles
 * - Monthly closing batches
 * - Cutoff time enforcement
 */
@Entity
@Table(name = "settlement_batches", indexes = {
    @Index(name = "idx_batch_date", columnList = "batchDate"),
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_batch_tenant", columnList = "tenantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String batchReference;

    private LocalDate batchDate;

    @Enumerated(EnumType.STRING)
    private SettlementFrequency frequency;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    private Integer totalMerchants;
    private Integer totalTransactions;
    private Integer totalSettlements;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalGrossAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalNetAmount;

    private LocalDateTime processedAt;
    private String processedBy;
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private String tenantId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}