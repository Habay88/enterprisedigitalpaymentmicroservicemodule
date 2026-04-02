package com.edpp.wallet.service;

import com.edpp.wallet.entity.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.wallet-events:wallet-events}")
    private String walletEventsTopic;

    /**
     * Publish wallet created event
     */
    public void publishWalletCreated(Wallet wallet) {
        Map<String, Object> event = buildBaseEvent(wallet, "WALLET_CREATED");
        event.put("walletType", wallet.getWalletType());
        event.put("currency", wallet.getCurrency());
        sendEvent(wallet.getId(), event);
    }

    /**
     * Publish wallet credited event
     */
    public void publishWalletCredited(Wallet wallet, BigDecimal amount) {
        Map<String, Object> event = buildBaseEvent(wallet, "WALLET_CREDITED");
        event.put("amount", amount);
        event.put("newBalance", wallet.getBalance());
        sendEvent(wallet.getId(), event);
    }

    /**
     * Publish wallet debited event
     */
    public void publishWalletDebited(Wallet wallet, BigDecimal amount) {
        Map<String, Object> event = buildBaseEvent(wallet, "WALLET_DEBITED");
        event.put("amount", amount);
        event.put("newBalance", wallet.getBalance());
        sendEvent(wallet.getId(), event);
    }

    /**
     * Publish wallet frozen event
     */
    public void publishWalletFrozen(Wallet wallet, String reason) {
        Map<String, Object> event = buildBaseEvent(wallet, "WALLET_FROZEN");
        event.put("reason", reason);
        sendEvent(wallet.getId(), event);
    }

    /**
     * Publish wallet unfrozen event
     */
    public void publishWalletUnfrozen(Wallet wallet, String reason) {
        Map<String, Object> event = buildBaseEvent(wallet, "WALLET_UNFROZEN");
        event.put("reason", reason);
        sendEvent(wallet.getId(), event);
    }

    private Map<String, Object> buildBaseEvent(Wallet wallet, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", eventType);
        event.put("timestamp", LocalDateTime.now());
        event.put("walletId", wallet.getId());
        event.put("walletNumber", wallet.getWalletNumber());
        event.put("customerId", wallet.getCustomerId());
        event.put("tenantId", wallet.getTenantId());
        event.put("balance", wallet.getBalance());
        return event;
    }

    private void sendEvent(String key, Map<String, Object> event) {
        try {
            kafkaTemplate.send(walletEventsTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Event published: {}", event.get("eventType"));
                        } else {
                            log.error("Failed to publish event: {}", event.get("eventType"), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending Kafka event", e);
        }
    }
}