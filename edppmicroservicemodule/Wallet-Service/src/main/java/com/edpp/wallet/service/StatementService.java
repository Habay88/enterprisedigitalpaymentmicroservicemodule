package com.edpp.wallet.service;

import com.edpp.wallet.dto.response.StatementEntry;
import com.edpp.wallet.dto.response.StatementResponse;
import com.edpp.wallet.entity.Wallet;
import com.edpp.wallet.entity.WalletTransaction;
import com.edpp.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final WalletTransactionRepository transactionRepository;
    private final WalletService walletService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generate statement for a wallet
     */
    public StatementResponse generateStatement(String walletNumber, LocalDateTime start, LocalDateTime end) {
        String tenantId = com.edpp.wallet.util.RequestContext.getCurrentTenantId();
        Wallet wallet = walletService.getWallet(walletNumber, tenantId);

        List<WalletTransaction> transactions = transactionRepository.findByWalletIdAndCreatedAtBetween(
                wallet.getId(), start, end);

        // Calculate totals
        BigDecimal totalCredit = transactions.stream()
                .filter(t -> t.getType().name().equals("CREDIT"))
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebit = transactions.stream()
                .filter(t -> t.getType().name().equals("DEBIT"))
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get opening balance (balance before start date)
        BigDecimal openingBalance = getBalanceBefore(wallet.getId(), start);

        // Calculate closing balance
        BigDecimal closingBalance = openingBalance.add(totalCredit).subtract(totalDebit);

        List<StatementEntry> entries = transactions.stream()
                .map(t -> new StatementEntry(
                        t.getId(),
                        t.getReference(),
                        t.getType().name(),
                        t.getAmount(),
                        t.getBalanceAfter(),
                        t.getDescription(),
                        t.getCreatedAt()
                ))
                .toList();

        String period = start.format(DATE_FORMATTER) + " to " + end.format(DATE_FORMATTER);

        return new StatementResponse(
                wallet.getWalletNumber(),
                wallet.getCustomerId(),
                period,
                openingBalance,
                closingBalance,
                totalCredit,
                totalDebit,
                entries
        );
    }

    /**
     * Get recent transactions (last N)
     */
    public List<StatementEntry> getRecentTransactions(String walletNumber, int limit) {
        String tenantId = com.edpp.wallet.util.RequestContext.getCurrentTenantId();
        Wallet wallet = walletService.getWallet(walletNumber, tenantId);

        Pageable pageable = PageRequest.of(0, limit);
        var transactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);

        return transactions.stream()
                .map(t -> new StatementEntry(
                        t.getId(),
                        t.getReference(),
                        t.getType().name(),
                        t.getAmount(),
                        t.getBalanceAfter(),
                        t.getDescription(),
                        t.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Get balance before a specific date
     */
    private BigDecimal getBalanceBefore(String walletId, LocalDateTime date) {
        var lastTransaction = transactionRepository.findByWalletIdAndCreatedAtBetween(
                walletId, LocalDateTime.MIN, date).stream()
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastTransaction != null) {
            return lastTransaction.getBalanceAfter();
        }
        return BigDecimal.ZERO;
    }
}