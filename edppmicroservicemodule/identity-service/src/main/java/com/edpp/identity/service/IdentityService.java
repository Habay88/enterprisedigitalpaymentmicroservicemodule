package com.edpp.identity.service;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.exception.CustomerBlockedException;
import com.edpp.identity.exception.CustomerNotFoundException;
import com.edpp.identity.exception.DuplicateCustomerException;
import com.edpp.identity.model.Customer;
import com.edpp.identity.model.KycDetails;
import com.edpp.identity.repository.CustomerRepository;
import com.edpp.identity.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityService {
    
    private final CustomerRepository customerRepository;
    private final FraudDetectionService fraudDetectionService;
    private final AuditService auditService;
    private final IdentityValidationService identityValidationService;
    
    @Transactional
    public Customer onboardCustomer(Customer customer) {
        String tenantId = TenantContext.getTenantId();
        log.info("Onboarding new customer for tenant: {} with email: {}", tenantId, customer.getEmail());
        
        // Check for duplicates within tenant
        if (customerRepository.existsByEmailAndTenantId(customer.getEmail(), tenantId)) {
            auditService.logFailedLogin(customer.getEmail(), "DUPLICATE_REGISTRATION");
            throw new DuplicateCustomerException("email", customer.getEmail());
        }
        
        if (customer.getPhoneNumber() != null && 
            customerRepository.existsByPhoneNumberAndTenantId(customer.getPhoneNumber(), tenantId)) {
            throw new DuplicateCustomerException("phone", customer.getPhoneNumber());
        }
        
        // Check BVN uniqueness within tenant
        if (customer.getBvn() != null && 
            customerRepository.existsByBvnAndTenantId(customer.getBvn(), tenantId)) {
            throw new DuplicateCustomerException("bvn", customer.getBvn());
        }
        
        // Check NIN uniqueness within tenant
        if (customer.getNin() != null && 
            customerRepository.existsByNinAndTenantId(customer.getNin(), tenantId)) {
            throw new DuplicateCustomerException("nin", customer.getNin());
        }
        
        // Generate CIF number (unique within tenant)
        customer.setCifNumber(generateCifNumber(tenantId));
        
        // Verify identity if BVN/NIN provided
        if (customer.getBvn() != null || customer.getNin() != null) {
            customer = identityValidationService.performIdentityVerification(
                customer, customer.getBvn(), customer.getNin()
            );
        }
        
        // Perform initial risk assessment
        var initialRisk = fraudDetectionService.assessInitialRisk(customer);
        customer.setRiskRating(initialRisk);
        
        // Set initial status
        customer.setStatus(CustomerStatus.PENDING_ACTIVATION);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setTenantId(tenantId);
        
        Customer savedCustomer = customerRepository.save(customer);
        
        // Audit log
        auditService.logCustomerOnboarding(savedCustomer);
        
        return savedCustomer;
    }
    
    @Cacheable(value = "customers", key = "#cifNumber + '_' + T(com.edpp.identity.tenant.TenantContext).getTenantId()")
    public Customer getCustomerByCif(String cifNumber) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching customer by CIF: {} for tenant: {}", cifNumber, tenantId);
        
        return customerRepository.findByCifNumberAndTenantId(cifNumber, tenantId)
            .orElseThrow(() -> new CustomerNotFoundException("cifNumber", cifNumber));
    }
    
    @Cacheable(value = "customers", key = "#email + '_' + T(com.edpp.identity.tenant.TenantContext).getTenantId()")
    public Customer getCustomerByEmail(String email) {
        String tenantId = TenantContext.getTenantId();
        
        return customerRepository.findByEmailAndTenantId(email, tenantId)
            .orElseThrow(() -> new CustomerNotFoundException("email", email));
    }
    
    public Customer getCustomerByBvn(String bvn) {
        String tenantId = TenantContext.getTenantId();
        
        return customerRepository.findByBvnAndTenantId(bvn, tenantId)
            .orElseThrow(() -> new CustomerNotFoundException("bvn", bvn));
    }
    
    public Customer getCustomerByNin(String nin) {
        String tenantId = TenantContext.getTenantId();
        
        return customerRepository.findByNinAndTenantId(nin, tenantId)
            .orElseThrow(() -> new CustomerNotFoundException("nin", nin));
    }
    
    @Transactional
    @CacheEvict(value = "customers", key = "#cifNumber + '_' + T(com.edpp.identity.tenant.TenantContext).getTenantId()")
    public Customer updateKycStatus(String cifNumber, KycDetails kycDetails, String updatedBy) {
        Customer customer = getCustomerByCif(cifNumber);
        
        if (customer.getStatus() == CustomerStatus.BLOCKED) {
            throw new CustomerBlockedException(customer.getId(), "Cannot update KYC for blocked customer");
        }
        
        customer.setKycDetails(kycDetails);
        customer.setUpdatedBy(updatedBy);
        customer.setUpdatedAt(LocalDateTime.now());
        
        if (kycDetails.isKycCompleted()) {
            customer.setStatus(CustomerStatus.ACTIVE);
        }
        
        Customer updatedCustomer = customerRepository.save(customer);
        auditService.logKycUpdate(updatedCustomer, updatedBy);
        
        return updatedCustomer;
    }
    
    @Transactional
    @CacheEvict(value = "customers", key = "#cifNumber + '_' + T(com.edpp.identity.tenant.TenantContext).getTenantId()")
    public Customer blockCustomer(String cifNumber, String reason, String blockedBy) {
        Customer customer = getCustomerByCif(cifNumber);
        CustomerStatus oldStatus = customer.getStatus();
        
        customer.setStatus(CustomerStatus.BLOCKED);
        customer.setBlockedReason(reason);
        customer.setUpdatedBy(blockedBy);
        customer.setUpdatedAt(LocalDateTime.now());
        
        Customer blockedCustomer = customerRepository.save(customer);
        
        auditService.logStatusChange(
            cifNumber, oldStatus, CustomerStatus.BLOCKED, reason, blockedBy
        );
        
        return blockedCustomer;
    }
    
    @Transactional
    @CacheEvict(value = "customers", key = "#cifNumber + '_' + T(com.edpp.identity.tenant.TenantContext).getTenantId()")
    public Customer unblockCustomer(String cifNumber, String reason, String unblockedBy) {
        Customer customer = getCustomerByCif(cifNumber);
        CustomerStatus oldStatus = customer.getStatus();
        
        if (oldStatus != CustomerStatus.BLOCKED) {
            throw new IllegalStateException("Customer is not blocked");
        }
        
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setBlockedReason(null);
        customer.setUpdatedBy(unblockedBy);
        customer.setUpdatedAt(LocalDateTime.now());
        
        Customer unblockedCustomer = customerRepository.save(customer);
        
        auditService.logStatusChange(
            cifNumber, oldStatus, CustomerStatus.ACTIVE, reason, unblockedBy
        );
        
        return unblockedCustomer;
    }
    
    @Transactional
    @CacheEvict(value = "customers", key = "#cifNumber + '_' + T(com.edpp.identity.tenant.TenantContext).getTenantId()")
    public Customer verifyBvn(String cifNumber, String bvn) {
        Customer customer = getCustomerByCif(cifNumber);
        
        // Verify BVN
        Customer verifiedCustomer = identityValidationService.verifyBvn(customer, bvn);
        
        // Update risk assessment
        RiskRating updatedRisk = fraudDetectionService.reassessRisk(verifiedCustomer);
        verifiedCustomer.setRiskRating(updatedRisk);
        
        return customerRepository.save(verifiedCustomer);
    }
    
    @Transactional
    @CacheEvict(value = "customers", key = "#cifNumber + '_' + T(com.edpp.identity.tenant.TenantContext).getTenantId()")
    public Customer verifyNin(String cifNumber, String nin) {
        Customer customer = getCustomerByCif(cifNumber);
        
        // Verify NIN
        Customer verifiedCustomer = identityValidationService.verifyNin(customer, nin);
        
        // Update risk assessment
        RiskRating updatedRisk = fraudDetectionService.reassessRisk(verifiedCustomer);
        verifiedCustomer.setRiskRating(updatedRisk);
        
        return customerRepository.save(verifiedCustomer);
    }
    
    public boolean customerExists(String cifNumber) {
        String tenantId = TenantContext.getTenantId();
        return customerRepository.existsByCifNumberAndTenantId(cifNumber, tenantId);
    }
    
    public Page<Customer> getAllCustomers(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return customerRepository.findByTenantId(tenantId, pageable);
    }
    
    public Page<Customer> searchCustomers(String email, String phone, CustomerStatus status, 
                                         String searchTerm, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return customerRepository.searchByTenant(tenantId, searchTerm, pageable);
        }
        
        if (email != null || phone != null || status != null) {
            return customerRepository.searchWithFilters(tenantId, email, phone, status, pageable);
        }
        
        return customerRepository.findByTenantId(tenantId, pageable);
    }
    
    public Map<String, Object> getCustomerStatistics() {
        String tenantId = TenantContext.getTenantId();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalCustomers", customerRepository.countByTenantId(tenantId));
        stats.put("activeCustomers", customerRepository.countByTenantIdAndStatus(tenantId, CustomerStatus.ACTIVE));
        stats.put("blockedCustomers", customerRepository.countByTenantIdAndStatus(tenantId, CustomerStatus.BLOCKED));
        stats.put("pendingCustomers", customerRepository.countByTenantIdAndStatus(tenantId, CustomerStatus.PENDING_ACTIVATION));
        stats.put("riskDistribution", customerRepository.getRiskDistributionByTenant(tenantId));
        
        return stats;
    }
    
    @Transactional
    @CacheEvict(value = "customers", key = "#cifNumber + '_' + T(com.edpp.identity.tenant.TenantContext).getTenantId()")
    public void deactivateCustomer(String cifNumber, String reason, String deactivatedBy) {
        Customer customer = getCustomerByCif(cifNumber);
        
        customer.setStatus(CustomerStatus.DEACTIVATED);
        customer.setDeactivationReason(reason);
        customer.setUpdatedBy(deactivatedBy);
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setDeactivatedAt(LocalDateTime.now());
        
        customerRepository.save(customer);
        
        auditService.logStatusChange(
            cifNumber, customer.getStatus(), CustomerStatus.DEACTIVATED, reason, deactivatedBy
        );
    }
    
    private String generateCifNumber(String tenantId) {
        // Format: CIF + TENANT_PREFIX + YYYYMMDD + Random 6 digits
        String tenantPrefix = tenantId.length() > 3 ? tenantId.substring(0, 3).toUpperCase() : tenantId.toUpperCase();
        return "CIF" + tenantPrefix + 
               LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE) + 
               String.format("%06d", new Random().nextInt(999999));
    }
}