package com.edpp.iso8583.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iso_message_logs", indexes = {
        @Index(name = "idx_iso_stan", columnList = "stan"),
        @Index(name = "idx_iso_mti", columnList = "mti"),
        @Index(name = "idx_iso_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IsoMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String mti;
    private String stan;
    private String pan;
    private String amount;
    private String terminalId;
    private String merchantId;
    private String responseCode;
    private String rawMessage;

    @Column(length = 4000)
    private String parsedData;

    private String direction; // INBOUND or OUTBOUND

    @CreationTimestamp
    private LocalDateTime createdAt;
}