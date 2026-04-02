package com.edpp.wallet.dtoresponse;

import com.edpp.wallet.enums.CustomerStatus;
import com.edpp.wallet.enums.RiskRating;

public record CustomerResponse(
    String id,
    String cifNumber,
    String fullName,
    String email,
    String phoneNumber,
    CustomerStatus status,
    RiskRating riskRating,
    boolean kycCompleted,
    String tenantId
) 
 {
    // Add method to check if customer is active
    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }
    
    // Add method to check if customer is blocked
    public boolean isBlocked() {
        return status == CustomerStatus.BLOCKED;
    }
    
    // Add method to check if KYC is completed
    public boolean isKycCompleted() {
        return kycCompleted;
    }
    
    // Add method to get display name
    public String getDisplayName() {
        return fullName != null ? fullName : email;
    }
}
