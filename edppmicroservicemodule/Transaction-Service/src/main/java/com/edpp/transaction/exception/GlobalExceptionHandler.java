package com.edpp.transaction.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.edpp.transaction.dtoresponse.ApiResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex, 
                                                                        WebRequest request) {
        log.error("Insufficient balance error: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getWalletId() != null) {
            details.put("walletId", ex.getWalletId());
        }
        if (ex.getRequiredAmount() != null) {
            details.put("requiredAmount", ex.getRequiredAmount());
        }
        if (ex.getAvailableBalance() != null) {
            details.put("availableBalance", ex.getAvailableBalance());
        }
        if (ex.getShortfall() != null && ex.getShortfall().compareTo(java.math.BigDecimal.ZERO) > 0) {
            details.put("shortfall", ex.getShortfall());
        }

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .error("INSUFFICIENT_BALANCE")
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.PAYMENT_REQUIRED);
    }

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionException(TransactionException ex, 
                                                                         WebRequest request) {
        log.error("Transaction error: {} - {}", ex.getCode(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getTransactionReference() != null) {
            details.put("transactionReference", ex.getTransactionReference());
        }
        if (ex.getTransactionId() != null) {
            details.put("transactionId", ex.getTransactionId());
        }

        HttpStatus status = getHttpStatusForErrorCode(ex.getCode());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .error(ex.getCode())
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(ProcessorException.class)
    public ResponseEntity<ApiResponse<Void>> handleProcessorException(ProcessorException ex, 
                                                                       WebRequest request) {
        log.error("Processor error: {} - {}", ex.getProcessorName(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getProcessorName() != null) {
            details.put("processor", ex.getProcessorName());
        }
        if (ex.getErrorCode() != null) {
            details.put("errorCode", ex.getErrorCode());
        }
        if (ex.getTransactionId() != null) {
            details.put("transactionId", ex.getTransactionId());
        }

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Payment processor error: " + ex.getMessage())
                .error(ex.getErrorCode() != null ? ex.getErrorCode() : "PROCESSOR_ERROR")
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.error("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> ((FieldError) error).getField(),
                        error -> error.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .error("VALIDATION_ERROR")
                .data(errors)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("An unexpected error occurred")
                .error("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus getHttpStatusForErrorCode(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        
        return switch (errorCode) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE" -> HttpStatus.CONFLICT;
            case "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
            case "INVALID_STATUS" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "PROCESSING_FAILED" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}