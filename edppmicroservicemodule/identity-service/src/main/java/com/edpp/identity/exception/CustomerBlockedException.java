package com.edpp.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CustomerBlockedException extends RuntimeException{
    @Getter
    private final String customerId;
    @Getter
    private final String blockReason;

    public CustomerBlockedException(String customerId,String blockReason) {
        super(String.format("Customer %s is blocked. Reason: %s", customerId, blockReason));
        this.customerId = customerId;
        this.blockReason = blockReason;
    }
}
