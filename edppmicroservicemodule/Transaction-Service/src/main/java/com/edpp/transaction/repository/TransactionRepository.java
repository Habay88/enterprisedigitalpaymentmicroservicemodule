package com.edpp.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.edpp.transaction.entity.Transaction;
import com.edpp.transaction.enums.TransactionStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // Basic find operations
    Optional<Transaction> findByTransactionReference(String transactionReference);
    
    Optional<Transaction> findByMerchantTransactionIdAndTenantId(String merchantTransactionId, String tenantId);
    
    Optional<Transaction> findByProcessorTransactionId(String processorTransactionId);

    // Tenant-aware queries
    Page<Transaction> findByTenantId(String tenantId, Pageable pageable);
    
    List<Transaction> findByTenantIdAndCustomerId(String tenantId, String customerId);
    
    Page<Transaction> findByTenantIdAndSourceWalletId(String tenantId, String sourceWalletId, Pageable pageable);

    // Status-based queries
    List<Transaction> findByStatus(TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt < :cutoffTime")
    List<Transaction> findStaleTransactions(@Param("status") TransactionStatus status, 
                                            @Param("cutoffTime") LocalDateTime cutoffTime);

    // Date range queries
    List<Transaction> findByTenantIdAndCreatedAtBetween(String tenantId, 
                                                        LocalDateTime start, 
                                                        LocalDateTime end);

    // Aggregation queries
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.tenantId = :tenantId AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :start AND :end")
    BigDecimal getTotalTransactionAmount(@Param("tenantId") String tenantId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.tenantId = :tenantId AND t.createdAt BETWEEN :start AND :end")
    Long getTransactionCount(@Param("tenantId") String tenantId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    // Search queries
    @Query("SELECT t FROM Transaction t WHERE t.tenantId = :tenantId AND " +
           "(LOWER(t.transactionReference) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.merchantTransactionId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customerEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.processorTransactionId) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Transaction> searchTransactions(@Param("tenantId") String tenantId,
                                         @Param("searchTerm") String searchTerm,
                                         Pageable pageable);

    // Locking for concurrent updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithLock(@Param("id") String id);

    // Statistics
    @Query("SELECT t.status, COUNT(t) FROM Transaction t WHERE t.tenantId = :tenantId GROUP BY t.status")
    List<Object[]> getTransactionStatusStats(@Param("tenantId") String tenantId);

    @Query("SELECT DATE(t.createdAt), COUNT(t), SUM(t.amount) FROM Transaction t " +
           "WHERE t.tenantId = :tenantId AND t.createdAt >= :since GROUP BY DATE(t.createdAt)")
    List<Object[]> getDailyTransactionStats(@Param("tenantId") String tenantId,
                                            @Param("since") LocalDateTime since);
}
