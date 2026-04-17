package com.edpp.merchant.service;

import com.edpp.merchant.dto.response.ApiKeyResponse;
import com.edpp.merchant.entity.MerchantApiKey;
import com.edpp.merchant.repository.MerchantApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final MerchantApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate API keys for a merchant
     */
    @Transactional
    public ApiKeyResponse generateApiKeys(String merchantId) {
        log.info("Generating API keys for merchant: {}", merchantId);

        // Deactivate existing keys
        apiKeyRepository.findByMerchantIdAndIsActiveTrue(merchantId)
                .ifPresent(key -> {
                    key.setActive(false);
                    apiKeyRepository.save(key);
                });

        // Generate new keys
        String publicKey = generatePublicKey();
        String secretKey = generateSecretKey();
        String webhookSecret = generateWebhookSecret();

        MerchantApiKey apiKey = MerchantApiKey.builder()
                .merchantId(merchantId)
                .publicKey(publicKey)
                .secretKeyHash(passwordEncoder.encode(secretKey))
                .secretKeyPrefix(secretKey.substring(0, 8))
                .webhookSecret(webhookSecret)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build();

        apiKeyRepository.save(apiKey);

        return new ApiKeyResponse(
            publicKey,
            secretKey,  // Return plain secret only once
            webhookSecret,
            apiKey.getExpiresAt()
        );
    }

    /**
     * Validate API key
     */
    public boolean validateApiKey(String publicKey, String secretKey) {
        return apiKeyRepository.findByPublicKey(publicKey)
                .filter(key -> key.isActive() && 
                       passwordEncoder.matches(secretKey, key.getSecretKeyHash()) &&
                       key.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    /**
     * Get merchant ID from API key
     */
    public String getMerchantIdFromApiKey(String publicKey) {
        return apiKeyRepository.findByPublicKey(publicKey)
                .map(MerchantApiKey::getMerchantId)
                .orElse(null);
    }

    /**
     * Revoke API keys
     */
    @Transactional
    public void revokeApiKeys(String merchantId) {
        apiKeyRepository.findByMerchantIdAndIsActiveTrue(merchantId)
                .ifPresent(key -> {
                    key.setActive(false);
                    apiKeyRepository.save(key);
                    log.info("API keys revoked for merchant: {}", merchantId);
                });
    }

    private String generatePublicKey() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return "pk_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateSecretKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "sk_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateWebhookSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}