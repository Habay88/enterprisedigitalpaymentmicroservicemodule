package com.edpp.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Settlement Transaction Entity - Links settlements to original transactions
 * 
 * This entity maintains the relationship between a settlement and the
 * individual transactions that make up that settlement.
 * 
 * Each completed payment transaction is mapped to a settlement record,
 * allowing for:
 * - Audit trail from transaction to settlement
 * - Detailed breakdown of settlement components
 * - Dispute resolution
 * - Reconciliation
 */
@Entity
@Table(name = "settlement_transactions", indexes = {
    @Index(name = "idx_settle_txn_settlement", columnList = "settlementId"),
    @Index(name = "idx_settle_txn_transaction", columnList = "transactionId"),
    @Index(name = "idx_settle_txn_merchant", columnList = "merchantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String settlementId;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String transactionReference;

    private String merchantId;

    private LocalDateTime transactionDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal transactionAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(precision = 19, scale = 4)
    private BigDecimal settlementAmount;

    private String currency;

    private String paymentMethod;
}