package com.edpp.identity.config;

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
public class TenantConfiguration {
    private boolean enableBvnValidation;
    private boolean enableNinValidation;
    private boolean enforceKyc;
    private Integer maxDailyTransactionLimit;
    private Integer maxMonthlyTransactionLimit;
    private String supportedCurrencies;
    private String defaultLanguage;
    private String timezone;
}
