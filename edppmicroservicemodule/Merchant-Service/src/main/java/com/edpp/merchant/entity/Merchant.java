package com.edpp.merchant.entity;

import com.edpp.merchant.enums.MerchantCategory;
import com.edpp.merchant.enums.MerchantStatus;
import com.edpp.merchant.enums.SettlementFrequency;
import com.edpp.merchant.enums.VerificationStatus;

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
 * Merchant Entity - Core merchant profile
 * 
 * Represents a business that accepts payments through the platform.
 * Each merchant has:
 * - Unique merchant code for identification
 * - Business information (name, address, contact)
 * - Bank account details for settlements
 * - Fee configuration (MDR, fixed fees)
 * - Settlement preferences
 * - Verification status for compliance
 */
@Entity
@Table(name = "merchants", indexes = {
    @Index(name = "idx_merchant_code", columnList = "merchantCode"),
    @Index(name = "idx_merchant_email", columnList = "email"),
    @Index(name = "idx_merchant_status", columnList = "status"),
    @Index(name = "idx_merchant_tenant", columnList = "tenantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String merchantCode;

    @Column(nullable = false)
    private String businessName;

    private String tradingName;

    @Column(nullable = false)
    private String email;

    private String phoneNumber;

    @Column(length = 500)
    private String address;

    private String city;
    private String state;
    private String country;
    private String postalCode;

    @Column(nullable = false)
    private String registrationNumber;  // RC Number / CAC Registration

    private String taxId;  // VAT Registration Number

    @Enumerated(EnumType.STRING)
    private MerchantCategory category;  // MCC

    @Enumerated(EnumType.STRING)
    private MerchantStatus status;

    private String website;
    private String callbackUrl;

    // Settlement preferences
    @Enumerated(EnumType.STRING)
    private SettlementFrequency settlementFrequency;

    private Integer settlementCutoffHour;  // Cutoff time for same-day settlement

    @Column(precision = 19, scale = 4)
    private BigDecimal minimumSettlementAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal reservePercentage;  // Held for chargebacks

    // Verification
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private String verifiedBy;
    private LocalDateTime verifiedAt;

    // Contact persons
    private String contactPersonName;
    private String contactPersonPhone;
    private String contactPersonEmail;

    private String technicalContactName;
    private String technicalContactEmail;
    private String technicalContactPhone;

    @Column(nullable = false)
    private String tenantId;

    private String createdBy;
    private String updatedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}