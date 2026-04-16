package com.edpp.ledger.repository;

import com.edpp.ledger.entity.JournalEntry;
import com.edpp.ledger.enums.JournalEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, String> {

    // Find by reference
    Optional<JournalEntry> findByReference(String reference);
    
    // Find by transaction ID
    Optional<JournalEntry> findByTransactionId(String transactionId);
    
    // Find multiple by transaction IDs
    List<JournalEntry> findByTransactionIdIn(List<String> transactionIds);

    // Paginated queries
    Page<JournalEntry> findByTenantIdOrderByEntryDateDesc(String tenantId, Pageable pageable);
    
    // Date range queries
    List<JournalEntry> findByEntryDateBetweenAndTenantId(LocalDateTime start, LocalDateTime end, String tenantId);
    
    // Status queries
    List<JournalEntry> findByStatusAndTenantId(JournalEntryStatus status, String tenantId);

    // Aggregation queries
    @Query("SELECT SUM(j.totalDebit) FROM JournalEntry j WHERE j.tenantId = :tenantId AND j.entryDate BETWEEN :start AND :end AND j.status = 'POSTED'")
    BigDecimal getTotalDebitForPeriod(@Param("tenantId") String tenantId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("SELECT SUM(j.totalCredit) FROM JournalEntry j WHERE j.tenantId = :tenantId AND j.entryDate BETWEEN :start AND :end AND j.status = 'POSTED'")
    BigDecimal getTotalCreditForPeriod(@Param("tenantId") String tenantId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
}