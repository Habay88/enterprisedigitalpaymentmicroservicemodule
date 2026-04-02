package com.edpp.wallet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.edpp.wallet.enums.WalletStatus;
import com.edpp.wallet.enums.WalletType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

@Entity
@Table(name = "wallets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenantId", "walletNumber"}),
    @UniqueConstraint(columnNames = {"tenantId", "customerId", "walletType"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String walletNumber;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletType walletType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyTransactionLimit = new BigDecimal("1000000");

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyTransactionLimit = new BigDecimal("5000000");

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal perTransactionLimit = new BigDecimal("500000");

    @Builder.Default
    private BigDecimal dailySpent = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal monthlySpent = BigDecimal.ZERO;

    private LocalDateTime lastTransactionAt;
    private LocalDateTime lastDailyResetAt;
    private LocalDateTime lastMonthlyResetAt;

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
        if (dailySpent == null) dailySpent = BigDecimal.ZERO;
        if (monthlySpent == null) monthlySpent = BigDecimal.ZERO;
        if (lastDailyResetAt == null) lastDailyResetAt = LocalDateTime.now();
        if (lastMonthlyResetAt == null) lastMonthlyResetAt = LocalDateTime.now();
        if (availableBalance == null) availableBalance = balance;
        if (ledgerBalance == null) ledgerBalance = balance;
    }
}
