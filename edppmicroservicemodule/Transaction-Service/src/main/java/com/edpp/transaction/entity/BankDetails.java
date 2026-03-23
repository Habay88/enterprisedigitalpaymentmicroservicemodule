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
public class BankDetails {
    private String accountNumber;
    private String routingNumber;
    private String bankName;
    private String accountType;
    private String beneficiaryName;
    private String beneficiaryAddress;
}
