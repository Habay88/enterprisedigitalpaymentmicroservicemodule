package com.edpp.wallet.service;

import com.edpp.wallet.entity.Wallet;
import com.edpp.wallet.exception.LimitExceededException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LimitService {

    /**
     * Validate transaction against all limits
     */
    public void validateTransactionLimit(Wallet wallet, BigDecimal amount) {
        resetDailyLimitIfNeeded(wallet);
        resetMonthlyLimitIfNeeded(wallet);

        // Check per transaction limit
        if (amount.compareTo(wallet.getPerTransactionLimit()) > 0) {
            throw new LimitExceededException(
                    "Per transaction limit exceeded. Maximum: " + wallet.getPerTransactionLimit(),
                    wallet.getPerTransactionLimit(),
                    amount
            );
        }

        // Check daily limit
        BigDecimal remainingDaily = wallet.getDailyTransactionLimit().subtract(wallet.getDailySpent());
        if (amount.compareTo(remainingDaily) > 0) {
            throw new LimitExceededException(
                    "Daily limit exceeded. Remaining: " + remainingDaily,
                    wallet.getDailyTransactionLimit(),
                    wallet.getDailySpent().add(amount)
            );
        }

        // Check monthly limit
        BigDecimal remainingMonthly = wallet.getMonthlyTransactionLimit().subtract(wallet.getMonthlySpent());
        if (amount.compareTo(remainingMonthly) > 0) {
            throw new LimitExceededException(
                    "Monthly limit exceeded. Remaining: " + remainingMonthly,
                    wallet.getMonthlyTransactionLimit(),
                    wallet.getMonthlySpent().add(amount)
            );
        }
    }

    /**
     * Update spent limits after transaction
     */
    public void updateSpentLimits(Wallet wallet, BigDecimal amount) {
        wallet.setDailySpent(wallet.getDailySpent().add(amount));
        wallet.setMonthlySpent(wallet.getMonthlySpent().add(amount));
    }

    /**
     * Reset daily limit if new day
     */
    private void resetDailyLimitIfNeeded(Wallet wallet) {
        LocalDateTime lastReset = wallet.getLastDailyResetAt();
        LocalDateTime now = LocalDateTime.now();

        if (lastReset.toLocalDate().isBefore(now.toLocalDate())) {
            wallet.setDailySpent(BigDecimal.ZERO);
            wallet.setLastDailyResetAt(now);
            log.debug("Daily limit reset for wallet: {}", wallet.getWalletNumber());
        }
    }

    /**
     * Reset monthly limit if new month
     */
    private void resetMonthlyLimitIfNeeded(Wallet wallet) {
        LocalDateTime lastReset = wallet.getLastMonthlyResetAt();
        LocalDateTime now = LocalDateTime.now();

        if (lastReset.getMonth() != now.getMonth() ||
            lastReset.getYear() != now.getYear()) {
            wallet.setMonthlySpent(BigDecimal.ZERO);
            wallet.setLastMonthlyResetAt(now);
            log.debug("Monthly limit reset for wallet: {}", wallet.getWalletNumber());
        }
    }
}