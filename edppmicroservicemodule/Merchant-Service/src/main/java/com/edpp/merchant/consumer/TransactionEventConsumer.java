package com.edpp.merchant.consumer;

import com.edpp.merchant.service.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"transaction-events", "settlement-events"}, groupId = "merchant-group")
    public void consumeEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("eventType").asText();
            String merchantId = event.has("merchantId") ? event.get("merchantId").asText() : null;

            if (merchantId != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("eventType", eventType);
                payload.put("data", event);
                
                webhookService.sendWebhook(merchantId, eventType, payload);
            }
        } catch (Exception e) {
            log.error("Error processing event", e);
        }
    }
}