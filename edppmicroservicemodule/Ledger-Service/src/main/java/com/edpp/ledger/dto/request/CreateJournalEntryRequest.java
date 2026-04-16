package com.edpp.ledger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Create Journal Entry Request - DTO for creating a new journal entry
 * 
 * This record encapsulates all data needed to create a journal entry.
 * It includes validation to ensure the entry follows double-entry rules.
 */
public record CreateJournalEntryRequest(
    
    @NotBlank(message = "Reference is required")
    String reference,
    
    String transactionId,
    
    @NotNull(message = "Entry date is required")
    LocalDateTime entryDate,
    
    String description,
    
    @NotEmpty(message = "At least one journal line is required")
    List<JournalLineRequest> lines
) {
    /**
     * Validate that the journal entry has at least one line
     */
    public boolean hasLines() {
        return lines != null && !lines.isEmpty();
    }
    
    /**
     * Get total number of lines
     */
    public int getLineCount() {
        return lines != null ? lines.size() : 0;
    }
}