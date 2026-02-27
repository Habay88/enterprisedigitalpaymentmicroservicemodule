package com.edpp.identity.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDetails {
    private String idType; // PASSPORT, DRIVERS_LICENSE, NATIONAL_ID
    private String idNumber;
    private LocalDateTime idExpiryDate;
    private boolean kycCompleted;
    private LocalDateTime kycVerifiedAt;
    private String kycVerifiedBy;
}