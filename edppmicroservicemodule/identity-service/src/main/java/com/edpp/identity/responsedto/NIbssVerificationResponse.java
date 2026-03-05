package com.edpp.identity.responsedto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NibssVerificationResponse {
    private boolean successful;
    private String message;
    private String responseCode;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDateTime dateOfBirth;
}
