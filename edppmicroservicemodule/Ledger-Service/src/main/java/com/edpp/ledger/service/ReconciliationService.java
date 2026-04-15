package com.edpp.ledger.service;

import com.edpp.ledger.dto.response.ReconciliationResponse;
import com.edpp.ledger.entity.JournalEntry;
import com.edpp.ledger.repository.JournalEntryRepository;
import com.edpp.ledger.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final JournalEntryRepository journalEntryRepository;

    /**
     * Reconcile ledger with bank statement
     */
    public ReconciliationResponse reconcile(String bankAccountId, LocalDate statementDate, BigDecimal statementBalance) {
        String tenantId = RequestContext.getCurrentTenantId();
        log.info("Reconciling bank account: {} as of: {} for tenant: {}", bankAccountId, statementDate, tenantId);

        // Get ledger balance up to statement date
        BigDecimal ledgerBalance = calculateLedgerBalance(bankAccountId, statementDate);
        
        // Calculate difference
        BigDecimal difference = ledgerBalance.subtract(statementBalance);
        
        // Find unmatched transactions (simplified)
        List<String> unmatchedTransactions = findUnmatchedTransactions(bankAccountId, statementDate);

        boolean isReconciled = difference.compareTo(BigDecimal.ZERO) == 0 && unmatchedTransactions.isEmpty();

        return ReconciliationResponse.builder()
                .bankAccountId(bankAccountId)
                .asOfDate(statementDate)
                .statementBalance(statementBalance)
                .ledgerBalance(ledgerBalance)
                .difference(difference)
                .isReconciled(isReconciled)
                .unmatchedTransactions(unmatchedTransactions)
                .build();
    }

    private BigDecimal calculateLedgerBalance(String accountId, LocalDate asOfDate) {
        // Implementation to calculate ledger balance
        // Sum all transactions up to the statement date
        return BigDecimal.ZERO;
    }

    private List<String> findUnmatchedTransactions(String accountId, LocalDate statementDate) {
        // Implementation to find transactions not matched with bank statement
        return new ArrayList<>();
    }
}