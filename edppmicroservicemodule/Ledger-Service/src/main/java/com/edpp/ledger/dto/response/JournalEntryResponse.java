package com.edpp.ledger.dto.response;

import com.edpp.ledger.enums.JournalEntryStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Journal Entry Response - DTO for returning journal entry data
 * 
 * This record provides a complete view of a journal entry including
 * all its lines, totals, and metadata.
 */
public record JournalEntryResponse(
    String id,
    String reference,
    String transactionId,
    LocalDateTime entryDate,
    String description,
    JournalEntryStatus status,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    List<JournalLineResponse> lines,
    LocalDateTime createdAt
) {
    /**
     * Check if the journal entry is balanced (debits = credits)
     */
    public boolean isBalanced() {
        if (totalDebit == null || totalCredit == null) {
            return false;
        }
        return totalDebit.compareTo(totalCredit) == 0;
    }
    
    /**
     * Check if the journal entry is posted
     */
    public boolean isPosted() {
        return status == JournalEntryStatus.POSTED;
    }
    
    /**
     * Get the difference between debits and credits
     */
    public BigDecimal getDifference() {
        if (totalDebit == null || totalCredit == null) {
            return BigDecimal.ZERO;
        }
        return totalDebit.subtract(totalCredit).abs();
    }
    
    /**
     * Get the number of lines in this journal entry
     */
    public int getLineCount() {
        return lines != null ? lines.size() : 0;
    }
}