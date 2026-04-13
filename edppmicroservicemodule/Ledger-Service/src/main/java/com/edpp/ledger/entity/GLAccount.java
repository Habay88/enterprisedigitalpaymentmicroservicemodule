package com.edpp.ledger.entity;

import com.edpp.ledger.enums.AccountType;
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
 * GL Account Entity - Represents an account in the Chart of Accounts
 * 
 * This is the foundation of double-entry accounting. Each account has:
 * - A unique account code (e.g., "1000" for Cash)
 * - An account type (Asset, Liability, Equity, Revenue, Expense)
 * - A normal balance (Debit or Credit)
 * - A current balance
 * 
 * The normal balance determines how increases are recorded:
 * - Asset/Expense accounts: Debit increases, Credit decreases
 * - Liability/Equity/Revenue accounts: Credit increases, Debit decreases
 */
@Entity
@Table(name = "gl_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tenantId", "accountCode" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String accountCode; // e.g., "1000", "2000", "3000"

    @Column(nullable = false)
    private String accountName; // e.g., "Cash - NGN", "Customer Deposits"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType; // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE

    @Column(nullable = false)
    private String normalBalance; // "DEBIT" or "CREDIT"

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    private String parentAccountCode; // For hierarchical account structures

    @Column(nullable = false)
    private String tenantId; // Multitenancy support

    private boolean active; // Soft delete flag

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}