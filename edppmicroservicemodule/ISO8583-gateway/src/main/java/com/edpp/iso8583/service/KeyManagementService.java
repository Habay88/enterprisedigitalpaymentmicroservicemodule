package com.edpp.iso8583.service;

import com.edpp.iso8583.entity.KeyExchangeRecord;
import com.edpp.iso8583.repository.KeyExchangeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Key Management Service - Handles cryptographic key exchange
 *
 * Manages:
 * - TMK (Terminal Master Key)
 * - PIN keys for PIN encryption
 * - MAC keys for message authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeyManagementService {

    private final KeyExchangeRecordRepository keyRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TMK_KEY_PREFIX = "tmk:";
    private static final String PIN_KEY_PREFIX = "pin:";
    private static final String MAC_KEY_PREFIX = "mac:";

    /**
     * Update cryptographic key
     */
    public void updateKey(String keyType, String encryptedKey) {
        log.info("Updating key of type: {}", keyType);

        // Decrypt the key (in production, use HSM)
        String decryptedKey = decryptKey(encryptedKey);

        // Store in Redis for fast access
        String redisKey = getRedisKeyForType(keyType);
        redisTemplate.opsForValue().set(redisKey, decryptedKey, 24, TimeUnit.HOURS);

        // Store in database for audit
        KeyExchangeRecord record = KeyExchangeRecord.builder()
                .keyType(keyType)
                .encryptedKey(encryptedKey)
                .keyHash(hashKey(decryptedKey))
                .exchangedAt(LocalDateTime.now())
                .build();

        keyRepository.save(record);

        log.info("Key updated successfully for type: {}", keyType);
    }

    /**
     * Get TMK (Terminal Master Key)
     */
    public String getTmk() {
        return redisTemplate.opsForValue().get(TMK_KEY_PREFIX);
    }

    /**
     * Get PIN encryption key
     */
    public String getPinKey() {
        return redisTemplate.opsForValue().get(PIN_KEY_PREFIX);
    }

    /**
     * Get MAC key
     */
    public String getMacKey() {
        return redisTemplate.opsForValue().get(MAC_KEY_PREFIX);
    }

    /**
     * Generate new keys (for initial setup)
     */
    public void generateInitialKeys() {
        log.info("Generating initial cryptographic keys");

        // Generate random keys (in production, use HSM)
        String tmk = generateRandomKey();
        String pinKey = generateRandomKey();
        String macKey = generateRandomKey();

        redisTemplate.opsForValue().set(TMK_KEY_PREFIX, tmk, 24, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(PIN_KEY_PREFIX, pinKey, 24, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(MAC_KEY_PREFIX, macKey, 24, TimeUnit.HOURS);

        log.info("Initial keys generated");
    }

    private String decryptKey(String encryptedKey) {
        // In production, use HSM for decryption
        // For simulation, simple decode
        try {
            return new String(Base64.getDecoder().decode(encryptedKey));
        } catch (Exception e) {
            log.warn("Failed to decode key, using as-is");
            return encryptedKey;
        }
    }

    private String hashKey(String key) {
        // Simple hash for audit (not for security)
        return Integer.toHexString(key.hashCode());
    }

    private String generateRandomKey() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String getRedisKeyForType(String keyType) {
        return switch (keyType) {
            case "301" -> TMK_KEY_PREFIX;
            case "302" -> PIN_KEY_PREFIX;
            case "303" -> MAC_KEY_PREFIX;
            default -> TMK_KEY_PREFIX;
        };
    }
}