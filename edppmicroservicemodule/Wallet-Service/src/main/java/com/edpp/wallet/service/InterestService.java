package com.edpp.wallet.service;

import com.edpp.wallet.entity.Wallet;
import com.edpp.wallet.enums.WalletType;
import com.edpp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestService {

    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final KafkaProducerService kafkaProducerService;

    // Annual interest rates (APR)
    private static final BigDecimal SAVINGS_INTEREST_RATE = new BigDecimal("0.04"); // 4%
    private static final BigDecimal CURRENT_INTEREST_RATE = new BigDecimal("0.01"); // 1%
    private static final BigDecimal ESCROW_INTEREST_RATE = BigDecimal.ZERO;

    /**
     * Calculate and apply interest for all savings wallets
     * Runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void applyDailyInterest() {
        log.info("Starting daily interest calculation");

        List<Wallet> savingsWallets = walletRepository.findByWalletType(WalletType.SAVINGS);

        for (Wallet wallet : savingsWallets) {
            try {
                applyInterest(wallet);
            } catch (Exception e) {
                log.error("Failed to apply interest for wallet: {}", wallet.getWalletNumber(), e);
            }
        }

        log.info("Completed daily interest calculation for {} wallets", savingsWallets.size());
    }

    /**
     * Apply interest to a specific wallet
     */
    @Transactional
    public void applyInterest(Wallet wallet) {
        if (wallet.getStatus().name().equals("ACTIVE") && 
            wallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal interest = calculateInterest(wallet);
            if (interest.compareTo(BigDecimal.ZERO) > 0) {
                // Credit interest to wallet
                var request = new com.edpp.wallet.dto.request.CreditRequest(
                        wallet.getWalletNumber(),
                        interest,
                        generateInterestReference(wallet),
                        "Interest earned for period ending " + LocalDateTime.now().toLocalDate(),
                        null
                );

                walletService.creditWallet(request, wallet.getTenantId());
                log.info("Applied interest of {} to wallet: {}", interest, wallet.getWalletNumber());
            }
        }
    }

    /**
     * Calculate interest for a wallet
     */
    public BigDecimal calculateInterest(Wallet wallet) {
        BigDecimal rate = getInterestRate(wallet.getWalletType());

        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Daily interest = (balance * rate) / 365
        BigDecimal dailyRate = rate.divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP);
        return wallet.getBalance().multiply(dailyRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get interest rate based on wallet type
     */
    private BigDecimal getInterestRate(WalletType type) {
        return switch (type) {
            case SAVINGS -> SAVINGS_INTEREST_RATE;
            case CURRENT -> CURRENT_INTEREST_RATE;
            default -> ESCROW_INTEREST_RATE;
        };
    }

    /**
     * Generate interest reference
     */
    private String generateInterestReference(Wallet wallet) {
        return "INT_" + wallet.getWalletNumber() + "_" + 
               LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}