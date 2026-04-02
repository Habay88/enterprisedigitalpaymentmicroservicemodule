package com.edpp.wallet.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.edpp.wallet.entity.WalletTransaction;
import com.edpp.wallet.enums.TransactionType;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {

     Optional<WalletTransaction> findByReference(String reference);

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(String walletId, Pageable pageable);

    Page<WalletTransaction> findByWalletNumberAndTenantIdOrderByCreatedAtDesc(String walletNumber, 
                                                                              String tenantId, 
                                                                              Pageable pageable);

    List<WalletTransaction> findByWalletIdAndCreatedAtBetween(String walletId, 
                                                              LocalDateTime start, 
                                                              LocalDateTime end);

    @Query("SELECT SUM(w.amount) FROM WalletTransaction w WHERE w.walletId = :walletId " +
           "AND w.type = :type AND w.createdAt >= :since AND w.status = 'COMPLETED'")
    BigDecimal getTotalAmountByTypeSince(@Param("walletId") String walletId,
                                         @Param("type") TransactionType type,
                                         @Param("since") LocalDateTime since);

}
