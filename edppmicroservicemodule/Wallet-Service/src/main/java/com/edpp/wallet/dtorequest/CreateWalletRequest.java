package com.edpp.wallet.dtorequest;

import com.edpp.wallet.enums.WalletType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateWalletRequest(
    @NotBlank(message = "Customer ID is required")
    String customerId,

    @NotNull(message = "Wallet type is required")
    WalletType walletType,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    String currency
) {}
