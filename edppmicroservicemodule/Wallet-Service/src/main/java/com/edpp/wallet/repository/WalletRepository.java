package com.edpp.wallet.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edpp.wallet.entity.Wallet;
import com.edpp.wallet.enums.WalletStatus;
import com.edpp.wallet.enums.WalletType;

import jakarta.persistence.LockModeType;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
Optional<Wallet> findByWalletNumberAndTenantId(String walletNumber, String tenantId);

 List<Wallet> findByCustomerIdAndTenantId(String customerId, String tenantId);

    Optional<Wallet> findByCustomerIdAndWalletTypeAndTenantId(String customerId, 
                                                              WalletType walletType, 
                                                              String tenantId);

    List<Wallet> findByStatus(WalletStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") String id);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount, " +
           "w.availableBalance = w.availableBalance + :amount, " +
           "w.ledgerBalance = w.ledgerBalance + :amount " +
           "WHERE w.id = :id")
    int updateBalance(@Param("id") String id, @Param("amount") BigDecimal amount);

    @Query("SELECT SUM(w.balance) FROM Wallet w WHERE w.customerId = :customerId AND w.tenantId = :tenantId")
    BigDecimal getTotalBalanceForCustomer(@Param("customerId") String customerId,
                                          @Param("tenantId") String tenantId);
}
