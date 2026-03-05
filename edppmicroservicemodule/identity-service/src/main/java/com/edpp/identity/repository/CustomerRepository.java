package com.edpp.identity.repository;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.model.Customer;
import com.edpp.identity.tenant.TenantContext;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer,String> {

    // Tenant-aware queries - automatically filter by tenant_id

    default Optional<Customer> findByCifNumber(String cifNumber) {
        return findByCifNumberAndTenantId(cifNumber, TenantContext.getTenantId());
    }

    Optional<Customer> findByCifNumberAndTenantId(String cifNumber, String tenantId);

    default Optional<Customer> findByEmail(String email) {
        return findByEmailAndTenantId(email, TenantContext.getTenantId());
    }

    Optional<Customer> findByEmailAndTenantId(String email, String tenantId);

    default Optional<Customer> findByBvn(String bvn) {
        return findByBvnAndTenantId(bvn, TenantContext.getTenantId());
    }

    Optional<Customer> findByBvnAndTenantId(String bvn, String tenantId);

    default Optional<Customer> findByNin(String nin) {
        return findByNinAndTenantId(nin, TenantContext.getTenantId());
    }

    Optional<Customer> findByNinAndTenantId(String nin, String tenantId);

    default boolean existsByEmail(String email) {
        return existsByEmailAndTenantId(email, TenantContext.getTenantId());
    }

    boolean existsByEmailAndTenantId(String email, String tenantId);

    default boolean existsByBvn(String bvn) {
        return existsByBvnAndTenantId(bvn, TenantContext.getTenantId());
    }

    boolean existsByBvnAndTenantId(String bvn, String tenantId);

    default boolean existsByNin(String nin) {
        return existsByNinAndTenantId(nin, TenantContext.getTenantId());
    }

    boolean existsByNinAndTenantId(String nin, String tenantId);

    // Find customers with unverified BVN
    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.bvnVerification.verified = false AND c.bvn IS NOT NULL")
    List<Customer> findCustomersWithUnverifiedBvn(@Param("tenantId") String tenantId);

    // Find customers with unverified NIN
    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.ninVerification.verified = false AND c.nin IS NOT NULL")
    List<Customer> findCustomersWithUnverifiedNin(@Param("tenantId") String tenantId);

    // Search across tenant
    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND " +
            "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "c.cifNumber LIKE CONCAT('%', :searchTerm, '%') OR " +
            "c.bvn LIKE CONCAT('%', :searchTerm, '%') OR " +
            "c.nin LIKE CONCAT('%', :searchTerm, '%'))")
    Page<Customer> searchByTenant(@Param("tenantId") String tenantId,
                                  @Param("searchTerm") String searchTerm,
                                  Pageable pageable);
    // complex queries
    @Query("SELECT c FROM Customer c WHERE c.status = :status AND c.created < :date")
    List<Customer> findStaleCustomers(@Param("status")CustomerStatus status,
                                      @Param("date")LocalDateTime date);
    //customers with incompleteKYC
    @Query("SELECT c FROM Customer c WHERE c.kycDetails.kycCompleted=false AND c.created < :cutoffDate")
    List<Customer> findCustomersWithIncompleteKYC(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Search customers(for admin dashboard interface)
/*
    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "c.cifNumber LIKE CONCAT('%', :searchTerm, '%')")
    List<Customer> searchCustomers(@Param("searchTerm") String searchTerm);
*/

    // Count by status (for dashboard)
    @Query("SELECT c.status, COUNT(c) FROM Customer c GROUP BY c.status")
    List<Object[]> countByStatus();

    // Find customers for risk review
    @Query("SELECT c FROM Customer c WHERE c.riskRating IN :ratings AND c.kycDetails.kycCompleted = true")
    List<Customer> findCustomersForRiskReview(@Param("ratings") List<RiskRating> ratings);

    // find recently updated customers with pessimistic lock concurrent updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdWithLock(@Param("id")String id);

    // find customers with high value transactions(joined with transaction service)
    @Query(value="SELECT DISTINCT c.* FROM customers c " +
                "JOIN wallets w ON w.customer_id = c.id " +
                "JOIN transactions t ON t.source_wallet_id = w.id " +
                "WHERE t.amount > :threshold AND t.created_at > :since",
                 nativeQuery = true)
    List<Customer> findHighValueCustomers(@Param("threshold") BigDecimal threshold,
                                          @Param("since") LocalDateTime since);
}

