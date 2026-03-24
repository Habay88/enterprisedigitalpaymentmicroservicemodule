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
    private static final String MERCHANT_PREFIX = "MCH";
    private static final String AUTHORIZATION_PREFIX = "AUTH";

    private final Random random = new Random();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generate unique transaction reference
     * Format: TXN + YYYYMMDDHHMMSS + Random 6 digits
     */
    public String generateTransactionReference() {
        return TRANSACTION_PREFIX + 
               LocalDateTime.now().format(dateFormatter) +
               String.format("%06d", random.nextInt(999999));
    }

    /**
     * Generate refund reference
     * Format: RFD + Original Transaction ID suffix + Random 4 digits
     */
    public String generateRefundReference(String originalTransactionReference) {
        String suffix = originalTransactionReference.length() > 8 ? 
                        originalTransactionReference.substring(originalTransactionReference.length() - 8) : 
                        originalTransactionReference;
        return REFUND_PREFIX + suffix + String.format("%04d", random.nextInt(9999));
    }

    /**
     * Generate merchant transaction ID
     * Format: MCH + Tenant prefix + Random 12 digits
     */
    public String generateMerchantTransactionId() {
        return MERCHANT_PREFIX + 
               String.format("%012d", random.nextLong() % 1_000_000_000_000L);
    }

    /**
     * Generate merchant transaction ID with custom prefix
     */
    public String generateMerchantTransactionId(String tenantId) {
        String tenantPrefix = tenantId.length() > 4 ? tenantId.substring(0, 4).toUpperCase() : tenantId;
        return MERCHANT_PREFIX + tenantPrefix + 
               String.format("%08d", random.nextInt(99999999));
    }

    /**
     * Generate authorization code
     * Format: AUTH + Random 12 alphanumeric
     */
    public String generateAuthorizationCode() {
        return AUTHORIZATION_PREFIX + generateRandomAlphanumeric(12);
    }

    /**
     * Generate random alphanumeric string
     */
    private String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generate unique request ID for tracing
     */
    public String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate batch ID for settlements
     */
    public String generateBatchId() {
        return "BATCH_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
               "_" + String.format("%06d", random.nextInt(999999));
    }

    /**
     * Generate callback reference
     */
    public String generateCallbackReference() {
        return "CALLBACK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}