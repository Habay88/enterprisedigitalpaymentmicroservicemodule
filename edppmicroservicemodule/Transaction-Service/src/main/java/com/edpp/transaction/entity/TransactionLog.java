package com.edpp.transaction.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import com.edpp.transaction.enums.TransactionStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    private TransactionStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private TransactionStatus newStatus;

    @Column(length = 1000)
    private String message;

    private String changedBy;

    @Column(length = 2000)
    private String metadata;

    @CreationTimestamp
    private LocalDateTime createdAt;
}