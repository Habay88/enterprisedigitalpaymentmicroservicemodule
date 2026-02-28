package com.edpp.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends RuntimeException{

    @Getter
    private final String identifier;

    public CustomerNotFoundException(String message) {
        super(message);
        this.identifier = null;
    }
    public CustomerNotFoundException(String field, String value) {
        super(String.format("Customer not found with %s: %s", field, value));
        this.identifier = value;
    }
}
