package com.edpp.ledger.controller;

import com.edpp.ledger.dto.response.response.FinancialReportResponse;
import com.edpp.ledger.dto.response.response.TrialBalanceResponse;
import com.edpp.ledger.service.FinancialReportService;
import com.edpp.ledger.service.TrialBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ledger/reports")
@RequiredArgsConstructor
@Tag(name = "Financial Reports", description = "Accounting and financial reports")
public class ReportController {

    private final TrialBalanceService trialBalanceService;
    private final FinancialReportService financialReportService;

    @GetMapping("/trial-balance")
    @Operation(summary = "Generate trial balance")
    public ResponseEntity<TrialBalanceResponse> getTrialBalance(@RequestParam LocalDate asOfDate) {
        TrialBalanceResponse response = trialBalanceService.generateTrialBalance(asOfDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance-sheet")
    @Operation(summary = "Generate balance sheet")
    public ResponseEntity<FinancialReportResponse> getBalanceSheet(@RequestParam LocalDate asOfDate) {
        FinancialReportResponse response = financialReportService.generateBalanceSheet(asOfDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/income-statement")
    @Operation(summary = "Generate income statement (P&L)")
    public ResponseEntity<FinancialReportResponse> getIncomeStatement(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        FinancialReportResponse response = financialReportService.generateIncomeStatement(startDate, endDate);
        return ResponseEntity.ok(response);
    }
}