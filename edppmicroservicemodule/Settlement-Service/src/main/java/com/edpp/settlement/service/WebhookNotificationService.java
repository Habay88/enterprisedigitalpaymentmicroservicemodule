package com.edpp.settlement.service;

import com.edpp.settlement.entity.MerchantSettlementConfig;
import com.edpp.settlement.entity.Settlement;
import com.edpp.settlement.repository.MerchantSettlementConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationService {

    private final MerchantSettlementConfigRepository configRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send webhook notification to merchant
     */
    public void notifyMerchant(Settlement settlement) {
        var config = configRepository.findByMerchantIdAndTenantId(
                settlement.getMerchantId(), settlement.getTenantId()).orElse(null);
        
        if (config == null || !config.isSendWebhook() || config.getWebhookUrl() == null) {
            log.debug("Webhook not configured for merchant: {}", settlement.getMerchantId());
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "settlement.completed");
            payload.put("settlementReference", settlement.getSettlementReference());
            payload.put("merchantId", settlement.getMerchantId());
            payload.put("settlementDate", settlement.getSettlementDate());
            payload.put("grossAmount", settlement.getGrossAmount());
            payload.put("totalFees", settlement.getTotalFees());
            payload.put("netAmount", settlement.getNetAmount());
            payload.put("transferReference", settlement.getTransferReference());
            payload.put("status", settlement.getStatus().name());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", generateSignature(payload));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(config.getWebhookUrl(), entity, String.class);
            log.info("Webhook sent to merchant: {}", settlement.getMerchantId());
            
        } catch (Exception e) {
            log.error("Failed to send webhook to merchant: {}", settlement.getMerchantId(), e);
        }
    }

    private String generateSignature(Map<String, Object> payload) {
        // In production, generate HMAC signature
        return "signature_placeholder";
    }
}