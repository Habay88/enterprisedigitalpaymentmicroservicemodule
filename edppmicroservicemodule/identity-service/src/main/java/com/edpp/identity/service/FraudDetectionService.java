package com.edpp.identity.service;

import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.model.Customer;
import com.edpp.identity.model.FraudCheckResult;
import com.edpp.identity.model.TransactionContext;
import com.edpp.identity.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {
    private final RestTemplate restTemplate;
    private final CustomerRepository customerRepository;
    private final Map<String, AtomicInteger> failedLoginAttempts
                   = new ConcurrentHashMap<String, AtomicInteger>() ;
    private final Map<String, List<LocalDateTime>> transactionTimeStamps
                   = new ConcurrentHashMap<>();
    @Value("${fraud.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${fraud.lockout-minutes:30}")
    private int lockoutMinutes;

    @Value("${fraud.max-transactions-per-hour:10}")
    private int maxTransactionsPerHour;

    @Value("${fraud.suspicious-countries:RU,NG,UA,PK}")
    private String[] suspiciousCountries;

    // to access initial risk when customer first registers
    public RiskRating assessInitialRisk(Customer customer){
        log.info("Assessing initial risk for customer: {}",
                customer.getEmail());
        int riskScore = 0;

        // Check email domain
        if (isDisposableEmail(customer.getEmail())) {
            riskScore += 30;
        }

        // Check age (if available)
        if (customer.getDateOfBirth() != null) {
            int age = Period.between(customer.getDateOfBirth().toLocalDate(), LocalDateTime.now().toLocalDate()).getYears();
            if (age < 18) {
                riskScore += 50; // Underage
            } else if (age > 80) {
                riskScore += 20; // Elderly (potential vulnerability)
            }
        }

        // Check high-risk country
        if (customer.getResidentialAddress() != null &&
                isHighRiskCountry(customer.getResidentialAddress().getCountry())) {
            riskScore += 25;
        }

        // Check against sanction lists (simulated)
        if (isOnSanctionList(customer)) {
            riskScore += 100; // Immediate high risk
        }

        return determineRiskRating(riskScore);
    }

    /**
     * Real-time fraud check during transaction
     */
    public FraudCheckResult checkTransactionFraud(TransactionContext context) {
        log.info("Performing fraud check for transaction: {}", context.getTransactionId());

        FraudCheckResult result = FraudCheckResult.builder()
                .transactionId(context.getTransactionId())
                .timestamp(LocalDateTime.now())
                .riskScore(0)
                .flags(new ArrayList<>())
                .requiresAdditionalAuth(false)
                .isAllowed(true)
                .build();

        // Velocity check
        if (exceedsVelocityLimit(context.getCustomerId())) {
            result.getFlags().add("HIGH_VELOCITY");
            result.setRiskScore(result.getRiskScore() + 40);
        }

        // Amount check
        if (exceedsNormalAmount(context.getCustomerId(), context.getAmount())) {
            result.getFlags().add("UNUSUAL_AMOUNT");
            result.setRiskScore(result.getRiskScore() + 30);
        }

        // Location check (if IP provided)
        if (context.getIpAddress() != null &&
                isSuspiciousLocation(context.getIpAddress())) {
            result.getFlags().add("SUSPICIOUS_LOCATION");
            result.setRiskScore(result.getRiskScore() + 50);
        }

        // Device fingerprint check (if available)
        if (context.getDeviceId() != null &&
                isUnknownDevice(context.getCustomerId(), context.getDeviceId())) {
            result.getFlags().add("UNKNOWN_DEVICE");
            result.setRiskScore(result.getRiskScore() + 20);
        }

        // Time of day check
        int hour = LocalDateTime.now().getHour();
        if (hour >= 23 || hour <= 5) {
            result.getFlags().add("UNUSUAL_HOUR");
            result.setRiskScore(result.getRiskScore() + 10);
        }

        // Determine if additional authentication needed
        result.setRequiresAdditionalAuth(result.getRiskScore() > 50);
        result.setAllowed(result.getRiskScore() < 80);

        // Update transaction history
        updateTransactionHistory(context.getCustomerId());

        return result;
    }

    /**
     * Track failed login attempts for brute force protection
     */
    public boolean isLoginBlocked(String email) {
        AtomicInteger attempts = failedLoginAttempts.computeIfAbsent(email, k -> new AtomicInteger(0));
        return attempts.get() >= maxLoginAttempts;
    }

    public void recordFailedLogin(String email) {
        failedLoginAttempts.computeIfAbsent(email, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void resetFailedLogins(String email) {
        failedLoginAttempts.remove(email);
    }

    /**
     * Check if email is from disposable email provider
     */
    private boolean isDisposableEmail(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        Set<String> disposableDomains = Set.of(
                "tempmail.com", "throwaway.com", "mailinator.com",
                "guerrillamail.com", "sharklasers.com", "yopmail.com"
        );
        return disposableDomains.contains(domain);
    }

    /**
     * Check if country is high-risk
     */
    private boolean isHighRiskCountry(String country) {
        return Arrays.asList(suspiciousCountries).contains(country);
    }

    /**
     * Simulated sanction list check
     */
    private boolean isOnSanctionList(Customer customer) {
        // In production, this would call external sanction screening service
        List<String> sanctionList = Arrays.asList("OFAC", "UN", "EU");
        return false; // Simulated result
    }

    /**
     * Check if transaction velocity exceeds limits
     */
    private boolean exceedsVelocityLimit(String customerId) {
        List<LocalDateTime> timestamps = transactionTimestamps.getOrDefault(customerId, new ArrayList<>());
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        long recentCount = timestamps.stream()
                .filter(t -> t.isAfter(oneHourAgo))
                .count();

        return recentCount >= maxTransactionsPerHour;
    }

    /**
     * Check if amount exceeds normal pattern for customer
     */
    private boolean exceedsNormalAmount(String customerId, BigDecimal amount) {
        // In production, this would use ML models based on customer history
        // Simplified implementation
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isPresent()) {
            RiskRating rating = customer.get().getRiskRating();
            if (rating == RiskRating.LOW && amount.compareTo(new BigDecimal("5000")) > 0) {
                return true;
            } else if (rating == RiskRating.MEDIUM && amount.compareTo(new BigDecimal("2000")) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if IP is from suspicious location
     */
    private boolean isSuspiciousLocation(String ipAddress) {
        // In production, this would call IP geolocation service
        // Simplified implementation
        return false;
    }

    /**
     * Check if device is unknown for this customer
     */
    private boolean isUnknownDevice(String customerId, String deviceId) {
        // In production, this would check device fingerprint database
        return false;
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
     * Determine risk rating based on score
     */
    private RiskRating determineRiskRating(int score) {
        if (score < 30) return RiskRating.LOW;
        if (score < 60) return RiskRating.MEDIUM;
        return RiskRating.HIGH;
    }
}

    }
}
