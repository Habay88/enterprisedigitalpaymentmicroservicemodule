package com.edpp.settlement.entity;

import com.edpp.settlement.enums.SettlementFrequency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Merchant Settlement Configuration - Per-merchant settlement rules
 * 
 * Each merchant can have custom settlement settings:
 * - Settlement frequency (Daily, Weekly, Monthly)
 * - Fee structure (MDR percentage, fixed fees)
 * - Minimum settlement amount
 * - Reserve percentage (held for chargebacks)
 * - Cutoff time for same-day settlement
 */
@Entity
@Table(name = "merchant_settlement_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"merchantId", "tenantId"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSettlementConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    private SettlementFrequency frequency;

    private Integer cutoffHour;          // Cutoff hour (0-23)
    private Integer cutoffMinute;        // Cutoff minute (0-59)

    // Fee configuration
    @Column(precision = 19, scale = 4)
    private BigDecimal mdrRate;          // Merchant Discount Rate (e.g., 1.5%)

    @Column(precision = 19, scale = 4)
    private BigDecimal fixedFeePerTransaction;

    @Column(precision = 19, scale = 4)
    private BigDecimal minimumSettlementAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal reservePercentage; // Held for chargebacks

    @Column(precision = 19, scale = 4)
    private BigDecimal reserveAmount;      // Fixed reserve amount

    private boolean autoSettlement;       // Auto-settle or manual approval
    private boolean sendWebhook;          // Send webhook on settlement

    private String webhookUrl;            // Merchant webhook endpoint

    @Column(nullable = false)
    private String tenantId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}