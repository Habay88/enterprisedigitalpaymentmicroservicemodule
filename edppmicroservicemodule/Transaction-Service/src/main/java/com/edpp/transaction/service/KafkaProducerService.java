package com.edpp.transaction.service;

import com.edpp.transaction.entity.Transaction;
import com.edpp.transaction.util.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RequestContext requestContext;

    @Value("${spring.kafka.producer.topic.transaction-events:transaction-events}")
    private String transactionEventsTopic;

    @Value("${spring.kafka.producer.topic.payment-events:payment-events}")
    private String paymentEventsTopic;

    @Value("${spring.kafka.producer.topic.audit-events:audit-events}")
    private String auditEventsTopic;

    @Value("${spring.kafka.producer.topic.notification-events:notification-events}")
    private String notificationEventsTopic;

    @Value("${spring.kafka.producer.topic.fraud-events:fraud-events}")
    private String fraudEventsTopic;

    // Track published events for deduplication
    private final Map<String, LocalDateTime> publishedEvents = new ConcurrentHashMap<>();

   /*  public KafkaProducerService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    } */
   @PostConstruct
public void init() {
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
}

    /**
     * Get current user from RequestContext
     */
    private String getCurrentUser() {
        return requestContext != null && requestContext.getUserId() != null 
                ? requestContext.getUserId() : "SYSTEM";
    }

    /**
     * Get current IP from RequestContext
     */
    private String getCurrentIp() {
        return requestContext != null && requestContext.getClientIp() != null 
                ? requestContext.getClientIp() : "UNKNOWN";
    }

    /**
     * Get user agent from RequestContext
     */
    private String getUserAgent() {
        return requestContext != null && requestContext.getUserAgent() != null 
                ? requestContext.getUserAgent() : "UNKNOWN";
    }

    /**
     * Get request ID from RequestContext
     */
    private String getRequestId() {
        return requestContext != null && requestContext.getRequestId() != null 
                ? requestContext.getRequestId() : RequestContext.getCurrentRequestId();
    }

    // ==================== TRANSACTION EVENTS ====================

    /**
     * Publish transaction created event
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<SendResult<String, Object>> publishTransactionCreated(Transaction transaction) {
        Map<String, Object> event = buildBaseEvent(transaction, "TRANSACTION_CREATED");
        event.put("status", transaction.getStatus().name());
        event.put("amount", transaction.getAmount());
        event.put("currency", transaction.getCurrency());
        event.put("paymentMethod", transaction.getPaymentMethod());
        event.put("sourceWalletId", transaction.getSourceWalletId());
        event.put("destinationWalletId", transaction.getDestinationWalletId());

        return sendEvent(transactionEventsTopic, transaction.getId(), event);
    }

    /**
     * Publish transaction processing started event
     */
    public CompletableFuture<SendResult<String, Object>> publishTransactionProcessing(Transaction transaction) {
        Map<String, Object> event = buildBaseEvent(transaction, "TRANSACTION_PROCESSING");
        event.put("status", transaction.getStatus().name());
        event.put("processorName", transaction.getProcessorName());
        event.put("amount", transaction.getAmount());

        return sendEvent(transactionEventsTopic, transaction.getId(), event);
    }

    /**
     * Publish transaction completed event
     */
    public CompletableFuture<SendResult<String, Object>> publishTransactionCompleted(Transaction transaction) {
        Map<String, Object> event = buildBaseEvent(transaction, "TRANSACTION_COMPLETED");
        event.put("status", transaction.getStatus().name());
        event.put("processorTransactionId", transaction.getProcessorTransactionId());
        event.put("settlementDate", transaction.getSettledAt());
        event.put("totalAmount", transaction.getTotalAmount());
        event.put("fee", transaction.getFee());

        return sendEvent(transactionEventsTopic, transaction.getId(), event);
    }

    /**
     * Publish transaction failed event
     */
    public CompletableFuture<SendResult<String, Object>> publishTransactionFailed(Transaction transaction) {
        Map<String, Object> event = buildBaseEvent(transaction, "TRANSACTION_FAILED");
        event.put("status", transaction.getStatus().name());
        event.put("failureReason", transaction.getProcessorResponseMessage());
        event.put("failedAt", transaction.getFailedAt());
        event.put("failureCode", transaction.getProcessorResponseCode());

        return sendEvent(transactionEventsTopic, transaction.getId(), event);
    }

    /**
     * Publish transaction reversed event
     */
    public CompletableFuture<SendResult<String, Object>> publishTransactionReversed(Transaction transaction, String reason) {
        Map<String, Object> event = buildBaseEvent(transaction, "TRANSACTION_REVERSED");
        event.put("reason", reason);
        event.put("reversedAt", LocalDateTime.now());

        return sendEvent(transactionEventsTopic, transaction.getId(), event);
    }

    // ==================== PAYMENT EVENTS ====================

    /**
     * Publish payment authorized event
     */
    public CompletableFuture<SendResult<String, Object>> publishPaymentAuthorized(Transaction transaction,
                                                                                  String authorizationCode) {
        Map<String, Object> event = buildBaseEvent(transaction, "PAYMENT_AUTHORIZED");
        event.put("authorizationCode", authorizationCode);
        event.put("amount", transaction.getAmount());
        event.put("currency", transaction.getCurrency());
        event.put("authorizedAt", LocalDateTime.now());

        return sendEvent(paymentEventsTopic, transaction.getId(), event);
    }

    /**
     * Publish payment captured event
     */
    public CompletableFuture<SendResult<String, Object>> publishPaymentCaptured(Transaction transaction) {
        Map<String, Object> event = buildBaseEvent(transaction, "PAYMENT_CAPTURED");
        event.put("amount", transaction.getAmount());
        event.put("settlementDate", transaction.getSettledAt());
        event.put("captureReference", generateReference());

        return sendEvent(paymentEventsTopic, transaction.getId(), event);
    }

    /**
     * Publish refund processed event
     */
    public CompletableFuture<SendResult<String, Object>> publishRefundProcessed(Transaction originalTransaction,
                                                                                Transaction refundTransaction,
                                                                                String reason) {
        Map<String, Object> event = buildBaseEvent(originalTransaction, "REFUND_PROCESSED");
        event.put("refundTransactionId", refundTransaction.getId());
        event.put("refundReference", refundTransaction.getTransactionReference());
        event.put("refundAmount", refundTransaction.getAmount());
        event.put("reason", reason);
        event.put("refundedAt", LocalDateTime.now());

        return sendEvent(paymentEventsTopic, originalTransaction.getId(), event);
    }

    /**
     * Publish settlement completed event
     */
    public CompletableFuture<SendResult<String, Object>> publishSettlementCompleted(String batchId,
                                                                                    String merchantId,
                                                                                    java.math.BigDecimal amount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SETTLEMENT_COMPLETED");
        event.put("eventId", generateEventId());
        event.put("timestamp", LocalDateTime.now());
        event.put("batchId", batchId);
        event.put("merchantId", merchantId);
        event.put("amount", amount);
        event.put("currency", "NGN");
        event.put("requestId", getRequestId());

        return sendEvent(paymentEventsTopic, batchId, event);
    }

    // ==================== FRAUD EVENTS ====================

    /**
     * Publish fraud detected event
     */
    public CompletableFuture<SendResult<String, Object>> publishFraudDetected(Transaction transaction,
                                                                              int riskScore,
                                                                              List<String> flags,
                                                                              String reason) {
        Map<String, Object> event = buildBaseEvent(transaction, "FRAUD_DETECTED");
        event.put("riskScore", riskScore);
        event.put("flags", flags);
        event.put("reason", reason);
        event.put("detectedAt", LocalDateTime.now());
        event.put("requiresReview", riskScore >= 50);

        return sendEvent(fraudEventsTopic, transaction.getId(), event);
    }

    /**
     * Publish fraud alert for manual review
     */
    public CompletableFuture<SendResult<String, Object>> publishFraudAlert(String customerId,
                                                                           String alertType,
                                                                           Map<String, Object> details) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "FRAUD_ALERT");
        event.put("eventId", generateEventId());
        event.put("timestamp", LocalDateTime.now());
        event.put("customerId", customerId);
        event.put("alertType", alertType);
        event.put("details", details);
        event.put("priority", getAlertPriority(alertType));
        event.put("requestId", getRequestId());

        return sendEvent(fraudEventsTopic, customerId, event);
    }

    // ==================== NOTIFICATION EVENTS ====================

    /**
     * Publish notification event for customer
     */
    public CompletableFuture<SendResult<String, Object>> publishNotification(String customerId,
                                                                             String notificationType,
                                                                             String subject,
                                                                             Map<String, Object> content) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "NOTIFICATION");
        event.put("eventId", generateEventId());
        event.put("timestamp", LocalDateTime.now());
        event.put("customerId", customerId);
        event.put("notificationType", notificationType);
        event.put("subject", subject);
        event.put("content", content);
        event.put("channels", List.of("EMAIL", "SMS", "PUSH"));
        event.put("requestId", getRequestId());

        return sendEvent(notificationEventsTopic, customerId, event);
    }

    /**
     * Publish transaction receipt notification
     */
    public CompletableFuture<SendResult<String, Object>> publishTransactionReceipt(Transaction transaction) {
        Map<String, Object> content = new HashMap<>();
        content.put("amount", transaction.getAmount());
        content.put("currency", transaction.getCurrency());
        content.put("reference", transaction.getTransactionReference());
        content.put("date", transaction.getTransactionDate());
        content.put("status", transaction.getStatus().name());

        return publishNotification(
                transaction.getCustomerId(),
                "TRANSACTION_RECEIPT",
                "Transaction Receipt - " + transaction.getTransactionReference(),
                content
        );
    }

    // ==================== AUDIT EVENTS ====================

    /**
     * Publish audit event
     */
    public CompletableFuture<SendResult<String, Object>> publishAuditEvent(String userId,
                                                                           String action,
                                                                           String resource,
                                                                           String resourceId,
                                                                           Map<String, Object> details) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "AUDIT");
        event.put("eventId", generateEventId());
        event.put("timestamp", LocalDateTime.now());
        event.put("userId", userId != null ? userId : getCurrentUser());
        event.put("action", action);
        event.put("resource", resource);
        event.put("resourceId", resourceId);
        event.put("details", details);
        event.put("ipAddress", getCurrentIp());
        event.put("userAgent", getUserAgent());
        event.put("requestId", getRequestId());

        return sendEvent(auditEventsTopic, resourceId, event);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build base event structure for transaction events
     */
    private Map<String, Object> buildBaseEvent(Transaction transaction, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("eventId", generateEventId());
        event.put("timestamp", LocalDateTime.now());
        event.put("transactionId", transaction.getId());
        event.put("transactionReference", transaction.getTransactionReference());
        event.put("tenantId", transaction.getTenantId());
        event.put("customerId", transaction.getCustomerId());
        event.put("customerEmail", transaction.getCustomerEmail());
        event.put("customerPhone", transaction.getCustomerPhone());
        event.put("type", transaction.getType() != null ? transaction.getType().name() : null);
        event.put("sourceWalletId", transaction.getSourceWalletId());
        event.put("destinationWalletId", transaction.getDestinationWalletId());
        event.put("requestId", getRequestId());

        return event;
    }

    /**
     * Send event to Kafka topic
     */
    private CompletableFuture<SendResult<String, Object>> sendEvent(String topic,
                                                                    String key,
                                                                    Map<String, Object> event) {
        try {
            // Check for duplicate events (within last 5 minutes)
            String eventKey = topic + "_" + key + "_" + event.get("eventType");
            if (isDuplicateEvent(eventKey)) {
                log.warn("Duplicate event detected, skipping: {}", eventKey);
                return CompletableFuture.completedFuture(null);
            }

            // Track published event
            publishedEvents.put(eventKey, LocalDateTime.now());

            // Serialize for logging
            String eventJson = objectMapper.writeValueAsString(event);
            log.debug("Publishing event to Kafka - Topic: {}, Key: {}, Event: {}",
                    topic, key, eventJson);

            // Send to Kafka
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Event published successfully - Topic: {}, Key: {}, Partition: {}, Offset: {}",
                            topic, key, result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event - Topic: {}, Key: {}", topic, key, ex);
                }
            });

            return future;

        } catch (JsonProcessingException e) {
            log.error("Error serializing event", e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("Error sending event to Kafka", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send raw message to Kafka
     */
    public CompletableFuture<SendResult<String, Object>> sendRawMessage(String topic,
                                                                        String key,
                                                                        Object message) {
        try {
            log.debug("Sending raw message to Kafka - Topic: {}, Key: {}", topic, key);

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send raw message to topic: {}", topic, ex);
                }
            });

            return future;

        } catch (Exception e) {
            log.error("Error sending raw message", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Batch publish multiple events
     */
    public List<CompletableFuture<SendResult<String, Object>>> batchPublish(List<Map<String, Object>> events,
                                                                            String topic) {
        List<CompletableFuture<SendResult<String, Object>>> futures = new ArrayList<>();

        for (Map<String, Object> event : events) {
            String key = (String) event.getOrDefault("key", generateEventId());
            futures.add(sendEvent(topic, key, event));
        }

        return futures;
    }

    /**
     * Check for duplicate event (within last 5 minutes)
     */
    private boolean isDuplicateEvent(String eventKey) {
        LocalDateTime publishedTime = publishedEvents.get(eventKey);
        if (publishedTime == null) {
            return false;
        }
        return publishedTime.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    /**
     * Clean up old published events cache (run every 10 minutes)
     */
    @Scheduled(fixedDelay = 600000)
    public void cleanPublishedEventsCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        publishedEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        log.debug("Cleaned published events cache. Current size: {}", publishedEvents.size());
    }

    /**
     * Generate unique event ID
     */
    private String generateEventId() {
        return "EVT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Generate reference
     */
    private String generateReference() {
        return "REF_" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get alert priority based on alert type
     */
    private String getAlertPriority(String alertType) {
        if (alertType == null) return "MEDIUM";
        
        if (alertType.equals("HIGH_RISK_TRANSACTION") || alertType.equals("MULTIPLE_FAILED_ATTEMPTS")) {
            return "HIGH";
        } else if (alertType.equals("SUSPICIOUS_AMOUNT") || alertType.equals("UNUSUAL_LOCATION")) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Check if Kafka is available
     */
    public boolean isKafkaAvailable() {
        try {
            return kafkaTemplate.getProducerFactory().createProducer() != null;
        } catch (Exception e) {
            log.warn("Kafka is not available", e);
            return false;
        }
    }

    /**
     * Get Kafka metrics
     */
    public Map<String, Object> getKafkaMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("available", isKafkaAvailable());
        metrics.put("publishedEventsCount", publishedEvents.size());
        metrics.put("topics", List.of(
                transactionEventsTopic,
                paymentEventsTopic,
                auditEventsTopic,
                notificationEventsTopic,
                fraudEventsTopic
        ));
        return metrics;
    }
}