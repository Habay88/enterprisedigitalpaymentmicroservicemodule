package com.edpp.transaction.dtorequest;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

import com.edpp.transaction.entity.BankDetails;
import com.edpp.transaction.entity.CardDetails;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Merchant transaction ID is required")
    private String merchantTransactionId;

    @NotBlank(message = "Source wallet ID is required")
    private String sourceWalletId;

    private String destinationWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "10000000", message = "Amount exceeds maximum limit")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    private String currency;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String description;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @Email(message = "Invalid email format")
    private String customerEmail;

    private String customerPhone;

    private CardDetails cardDetails;

    private BankDetails bankDetails;

    private Boolean savePaymentMethod;

    private String callbackUrl;

    private Map<String, String> metadata;
}