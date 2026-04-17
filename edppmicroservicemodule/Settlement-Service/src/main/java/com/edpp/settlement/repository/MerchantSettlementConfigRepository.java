package com.edpp.settlement.repository;

import com.edpp.settlement.entity.MerchantSettlementConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantSettlementConfigRepository extends JpaRepository<MerchantSettlementConfig, String> {

    Optional<MerchantSettlementConfig> findByMerchantIdAndTenantId(String merchantId, String tenantId);
}