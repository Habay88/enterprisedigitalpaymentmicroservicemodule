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
 * Merchant API Key - For merchant integration
 * 
 * Each merchant gets API keys to integrate with the platform.
 * Supports:
 * - Public key (publishable) for client-side
 * - Secret key (private) for server-side
 * - Webhook secret for signature verification
 */
@Entity
@Table(name = "merchant_api_keys", indexes = {
    @Index(name = "idx_mak_merchant", columnList = "merchantId"),
    @Index(name = "idx_mak_public_key", columnList = "publicKey"),
    @Index(name = "idx_mak_secret_key", columnList = "secretKeyHash")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String merchantId;

    @Column(unique = true, nullable = false)
    private String publicKey;  // Publishable key

    @Column(nullable = false)
    private String secretKeyHash;  // Hashed secret key

    private String secretKeyPrefix;  // First 8 chars for display

    private String webhookSecret;  // For webhook signature verification

    private boolean isActive;

    private LocalDateTime expiresAt;

    private String lastUsedIp;
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}