package com.edpp.identity.model;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class NinVerification {
    private boolean verified;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private String verificationReference;
    private String responseCode;
    private String responseMessage;
}
