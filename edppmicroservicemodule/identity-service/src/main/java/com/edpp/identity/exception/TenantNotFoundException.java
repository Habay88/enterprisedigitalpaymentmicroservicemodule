package com.edpp.identity.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantNotFoundException extends RuntimeException {
    
    private final String field;
    private final String value;
    
    public TenantNotFoundException(String field, String value) {
        super(String.format("Tenant not found with %s: %s", field, value));
        this.field = field;
        this.value = value;
    }
    
    public TenantNotFoundException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }
    
    public String getField() {
        return field;
    }
    
    public String getValue() {
        return value;
    }
}
