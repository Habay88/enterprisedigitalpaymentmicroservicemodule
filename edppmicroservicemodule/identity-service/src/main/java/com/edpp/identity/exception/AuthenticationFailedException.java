package com.edpp.identity.exception;

import lombok.Getter;

public class AuthenticationFailedException extends RuntimeException{
    @Getter
    private final String email;

    public AuthenticationFailedException(String email, String message) {
        super(message);
        this.email = email;
    }
}
