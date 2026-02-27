package com.edpp.identity.model;

import com.edpp.identity.enums.WalletStatus;
import com.edpp.identity.enums.WalletType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

@Entity
@Table(name = "wallets")
@Data
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String walletNumber; // Account number equivalent

    @Column(nullable = false)
    private String customerId; // Reference to CIF

    @Enumerated(EnumType.STRING)
    private WalletType walletType; // SAVINGS, CURRENT, ESCROW

    @Enumerated(EnumType.STRING)
    private WalletStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(precision = 19, scale = 4)
    private BigDecimal availableBalance;

    @Column(precision = 19, scale = 4)
    private BigDecimal ledgerBalance; // Actual balance in general ledger

    private Currency currency;

    private BigDecimal dailyTransactionLimit;
    private BigDecimal monthlyTransactionLimit;
    private BigDecimal perTransactionLimit;

    private LocalDateTime lastTransactionAt;

    @Version
    private Long version; // Optimistic locking

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}