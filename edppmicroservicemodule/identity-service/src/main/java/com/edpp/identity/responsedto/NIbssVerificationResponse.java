package com.edpp.identity.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
class NibssVerificationResponse {
    private boolean successful;
    private String message;
    private String responseCode;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDateTime dateOfBirth;
}
