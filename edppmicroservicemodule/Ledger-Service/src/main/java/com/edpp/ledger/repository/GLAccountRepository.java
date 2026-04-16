package com.edpp.ledger.repository;

import com.edpp.ledger.entity.GLAccount;
import com.edpp.ledger.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GLAccountRepository extends JpaRepository<GLAccount, String> {

    // Basic queries
    Optional<GLAccount> findByAccountCodeAndTenantId(String accountCode, String tenantId);
    
    List<GLAccount> findByTenantIdOrderByAccountCode(String tenantId);
    
    List<GLAccount> findByAccountTypeAndTenantId(AccountType accountType, String tenantId);
    
    List<GLAccount> findByActiveTrueAndTenantId(String tenantId);

    // Balance aggregation
    @Query("SELECT SUM(a.balance) FROM GLAccount a WHERE a.accountType = :type AND a.tenantId = :tenantId")
    BigDecimal getTotalBalanceByType(@Param("type") AccountType type, @Param("tenantId") String tenantId);

    // Parent-child relationships
    List<GLAccount> findByParentAccountCodeAndTenantId(String parentAccountCode, String tenantId);

    // Lock for concurrent updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM GLAccount a WHERE a.id = :id")
    Optional<GLAccount> findByIdWithLock(@Param("id") String id);

    // Bulk update
    @Modifying
    @Query("UPDATE GLAccount a SET a.balance = a.balance + :amount WHERE a.id = :id")
    int updateBalance(@Param("id") String id, @Param("amount") BigDecimal amount);
}