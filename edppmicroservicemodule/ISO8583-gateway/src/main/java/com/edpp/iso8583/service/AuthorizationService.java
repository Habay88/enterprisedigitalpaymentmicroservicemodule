package com.edpp.iso8583.service;

import com.edpp.iso8583.entity.TerminalSession;
import com.edpp.iso8583.repository.TerminalSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Authorization Service - Handles card authorization logic
 *
 * Responsibilities:
 * - PIN validation
 * - Duplicate STAN detection
 * - Terminal session management
 * - Risk scoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final TerminalSessionRepository sessionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // In-memory cache for STAN tracking (prevides duplicates within 24 hours)
    private static final String STAN_CACHE_PREFIX = "stan:";

    /**
     * Validate PIN using Hardware Security Module (HSM) or encryption
     */
    public boolean validatePin(String pan, String pinData) {
        // In production, this would call an HSM or use 3DES encryption
        // For simulation, simple validation
        log.debug("Validating PIN for PAN: {}", maskPan(pan));

        // Extract PIN block and validate
        // This is a simplified version - production uses proper PIN block validation
        return pinData != null && pinData.length() == 16;
    }

    /**
     * Check for duplicate STAN (System Trace Audit Number)
     * Prevents replay attacks
     */
    public boolean isDuplicateStan(String stan) {
        String key = STAN_CACHE_PREFIX + stan;
        Boolean exists = redisTemplate.hasKey(key);

        if (!exists) {
            // Store for 24 hours
            redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);
            return false;
        }
        return true;
    }

    /**
     * Register terminal session
     */
    public TerminalSession registerTerminal(String terminalId, String merchantId) {
        TerminalSession session = TerminalSession.builder()
                .terminalId(terminalId)
                .merchantId(merchantId)
                .connectedAt(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .isActive(true)
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Update terminal heartbeat
     */
    public void updateHeartbeat(String terminalId) {
        sessionRepository.findByTerminalId(terminalId).ifPresent(session -> {
            session.setLastHeartbeat(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}