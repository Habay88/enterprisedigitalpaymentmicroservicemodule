package com.edpp.merchant.controller;

import com.edpp.merchant.dto.request.MerchantOnboardingRequest;
import com.edpp.merchant.dto.request.MerchantUpdateRequest;
import com.edpp.merchant.dto.response.MerchantResponse;
import com.edpp.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant Management", description = "APIs for managing merchants")
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    @Operation(summary = "Onboard a new merchant")
    public ResponseEntity<MerchantResponse> onboardMerchant(@Valid @RequestBody MerchantOnboardingRequest request) {
        MerchantResponse response = merchantService.onboardMerchant(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{merchantCode}")
    @Operation(summary = "Get merchant by code")
    public ResponseEntity<MerchantResponse> getMerchant(@PathVariable String merchantCode) {
        MerchantResponse response = merchantService.getMerchantResponse(merchantCode);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{merchantCode}")
    @Operation(summary = "Update merchant")
    public ResponseEntity<MerchantResponse> updateMerchant(
            @PathVariable String merchantCode,
            @Valid @RequestBody MerchantUpdateRequest request) {
        MerchantResponse response = merchantService.updateMerchant(merchantCode, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all merchants")
    public ResponseEntity<Page<MerchantResponse>> getAllMerchants(Pageable pageable) {
        Page<MerchantResponse> merchants = merchantService.getAllMerchants(pageable);
        return ResponseEntity.ok(merchants);
    }

    @GetMapping("/search")
    @Operation(summary = "Search merchants")
    public ResponseEntity<Page<MerchantResponse>> searchMerchants(
            @RequestParam String q,
            Pageable pageable) {
        Page<MerchantResponse> merchants = merchantService.searchMerchants(q, pageable);
        return ResponseEntity.ok(merchants);
    }

    @PatchMapping("/{merchantCode}/status")
    @Operation(summary = "Update merchant status")
    public ResponseEntity<MerchantResponse> updateStatus(
            @PathVariable String merchantCode,
            @RequestParam MerchantStatus status,
            @RequestParam(required = false) String reason) {
        MerchantResponse response = merchantService.updateStatus(merchantCode, status, reason);
        return ResponseEntity.ok(response);
    }
}