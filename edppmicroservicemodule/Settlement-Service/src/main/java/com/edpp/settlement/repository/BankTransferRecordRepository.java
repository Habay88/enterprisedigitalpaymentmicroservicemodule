package com.edpp.settlement.repository;

import com.edpp.settlement.entity.BankTransferRecord;
import com.edpp.settlement.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Bank Transfer Record Repository - Data access for bank transfer records
 * 
 * Manages tracking of actual bank transfers to merchant accounts.
 */
@Repository
public interface BankTransferRecordRepository extends JpaRepository<BankTransferRecord, String> {

    /**
     * Find transfer by unique reference
     */
    Optional<BankTransferRecord> findByTransferReference(String transferReference);

    /**
     * Find transfers by settlement ID
     */
    List<BankTransferRecord> findBySettlementId(String settlementId);

    /**
     * Find transfers by merchant ID
     */
    List<BankTransferRecord> findByMerchantId(String merchantId);

    /**
     * Find transfers by status
     */
    List<BankTransferRecord> findByStatus(TransferStatus status);

    /**
     * Find pending transfers that need processing
     */
    @Query("SELECT b FROM BankTransferRecord b WHERE b.status = 'PENDING' OR " +
           "(b.status = 'FAILED' AND b.retryCount < 3)")
    List<BankTransferRecord> findPendingAndRetryableTransfers();

    /**
     * Find transfers by date range
     */
    List<BankTransferRecord> findByInitiatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Update transfer status
     */
    @Modifying
    @Query("UPDATE BankTransferRecord b SET b.status = :status, b.completedAt = :completedAt " +
           "WHERE b.id = :id")
    int updateTransferStatus(@Param("id") String id, 
                             @Param("status") TransferStatus status,
                             @Param("completedAt") LocalDateTime completedAt);

    /**
     * Increment retry count for failed transfers
     */
    @Modifying
    @Query("UPDATE BankTransferRecord b SET b.retryCount = b.retryCount + 1 " +
           "WHERE b.id = :id")
    int incrementRetryCount(@Param("id") String id);
}