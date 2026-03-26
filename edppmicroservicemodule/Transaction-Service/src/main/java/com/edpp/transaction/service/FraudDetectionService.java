package com.edpp.transaction.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.edpp.transaction.dtorequest.PaymentRequest;
import com.edpp.transaction.entity.FraudCheckResult;
import com.edpp.transaction.entity.Transaction;
import com.edpp.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;

    @Value("${fraud.max-amount-per-transaction:5000000}")
    private BigDecimal maxAmountPerTransaction;

    @Value("${fraud.suspicious-amount-threshold:1000000}")
    private BigDecimal suspiciousAmountThreshold;

    @Value("${fraud.max-transactions-per-hour:10}")
    private int maxTransactionsPerHour;

    @Value("${fraud.max-failed-attempts:3}")
    private int maxFailedAttempts;

    @Value("${fraud.lockout-minutes:30}")
    private int lockoutMinutes;

    @Value("${fraud.enable-velocity-check:true}")
    private boolean enableVelocityCheck;

    @Value("${fraud.enable-ip-blacklist:true}")
    private boolean enableIpBlacklist;

    @Value("${fraud.enable-card-velocity:true}")
    private boolean enableCardVelocity;

    // In-memory caches for fraud detection
    private final Map<String, List<LocalDateTime>> transactionTimestamps = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockedCustomers = new ConcurrentHashMap<>();
    private final Map<String, List<LocalDateTime>> cardUsageHistory = new ConcurrentHashMap<>();
    private final Map<String, Integer> ipBlacklist = new ConcurrentHashMap<>();
    private final Map<String, Integer> emailBlacklist = new ConcurrentHashMap<>();

    // Suspicious patterns
    private static final Pattern SUSPICIOUS_EMAIL_PATTERN = Pattern.compile(".*(tempmail|throwaway|mailinator|guerrillamail|yopmail).*");
    private static final Pattern SUSPICIOUS_NAME_PATTERN = Pattern.compile(".*(test|demo|sample|user\\d+).*", Pattern.CASE_INSENSITIVE);
    private static final List<String> SUSPICIOUS_COUNTRIES = Arrays.asList("RU", "NG", "UA", "PK", "CN", "IR", "SY", "KP");
    private static final List<String> SUSPICIOUS_IPS = Arrays.asList("185.130.5.253", "45.33.32.156", "185.220.101.1");

    /**
     * Perform comprehensive fraud check on transaction request
     */
    public FraudCheckResult checkTransaction(PaymentRequest request) {
        log.info("Performing fraud check for transaction: {}", request.getMerchantTransactionId());

        List<String> flags = new ArrayList<>();
        int riskScore = 0;

        // 1. Amount-based checks
        riskScore += checkAmountBasedFraud(request.getAmount(), flags);

        // 2. Velocity checks
        if (enableVelocityCheck) {
            riskScore += checkVelocityFraud(request.getCustomerId(), flags);
        }

        // 3. Time-based checks
        riskScore += checkTimeBasedFraud(flags);

        // 4. Customer risk profile
        riskScore += checkCustomerRiskProfile(request.getCustomerId(), flags);

        // 5. Payment method checks
        riskScore += checkPaymentMethodFraud(request, flags);

        // 6. Location/IP checks
        if (enableIpBlacklist) {
            riskScore += checkIpLocationFraud(request.getIpAddress(), flags);
        }

        // 7. Email checks
        riskScore += checkEmailFraud(request.getCustomerEmail(), flags);

        // 8. Card velocity checks (if card payment)
        if (enableCardVelocity && "CARD".equalsIgnoreCase(request.getPaymentMethod())) {
            riskScore += checkCardVelocityFraud(request.getCardDetails(), flags);
        }

        // Determine if transaction is allowed
        boolean allowed = riskScore < 70;
        boolean requiresAdditionalAuth = riskScore >= 50 && riskScore < 70;

        // Update transaction history for velocity tracking
        updateTransactionHistory(request.getCustomerId());

        // Update card usage history if applicable
        if ("CARD".equalsIgnoreCase(request.getPaymentMethod()) && request.getCardDetails() != null) {
            updateCardUsageHistory(request.getCardDetails().getMaskedPan());
        }

        FraudCheckResult result = FraudCheckResult.builder()
                .allowed(allowed)
                .riskScore(riskScore)
                .flags(flags)
                .reason(generateReason(flags, riskScore))
                .requiresAdditionalAuth(requiresAdditionalAuth)
                .build();

        log.info("Fraud check completed - Risk Score: {}, Allowed: {}, Flags: {}", 
                 riskScore, allowed, flags);

        return result;
    }

    /**
     * Check amount-based fraud indicators
     */
    private int checkAmountBasedFraud(BigDecimal amount, List<String> flags) {
        int score = 0;

        if (amount.compareTo(maxAmountPerTransaction) > 0) {
            flags.add("EXCEEDS_MAX_AMOUNT");
            score += 50;
        } else if (amount.compareTo(suspiciousAmountThreshold) > 0) {
            flags.add("SUSPICIOUS_AMOUNT");
            score += 30;
        }

        // Check for round numbers (often associated with testing)
        if (amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0 &&
            amount.compareTo(new BigDecimal("10000")) > 0) {
            flags.add("ROUND_NUMBER_AMOUNT");
            score += 5;
        }

        return score;
    }

    /**
     * Check velocity fraud (too many transactions in short period)
     */
    private int checkVelocityFraud(String customerId, List<String> flags) {
        List<LocalDateTime> timestamps = transactionTimestamps.getOrDefault(customerId, new ArrayList<>());
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        long recentCount = timestamps.stream()
                .filter(t -> t.isAfter(oneHourAgo))
                .count();

        if (recentCount >= maxTransactionsPerHour) {
            flags.add("HIGH_VELOCITY");
            return 40;
        }

        // Check for unusual frequency (more than 2x normal)
        long lastHourCount = timestamps.stream()
                .filter(t -> t.isAfter(oneHourAgo))
                .count();
        
        long historicalAvg = getHistoricalAverageTransactionCount(customerId);
        if (historicalAvg > 0 && lastHourCount > historicalAvg * 2) {
            flags.add("UNUSUAL_VELOCITY");
            return 20;
        }

        return 0;
    }

    /**
     * Check time-based fraud indicators
     */
    private int checkTimeBasedFraud(List<String> flags) {
        int score = 0;
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int dayOfWeek = now.getDayOfWeek().getValue();

        // Check for unusual hours (late night/early morning)
        if (hour >= 23 || hour <= 4) {
            flags.add("UNUSUAL_HOUR");
            score += 15;
        }

        // Check for weekends
        if (dayOfWeek >= 6) { // Saturday or Sunday
            flags.add("WEEKEND_TRANSACTION");
            score += 10;
        }

        // Check for holidays (simplified - in production, would check holiday calendar)
        if (isHoliday(now)) {
            flags.add("HOLIDAY_TRANSACTION");
            score += 5;
        }

        return score;
    }

    /**
     * Check customer risk profile
     */
    private int checkCustomerRiskProfile(String customerId, List<String> flags) {
        int score = 0;

        // Check if customer is locked
        if (isCustomerLocked(customerId)) {
            flags.add("CUSTOMER_LOCKED");
            score += 100; // Immediate block
        }

        // Check failed attempts
        int failedCount = getFailedAttemptCount(customerId);
        if (failedCount >= maxFailedAttempts) {
            flags.add("EXCESSIVE_FAILED_ATTEMPTS");
            score += 30;
        } else if (failedCount >= maxFailedAttempts / 2) {
            flags.add("MULTIPLE_FAILED_ATTEMPTS");
            score += 15;
        }

        return score;
    }

    /**
     * Check payment method fraud indicators
     */
    private int checkPaymentMethodFraud(PaymentRequest request, List<String> flags) {
        int score = 0;

        String paymentMethod = request.getPaymentMethod();

        // Check for unusual payment methods for amount
        if ("USSD".equalsIgnoreCase(paymentMethod) && 
            request.getAmount().compareTo(new BigDecimal("500000")) > 0) {
            flags.add("LARGE_USSD_TRANSACTION");
            score += 25;
        }

        // Check for rapid changes in payment methods
        if (hasPaymentMethodChanged(request.getCustomerId(), paymentMethod)) {
            flags.add("PAYMENT_METHOD_CHANGED");
            score += 20;
        }

        return score;
    }

    /**
     * Check IP and location-based fraud
     */
    private int checkIpLocationFraud(String ipAddress, List<String> flags) {
        int score = 0;

        if (ipAddress == null) {
            flags.add("MISSING_IP");
            score += 10;
            return score;
        }

        // Check against blacklist
        if (SUSPICIOUS_IPS.contains(ipAddress)) {
            flags.add("BLACKLISTED_IP");
            score += 50;
        }

        // Check if IP is from suspicious country (simplified)
        String country = getCountryFromIp(ipAddress);
        if (SUSPICIOUS_COUNTRIES.contains(country)) {
            flags.add("SUSPICIOUS_COUNTRY");
            score += 25;
        }

        // Check for VPN/Proxy (simplified)
        if (isVpnIp(ipAddress)) {
            flags.add("VPN_DETECTED");
            score += 20;
        }

        return score;
    }

    /**
     * Check email fraud indicators
     */
    private int checkEmailFraud(String email, List<String> flags) {
        int score = 0;

        if (email == null) {
            flags.add("MISSING_EMAIL");
            score += 10;
            return score;
        }

        // Check for disposable email
        if (SUSPICIOUS_EMAIL_PATTERN.matcher(email).matches()) {
            flags.add("DISPOSABLE_EMAIL");
            score += 30;
        }

        // Check for email domain reputation
        String domain = email.substring(email.indexOf('@') + 1);
        if (isLowReputationDomain(domain)) {
            flags.add("LOW_REPUTATION_DOMAIN");
            score += 20;
        }

        return score;
    }

    /**
     * Check card velocity fraud (same card used too many times)
     */
    private int checkCardVelocityFraud(Object cardDetails, List<String> flags) {
        if (cardDetails == null) {
            return 0;
        }

        // Extract masked PAN from card details
        String maskedPan = extractMaskedPan(cardDetails);
        if (maskedPan == null) {
            return 0;
        }

        List<LocalDateTime> usage = cardUsageHistory.getOrDefault(maskedPan, new ArrayList<>());
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        long recentUsage = usage.stream()
                .filter(t -> t.isAfter(oneHourAgo))
                .count();

        if (recentUsage >= 5) { // More than 5 transactions per hour with same card
            flags.add("HIGH_CARD_VELOCITY");
            return 35;
        }

        return 0;
    }

    /**
     * Enhanced fraud check with additional parameters
     */
    public FraudCheckResult enhancedFraudCheck(PaymentRequest request, 
                                                String deviceId, 
                                                String fingerprint) {
        FraudCheckResult result = checkTransaction(request);

        // Device fingerprint checks
        if (fingerprint != null && isSuspiciousDevice(fingerprint)) {
            result.getFlags().add("SUSPICIOUS_DEVICE");
            result.setRiskScore(result.getRiskScore() + 25);
        }

        // Device ID checks
        if (deviceId != null && isUnknownDevice(request.getCustomerId(), deviceId)) {
            result.getFlags().add("UNKNOWN_DEVICE");
            result.setRiskScore(result.getRiskScore() + 20);
        }

        // Re-evaluate allowed status
        result.setAllowed(result.getRiskScore() < 70);
        result.setRequiresAdditionalAuth(result.getRiskScore() >= 50 && result.getRiskScore() < 70);

        if (!result.isAllowed()) {
            result.setReason("Transaction blocked: " + String.join(", ", result.getFlags()));
        }

        return result;
    }

    /**
     * Check if customer is locked due to suspicious activity
     */
    public boolean isCustomerLocked(String customerId) {
        LocalDateTime lockTime = lockedCustomers.get(customerId);
        if (lockTime == null) {
            return false;
        }
        
        if (lockTime.plusMinutes(lockoutMinutes).isBefore(LocalDateTime.now())) {
            // Lock expired
            lockedCustomers.remove(customerId);
            return false;
        }
        
        return true;
    }

    /**
     * Lock customer due to suspicious activity
     */
    public void lockCustomer(String customerId, String reason) {
        lockedCustomers.put(customerId, LocalDateTime.now());
        log.warn("Customer locked: {} - Reason: {}", customerId, reason);
    }

    /**
     * Record failed transaction attempt
     */
    public void recordFailedAttempt(String customerId) {
        failedAttempts.computeIfAbsent(customerId, k -> new AtomicInteger(0))
                .incrementAndGet();
        
        int attemptCount = failedAttempts.get(customerId).get();
        if (attemptCount >= maxFailedAttempts) {
            lockCustomer(customerId, "Excessive failed attempts: " + attemptCount);
        }
    }

    /**
     * Reset failed attempts after successful transaction
     */
    public void resetFailedAttempts(String customerId) {
        failedAttempts.remove(customerId);
    }

    /**
     * Get failed attempt count
     */
    public int getFailedAttemptCount(String customerId) {
        return failedAttempts.getOrDefault(customerId, new AtomicInteger(0)).get();
    }

    /**
     * Update transaction history for velocity tracking
     */
    private void updateTransactionHistory(String customerId) {
        transactionTimestamps.computeIfAbsent(customerId, k -> new ArrayList<>())
                .add(LocalDateTime.now());

        // Clean up old entries (keep last 24 hours)
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        transactionTimestamps.get(customerId)
                .removeIf(t -> t.isBefore(oneDayAgo));
    }

    /**
     * Update card usage history for card velocity tracking
     */
    private void updateCardUsageHistory(String maskedPan) {
        if (maskedPan == null) {
            return;
        }
        
        cardUsageHistory.computeIfAbsent(maskedPan, k -> new ArrayList<>())
                .add(LocalDateTime.now());

        // Clean up old entries (keep last 24 hours)
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        cardUsageHistory.get(maskedPan)
                .removeIf(t -> t.isBefore(oneDayAgo));
    }

    /**
     * Get historical average transaction count for customer
     */
    private long getHistoricalAverageTransactionCount(String customerId) {
        // In production, this would query the database for historical data
        // Simplified implementation
        List<Transaction> transactions = transactionRepository.findByTenantIdAndCustomerId(
                getCurrentTenantId(), customerId);
        
        if (transactions.isEmpty()) {
            return 0;
        }
        
        // Calculate average transactions per day over last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentCount = transactions.stream()
                .filter(t -> t.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();
        
        return recentCount / 30;
    }

    /**
     * Check if payment method has changed recently
     */
    private boolean hasPaymentMethodChanged(String customerId, String currentMethod) {
        // In production, check last 5 transactions to see if payment method changed
        return false; // Simplified
    }

    /**
     * Check if it's a holiday
     */
    private boolean isHoliday(LocalDateTime date) {
        // In production, check against holiday calendar
        // Simplified - check major Nigerian holidays
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        
        // New Year's Day
        if (month == 1 && day == 1) return true;
        // Independence Day
        if (month == 10 && day == 1) return true;
        // Christmas
        if (month == 12 && (day == 25 || day == 26)) return true;
        
        return false;
    }

    /**
     * Get country from IP address (simplified)
     */
    private String getCountryFromIp(String ip) {
        // In production, call IP geolocation service
        // Simplified implementation
        return "NG"; // Default to Nigeria
    }

    /**
     * Check if IP is from VPN/proxy (simplified)
     */
    private boolean isVpnIp(String ip) {
        // In production, check against VPN database
        return false;
    }

    /**
     * Check if email domain has low reputation
     */
    private boolean isLowReputationDomain(String domain) {
        List<String> lowReputationDomains = Arrays.asList(
            "mail.ru", "yandex.ru", "protonmail.com", "guerrillamail.com"
        );
        return lowReputationDomains.contains(domain);
    }

    /**
     * Check if device fingerprint is suspicious
     */
    private boolean isSuspiciousDevice(String fingerprint) {
        // In production, check against device fingerprint database
        return false;
    }

    /**
     * Check if device is unknown for this customer
     */
    private boolean isUnknownDevice(String customerId, String deviceId) {
        // In production, check device fingerprint database
        return true; // Default to unknown for now
    }

    /**
     * Extract masked PAN from card details
     */
    private String extractMaskedPan(Object cardDetails) {
        // Extract from card details object
        // Implementation depends on your CardDetails class structure
        return null;
    }

    /**
     * Generate reason message based on flags and risk score
     */
    private String generateReason(List<String> flags, int riskScore) {
        if (flags.isEmpty()) {
            return "Transaction appears legitimate";
        }
        
        if (riskScore >= 70) {
            return "Transaction blocked: " + String.join(", ", flags);
        } else if (riskScore >= 50) {
            return "Additional verification required: " + String.join(", ", flags);
        } else {
            return "Suspicious activity detected: " + String.join(", ", flags);
        }
    }

    /**
     * Get current tenant ID (implementation depends on your tenant context)
     */
    private String getCurrentTenantId() {
        // Implement based on your tenant context
        return "DEFAULT";
    }

    /**
     * Get fraud statistics for dashboard
     */
    public Map<String, Object> getFraudStatistics(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalFlaggedTransactions", getFlaggedTransactionCount(start, end));
        stats.put("blockedTransactions", getBlockedTransactionCount(start, end));
        stats.put("averageRiskScore", getAverageRiskScore(start, end));
        stats.put("topFraudFlags", getTopFraudFlags(start, end));
        stats.put("lockedCustomers", lockedCustomers.size());
        
        return stats;
    }

    /**
     * Get count of flagged transactions (helper method)
     */
    private long getFlaggedTransactionCount(LocalDateTime start, LocalDateTime end) {
        // In production, query from database
        return 0;
    }

    /**
     * Get count of blocked transactions (helper method)
     */
    private long getBlockedTransactionCount(LocalDateTime start, LocalDateTime end) {
        // In production, query from database
        return 0;
    }

    /**
     * Get average risk score (helper method)
     */
    private double getAverageRiskScore(LocalDateTime start, LocalDateTime end) {
        // In production, calculate from database
        return 0;
    }

    /**
     * Get top fraud flags (helper method)
     */
    private List<Map<String, Object>> getTopFraudFlags(LocalDateTime start, LocalDateTime end) {
        // In production, aggregate from database
        return new ArrayList<>();
    }
}

