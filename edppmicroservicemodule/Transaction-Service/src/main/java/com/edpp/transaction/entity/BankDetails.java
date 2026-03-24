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
public class BankDetails implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String accountNumber;      // Bank account number
    private String accountName;        // Account holder name
    private String routingNumber;      // Routing/ABA number
    private String swiftCode;          // SWIFT/BIC code for international transfers
    private String bankName;           // Name of the bank
    private String bankCode;           // Bank code (e.g., 001 for Central Bank)
    private String branchCode;         // Branch code
    private String accountType;        // SAVINGS, CURRENT, etc.
    private String beneficiaryName;     // Name of the beneficiary
    private String beneficiaryAddress;  // Address of the beneficiary
    private String iban;               // IBAN for international accounts
    private String sortCode;           // Sort code for UK accounts
    private String ifscCode;           // IFSC code for Indian accounts
    
    /**
     * Get masked account number (show only last 4 digits)
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "****" + lastFour;
    }
    
    /**
     * Get full account details for display
     */
    public String getDisplayDetails() {
        return String.format("%s - %s (%s)", 
            bankName != null ? bankName : "Unknown", 
            getMaskedAccountNumber(),
            accountType != null ? accountType : "Unknown");
    }
    
    /**
     * Validate if account number is valid (basic validation)
     */
    public boolean isValidAccountNumber() {
        if (accountNumber == null) {
            return false;
        }
        // Nigerian account numbers are 10 digits
        // International accounts may have different lengths
        return accountNumber.matches("^[0-9]{10,20}$");
    }
    
    /**
     * Validate if routing number is valid
     */
    public boolean isValidRoutingNumber() {
        if (routingNumber == null) {
            return false;
        }
        // US routing numbers are 9 digits
        return routingNumber.matches("^[0-9]{9}$");
    }
    
    /**
     * Validate if SWIFT code is valid
     */
    public boolean isValidSwiftCode() {
        if (swiftCode == null) {
            return false;
        }
        // SWIFT codes are 8 or 11 characters
        return swiftCode.matches("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    }
    
    /**
     * Get bank name from bank code (for Nigerian banks)
     */
    public static String getBankNameFromCode(String bankCode) {
        if (bankCode == null) return null;
        
        return switch (bankCode) {
            case "001" -> "Central Bank of Nigeria";
            case "002" -> "First Bank of Nigeria";
            case "003" -> "United Bank for Africa";
            case "004" -> "Access Bank";
            case "005" -> "Zenith Bank";
            case "006" -> "Guaranty Trust Bank";
            case "007" -> "Ecobank Nigeria";
            case "008" -> "Stanbic IBTC Bank";
            case "009" -> "Union Bank of Nigeria";
            case "010" -> "Fidelity Bank";
            case "011" -> "Polaris Bank";
            case "012" -> "Keystone Bank";
            case "013" -> "Wema Bank";
            case "014" -> "Heritage Bank";
            case "015" -> "Unity Bank";
            default -> "Other Bank";
        };
    }
    
    /**
     * Create Nigerian bank details
     */
    public static BankDetails createNigerianBankDetails(String accountNumber, 
                                                         String bankCode, 
                                                         String accountName) {
        return BankDetails.builder()
                .accountNumber(accountNumber)
                .accountName(accountName)
                .bankName(getBankNameFromCode(bankCode))
                .bankCode(bankCode)
                .accountType("SAVINGS")
                .build();
    }
}