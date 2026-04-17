package com.edpp.merchant.repository;

import com.edpp.merchant.entity.Merchant;
import com.edpp.merchant.enums.MerchantCategory;
import com.edpp.merchant.enums.MerchantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {

    Optional<Merchant> findByMerchantCode(String merchantCode);

    Optional<Merchant> findByEmailAndTenantId(String email, String tenantId);

    List<Merchant> findByStatus(MerchantStatus status);

    Page<Merchant> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    @Query("SELECT m FROM Merchant m WHERE m.tenantId = :tenantId AND " +
           "(LOWER(m.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.merchantCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Merchant> searchMerchants(@Param("tenantId") String tenantId,
                                    @Param("search") String search,
                                    Pageable pageable);

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.status = :status AND m.tenantId = :tenantId")
    long countByStatus(@Param("status") MerchantStatus status,
                       @Param("tenantId") String tenantId);

    @Query("SELECT m.category, COUNT(m) FROM Merchant m WHERE m.tenantId = :tenantId GROUP BY m.category")
    List<Object[]> countByCategory(@Param("tenantId") String tenantId);
}