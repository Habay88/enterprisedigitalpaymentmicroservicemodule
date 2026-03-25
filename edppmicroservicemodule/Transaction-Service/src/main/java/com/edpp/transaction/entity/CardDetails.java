package com.edpp.transaction.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDetails implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String maskedPan;          // Masked card number (e.g., **** **** **** 1234)
    private String cardType;            // VISA, MASTERCARD, AMEX, VERVE, etc.
    private String expiryMonth;         // MM
    private String expiryYear;          // YYYY
    private String cardholderName;      // Name on card
    private String issuerCountry;       // Country where card was issued
    private String authorizationCode;   // Authorization code from processor
    private String cardBrand;           // Visa, Mastercard, etc.
    private String lastFour;            // Last 4 digits of card
    private String firstSix;            // First 6 digits (BIN/IIN)
    private String cardFingerprint;     // Unique identifier for the card
    
    /**
     * Get full masked card number
     */
    public String getMaskedCardNumber() {
        if (maskedPan != null) {
            return maskedPan;
        }
        if (lastFour != null) {
            return "**** **** **** " + lastFour;
        }
        return "****";
    }
    
    /**
     * Check if card is expired
     */
    public boolean isExpired() {
        if (expiryMonth == null || expiryYear == null) {
            return false;
        }
        try {
            java.time.YearMonth expiry = java.time.YearMonth.of(
                Integer.parseInt(expiryYear), 
                Integer.parseInt(expiryMonth)
            );
            return expiry.isBefore(java.time.YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get expiry date string
     */
    public String getExpiryDate() {
        if (expiryMonth != null && expiryYear != null) {
            return expiryMonth + "/" + expiryYear;
        }
        return null;
    }
    
    /**
     * Create from full card details (for internal use only, never log full card)
     */
    public static CardDetails fromFullDetails(String fullPan, String expiryMonth, 
                                               String expiryYear, String cardholderName,
                                               String cardType) {
        return CardDetails.builder()
                .maskedPan(maskCardNumber(fullPan))
                .lastFour(getLastFour(fullPan))
                .firstSix(getFirstSix(fullPan))
                .expiryMonth(expiryMonth)
                .expiryYear(expiryYear)
                .cardholderName(cardholderName)
                .cardType(cardType)
                .cardBrand(detectCardBrand(fullPan))
                .build();
    }
    
    /**
     * Mask card number (show only last 4 digits)
     */
    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
    
    /**
     * Get last 4 digits of card
     */
    private static String getLastFour(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return null;
        }
        return cardNumber.substring(cardNumber.length() - 4);
    }
    
    /**
     * Get first 6 digits (BIN) of card
     */
    private static String getFirstSix(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 6) {
            return null;
        }
        return cardNumber.substring(0, 6);
    }
    
    /**
     * Detect card brand from BIN
     */
    private static String detectCardBrand(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "UNKNOWN";
        }
        
        String firstDigit = cardNumber.substring(0, 1);
        String firstTwoDigits = cardNumber.length() >= 2 ? cardNumber.substring(0, 2) : "";
        String firstFourDigits = cardNumber.length() >= 4 ? cardNumber.substring(0, 4) : "";
        
        // Visa
        if (firstDigit.equals("4")) {
            return "VISA";
        }
        // Mastercard
        if (firstTwoDigits.matches("5[1-5]")) {
            return "MASTERCARD";
        }
        // American Express
        if (firstTwoDigits.equals("34") || firstTwoDigits.equals("37")) {
            return "AMEX";
        }
        // Discover
        if (firstFourDigits.equals("6011") || 
            (firstTwoDigits.matches("6[4-5]")) ||
            firstFourDigits.equals("6229")) {
            return "DISCOVER";
        }
        // Verve (Nigerian)
        if (firstTwoDigits.matches("5[0-5]") && cardNumber.length() == 16) {
            return "VERVE";
        }
        
        return "UNKNOWN";
    }
}