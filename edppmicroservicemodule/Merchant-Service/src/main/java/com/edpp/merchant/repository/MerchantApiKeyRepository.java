package com.edpp.merchant.repository;

import com.edpp.merchant.entity.MerchantApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, String> {

    Optional<MerchantApiKey> findByPublicKey(String publicKey);

    Optional<MerchantApiKey> findByMerchantIdAndIsActiveTrue(String merchantId);

    @Modifying
    @Query("UPDATE MerchantApiKey k SET k.lastUsedAt = :lastUsedAt, k.lastUsedIp = :ip WHERE k.id = :id")
    int updateLastUsed(@Param("id") String id,
                       @Param("lastUsedAt") LocalDateTime lastUsedAt,
                       @Param("ip") String ip);

    @Modifying
    @Query("UPDATE MerchantApiKey k SET k.isActive = false WHERE k.expiresAt < :now")
    int deactivateExpiredKeys(@Param("now") LocalDateTime now);
}