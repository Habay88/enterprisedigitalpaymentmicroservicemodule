package com.edpp.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Journal Line Entity - Individual debit or credit entry
 * 
 * Each journal entry consists of multiple journal lines.
 * Each line represents either a DEBIT or CREDIT to a specific GL account.
 * 
 * Example: A payment of ₦50,000 would have:
 * Line 1: DEBIT to Customer Asset Account for ₦50,000
 * Line 2: CREDIT to Merchant Asset Account for ₦50,000
 */
@Entity
@Table(name = "journal_lines", indexes = {
        @Index(name = "idx_journal_line_entry", columnList = "journal_entry_id"),
        @Index(name = "idx_journal_line_account", columnList = "account_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private GLAccount account;

    @Column(nullable = false)
    private String direction; // "DEBIT" or "CREDIT"

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    private String description; // Line-specific description
}