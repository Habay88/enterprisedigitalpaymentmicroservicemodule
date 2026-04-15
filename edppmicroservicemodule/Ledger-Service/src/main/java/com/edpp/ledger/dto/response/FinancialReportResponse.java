package com.edpp.ledger.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Financial Report Response - DTO for financial reports
 */
@Data
@Builder
public class FinancialReportResponse {
    private String reportType;
    private LocalDate reportDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Map<String, BigDecimal> assets;
    private Map<String, BigDecimal> liabilities;
    private Map<String, BigDecimal> equity;
    private Map<String, BigDecimal> revenue;
    private Map<String, BigDecimal> expenses;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal netIncome;
    private boolean isBalanced;
    
    /**
     * Calculate total assets
     */
    public BigDecimal calculateTotalAssets() {
        if (assets == null) return BigDecimal.ZERO;
        return assets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate total liabilities
     */
    public BigDecimal calculateTotalLiabilities() {
        if (liabilities == null) return BigDecimal.ZERO;
        return liabilities.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate total equity
     */
    public BigDecimal calculateTotalEquity() {
        if (equity == null) return BigDecimal.ZERO;
        return equity.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Verify accounting equation: Assets = Liabilities + Equity
     */
    public boolean verifyAccountingEquation() {
        BigDecimal totalAssets = calculateTotalAssets();
        BigDecimal totalLiabilitiesEquity = calculateTotalLiabilities().add(calculateTotalEquity());
        return totalAssets.compareTo(totalLiabilitiesEquity) == 0;
    }
}