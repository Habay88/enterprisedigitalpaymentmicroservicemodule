package com.edpp.ledger.entity;

import com.edpp.ledger.enums.JournalEntryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Journal Entry Entity - Records a financial transaction
 * 
 * A journal entry is the primary record of a financial transaction.
 * Each journal entry must follow double-entry accounting principles:
 * - Total Debits = Total Credits
 * - At least two accounts are affected
 * - Each line has a direction (DEBIT or CREDIT)
 * 
 * Journal entries are created from:
 * - Payment transactions
 * - Fee collections
 * - Refunds
 * - Reversals
 * - Manual adjustments
 */
@Entity
@Table(name = "journal_entries", indexes = {
    @Index(name = "idx_journal_reference", columnList = "reference"),
    @Index(name = "idx_journal_transaction", columnList = "transactionId"),
    @Index(name = "idx_journal_date", columnList = "entryDate"),
    @Index(name = "idx_journal_tenant", columnList = "tenantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String reference;           // Unique journal entry reference

    private String transactionId;       // Reference to original transaction

    private LocalDateTime entryDate;    // Accounting date

    private String description;         // Description of the transaction

    @Enumerated(EnumType.STRING)
    private JournalEntryStatus status;  // DRAFT, POSTED, REVERSED, CANCELLED

    @Column(precision = 19, scale = 4)
    private BigDecimal totalDebit;      // Sum of all debit amounts

    @Column(precision = 19, scale = 4)
    private BigDecimal totalCredit;     // Sum of all credit amounts

    private String tenantId;            // Multitenancy

    private String createdBy;           // User who created the entry

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (entryDate == null) {
            entryDate = LocalDateTime.now();
        }
        if (status == null) {
            status = JournalEntryStatus.DRAFT;
        }
    }
}