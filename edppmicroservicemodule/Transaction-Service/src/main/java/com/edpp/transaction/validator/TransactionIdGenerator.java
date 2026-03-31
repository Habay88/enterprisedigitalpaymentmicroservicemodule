package com.edpp.transaction.validator;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

@Component
public class TransactionIdGenerator {

    private static final String TRANSACTION_PREFIX = "TXN";
    private static final String REFUND_PREFIX = "RFD";
    private static final String REVERSAL_PREFIX = "REV";
    private static final String RETRY_PREFIX = "RTY";
    private static final String MERCHANT_PREFIX = "MCH";

    private final Random random = new Random();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generateTransactionReference() {
        return TRANSACTION_PREFIX + LocalDateTime.now().format(dateFormatter) +
               String.format("%06d", random.nextInt(999999));
    }

    public String generateRefundReference(String originalReference) {
        return REFUND_PREFIX + "_" + originalReference + "_" + System.currentTimeMillis();
    }

    public String generateReversalReference(String originalReference) {
        return REVERSAL_PREFIX + "_" + originalReference + "_" + System.currentTimeMillis();
    }

    public String generateRetryReference(String originalReference) {
        return RETRY_PREFIX + "_" + originalReference + "_" + System.currentTimeMillis();
    }

    public String generateMerchantTransactionId() {
        return MERCHANT_PREFIX + String.format("%012d", Math.abs(random.nextLong() % 1_000_000_000_000L));
    }

    public String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    public String generateBatchId() {
        return "BATCH_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
               "_" + String.format("%06d", random.nextInt(999999));
    }
}