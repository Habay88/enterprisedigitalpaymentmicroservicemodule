package com.edpp.transaction.dtoresponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.edpp.transaction.enums.TransactionStatus;
import com.edpp.transaction.enums.TransactionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String id;
    private String transactionReference;
    private String merchantTransactionId;
    private TransactionType type;
    private TransactionStatus status;
    private String sourceWalletId;
    private String destinationWalletId;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentMethod;
    private String processorName;
    private String processorTransactionId;
    private String processorResponseCode;
    private String processorResponseMessage;
    private String description;
    private String customerId;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime transactionDate;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;    
}