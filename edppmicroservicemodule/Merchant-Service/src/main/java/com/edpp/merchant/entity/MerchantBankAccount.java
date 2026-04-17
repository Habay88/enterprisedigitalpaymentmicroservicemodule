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
 * Merchant Bank Account - Bank accounts for settlement
 * 
 * Merchants can have multiple bank accounts for settlement.
 * One account can be marked as primary for settlements.
 */
@Entity
@Table(name = "merchant_bank_accounts", indexes = {
    @Index(name = "idx_mba_merchant", columnList = "merchantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private String bankCode;

    @Column(nullable = false)
    private String bankName;

    private String branchCode;
    private String swiftCode;

    private boolean isPrimary;

    private String currency;  // NGN, USD, etc.

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}