package com.edpp.ledger.mapper;

import com.edpp.ledger.dto.response.*;
import com.edpp.ledger.entity.GLAccount;
import com.edpp.ledger.entity.JournalEntry;
import com.edpp.ledger.entity.JournalLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LedgerMapper {

    public GLAccountResponse toGLAccountResponse(GLAccount account) {
        if (account == null) return null;

        return new GLAccountResponse(
                account.getId(),
                account.getAccountCode(),
                account.getAccountName(),
                account.getAccountType(),
                account.getNormalBalance(),
                account.getBalance(),
                account.getParentAccountCode(),
                account.isActive(),
                account.getDescription()
        );
    }

    public JournalEntryResponse toJournalEntryResponse(JournalEntry entry) {
        if (entry == null) return null;

        List<JournalLineResponse> lines = entry.getLines().stream()
                .map(this::toJournalLineResponse)
                .toList();

        return new JournalEntryResponse(
                entry.getId(),
                entry.getReference(),
                entry.getTransactionId(),
                entry.getEntryDate(),
                entry.getDescription(),
                entry.getStatus(),
                entry.getTotalDebit(),
                entry.getTotalCredit(),
                lines,
                entry.getCreatedAt()
        );
    }

    public JournalLineResponse toJournalLineResponse(JournalLine line) {
        if (line == null) return null;

        return new JournalLineResponse(
                line.getId(),
                line.getAccount().getAccountCode(),
                line.getAccount().getAccountName(),
                line.getDirection(),
                line.getAmount(),
                line.getDescription()
        );
    }

    public List<GLAccountResponse> toGLAccountResponseList(List<GLAccount> accounts) {
        if (accounts == null) return List.of();
        return accounts.stream()
                .map(this::toGLAccountResponse)
                .toList();
    }

    public List<JournalEntryResponse> toJournalEntryResponseList(List<JournalEntry> entries) {
        if (entries == null) return List.of();
        return entries.stream()
                .map(this::toJournalEntryResponse)
                .toList();
    }
}