package com.edpp.merchant.service;

import com.edpp.merchant.entity.MerchantWebhook;
import com.edpp.merchant.repository.MerchantWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final MerchantWebhookRepository webhookRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send webhook notification
     */
    @Async
    public void sendWebhook(String merchantId, String eventType, Map<String, Object> payload) {
        var webhooks = webhookRepository.findByMerchantIdAndIsActiveTrue(merchantId);
        
        for (MerchantWebhook webhook : webhooks) {
            if (shouldSendEvent(webhook, eventType)) {
                sendWebhookWithRetry(webhook, eventType, payload);
            }
        }
    }

    private void sendWebhookWithRetry(MerchantWebhook webhook, String eventType, Map<String, Object> payload) {
        int attempt = 0;
        while (attempt < webhook.getRetryCount()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Webhook-Signature", generateSignature(payload));

                Map<String, Object> webhookPayload = new HashMap<>();
                webhookPayload.put("event", eventType);
                webhookPayload.put("timestamp", System.currentTimeMillis());
                webhookPayload.put("data", payload);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(webhookPayload, headers);
                restTemplate.postForEntity(webhook.getUrl(), entity, String.class);
                
                log.info("Webhook sent successfully to: {} for event: {}", webhook.getUrl(), eventType);
                break;
                
            } catch (Exception e) {
                attempt++;
                log.error("Webhook attempt {} failed for {}: {}", attempt, webhook.getUrl(), e.getMessage());
                if (attempt >= webhook.getRetryCount()) {
                    log.error("Webhook failed after {} attempts", webhook.getRetryCount());
                }
            }
        }
    }

    private boolean shouldSendEvent(MerchantWebhook webhook, String eventType) {
        if (webhook.getEvents() == null || webhook.getEvents().isEmpty()) {
            return true;
        }
        return webhook.getEvents().contains(eventType);
    }

    private String generateSignature(Map<String, Object> payload) {
        // In production, generate HMAC signature using webhook secret
        return "signature_placeholder";
    }
}