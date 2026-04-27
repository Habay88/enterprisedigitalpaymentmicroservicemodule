package com.edpp.iso8583.service;

import com.edpp.iso8583.parser.Iso8583Builder;
import com.edpp.iso8583.util.MacCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Network Management Service - Handles ISO 8583 network management messages
 *
 * Message Types:
 * - 0800: Network Management Request
 * - 0810: Network Management Response
 *
 * Functions:
 * - 301: Echo test (connectivity check)
 * - 302: Key exchange (cryptographic key update)
 * - 303: Logon/Logoff
 * - 304: Sign-on/Sign-off
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkManagementService {

    private final Iso8583Builder builder;
    private final MacCalculator macCalculator;
    private final KeyManagementService keyManagementService;

    /**
     * Process echo test request
     * Used to verify connectivity between systems
     */
    public ISOMsg processEchoTest(ISOMsg request) throws Exception {
        log.info("Processing echo test request");

        // Echo test - just return the same data
        return builder.buildNetworkResponse(request, "00", "Echo test successful");
    }

    /**
     * Process key exchange request
     * Used to update cryptographic keys securely
     */
    public ISOMsg processKeyExchange(ISOMsg request) throws Exception {
        log.info("Processing key exchange request");

        String keyType = request.getString(53); // Key type (MK/SK/TMK)
        String encryptedKey = request.getString(55); // Encrypted key data

        // Store new key
        keyManagementService.updateKey(keyType, encryptedKey);

        return builder.buildNetworkResponse(request, "00", "Key exchange successful");
    }

    /**
     * Process sign-on request
     * Terminal authentication
     */
    public ISOMsg processSignOn(ISOMsg request) throws Exception {
        String terminalId = request.getString(41);
        String merchantId = request.getString(42);

        log.info("Processing sign-on for terminal: {}, merchant: {}", terminalId, merchantId);

        // Validate terminal and merchant
        // In production, check against merchant service

        return builder.buildNetworkResponse(request, "00", "Sign-on successful");
    }
}