package com.edpp.transaction.service;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.edpp.transaction.dtoresponse.WalletValidationResponse;

@FeignClient(name = "wallet-service", path = "/api/v1/wallets")
public interface WalletServiceClient {

    @GetMapping("/{walletNumber}/validate")
    WalletValidationResponse validateWallet(@PathVariable("walletNumber") String walletNumber);

    @PostMapping("/{walletNumber}/debit")
    void debitWallet(@PathVariable("walletNumber") String walletNumber,
                     @RequestParam("amount") BigDecimal amount,
                     @RequestParam("reference") String reference);

    @PostMapping("/{walletNumber}/credit")
    void creditWallet(@PathVariable("walletNumber") String walletNumber,
                      @RequestParam("amount") BigDecimal amount,
                      @RequestParam("reference") String reference);

    @GetMapping("/{walletNumber}/balance")
    BigDecimal getWalletBalance(@PathVariable("walletNumber") String walletNumber);
}
