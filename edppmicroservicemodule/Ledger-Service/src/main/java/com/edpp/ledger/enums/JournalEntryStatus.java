package com.edpp.ledger.enums;

/**
 * Journal Entry Status Enum
 * 
 * DRAFT: Entry created but not yet posted
 * POSTED: Entry has been posted to GL accounts
 * REVERSED: Entry has been reversed (opposite entry created)
 * CANCELLED: Entry was cancelled before posting
 */
public enum JournalEntryStatus {
    DRAFT,
    POSTED,
    REVERSED,
    CANCELLED
}