package com.edpp.ledger.repository;

import com.edpp.ledger.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Journal Line Repository - Data access for journal line entities
 * 
 * Provides methods to query journal lines by various criteria
 * including journal entry, account, and date ranges.
 */
@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, String> {

    /**
     * Find all lines for a specific journal entry
     */
    List<JournalLine> findByJournalEntryId(String journalEntryId);

    /**
     * Find all lines for a specific account
     */
    List<JournalLine> findByAccountId(String accountId);

    /**
     * Find all lines for a specific account within a date range
     */
    @Query("SELECT l FROM JournalLine l WHERE l.account.id = :accountId " +
           "AND l.journalEntry.entryDate BETWEEN :startDate AND :endDate")
    List<JournalLine> findByAccountIdAndDateRange(@Param("accountId") String accountId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total debit amount for an account within a date range
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM JournalLine l " +
           "WHERE l.account.id = :accountId AND l.direction = 'DEBIT' " +
           "AND l.journalEntry.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalDebitForAccount(@Param("accountId") String accountId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total credit amount for an account within a date range
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM JournalLine l " +
           "WHERE l.account.id = :accountId AND l.direction = 'CREDIT' " +
           "AND l.journalEntry.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCreditForAccount(@Param("accountId") String accountId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Find lines by multiple journal entry IDs (batch query)
     */
    List<JournalLine> findByJournalEntryIdIn(List<String> journalEntryIds);

    /**
     * Delete all lines for a specific journal entry
     */
    void deleteByJournalEntryId(String journalEntryId);
}