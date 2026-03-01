package com.edpp.identity.response;


import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.CustomerType;
import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.model.Address;
import com.edpp.identity.model.KycDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private String id;
    private String cifNumber;
    private String fullName;
    private String email;
    private String phoneNumber;
    private CustomerType customerType;
    private CustomerStatus status;
    private RiskRating riskRating;
    private boolean kycCompleted;
    private KycDetails kycDetails;
    private Address address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
