package com.edpp.transaction.dtoresponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessorResponse {
    private boolean successful;
    private String transactionId;
    private String responseCode;
    private String responseMessage;
    private String rawResponse;
    private LocalDateTime processedAt;
    private String authorizationUrl;
    
    // Helper methods
    public static ProcessorResponse success(String transactionId) {
        return ProcessorResponse.builder()
                .successful(true)
                .transactionId(transactionId)
                .responseCode("00")
                .responseMessage("SUCCESS")
                .processedAt(LocalDateTime.now())
                .build();
    }
    
    public static ProcessorResponse failure(String message) {
        return ProcessorResponse.builder()
                .successful(false)
                .responseCode("99")
                .responseMessage(message)
                .processedAt(LocalDateTime.now())
                .build();
    }
}