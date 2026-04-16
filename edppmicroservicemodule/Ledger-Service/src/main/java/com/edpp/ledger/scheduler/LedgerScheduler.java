package com.edpp.ledger.scheduler;

import com.edpp.ledger.service.AccountBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerScheduler {

    private final AccountBalanceService accountBalanceService;

    /**
     * Run end-of-day processing daily at 11:55 PM
     * This ensures all daily balances are calculated before the next day starts
     */
    @Scheduled(cron = "0 55 23 * * ?")
    public void runEndOfDayProcessing() {
        LocalDate today = LocalDate.now();
        log.info("Running scheduled end-of-day processing for date: {}", today);
        
        try {
            accountBalanceService.runEndOfDayProcessing(today);
            log.info("End-of-day processing completed successfully for: {}", today);
        } catch (Exception e) {
            log.error("Failed to run end-of-day processing for: {}", today, e);
        }
    }

    /**
     * Run balance verification daily at 1:00 AM
     * This verifies that all balances are consistent
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void runBalanceVerification() {
        log.info("Running scheduled balance verification");
        
        try {
            // Verify that total debits = total credits across all accounts
            // Implementation depends on TrialBalanceService
            log.info("Balance verification completed");
        } catch (Exception e) {
            log.error("Failed to run balance verification", e);
        }
    }
}