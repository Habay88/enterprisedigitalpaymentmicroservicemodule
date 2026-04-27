package com.edpp.iso8583.service;

import com.edpp.iso8583.client.TransactionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Reversal Service - Handles transaction reversals
 *
 * Reversals are used when:
 * - Authorization succeeded but capture failed
 * - Timeout occurred
 * - Customer cancelled transaction
 * - Technical error after authorization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReversalService {

    private final TransactionServiceClient transactionClient;

    /**
     * Process reversal for original transaction
     */
    public void processReversal(String originalStan, BigDecimal amount) {
        log.info("Processing reversal for STAN: {}, Amount: {}", originalStan, amount);

        // Call transaction service to reverse the original transaction
        transactionClient.reverseTransaction(originalStan, "POS reversal");
    }
}