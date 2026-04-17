package com.edpp.settlement.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Bank API Client - Handles communication with external bank APIs
 * 
 * This client abstracts the complexity of different bank integration protocols.
 * It supports:
 * - NIBSS (Nigeria Inter-Bank Settlement System)
 * - NAPS (Nigerian Automated Payment System)
 * - Individual bank APIs
 */
@Component
@Slf4j
public class BankAPIClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${bank.api.base-url:https://api.nibss.com}")
    private String baseUrl;

    @Value("${bank.api.key:test-key}")
    private String apiKey;

    @Value("${bank.api.secret:test-secret}")
    private String apiSecret;

    /**
     * Transfer funds to merchant bank account
     */
    public BankTransferResponse transferFunds(String accountNumber, String bankCode, 
                                               BigDecimal amount, String reference) {
        log.info("Initiating bank transfer: Account: {}, Bank: {}, Amount: {}, Reference: {}", 
                accountNumber, bankCode, amount, reference);

        try {
            // Build request payload
            Map<String, Object> request = new HashMap<>();
            request.put("accountNumber", accountNumber);
            request.put("bankCode", bankCode);
            request.put("amount", amount);
            request.put("reference", reference);
            request.put("narration", "Settlement payment from EDPP");
            request.put("currency", "NGN");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            headers.set("X-API-Secret", apiSecret);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Call bank API
            String url = baseUrl + "/api/v1/transfers";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                boolean success = "00".equals(responseBody.get("responseCode"));

                return new BankTransferResponse(
                    success,
                    (String) responseBody.get("transactionId"),
                    (String) responseBody.get("responseCode"),
                    (String) responseBody.get("responseMessage")
                );
            }

            return new BankTransferResponse(false, null, "99", "Transfer failed");

        } catch (Exception e) {
            log.error("Bank transfer failed", e);
            return new BankTransferResponse(false, null, "98", e.getMessage());
        }
    }

    /**
     * Query transfer status
     */
    public BankTransferResponse queryTransferStatus(String transactionId) {
        log.info("Querying transfer status: {}", transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            headers.set("X-API-Secret", apiSecret);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = baseUrl + "/api/v1/transfers/" + transactionId;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");
                boolean success = "COMPLETED".equals(status);

                return new BankTransferResponse(
                    success,
                    transactionId,
                    (String) responseBody.get("responseCode"),
                    (String) responseBody.get("responseMessage")
                );
            }

            return new BankTransferResponse(false, transactionId, "99", "Status query failed");

        } catch (Exception e) {
            log.error("Transfer status query failed", e);
            return new BankTransferResponse(false, transactionId, "98", e.getMessage());
        }
    }

    /**
     * Response record for bank transfer
     */
    public record BankTransferResponse(boolean isSuccess, String transactionId, 
                                        String responseCode, String message) {}
}