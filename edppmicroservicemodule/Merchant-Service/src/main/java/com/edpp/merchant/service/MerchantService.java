package com.edpp.merchant.service;

import com.edpp.merchant.dto.request.MerchantOnboardingRequest;
import com.edpp.merchant.dto.request.MerchantUpdateRequest;
import com.edpp.merchant.dto.response.MerchantResponse;
import com.edpp.merchant.entity.Merchant;
import com.edpp.merchant.entity.MerchantBankAccount;
import com.edpp.merchant.entity.MerchantFeeConfig;
import com.edpp.merchant.enums.MerchantStatus;
import com.edpp.merchant.enums.VerificationStatus;
import com.edpp.merchant.exception.MerchantException;
import com.edpp.merchant.exception.MerchantNotFoundException;
import com.edpp.merchant.mapper.MerchantMapper;
import com.edpp.merchant.repository.MerchantBankAccountRepository;
import com.edpp.merchant.repository.MerchantFeeConfigRepository;
import com.edpp.merchant.repository.MerchantRepository;
import com.edpp.merchant.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantBankAccountRepository bankAccountRepository;
    private final MerchantFeeConfigRepository feeConfigRepository;
    private final ApiKeyService apiKeyService;
    private final VerificationService verificationService;
    private final MerchantMapper mapper;
    private final RequestContext requestContext;

    /**
     * Onboard a new merchant
     */
    @Transactional
    public MerchantResponse onboardMerchant(MerchantOnboardingRequest request) {
        String tenantId = requestContext.getTenantId();
        log.info("Onboarding new merchant: {} for tenant: {}", request.businessName(), tenantId);

        // Check if merchant already exists
        if (merchantRepository.findByEmailAndTenantId(request.email(), tenantId).isPresent()) {
            throw new MerchantException("Merchant already exists with email: " + request.email());
        }

        // Create merchant
        Merchant merchant = Merchant.builder()
                .merchantCode(generateMerchantCode())
                .businessName(request.businessName())
                .tradingName(request.tradingName())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .country(request.country())
                .postalCode(request.postalCode())
                .registrationNumber(request.registrationNumber())
                .taxId(request.taxId())
                .category(request.category())
                .status(MerchantStatus.PENDING_VERIFICATION)
                .verificationStatus(VerificationStatus.PENDING)
                .website(request.website())
                .callbackUrl(request.callbackUrl())
                .settlementFrequency(request.settlementFrequency())
                .settlementCutoffHour(request.settlementCutoffHour())
                .minimumSettlementAmount(request.minimumSettlementAmount())
                .contactPersonName(request.contactPersonName())
                .contactPersonPhone(request.contactPersonPhone())
                .contactPersonEmail(request.contactPersonEmail())
                .tenantId(tenantId)
                .createdBy(requestContext.getUserId())
                .build();

        Merchant savedMerchant = merchantRepository.save(merchant);

        // Save bank account
        MerchantBankAccount bankAccount = MerchantBankAccount.builder()
                .merchantId(savedMerchant.getId())
                .accountNumber(request.bankAccountNumber())
                .accountName(request.bankAccountName())
                .bankCode(request.bankCode())
                .bankName(request.bankName())
                .isPrimary(true)
                .currency(request.currency())
                .build();
        bankAccountRepository.save(bankAccount);

        // Create default fee configuration
        MerchantFeeConfig feeConfig = MerchantFeeConfig.builder()
                .merchantId(savedMerchant.getId())
                .cardPresentMdr(request.cardPresentMdr())
                .cardNotPresentMdr(request.cardNotPresentMdr())
                .fixedFee(request.fixedFee())
                .effectiveFrom(LocalDateTime.now())
                .createdBy(requestContext.getUserId())
                .build();
        feeConfigRepository.save(feeConfig);

        // Generate API keys
        apiKeyService.generateApiKeys(savedMerchant.getId());

        // Initiate verification process
        verificationService.initiateVerification(savedMerchant.getId());

        log.info("Merchant onboarded successfully: {} ({})", savedMerchant.getBusinessName(), savedMerchant.getMerchantCode());
        return mapper.toResponse(savedMerchant);
    }

    /**
     * Get merchant by code
     */
    @Cacheable(value = "merchants", key = "#merchantCode")
    public Merchant getMerchantByCode(String merchantCode) {
        String tenantId = requestContext.getTenantId();
        return merchantRepository.findByMerchantCode(merchantCode)
                .filter(m -> m.getTenantId().equals(tenantId))
                .orElseThrow(() -> new MerchantNotFoundException(merchantCode));
    }

    /**
     * Get merchant response by code
     */
    public MerchantResponse getMerchantResponse(String merchantCode) {
        Merchant merchant = getMerchantByCode(merchantCode);
        return mapper.toResponse(merchant);
    }

    /**
     * Update merchant
     */
    @Transactional
    @CacheEvict(value = "merchants", key = "#merchantCode")
    public MerchantResponse updateMerchant(String merchantCode, MerchantUpdateRequest request) {
        Merchant merchant = getMerchantByCode(merchantCode);

        if (request.businessName() != null) merchant.setBusinessName(request.businessName());
        if (request.tradingName() != null) merchant.setTradingName(request.tradingName());
        if (request.phoneNumber() != null) merchant.setPhoneNumber(request.phoneNumber());
        if (request.address() != null) merchant.setAddress(request.address());
        if (request.website() != null) merchant.setWebsite(request.website());
        if (request.callbackUrl() != null) merchant.setCallbackUrl(request.callbackUrl());

        merchant.setUpdatedBy(requestContext.getUserId());
        merchant.setUpdatedAt(LocalDateTime.now());

        Merchant updatedMerchant = merchantRepository.save(merchant);
        return mapper.toResponse(updatedMerchant);
    }

    /**
     * Update merchant status
     */
    @Transactional
    @CacheEvict(value = "merchants", key = "#merchantCode")
    public MerchantResponse updateStatus(String merchantCode, MerchantStatus status, String reason) {
        Merchant merchant = getMerchantByCode(merchantCode);
        
        MerchantStatus oldStatus = merchant.getStatus();
        merchant.setStatus(status);
        merchant.setUpdatedBy(requestContext.getUserId());
        
        Merchant updatedMerchant = merchantRepository.save(merchant);
        
        log.info("Merchant status updated: {} -> {} for merchant: {}",
                oldStatus, status, merchantCode);
        
        return mapper.toResponse(updatedMerchant);
    }

    /**
     * Get all merchants (paginated)
     */
    public Page<MerchantResponse> getAllMerchants(Pageable pageable) {
        String tenantId = requestContext.getTenantId();
        return merchantRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(mapper::toResponse);
    }

    /**
     * Search merchants
     */
    public Page<MerchantResponse> searchMerchants(String searchTerm, Pageable pageable) {
        String tenantId = requestContext.getTenantId();
        return merchantRepository.searchMerchants(tenantId, searchTerm, pageable)
                .map(mapper::toResponse);
    }

    /**
     * Generate unique merchant code
     */
    private String generateMerchantCode() {
        String prefix = "MCH";
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "_" + uniqueId;
    }
}