package com.edpp.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edpp.identity.config.TenantConfiguration;
import com.edpp.identity.enums.TenantStatus;
import com.edpp.identity.exception.TenantNotFoundException;
import com.edpp.identity.model.Tenant;
import com.edpp.identity.repository.TenantRepository;
import com.edpp.identity.tenant.TenantContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {
    
    private final TenantRepository tenantRepository;
    
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        log.info("Creating new tenant: {}", tenant.getTenantId());
        
        // Generate unique tenant ID if not provided
        if (tenant.getTenantId() == null) {
            tenant.setTenantId(generateTenantId(tenant.getName()));
        }
        
        // Set initial status
        tenant.setStatus(TenantStatus.PENDING_ACTIVATION);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setCreatedBy("SYSTEM");
        
        return tenantRepository.save(tenant);
    }
    
    @Cacheable(value = "tenants", key = "#tenantId")
    public Tenant getTenantByTenantId(String tenantId) {
        log.debug("Fetching tenant by ID: {}", tenantId);
        
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("tenantId", tenantId));
    }
    
    @Cacheable(value = "tenants", key = "#schemaName")
    public Tenant getTenantBySchemaName(String schemaName) {
        return tenantRepository.findBySchemaName(schemaName)
                .orElseThrow(() -> new TenantNotFoundException("schemaName", schemaName));
    }
    
    @Cacheable(value = "tenants", key = "#domain")
    public Tenant getTenantByDomain(String domain) {
        return tenantRepository.findByDomain(domain)
                .orElseThrow(() -> new TenantNotFoundException("domain", domain));
    }
    
    public List<Tenant> getAllActiveTenants() {
        return tenantRepository.findAllActiveTenants();
    }
    
    @Transactional
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant updateTenantStatus(String tenantId, TenantStatus newStatus, String reason) {
        Tenant tenant = getTenantByTenantId(tenantId);
        TenantStatus oldStatus = tenant.getStatus();
        
        tenant.setStatus(newStatus);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenant.setUpdatedBy("SYSTEM");
        
        log.info("Tenant {} status changed from {} to {} - Reason: {}", 
                tenantId, oldStatus, newStatus, reason);
        
        return tenantRepository.save(tenant);
    }
    
    @Transactional
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant updateTenantConfiguration(String tenantId, TenantConfiguration config) {
        Tenant tenant = getTenantByTenantId(tenantId);
        tenant.setConfiguration(config);
        tenant.setUpdatedAt(LocalDateTime.now());
        
        return tenantRepository.save(tenant);
    }
    
    public boolean validateTenantAccess(String tenantId) {
        String currentTenant = TenantContext.getTenantId();
        
        if (currentTenant == null) {
            log.warn("No tenant context found for request");
            return false;
        }
        
        // Check if tenant exists and is active
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        
        if (tenant == null) {
            log.warn("Tenant not found: {}", tenantId);
            return false;
        }
        
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            log.warn("Tenant is not active: {} - Status: {}", tenantId, tenant.getStatus());
            return false;
        }
        
        return currentTenant.equals(tenantId);
    }
    
    private String generateTenantId(String companyName) {
        // Generate tenant ID from company name (e.g., "Bank A" -> "BANK_A")
        String baseId = companyName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .trim();
        
        // Add unique suffix if needed
        if (tenantRepository.existsByTenantId(baseId)) {
            return baseId + "_" + UUID.randomUUID().toString().substring(0, 4);
        }
        
        return baseId;
    }
}