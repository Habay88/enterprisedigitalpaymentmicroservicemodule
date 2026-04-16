package com.edpp.ledger.service;

import com.edpp.ledger.entity.AccountBalance;
import com.edpp.ledger.entity.GLAccount;
import com.edpp.ledger.entity.JournalEntry;
import com.edpp.ledger.entity.JournalLine;
import com.edpp.ledger.repository.AccountBalanceRepository;
import com.edpp.ledger.repository.GLAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Account Balance Service - Manages daily balance snapshots
 * 
 * This service maintains daily closing balances for all GL accounts.
 * Benefits:
 * - Fast balance lookup without aggregating all transactions
 * - Historical balance tracking for reporting
 * - Performance optimization for financial reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceService {

    private final AccountBalanceRepository accountBalanceRepository;
    private final GLAccountRepository glAccountRepository;

    /**
     * Update daily balance for all accounts affected by a journal entry
     */
    @Transactional
    public void updateDailyBalance(JournalEntry journalEntry) {
        LocalDate balanceDate = journalEntry.getEntryDate().toLocalDate();
        
        // Group lines by account
        Map<String, List<JournalLine>> linesByAccount = journalEntry.getLines().stream()
                .collect(Collectors.groupingBy(line -> line.getAccount().getId()));

        for (Map.Entry<String, List<JournalLine>> entry : linesByAccount.entrySet()) {
            String accountId = entry.getKey();
            List<JournalLine> lines = entry.getValue();

            // Calculate total debit and credit for this account
            BigDecimal totalDebit = lines.stream()
                    .filter(line -> line.getDirection().equals("DEBIT"))
                    .map(JournalLine::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredit = lines.stream()
                    .filter(line -> line.getDirection().equals("CREDIT"))
                    .map(JournalLine::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Get or create daily balance
            AccountBalance dailyBalance = accountBalanceRepository
                    .findByAccountIdAndBalanceDate(accountId, balanceDate)
                    .orElseGet(() -> createDailyBalance(accountId, balanceDate));

            // Update totals
            dailyBalance.setTotalDebit(dailyBalance.getTotalDebit().add(totalDebit));
            dailyBalance.setTotalCredit(dailyBalance.getTotalCredit().add(totalCredit));

            // Calculate closing balance
            BigDecimal netChange = dailyBalance.getTotalDebit().subtract(dailyBalance.getTotalCredit());
            GLAccount account = glAccountRepository.findById(accountId).orElseThrow();
            
            if (account.getNormalBalance().equals("DEBIT")) {
                dailyBalance.setClosingBalance(dailyBalance.getOpeningBalance().add(netChange));
            } else {
                dailyBalance.setClosingBalance(dailyBalance.getOpeningBalance().subtract(netChange));
            }

            accountBalanceRepository.save(dailyBalance);
            log.debug("Updated balance for account {} on {}: Closing balance = {}", 
                     accountId, balanceDate, dailyBalance.getClosingBalance());
        }
    }

    /**
     * Create daily balance record for an account
     */
    private AccountBalance createDailyBalance(String accountId, LocalDate balanceDate) {
        // Get previous day's closing balance as opening balance
        AccountBalance previousDay = accountBalanceRepository
                .findByAccountIdAndBalanceDate(accountId, balanceDate.minusDays(1))
                .orElse(null);

        BigDecimal openingBalance = previousDay != null ? previousDay.getClosingBalance() : BigDecimal.ZERO;

        GLAccount account = glAccountRepository.findById(accountId).orElseThrow();

        return AccountBalance.builder()
                .accountId(accountId)
                .balanceDate(balanceDate)
                .openingBalance(openingBalance)
                .closingBalance(openingBalance)
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .tenantId(account.getTenantId())
                .build();
    }

    /**
     * Get closing balance for an account on a specific date
     */
    public BigDecimal getClosingBalance(String accountId, LocalDate date) {
        return accountBalanceRepository
                .findByAccountIdAndBalanceDate(accountId, date)
                .map(AccountBalance::getClosingBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get opening balance for an account on a specific date
     */
    public BigDecimal getOpeningBalance(String accountId, LocalDate date) {
        return accountBalanceRepository
                .findByAccountIdAndBalanceDate(accountId, date)
                .map(AccountBalance::getOpeningBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Run end-of-day processing to ensure all balances are calculated
     */
    @Transactional
    public void runEndOfDayProcessing(LocalDate date) {
        log.info("Running end-of-day processing for date: {}", date);
        
        List<GLAccount> accounts = glAccountRepository.findAll();
        int processedCount = 0;

        for (GLAccount account : accounts) {
            if (accountBalanceRepository.findByAccountIdAndBalanceDate(account.getId(), date).isEmpty()) {
                createDailyBalance(account.getId(), date);
                processedCount++;
            }
        }

        log.info("End-of-day processing completed. Created {} balance records for date: {}", 
                processedCount, date);
    }

    /**
     * Get balance history for an account over a date range
     */
    public List<AccountBalance> getBalanceHistory(String accountId, LocalDate startDate, LocalDate endDate) {
        return accountBalanceRepository.findByAccountIdAndBalanceDateBetween(accountId, startDate, endDate);
    }

    /**
     * Get the latest balance for an account
     */
    public BigDecimal getLatestBalance(String accountId) {
        return accountBalanceRepository
                .findLatestBalanceByAccountId(accountId)
                .map(AccountBalance::getClosingBalance)
                .orElse(BigDecimal.ZERO);
    }
}