package com.edpp.merchant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Merchant Webhook - Event notification endpoints
 * 
 * Merchants can register webhook URLs to receive real-time
 * notifications about transactions, settlements, and disputes.
 */
@Entity
@Table(name = "merchant_webhooks", indexes = {
    @Index(name = "idx_mw_merchant", columnList = "merchantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String url;

    private String events;  // Comma-separated list of events

    private boolean isActive;

    private int retryCount;

    private int timeoutMs;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}