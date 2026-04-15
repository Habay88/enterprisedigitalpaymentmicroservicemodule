package com.edpp.ledger.controller;

import com.edpp.ledger.dto.response.request.CreateJournalEntryRequest;
import com.edpp.ledger.dto.response.response.JournalEntryResponse;
import com.edpp.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ledger/journal-entries")
@RequiredArgsConstructor
@Tag(name = "Journal Entries", description = "Double-entry transaction recording")
public class JournalEntryController {

    private final LedgerService ledgerService;

    @PostMapping
    @Operation(summary = "Create a journal entry")
    public ResponseEntity<JournalEntryResponse> createJournalEntry(@Valid @RequestBody CreateJournalEntryRequest request) {
        JournalEntryResponse response = ledgerService.createJournalEntry(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get journal entry by reference")
    public ResponseEntity<JournalEntryResponse> getJournalEntry(@PathVariable String reference) {
        JournalEntryResponse response = ledgerService.getJournalEntryByReference(reference);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reverse")
    @Operation(summary = "Reverse a journal entry")
    public ResponseEntity<JournalEntryResponse> reverseJournalEntry(@PathVariable String id, @RequestParam String reason) {
        JournalEntryResponse response = ledgerService.reverseJournalEntry(id, reason);
        return ResponseEntity.ok(response);
    }
}