package com.edpp.transaction.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDetails {

    private String maskedPan;
    private String cardType;
    private String expiryMonth;
    private String expiryYear;
    private String cardholderName;
    private String issuerCountry;
    private String authorizationCode;
    private String cardBrand;
}
