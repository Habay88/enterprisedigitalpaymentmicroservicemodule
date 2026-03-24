package com.edpp.transaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.edpp.transaction.entity.TransactionLog;
import com.edpp.transaction.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    
    List<TransactionLog> findByTransactionIdOrderByCreatedAtDesc(String transactionId);
    
    List<TransactionLog> findByTransactionReferenceOrderByCreatedAtDesc(String transactionReference);

    Page<TransactionLog> findByTransactionId(String transactionId, Pageable pageable);

    List<TransactionLog> findByNewStatus(TransactionStatus status);

    List<TransactionLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long deleteByCreatedAtBefore(LocalDateTime olderThan);
}
