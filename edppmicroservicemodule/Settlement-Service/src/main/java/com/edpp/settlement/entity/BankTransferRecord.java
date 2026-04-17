package com.edpp.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transfer_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String settlementId;

    @Column(unique = true, nullable = false)
    private String transferReference;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String bankAccountNumber;

    @Column(nullable = false)
    private String bankCode;

    @Column(nullable = false)
    private String bankName;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    private String responseCode;
    private String responseMessage;

    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    private Integer retryCount;

    private String tenantId;
}