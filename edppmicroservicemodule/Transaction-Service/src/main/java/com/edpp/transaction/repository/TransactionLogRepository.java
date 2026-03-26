package com.edpp.transaction.repository;

import com.edpp.transaction.entity.TransactionLog;
import com.edpp.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {

    // Returns List - for non-paginated queries
    List<TransactionLog> findByTransactionIdOrderByCreatedAtDesc(String transactionId);
    
    List<TransactionLog> findByTransactionReferenceOrderByCreatedAtDesc(String transactionReference);
    
    List<TransactionLog> findByNewStatus(TransactionStatus status);
    
    List<TransactionLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<TransactionLog> findByMessageContainingIgnoreCase(String keyword);
    
    // Returns Page - for paginated queries
    Page<TransactionLog> findByTransactionId(String transactionId, Pageable pageable);
    
    Page<TransactionLog> findByMessageContainingIgnoreCase(String keyword, Pageable pageable);
    
    // Delete operation
    @Modifying
    @Transactional
    @Query("DELETE FROM TransactionLog l WHERE l.createdAt < :cutoffDate")
    long deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}