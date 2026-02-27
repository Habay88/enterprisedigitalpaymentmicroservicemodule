package com.edpp.identity.repository;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.model.Customer;
import jakarta.persistence.LockModeType;
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
Optional<Customer> findByCifNumber(String cifNumber);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByTaxId(String taxId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByTaxId(String taxId);

    // find by status
    List<Customer> findByStatus(CustomerStatus status);
    List<Customer> findByRiskRating(RiskRating riskRating);

    // complex queries
    @Query("SELECT c FROM Customer c WHERE c.status = :status AND c.created < :date")
    List<Customer> findStaleCustomers(@Param("status")CustomerStatus status,
                                      @Param("date")LocalDateTime date);
    //customers with incompleteKYC
    @Query("SELECT c FROM Customer c WHERE c.kycDetails.kycCompleted=false AND c.created < :cutoffDate")
    List<Customer> findCustomersWithIncompleteKYC(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Search customers(for admin dashboard interface)
    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "c.cifNumber LIKE CONCAT('%', :searchTerm, '%')")
    List<Customer> searchCustomers(@Param("searchTerm") String searchTerm);

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

