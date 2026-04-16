package com.edpp.ledger.service;

import com.edpp.ledger.dto.response.response.TrialBalanceLine;
import com.edpp.ledger.dto.response.response.TrialBalanceResponse;
import com.edpp.ledger.entity.GLAccount;
import com.edpp.ledger.repository.GLAccountRepository;
import com.edpp.ledger.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Trial Balance Service - Verifies accounting equation
 * 
 * The trial balance ensures that total debits equal total credits
 * across all GL accounts. This is a fundamental accounting principle.
 * 
 * Formula: Total Debits = Total Credits
 * If not equal, there is an error in the ledger.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrialBalanceService {

    private final GLAccountRepository glAccountRepository;

    /**
     * Generate trial balance as of a specific date
     */
    @Cacheable(value = "trialBalance", key = "#asOfDate")
    public TrialBalanceResponse generateTrialBalance(LocalDate asOfDate) {
        String tenantId = RequestContext.getCurrentTenantId();
        log.info("Generating trial balance as of: {} for tenant: {}", asOfDate, tenantId);

        List<GLAccount> accounts = glAccountRepository.findByTenantIdOrderByAccountCode(tenantId);
        List<TrialBalanceLine> lines = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (GLAccount account : accounts) {
            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            if (account.getNormalBalance().equals("DEBIT")) {
                debitBalance = account.getBalance();
                totalDebit = totalDebit.add(debitBalance);
            } else {
                creditBalance = account.getBalance();
                totalCredit = totalCredit.add(creditBalance);
            }

            lines.add(new TrialBalanceLine(
                    account.getAccountCode(),
                    account.getAccountName(),
                    account.getAccountType(),
                    debitBalance,
                    creditBalance
            ));
        }

        boolean isBalanced = totalDebit.compareTo(totalCredit) == 0;

        if (!isBalanced) {
            log.error("Trial balance is unbalanced! Debits: {}, Credits: {}", totalDebit, totalCredit);
        }

        return new TrialBalanceResponse(
                asOfDate.toString(),
                lines,
                totalDebit,
                totalCredit,
                isBalanced
        );
    }
}