package com.edpp.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateCustomerException extends RuntimeException{

    @Getter
    private final String field;
    @Getter
    private final String value;
    public DuplicateCustomerException(String message){
        super(message);
        this.field=null;
        this.value=null;
    }

    public DuplicateCustomerException( String field, String value) {
        super(String.format("Customer with %s '%s' already exists",field,value));
        this.field = field;
        this.value = value;


    }
}
