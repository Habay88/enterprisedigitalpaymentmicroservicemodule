package com.edpp.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidKycDataException extends RuntimeException{

    @Getter
    private final String reason;

    public InvalidKycDataException(String reason) {
       super(String.format("Invalid KYC data: %s", reason));
        this.reason = reason;
    }
}
