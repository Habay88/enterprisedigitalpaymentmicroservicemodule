package com.edpp.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Settlement Fee Entity - Detailed fee breakdown per settlement
 * 
 * Provides granular fee information for transparency and reporting.
 * Fees include:
 * - MDR (Merchant Discount Rate)
 * - Fixed per-transaction fees
 * - Interchange fees (paid to issuing bank)
 * - Scheme fees (paid to card network)
 * - Taxes (VAT, withholding tax)
 */
@Entity
@Table(name = "settlement_fees", indexes = {
    @Index(name = "idx_fee_settlement", columnList = "settlementId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String settlementId;

    private String feeType;               // MDR, FIXED, INTERCHANGE, SCHEME, TAX

    @Column(precision = 19, scale = 4)
    private BigDecimal rate;              // Percentage rate if applicable

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    private String description;

    private Integer transactionCount;
}