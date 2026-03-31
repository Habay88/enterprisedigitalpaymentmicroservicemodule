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

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String transactionReference;

    private String merchantTransactionId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String sourceWalletId;
    private String destinationWalletId;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;

    private String currency;
    private String paymentMethod;

    // Payment processor details
    private String processorName;
    private String processorTransactionId;
    private String processorResponseCode;
    private String processorResponseMessage;

    @Column(length = 2000)
    private String processorRawResponse;

    @Column(length = 2000)
    private String description;

    private String customerId;
    private String customerEmail;
    private String customerPhone;

    private String ipAddress;
    private String userAgent;

    private LocalDateTime transactionDate;
    private LocalDateTime settledAt;
    private LocalDateTime failedAt;

    // Card and Bank Details (Embedded)
    @Embedded
    private CardDetails cardDetails;

    @Embedded
    private BankDetails bankDetails;

    // Reversal fields
    private String reversalReference;
    private LocalDateTime reversedAt;

    // Refund fields
    @Column(precision = 19, scale = 4)
    private BigDecimal refundedAmount;

    // Capture fields (for auth/capture flow)
    @Column(precision = 19, scale = 4)
    private BigDecimal capturedAmount;
    private LocalDateTime capturedAt;
    private LocalDateTime voidedAt;
    private String voidReason;

    // Retry fields
    private String retryReference;
    private Integer retryCount;

    // Original transaction reference for refunds/reversals/retries
    private String originalTransactionId;

    @Version
    private Long version;

    private String tenantId;

    @Column(length = 2000)
    private String fraudCheckResult;

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
        if (retryCount == null) {
            retryCount = 0;
        }
        if (refundedAmount == null) {
            refundedAmount = BigDecimal.ZERO;
        }
    }
}