package com.edpp.ledger.repository;

import com.edpp.ledger.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Account Balance Repository - Data access for daily balance snapshots
 * 
 * This repository manages the daily closing balances for all GL accounts.
 * It provides methods for:
 * - Retrieving balances for specific dates
 * - Calculating balance changes over periods
 * - Batch updates for end-of-day processing
 */
@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, String> {

    /**
     * Find balance for a specific account on a specific date
     */
    Optional<AccountBalance> findByAccountIdAndBalanceDate(String accountId, LocalDate balanceDate);

    /**
     * Find all balances for a specific account within a date range
     */
    List<AccountBalance> findByAccountIdAndBalanceDateBetween(String accountId, 
                                                               LocalDate startDate, 
                                                               LocalDate endDate);

    /**
     * Find all balances for a specific tenant on a specific date
     */
    List<AccountBalance> findByTenantIdAndBalanceDate(String tenantId, LocalDate balanceDate);

    /**
     * Find the most recent balance for an account
     */
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountId = :accountId " +
           "ORDER BY ab.balanceDate DESC LIMIT 1")
    Optional<AccountBalance> findLatestBalanceByAccountId(@Param("accountId") String accountId);

    /**
     * Find opening balance for an account on a specific date
     * Returns the closing balance from the previous day
     */
    @Query("SELECT ab.closingBalance FROM AccountBalance ab " +
           "WHERE ab.accountId = :accountId AND ab.balanceDate = :previousDate")
    Optional<BigDecimal> findOpeningBalance(@Param("accountId") String accountId,
                                             @Param("previousDate") LocalDate previousDate);

    /**
     * Get all accounts that had activity on a specific date
     */
    @Query("SELECT DISTINCT ab.accountId FROM AccountBalance ab WHERE ab.balanceDate = :date")
    List<String> findActiveAccountsOnDate(@Param("date") LocalDate date);

    /**
     * Calculate total debit for an account over a period
     */
    @Query("SELECT SUM(ab.totalDebit) FROM AccountBalance ab " +
           "WHERE ab.accountId = :accountId AND ab.balanceDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalDebitByAccountAndDateRange(@Param("accountId") String accountId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * Calculate total credit for an account over a period
     */
    @Query("SELECT SUM(ab.totalCredit) FROM AccountBalance ab " +
           "WHERE ab.accountId = :accountId AND ab.balanceDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalCreditByAccountAndDateRange(@Param("accountId") String accountId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * Bulk update closing balances for end of day processing
     */
    @Modifying
    @Transactional
    @Query("UPDATE AccountBalance ab SET ab.closingBalance = :closingBalance, " +
           "ab.totalDebit = :totalDebit, ab.totalCredit = :totalCredit " +
           "WHERE ab.id = :id")
    int updateBalance(@Param("id") String id,
                      @Param("closingBalance") BigDecimal closingBalance,
                      @Param("totalDebit") BigDecimal totalDebit,
                      @Param("totalCredit") BigDecimal totalCredit);

    /**
     * Delete all balances older than a specific date (data retention)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AccountBalance ab WHERE ab.balanceDate < :cutoffDate")
    int deleteBalancesOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Check if balances exist for a specific date
     */
    boolean existsByBalanceDate(LocalDate balanceDate);

    /**
     * Get the latest balance date in the system
     */
    @Query("SELECT MAX(ab.balanceDate) FROM AccountBalance ab")
    Optional<LocalDate> findLatestBalanceDate();

    /**
     * Get balance summary for all accounts on a specific date
     */
    @Query("SELECT new map(ab.accountId as accountId, " +
           "ab.openingBalance as openingBalance, " +
           "ab.closingBalance as closingBalance, " +
           "ab.totalDebit as totalDebit, " +
           "ab.totalCredit as totalCredit) " +
           "FROM AccountBalance ab WHERE ab.balanceDate = :date AND ab.tenantId = :tenantId")
    List<Object[]> getBalanceSummaryForDate(@Param("tenantId") String tenantId,
                                             @Param("date") LocalDate date);

    /**
     * Get accounts with non-zero balance on a specific date
     */
    @Query("SELECT ab FROM AccountBalance ab " +
           "WHERE ab.balanceDate = :date AND ab.closingBalance <> 0 " +
           "ORDER BY ab.closingBalance DESC")
    List<AccountBalance> findAccountsWithNonZeroBalance(@Param("date") LocalDate date);
}