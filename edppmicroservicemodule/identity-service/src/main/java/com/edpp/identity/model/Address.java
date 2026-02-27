package com.edpp.identity.model;

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
public class Address {
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
}
