package com.edpp.settlement.repository;

import com.edpp.settlement.entity.SettlementTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementTransactionRepository extends JpaRepository<SettlementTransaction, String> {

    List<SettlementTransaction> findBySettlementId(String settlementId);

    Optional<SettlementTransaction> findByTransactionId(String transactionId);

    List<SettlementTransaction> findByMerchantId(String merchantId);
}