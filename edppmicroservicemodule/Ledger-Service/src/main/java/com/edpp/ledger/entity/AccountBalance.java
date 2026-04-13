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
 * This table stores the closing balance for each account at the end of each
 * day.
 * Benefits:
 * - Fast balance lookup without summing all transactions
 * - Historical balance tracking
 * - Audit trail for balance changes
 * - Performance optimization for reporting
 */
@Entity
@Table(name = "account_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "accountId", "balanceDate" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String accountId;

    private LocalDate balanceDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal openingBalance;

    @Column(precision = 19, scale = 4)
    private BigDecimal closingBalance;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalDebit;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalCredit;

    private String tenantId;
}