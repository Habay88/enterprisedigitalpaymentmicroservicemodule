package com.edpp.ledger.service;

import com.edpp.ledger.dto.response.request.CreateJournalEntryRequest;
import com.edpp.ledger.dto.response.request.JournalLineRequest;
import com.edpp.ledger.dto.response.response.JournalEntryResponse;
import com.edpp.ledger.entity.GLAccount;
import com.edpp.ledger.entity.JournalEntry;
import com.edpp.ledger.entity.JournalLine;
import com.edpp.ledger.enums.JournalEntryStatus;
import com.edpp.ledger.exception.AccountNotFoundException;
import com.edpp.ledger.exception.DuplicateEntryException;
import com.edpp.ledger.exception.UnbalancedJournalException;
import com.edpp.ledger.mapper.LedgerMapper;
import com.edpp.ledger.repository.GLAccountRepository;
import com.edpp.ledger.repository.JournalEntryRepository;
import com.edpp.ledger.repository.JournalLineRepository;
import com.edpp.ledger.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ledger Service - Core accounting engine
 * 
 * This service implements the fundamental double-entry accounting principles:
 * 1. Every transaction must have at least two accounts affected
 * 2. Total Debits must equal Total Credits
 * 3. The accounting equation must always balance: Assets = Liabilities + Equity
 * 
 * Key Operations:
 * - Create and post journal entries
 * - Validate double-entry integrity
 * - Update GL account balances
 * - Reverse incorrect entries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final GLAccountRepository glAccountRepository;
    private final LedgerMapper ledgerMapper;
    private final AccountBalanceService accountBalanceService;

    /**
     * Create and post a journal entry
     * 
     * This is the main method for recording financial transactions.
     * It validates that the entry follows double-entry rules before posting.
     */
    @Transactional
    public JournalEntryResponse createJournalEntry(CreateJournalEntryRequest request) {
        String tenantId = RequestContext.getCurrentTenantId();
        log.info("Creating journal entry: {} for tenant: {}", request.reference(), tenantId);

        // Check for duplicate reference
        if (journalEntryRepository.findByReference(request.reference()).isPresent()) {
            throw new DuplicateEntryException("Journal entry reference already exists: " + request.reference());
        }

        // Validate double-entry (debits = credits)
        validateDoubleEntry(request);

        // Create journal entry header
        JournalEntry journalEntry = buildJournalEntry(request, tenantId);
        JournalEntry savedJournal = journalEntryRepository.save(journalEntry);

        // Create journal lines and update account balances
        List<JournalLine> lines = createJournalLines(request, savedJournal);
        savedJournal.setLines(lines);

        // Update daily balances
        accountBalanceService.updateDailyBalance(savedJournal);

        log.info("Journal entry created successfully: {}", savedJournal.getReference());
        return ledgerMapper.toJournalEntryResponse(savedJournal);
    }

    /**
     * Post a journal entry from a transaction event (Kafka consumer)
     * 
     * This method is called automatically when a payment is processed.
     * It creates the appropriate journal entry based on transaction type.
     */
    @Transactional
    public JournalEntryResponse postFromTransaction(String transactionId, String transactionType,
                                                     BigDecimal amount, String currency,
                                                     String sourceWalletId, String destinationWalletId) {
        log.info("Posting journal entry from transaction: {} - Type: {}", transactionId, transactionType);

        List<JournalLineRequest> lines = determineJournalLines(transactionType, amount, sourceWalletId, destinationWalletId);

        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                "JRN_" + transactionId,
                transactionId,
                LocalDateTime.now(),
                "Journal entry for " + transactionType + ": " + transactionId,
                lines
        );

        return createJournalEntry(request);
    }

    /**
     * Reverse a journal entry
     * 
     * Creates an opposite journal entry to reverse a previous entry.
     * This is used for correcting errors or processing refunds.
     */
    @Transactional
    public JournalEntryResponse reverseJournalEntry(String journalEntryId, String reason) {
        log.info("Reversing journal entry: {} reason: {}", journalEntryId, reason);

        JournalEntry original = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));

        if (original.getStatus() == JournalEntryStatus.REVERSED) {
            throw new RuntimeException("Journal entry already reversed");
        }

        // Create reversal lines (opposite direction)
        List<JournalLineRequest> reversalLines = createReversalLines(original, reason);

        CreateJournalEntryRequest reversalRequest = new CreateJournalEntryRequest(
                "REV_" + original.getReference(),
                original.getTransactionId(),
                LocalDateTime.now(),
                "Reversal of " + original.getReference() + ": " + reason,
                reversalLines
        );

        JournalEntryResponse reversal = createJournalEntry(reversalRequest);

        // Mark original as reversed
        original.setStatus(JournalEntryStatus.REVERSED);
        journalEntryRepository.save(original);

        return reversal;
    }

    /**
     * Get journal entry by reference
     */
    @Cacheable(value = "journalEntries", key = "#reference")
    public JournalEntryResponse getJournalEntryByReference(String reference) {
        JournalEntry entry = journalEntryRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Journal entry not found: " + reference));
        return ledgerMapper.toJournalEntryResponse(entry);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate that total debits equal total credits
     */
    private void validateDoubleEntry(CreateJournalEntryRequest request) {
        BigDecimal totalDebit = calculateTotalByDirection(request, "DEBIT");
        BigDecimal totalCredit = calculateTotalByDirection(request, "CREDIT");

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new UnbalancedJournalException(
                    String.format("Journal entry is unbalanced. Debits: %s, Credits: %s",
                            totalDebit, totalCredit),
                    totalDebit,
                    totalCredit
            );
        }
    }

    /**
     * Calculate total for a specific direction (DEBIT or CREDIT)
     */
    private BigDecimal calculateTotalByDirection(CreateJournalEntryRequest request, String direction) {
        return request.lines().stream()
                .filter(line -> line.direction().equals(direction))
                .map(JournalLineRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Build journal entry entity from request
     */
    private JournalEntry buildJournalEntry(CreateJournalEntryRequest request, String tenantId) {
        BigDecimal totalDebit = calculateTotalByDirection(request, "DEBIT");
        BigDecimal totalCredit = calculateTotalByDirection(request, "CREDIT");

        return JournalEntry.builder()
                .reference(request.reference())
                .transactionId(request.transactionId())
                .entryDate(request.entryDate())
                .description(request.description())
                .status(JournalEntryStatus.POSTED)
                .tenantId(tenantId)
                .createdBy(RequestContext.getCurrentUserId())
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .build();
    }

    /**
     * Create journal lines and update account balances
     */
    private List<JournalLine> createJournalLines(CreateJournalEntryRequest request, JournalEntry journalEntry) {
        List<JournalLine> lines = new ArrayList<>();

        for (JournalLineRequest lineRequest : request.lines()) {
            GLAccount account = glAccountRepository.findById(lineRequest.accountId())
                    .orElseThrow(() -> new AccountNotFoundException(lineRequest.accountId()));

            // Update account balance based on direction
            updateAccountBalance(account, lineRequest.direction(), lineRequest.amount());

            // Create journal line
            JournalLine line = JournalLine.builder()
                    .journalEntry(journalEntry)
                    .account(account)
                    .direction(lineRequest.direction())
                    .amount(lineRequest.amount())
                    .description(lineRequest.description())
                    .build();

            lines.add(journalLineRepository.save(line));
        }

        return lines;
    }

    /**
     * Update GL account balance based on transaction direction
     * 
     * Rules for balance updates:
     * - If direction matches normal balance: ADD to balance
     * - If direction opposes normal balance: SUBTRACT from balance
     */
    private void updateAccountBalance(GLAccount account, String direction, BigDecimal amount) {
        BigDecimal newBalance = account.getBalance();

        if (direction.equals("DEBIT")) {
            if (account.getNormalBalance().equals("DEBIT")) {
                newBalance = newBalance.add(amount);  // Asset/Expense increase
            } else {
                newBalance = newBalance.subtract(amount);  // Liability/Revenue decrease
            }
        } else { // CREDIT
            if (account.getNormalBalance().equals("CREDIT")) {
                newBalance = newBalance.add(amount);  // Liability/Revenue increase
            } else {
                newBalance = newBalance.subtract(amount);  // Asset/Expense decrease
            }
        }

        account.setBalance(newBalance);
        glAccountRepository.save(account);
    }

    /**
     * Determine journal lines based on transaction type
     */
    private List<JournalLineRequest> determineJournalLines(String transactionType, BigDecimal amount,
                                                            String sourceWalletId, String destinationWalletId) {
        List<JournalLineRequest> lines = new ArrayList<>();

        switch (transactionType) {
            case "PAYMENT":
                // Debit source wallet (money leaves)
                lines.add(new JournalLineRequest("ASSET_CUSTOMER", "DEBIT", amount, 
                        "Payment from customer wallet"));
                // Credit destination wallet (money arrives)
                lines.add(new JournalLineRequest("ASSET_MERCHANT", "CREDIT", amount,
                        "Payment to merchant wallet"));
                // Add fee income
                BigDecimal fee = amount.multiply(new BigDecimal("0.015"));
                lines.add(new JournalLineRequest("FEE_INCOME", "CREDIT", fee,
                        "Transaction fee income"));
                lines.add(new JournalLineRequest("FEE_EXPENSE", "DEBIT", fee,
                        "Processing fee expense"));
                break;

            case "REFUND":
                // Opposite of payment
                lines.add(new JournalLineRequest("ASSET_MERCHANT", "DEBIT", amount,
                        "Refund from merchant"));
                lines.add(new JournalLineRequest("ASSET_CUSTOMER", "CREDIT", amount,
                        "Refund to customer"));
                break;

            default:
                throw new IllegalArgumentException("Unknown transaction type: " + transactionType);
        }

        return lines;
    }

    /**
     * Create reversal lines (opposite direction of original)
     */
    private List<JournalLineRequest> createReversalLines(JournalEntry original, String reason) {
        return original.getLines().stream()
                .map(line -> new JournalLineRequest(
                        line.getAccount().getId(),
                        line.getDirection().equals("DEBIT") ? "CREDIT" : "DEBIT",
                        line.getAmount(),
                        "Reversal: " + reason + " - " + line.getDescription()
                ))
                .toList();
    }
}