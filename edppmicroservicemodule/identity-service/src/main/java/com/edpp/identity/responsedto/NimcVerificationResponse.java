package com.edpp.identity.responsedto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class NimcVerificationResponse {
    private boolean successful;
    private String message;
    private String responseCode;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
}