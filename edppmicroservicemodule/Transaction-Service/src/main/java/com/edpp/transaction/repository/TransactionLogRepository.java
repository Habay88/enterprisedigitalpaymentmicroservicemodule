package com.edpp.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.edpp.transaction.entity.TransactionLog;

import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    
    List<TransactionLog> findByTransactionIdOrderByCreatedAtDesc(String transactionId);
    
    List<TransactionLog> findByTransactionReferenceOrderByCreatedAtDesc(String transactionReference);
}
