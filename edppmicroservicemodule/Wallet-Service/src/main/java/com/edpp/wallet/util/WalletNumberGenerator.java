package com.edpp.wallet.util;


@Component
public class WalletNumberGenerator {
private static final String INSTITUTION_CODE = "1001";
private static final String BRANCH_CODE = "0001";
private static final SecureRandom random = new SecureRandom();



    /**
     * Generate unique wallet number
     * Format: INSTITUTION_CODE + BRANCH_CODE + YYYYMMDD + 6-digit random
     */
    public String generate() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%06d", random.nextInt(999999));
        return INSTITUTION_CODE + BRANCH_CODE + datePrefix + randomSuffix;
    }
    
}
