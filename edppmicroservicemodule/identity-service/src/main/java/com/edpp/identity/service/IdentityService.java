package com.edpp.identity.service;


import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.exception.CustomerNotFoundException;
import com.edpp.identity.exception.DuplicateCustomerException;
import com.edpp.identity.model.Customer;
import com.edpp.identity.model.KycDetails;
import com.edpp.identity.repository.CustomerRepository;

import com.edpp.identity.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityService {

    private final CustomerRepository customerRepository;
    private final FraudDetectionService fraudDetectionService;
    private final AuditService auditService;
    private final IdentityValidationService identityValidationService;

    @Transactional
    public Customer onboardCustomer(Customer customer) {
        String tenantId = TenantContext.getTenantId()
        log.info("Onboarding new customer: {}", customer.getEmail());

        // Check for duplicate
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new DuplicateCustomerException("Customer already exists");
        }

        // Generate CIF number
        customer.setCifNumber(generateCifNumber());

        // Perform initial risk assessment
        RiskRating initialRisk = fraudDetectionService.assessInitialRisk(customer);
        customer.setRiskRating(initialRisk);

        // Set initial status
        customer.setStatus(CustomerStatus.PENDING_ACTIVATION);

        Customer savedCustomer = customerRepository.save(customer);

        // Audit log
        auditService.logCustomerOnboarding(savedCustomer);

        return savedCustomer;
    }

    @Cacheable(value = "customers", key = "#cifNumber")
    public Customer getCustomerByCif(String cifNumber) {
        return customerRepository.findByCifNumber(cifNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#cifNumber")
    public Customer updateKycStatus(String cifNumber, KycDetails kycDetails) {
        Customer customer = getCustomerByCif(cifNumber);
        customer.setKycDetails(kycDetails);

        if (kycDetails.isKycCompleted()) {
            customer.setStatus(CustomerStatus.ACTIVE);
        }

        return customerRepository.save(customer);
    }

    private String generateCifNumber() {
        // Format: CIF + YYYYMMDD + Random 6 digits
        return "CIF" + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%06d", new Random().nextInt(999999));
    }
}
