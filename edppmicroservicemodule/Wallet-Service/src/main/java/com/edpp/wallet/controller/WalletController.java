package com.edpp.wallet.controller;

import com.edpp.wallet.dto.request.CreateWalletRequest;
import com.edpp.wallet.dto.request.CreditRequest;
import com.edpp.wallet.dto.request.DebitRequest;
import com.edpp.wallet.dto.response.BalanceResponse;
import com.edpp.wallet.dto.response.WalletResponse;
import com.edpp.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet Management", description = "APIs for managing customer wallets/accounts")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a new wallet")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(
                request.customerId(),
                request.walletType(),
                request.currency()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{walletNumber}")
    @Operation(summary = "Get wallet by number")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable String walletNumber) {
        WalletResponse response = walletService.getWalletResponse(walletNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletNumber}/balance")
    @Operation(summary = "Get wallet balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String walletNumber) {
        BalanceResponse response = walletService.getBalance(walletNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/credit")
    @Operation(summary = "Credit wallet")
    public ResponseEntity<Void> creditWallet(@Valid @RequestBody CreditRequest request) {
        walletService.creditWallet(request, getTenantId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/debit")
    @Operation(summary = "Debit wallet")
    public ResponseEntity<Void> debitWallet(@Valid @RequestBody DebitRequest request) {
        walletService.debitWallet(request, getTenantId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{walletNumber}/freeze")
    @Operation(summary = "Freeze wallet")
    public ResponseEntity<WalletResponse> freezeWallet(
            @PathVariable String walletNumber,
            @RequestParam String reason) {
        WalletResponse response = walletService.freezeWallet(walletNumber, reason, getTenantId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{walletNumber}/unfreeze")
    @Operation(summary = "Unfreeze wallet")
    public ResponseEntity<WalletResponse> unfreezeWallet(
            @PathVariable String walletNumber,
            @RequestParam String reason) {
        WalletResponse response = walletService.unfreezeWallet(walletNumber, reason, getTenantId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all wallets for a customer")
    public ResponseEntity<List<WalletResponse>> getCustomerWallets(@PathVariable String customerId) {
        List<WalletResponse> wallets = walletService.getCustomerWallets(customerId);
        return ResponseEntity.ok(wallets);
    }

    private String getTenantId() {
        // In production,  we will get from the RequestContext
        return "DEFAULT_TENANT";
    }
}
