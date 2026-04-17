package com.edpp.merchant.controller;

import com.edpp.merchant.dto.response.ApiKeyResponse;
import com.edpp.merchant.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchants/{merchantCode}/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final MerchantService merchantService;

    @PostMapping("/generate")
    @Operation(summary = "Generate new API keys")
    public ResponseEntity<ApiKeyResponse> generateApiKeys(@PathVariable String merchantCode) {
        Merchant merchant = merchantService.getMerchantByCode(merchantCode);
        ApiKeyResponse response = apiKeyService.generateApiKeys(merchant.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    @Operation(summary = "Revoke API keys")
    public ResponseEntity<Void> revokeApiKeys(@PathVariable String merchantCode) {
        Merchant merchant = merchantService.getMerchantByCode(merchantCode);
        apiKeyService.revokeApiKeys(merchant.getId());
        return ResponseEntity.noContent().build();
    }
}