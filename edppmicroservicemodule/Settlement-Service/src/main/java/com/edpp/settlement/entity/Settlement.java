package com.edpp.settlement.entity;

import com.edpp.settlement.enums.SettlementStatus;
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
 * Settlement Entity - Core settlement record
 * 
 * This represents a settlement batch for a specific merchant on a specific date.
 * A settlement aggregates multiple transactions and calculates the net amount
 * to be transferred to the merchant's bank account.
 * 
 * Key Concepts:
 * - Gross Amount: Total transaction value before fees
 * - Total Fees: All fees (MDR, fixed, interchange) deducted
 * - Net Amount: Final amount to transfer (Gross - Fees)
 * - Settlement Date: When funds are actually transferred
 * - Cutoff Date: Transactions after this date go to next settlement
 */
@Entity
@Table(name = "settlements", indexes = {
    @Index(name = "idx_settlement_merchant", columnList = "merchantId"),
    @Index(name = "idx_settlement_batch", columnList = "batchId"),
    @Index(name = "idx_settlement_date", columnList = "settlementDate"),
    @Index(name = "idx_settlement_status", columnList = "status"),
    @Index(name = "idx_settlement_tenant", columnList = "tenantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String settlementReference;

    @Column(nullable = false)
    private String batchId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String merchantName;

    @Column(nullable = false)
    private String merchantEmail;

    @Column(nullable = false)
    private LocalDate settlementDate;

    private LocalDate cutoffDate;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    // Amount breakdown
    @Column(precision = 19, scale = 4)
    private BigDecimal grossAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalFees;

    @Column(precision = 19, scale = 4)
    private BigDecimal netAmount;

    // Transaction counts
    private Integer transactionCount;
    private Integer refundCount;

    // Fee components
    @Column(precision = 19, scale = 4)
    private BigDecimal mdrFee;           // Merchant Discount Rate

    @Column(precision = 19, scale = 4)
    private BigDecimal fixedFee;         // Per-transaction fixed fee

    @Column(precision = 19, scale = 4)
    private BigDecimal interchangeFee;   // Interchange fee

    @Column(precision = 19, scale = 4)
    private BigDecimal schemeFee;        // Card scheme fee

    @Column(precision = 19, scale = 4)
    private BigDecimal tax;              // VAT or other taxes

    // Bank transfer details
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankCode;
    private String bankName;
    private String transferReference;
    private LocalDateTime transferInitiatedAt;
    private LocalDateTime transferCompletedAt;

    // Metadata
    private String processedBy;
    private String rejectionReason;

    @Column(nullable = false)
    private String tenantId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}