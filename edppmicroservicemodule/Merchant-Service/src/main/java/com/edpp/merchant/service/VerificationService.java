package com.edpp.merchant.service;

import com.edpp.merchant.entity.Merchant;
import com.edpp.merchant.enums.VerificationStatus;
import com.edpp.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final MerchantRepository merchantRepository;

    /**
     * Initiate merchant verification
     */
    @Transactional
    public void initiateVerification(String merchantId) {
        log.info("Initiating verification for merchant: {}", merchantId);
        
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        
        merchant.setVerificationStatus(VerificationStatus.IN_PROGRESS);
        merchantRepository.save(merchant);
        
        // In production, this would trigger:
        // - CAC verification (business registration)
        // - BVN validation for directors
        // - Address verification
        // - Bank account verification
    }

    /**
     * Complete verification
     */
    @Transactional
    public void completeVerification(String merchantId, boolean approved, String notes) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        
        if (approved) {
            merchant.setVerificationStatus(VerificationStatus.VERIFIED);
            merchant.setVerifiedAt(java.time.LocalDateTime.now());
            merchant.setVerifiedBy("SYSTEM");
            log.info("Merchant verified: {}", merchantId);
        } else {
            merchant.setVerificationStatus(VerificationStatus.REJECTED);
            log.warn("Merchant verification rejected: {}", merchantId);
        }
        
        merchantRepository.save(merchant);
    }
}