package com.edpp.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Account Balance Entity - Daily balance snapshot
 * 
 * This table stores the closing balance for each account at the end of each day.
 * Benefits:
 * - Fast balance lookup without summing all transactions
 * - Historical balance tracking
 * - Audit trail for balance changes
 * - Performance optimization for reporting
 */
@Entity
@Table(name = "account_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"accountId", "balanceDate"})
}, indexes = {
    @Index(name = "idx_balance_account_date", columnList = "accountId, balanceDate"),
    @Index(name = "idx_balance_tenant_date", columnList = "tenantId, balanceDate"),
    @Index(name = "idx_balance_date", columnList = "balanceDate")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private LocalDate balanceDate;

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(nullable = false)
    private String tenantId;

    @Version
    private Long version;
}