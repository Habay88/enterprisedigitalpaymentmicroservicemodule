package com.edpp.transaction.entity;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.edpp.transaction.enums.TransactionStatus;
import com.edpp.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_reference", columnList = "transactionReference"),
    @Index(name = "idx_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_source_wallet", columnList = "sourceWalletId"),
    @Index(name = "idx_status_created", columnList = "status, createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
   @Column(columnDefinition = "TEXT")
private String fraudCheckResult; // or store as JSON stri
    @Column(unique = true, nullable = false, length = 50)
    private String transactionReference;

    @Column(nullable = false, length = 50)
    private String merchantTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private String sourceWalletId;

    private String destinationWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 50)
    private String paymentMethod;

    @Column(nullable = false)
    private String tenantId;

    // Payment processor details
    private String processorName;
    private String processorTransactionId;
    private String processorResponseCode;
    private String processorResponseMessage;
    @Column(length = 2000)
    private String processorRawResponse;

    @Embedded
    private CardDetails cardDetails;

    @Embedded
    private BankDetails bankDetails;

    @Column(length = 500)
    private String description;

    private String customerId; // Reference to Identity Service
    private String customerEmail;
    private String customerPhone;

    @Column(length = 45)
    private String ipAddress;
    private String userAgent;

    private LocalDateTime transactionDate;
    private LocalDateTime settledAt;
    private LocalDateTime failedAt;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (totalAmount == null && amount != null) {
            totalAmount = amount.add(fee != null ? fee : BigDecimal.ZERO);
        }
    }}


