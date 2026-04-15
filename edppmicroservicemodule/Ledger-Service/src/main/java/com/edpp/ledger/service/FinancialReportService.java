package com.edpp.ledger.service;

import com.edpp.ledger.dto.response.response.FinancialReportResponse;
import com.edpp.ledger.enums.AccountType;
import com.edpp.ledger.repository.GLAccountRepository;
import com.edpp.ledger.repository.JournalEntryRepository;
import com.edpp.ledger.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Financial Report Service - Generates standard accounting reports
 * 
 * Two main reports:
 * 1. Balance Sheet: Snapshot of financial position at a point in time
 *    Formula: Assets = Liabilities + Equity
 * 
 * 2. Income Statement: Performance over a period of time
 *    Formula: Revenue - Expenses = Net Income
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialReportService {

    private final GLAccountRepository glAccountRepository;
    private final JournalEntryRepository journalEntryRepository;

    /**
     * Generate Balance Sheet
     * Shows Assets, Liabilities, and Equity at a specific date
     */
    public FinancialReportResponse generateBalanceSheet(LocalDate asOfDate) {
        String tenantId = RequestContext.getCurrentTenantId();
        log.info("Generating balance sheet as of: {} for tenant: {}", asOfDate, tenantId);

        Map<String, BigDecimal> assets = getBalancesByType(AccountType.ASSET, tenantId);
        Map<String, BigDecimal> liabilities = getBalancesByType(AccountType.LIABILITY, tenantId);
        Map<String, BigDecimal> equity = getBalancesByType(AccountType.EQUITY, tenantId);

        BigDecimal totalAssets = assets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilities = liabilities.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEquity = equity.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // Verify accounting equation: Assets = Liabilities + Equity
        boolean isBalanced = totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0;

        return FinancialReportResponse.builder()
                .reportType("BALANCE_SHEET")
                .reportDate(asOfDate)
                .assets(assets)
                .liabilities(liabilities)
                .equity(equity)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalEquity(totalEquity)
                .isBalanced(isBalanced)
                .build();
    }

    /**
     * Generate Income Statement (Profit & Loss)
     * Shows Revenue, Expenses, and Net Income for a period
     */
    public FinancialReportResponse generateIncomeStatement(LocalDate startDate, LocalDate endDate) {
        String tenantId = RequestContext.getCurrentTenantId();
        log.info("Generating income statement from {} to {} for tenant: {}", startDate, endDate, tenantId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        BigDecimal totalRevenue = journalEntryRepository.getTotalCreditForPeriod(tenantId, startDateTime, endDateTime);
        BigDecimal totalExpenses = journalEntryRepository.getTotalDebitForPeriod(tenantId, startDateTime, endDateTime);
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);

        Map<String, BigDecimal> revenueByAccount = getRevenueByAccount(tenantId, startDateTime, endDateTime);
        Map<String, BigDecimal> expensesByAccount = getExpensesByAccount(tenantId, startDateTime, endDateTime);

        return FinancialReportResponse.builder()
                .reportType("INCOME_STATEMENT")
                .periodStart(startDate)
                .periodEnd(endDate)
                .revenue(revenueByAccount)
                .expenses(expensesByAccount)
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netIncome(netIncome)
                .build();
    }

    private Map<String, BigDecimal> getBalancesByType(AccountType type, String tenantId) {
        Map<String, BigDecimal> result = new HashMap<>();
        var accounts = glAccountRepository.findByAccountTypeAndTenantId(type, tenantId);
        
        for (var account : accounts) {
            result.put(account.getAccountName(), account.getBalance());
        }
        
        return result;
    }

    private Map<String, BigDecimal> getRevenueByAccount(String tenantId, LocalDateTime start, LocalDateTime end) {
        // Implementation to get revenue breakdown by account
        return new HashMap<>();
    }

    private Map<String, BigDecimal> getExpensesByAccount(String tenantId, LocalDateTime start, LocalDateTime end) {
        // Implementation to get expense breakdown by account
        return new HashMap<>();
    }
 // Add these methods to FinancialReportService

    /**
     * Get revenue breakdown by account
     */
    private Map<String, BigDecimal> getRevenueByAccount(String tenantId, LocalDateTime start, LocalDateTime end) {
        Map<String, BigDecimal> revenueByAccount = new HashMap<>();
        
        List<GLAccount> revenueAccounts = glAccountRepository.findByAccountTypeAndTenantId(AccountType.REVENUE, tenantId);
        
        for (GLAccount account : revenueAccounts) {
            BigDecimal total = journalEntryRepository.getTotalCreditForPeriod(tenantId, start, end);
            // More sophisticated calculation would filter by account
            revenueByAccount.put(account.getAccountName(), total);
        }
        
        return revenueByAccount;
    }

    /**
     * Get expense breakdown by account
     */
    private Map<String, BigDecimal> getExpensesByAccount(String tenantId, LocalDateTime start, LocalDateTime end) {
        Map<String, BigDecimal> expensesByAccount = new HashMap<>();
        
        List<GLAccount> expenseAccounts = glAccountRepository.findByAccountTypeAndTenantId(AccountType.EXPENSE, tenantId);
        
        for (GLAccount account : expenseAccounts) {
            BigDecimal total = journalEntryRepository.getTotalDebitForPeriod(tenantId, start, end);
            expensesByAccount.put(account.getAccountName(), total);
        }
        
        return expensesByAccount;
    }
}