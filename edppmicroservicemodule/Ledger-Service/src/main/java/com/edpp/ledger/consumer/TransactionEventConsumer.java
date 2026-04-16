package com.edpp.ledger.consumer;

import com.edpp.ledger.service.LedgerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Transaction Event Consumer - Listens to transaction events from Kafka
 * 
 * This consumer automatically creates journal entries when transactions
 * are processed by the Transaction Service.
 * 
 * Event Types:
 * - TRANSACTION_COMPLETED: Create journal entry for successful payment
 * - TRANSACTION_FAILED: No journal entry created
 * - TRANSACTION_REVERSED: Reverse the original journal entry
 * - REFUND_PROCESSED: Create refund journal entry
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transaction-events", groupId = "ledger-group")
    public void consumeTransactionEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("eventType").asText();
            String transactionId = event.get("transactionId").asText();

            log.info("Received transaction event: {} for transaction: {}", eventType, transactionId);

            switch (eventType) {
                case "TRANSACTION_COMPLETED":
                    handleTransactionCompleted(event);
                    break;
                case "TRANSACTION_REVERSED":
                    handleTransactionReversed(event);
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
        BigDecimal amount = new BigDecimal(event.get("amount").asText());
        String currency = event.get("currency").asText();

        ledgerService.postFromTransaction(
                transactionId,
                "PAYMENT",
                amount,
                currency,
                event.get("sourceWalletId").asText(),
                event.get("destinationWalletId").asText()
        );

        log.info("Posted journal entry for completed transaction: {}", transactionId);
    }

    private void handleTransactionReversed(JsonNode event) {
        String transactionId = event.get("transactionId").asText();
        // Find and reverse the original journal entry
        log.info("Need to reverse journal entry for transaction: {}", transactionId);
    }

    private void handleRefundProcessed(JsonNode event) {
        String transactionId = event.get("transactionId").asText();
        BigDecimal amount = new BigDecimal(event.get("refundAmount").asText());

        ledgerService.postFromTransaction(
                transactionId + "_REFUND",
                "REFUND",
                amount,
                "NGN",
                null,
                null
        );

        log.info("Posted journal entry for refund: {}", transactionId);
    }
}