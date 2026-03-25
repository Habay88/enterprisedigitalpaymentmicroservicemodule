package com.edpp.transaction.validator;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.edpp.transaction.dtorequest.PaymentRequest;
import com.edpp.transaction.exception.TransactionException;

public class TransactionValidator {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of
    (  "NGN", "USD", "GBP", "EUR", "GHS", "KES", "ZAR");

private static final Set<String> SUPPORTED_PAYMENT_METHODS = Set.of
    (  "CARD", "BANK_TRANSFER", "MOBILE_MONEY", "USSD", "CRYPTO", "WALLET","QR_CODE");


private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000"); // 10million naira

/**
     * Validate payment request
     */
    public void validatePaymentRequest(PaymentRequest request) {
        Set<String> errors = new HashSet<>();

        // Required fields
        if (request.getMerchantTransactionId() == null || request.getMerchantTransactionId().isBlank()) {
            errors.add("Merchant transaction ID is required");
        }

        if (request.getSourceWalletId() == null || request.getSourceWalletId().isBlank()) {
            errors.add("Source wallet ID is required");
        }

        if (request.getAmount() == null) {
            errors.add("Amount is required");
        } else {
            // Amount validation
            if (request.getAmount().compareTo(MIN_AMOUNT) < 0) {
                errors.add("Amount must be at least " + MIN_AMOUNT);
            }
            if (request.getAmount().compareTo(MAX_AMOUNT) > 0) {
                errors.add("Amount cannot exceed " + MAX_AMOUNT);
            }
        }

        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            errors.add("Currency is required");
        } else if (!SUPPORTED_CURRENCIES.contains(request.getCurrency().toUpperCase())) {
            errors.add("Unsupported currency: " + request.getCurrency());
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            errors.add("Payment method is required");
        } else if (!SUPPORTED_PAYMENT_METHODS.contains(request.getPaymentMethod().toUpperCase())) {
            errors.add("Unsupported payment method: " + request.getPaymentMethod());
        }

        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            errors.add("Customer ID is required");
        }

        // Validate card details if payment method is CARD
        if ("CARD".equalsIgnoreCase(request.getPaymentMethod())) {
            validateCardDetails(request, errors);
        }

        // Validate bank details if payment method is BANK_TRANSFER
        if ("BANK_TRANSFER".equalsIgnoreCase(request.getPaymentMethod())) {
            validateBankDetails(request, errors);
        }

        if (!errors.isEmpty()) {
            throw new TransactionException("Validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Validate card details
     */
    private void validateCardDetails(PaymentRequest request, Set<String> errors) {
        if (request.getCardDetails() == null) {
            errors.add("Card details are required for card payments");
            return;
        }

        if (request.getCardDetails().getMaskedPan() == null || 
            request.getCardDetails().getMaskedPan().isBlank()) {
            errors.add("Card number is required");
        }

        if (request.getCardDetails().getExpiryMonth() == null || 
            request.getCardDetails().getExpiryMonth().isBlank()) {
            errors.add("Expiry month is required");
        }

        if (request.getCardDetails().getExpiryYear() == null || 
            request.getCardDetails().getExpiryYear().isBlank()) {
            errors.add("Expiry year is required");
        }

        if (request.getCardDetails().getCardholderName() == null || 
            request.getCardDetails().getCardholderName().isBlank()) {
            errors.add("Cardholder name is required");
        }
    }

    /**
     * Validate bank details
     */
    private void validateBankDetails(PaymentRequest request, Set<String> errors) {
        if (request.getBankDetails() == null) {
            errors.add("Bank details are required for bank transfers");
            return;
        }

        if (request.getBankDetails().getAccountNumber() == null || 
            request.getBankDetails().getAccountNumber().isBlank()) {
            errors.add("Account number is required");
        }

        if (request.getBankDetails().getBankName() == null || 
            request.getBankDetails().getBankName().isBlank()) {
            errors.add("Bank name is required");
        }
    }

    /**
     * Validate refund request
     */
    public void validateRefundRequest(String originalTransactionReference, BigDecimal amount) {
        Set<String> errors = new HashSet<>();

        if (originalTransactionReference == null || originalTransactionReference.isBlank()) {
            errors.add("Original transaction reference is required");
        }

        if (amount == null) {
            errors.add("Refund amount is required");
        } else if (amount.compareTo(MIN_AMOUNT) < 0) {
            errors.add("Refund amount must be at least " + MIN_AMOUNT);
        }

        if (!errors.isEmpty()) {
            throw new TransactionException("Refund validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Check if transaction can be refunded based on time
     */
    public boolean isWithinRefundWindow(java.time.LocalDateTime transactionDate) {
        // Standard refund window is 30 days
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(30);
        return transactionDate.isAfter(cutoffDate);
    }
}

