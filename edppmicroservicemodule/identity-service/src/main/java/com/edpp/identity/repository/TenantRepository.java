package com.edpp.identity.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edpp.identity.enums.TenantStatus;
import com.edpp.identity.enums.TenantType;
import com.edpp.identity.model.Tenant;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Tenant entities
 * Handles multitenancy configuration and tenant-specific operations
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {
    
    // ==================== Basic Find Operations ====================
    
    /**
     * Find tenant by unique tenant ID
     */
    Optional<Tenant> findByTenantId(String tenantId);
    
    /**
     * Find tenant by schema name
     */
    Optional<Tenant> findBySchemaName(String schemaName);
    
    /**
     * Find tenant by domain
     */
    Optional<Tenant> findByDomain(String domain);
    
    /**
     * Find tenant by name (partial match, case insensitive)
     */
    List<Tenant> findByNameContainingIgnoreCase(String name);
    
    // ==================== Status-Based Queries ====================
    
    /**
     * Find all tenants with specific status
     */
    List<Tenant> findByStatus(TenantStatus status);
    
    /**
     * Find active tenants
     */
    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE'")
    List<Tenant> findAllActiveTenants();
    
    /**
     * Find tenants created after specific date
     */
    List<Tenant> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find tenants that haven't been updated recently (potential stale tenants)
     */
    @Query("SELECT t FROM Tenant t WHERE t.updatedAt < :date")
    List<Tenant> findTenantsNotUpdatedSince(@Param("date") LocalDateTime date);
    
    // ==================== Existence Checks ====================
    
    /**
     * Check if tenant ID exists
     */
    boolean existsByTenantId(String tenantId);
    
    /**
     * Check if schema name exists
     */
    boolean existsBySchemaName(String schemaName);
    
    /**
     * Check if domain exists
     */
    boolean existsByDomain(String domain);
    
    // ==================== Type-Based Queries ====================
    
    /**
     * Find tenants by type
     */
    List<Tenant> findByTenantType(TenantType tenantType);
    
    /**
     * Find tenants by type and status
     */
    List<Tenant> findByTenantTypeAndStatus(TenantType tenantType, TenantStatus status);
    
    // ==================== Configuration Queries ====================
    
    /**
     * Find tenants with specific configuration settings
     */
    @Query("SELECT t FROM Tenant t WHERE t.configuration.enableBvnValidation = :enableBvn")
    List<Tenant> findByBvnValidationEnabled(@Param("enableBvn") boolean enableBvn);
    
    /**
     * Find tenants supporting specific currency
     */
    @Query("SELECT t FROM Tenant t WHERE t.configuration.supportedCurrencies LIKE %:currency%")
    List<Tenant> findBySupportedCurrency(@Param("currency") String currency);
    
    // ==================== Count Queries ====================
    
    /**
     * Count tenants by status
     */
    @Query("SELECT t.status, COUNT(t) FROM Tenant t GROUP BY t.status")
    List<Object[]> countByStatus();
    
    /**
     * Count tenants by type
     */
    @Query("SELECT t.tenantType, COUNT(t) FROM Tenant t GROUP BY t.tenantType")
    List<Object[]> countByType();
    
    // ==================== Search Operations ====================
    
    /**
     * Search tenants by various fields
     */
    @Query("SELECT t FROM Tenant t WHERE " +
           "LOWER(t.tenantId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.domain) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tenant> searchTenants(@Param("searchTerm") String searchTerm);
    
    // ==================== Locking Operations ====================
    
    /**
     * Find tenant with pessimistic lock for updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tenant t WHERE t.tenantId = :tenantId")
    Optional<Tenant> findByTenantIdWithLock(@Param("tenantId") String tenantId);
    
    // ==================== Batch Operations ====================
    
    /**
     * Find tenants with expired configurations or needing review
     */
    @Query("SELECT t FROM Tenant t WHERE t.updatedAt < :cutoffDate OR t.status = 'MAINTENANCE'")
    List<Tenant> findTenantsNeedingReview(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // ==================== Native Queries ====================
    
    /**
     * Get tenant database connection details (using native query for sensitive data)
     */
    @Query(value = "SELECT tenant_id, database_url, database_username FROM tenants WHERE status = 'ACTIVE'", 
           nativeQuery = true)
    List<Object[]> getActiveTenantConnectionDetails();
    
    /**
     * Update tenant status (batch update)
     */
    @Query("UPDATE Tenant t SET t.status = :newStatus WHERE t.status = :oldStatus")
    int bulkUpdateTenantStatus(@Param("oldStatus") TenantStatus oldStatus, 
                               @Param("newStatus") TenantStatus newStatus);
}

