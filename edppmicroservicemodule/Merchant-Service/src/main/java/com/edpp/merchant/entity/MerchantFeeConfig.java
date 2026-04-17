package com.edpp.merchant.entity;

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
 * Merchant Fee Configuration - Custom fee structure per merchant
 * 
 * Defines the fees charged to the merchant:
 * - MDR (Merchant Discount Rate) - Percentage of transaction
 * - Fixed fee - Per-transaction fee
 * - Interchange fees - Passed through
 * - Scheme fees - Card network fees
 * - Monthly minimum fee
 * - Volume discounts
 */
@Entity
@Table(name = "merchant_fee_configs", indexes = {
    @Index(name = "idx_mfc_merchant", columnList = "merchantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantFeeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String merchantId;

    // Card present fees
    @Column(precision = 19, scale = 4)
    private BigDecimal cardPresentMdr;

    // Card not present (e-commerce)
    @Column(precision = 19, scale = 4)
    private BigDecimal cardNotPresentMdr;

    // Fixed fees
    @Column(precision = 19, scale = 4)
    private BigDecimal fixedFee;

    // International card fees
    @Column(precision = 19, scale = 4)
    private BigDecimal internationalMdr;

    // Refund fees
    @Column(precision = 19, scale = 4)
    private BigDecimal refundFee;

    // Monthly minimum
    @Column(precision = 19, scale = 4)
    private BigDecimal monthlyMinimumFee;

    // Volume discount tiers
    private String volumeDiscountTiers;  // JSON string

    // Effective dates
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    private String createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}