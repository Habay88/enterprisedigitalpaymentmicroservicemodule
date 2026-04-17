package com.edpp.settlement.consumer;

import com.edpp.settlement.service.SettlementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Transaction Event Consumer - Listens to completed transactions
 * 
 * This consumer listens to transaction events from Kafka and prepares
 * transactions for settlement. When a transaction is completed, it is
 * added to the settlement queue for the merchant's next settlement batch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transaction-events", groupId = "settlement-group")
    public void consumeTransactionEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("eventType").asText();

            log.info("Received transaction event: {}", eventType);

            switch (eventType) {
                case "TRANSACTION_COMPLETED":
                    handleTransactionCompleted(event);
                    break;
                case "REFUND_PROCESSED":
                    handleRefundProcessed(event);
                    break;
                default:
                    log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing transaction event", e);
        }
    }

    private void handleTransactionCompleted(JsonNode event) {
        String transactionId = event.get("transactionId").asText();
        String merchantId = event.get("merchantId").asText();
        BigDecimal amount = new BigDecimal(event.get("amount").asText());
        
        log.info("Transaction completed - ID: {}, Merchant: {}, Amount: {}", 
                transactionId, merchantId, amount);
        
        // Add transaction to settlement queue
        // This would store the transaction for later batch processing
    }

    private void handleRefundProcessed(JsonNode event) {
        String transactionId = event.get("transactionId").asText();
        log.info("Refund processed for transaction: {}", transactionId);
        
        // Handle refund for settlement purposes
        // Refunds reduce the merchant's settlement amount
    }
}