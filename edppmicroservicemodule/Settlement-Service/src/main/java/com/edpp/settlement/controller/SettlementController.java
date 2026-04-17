package com.edpp.settlement.controller;

import com.edpp.settlement.dto.response.SettlementResponse;
import com.edpp.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@Tag(name = "Settlement Management", description = "APIs for managing merchant settlements")
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/{reference}")
    @Operation(summary = "Get settlement by reference")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable String reference) {
        SettlementResponse response = settlementService.getSettlementByReference(reference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get all settlements for a merchant")
    public ResponseEntity<List<SettlementResponse>> getMerchantSettlements(@PathVariable String merchantId) {
        List<SettlementResponse> settlements = settlementService.getMerchantSettlements(merchantId);
        return ResponseEntity.ok(settlements);
    }
}