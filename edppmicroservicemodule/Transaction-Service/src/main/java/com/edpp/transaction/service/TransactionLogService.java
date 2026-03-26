package com.edpp.transaction.service;

import com.edpp.transaction.entity.Transaction;
import com.edpp.transaction.entity.TransactionLog;
import com.edpp.transaction.enums.TransactionStatus;
import com.edpp.transaction.repository.TransactionLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLogService {

    private final TransactionLogRepository transactionLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Post construct to configure ObjectMapper
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        // Configure ObjectMapper for better JSON serialization
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        log.debug("TransactionLogService initialized with configured ObjectMapper");
    }

    /**
     * Log a simple message for a transaction
     */
    @Transactional
    public TransactionLog log(Transaction transaction, String message) {
        return log(transaction, message, null, null);
    }

    /**
     * Log a message with additional metadata
     */
    @Transactional
    public TransactionLog log(Transaction transaction, String message, Map<String, Object> metadata) {
        return log(transaction, message, metadata, null);
    }

    /**
     * Log a status change for a transaction
     */
    @Transactional
    public TransactionLog logStatusChange(Transaction transaction, 
                                         TransactionStatus oldStatus, 
                                         TransactionStatus newStatus, 
                                         String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason", reason);
        metadata.put("changedAt", LocalDateTime.now().toString());
        
        String message = String.format("Status changed from %s to %s. Reason: %s", 
                                      oldStatus, newStatus, reason);
        
        return log(transaction, message, metadata, newStatus);
    }

    /**
     * Core logging method
     */
    @Transactional
    public TransactionLog log(Transaction transaction, 
                             String message, 
                             Map<String, Object> metadata, 
                             TransactionStatus newStatus) {
        
        TransactionLog transactionLog = TransactionLog.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .previousStatus(getCurrentStatus(transaction))
                .newStatus(newStatus != null ? newStatus : transaction.getStatus())
                .message(message)
                .changedBy(getChangedBy(transaction))
                .metadata(serializeMetadata(metadata))
                .createdAt(LocalDateTime.now())
                .build();

        TransactionLog savedLog = transactionLogRepository.save(transactionLog);
        
        log.debug("Transaction log saved - Reference: {}, Message: {}", 
                  transaction.getTransactionReference(), message);
        
        return savedLog;
    }

    /**
     * Log transaction initiation
     */
    @Transactional
    public TransactionLog logTransactionInitiated(Transaction transaction) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("amount", transaction.getAmount());
        metadata.put("currency", transaction.getCurrency());
        metadata.put("paymentMethod", transaction.getPaymentMethod());
        metadata.put("sourceWallet", transaction.getSourceWalletId());
        metadata.put("destinationWallet", transaction.getDestinationWalletId());
        
        return log(transaction, "Transaction initiated", metadata);
    }

    /**
     * Log transaction processing start
     */
    @Transactional
    public TransactionLog logProcessingStarted(Transaction transaction, String processorName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("processor", processorName);
        metadata.put("startedAt", LocalDateTime.now().toString());
        
        return log(transaction, "Transaction processing started", metadata);
    }

    /**
     * Log transaction completed successfully
     */
    @Transactional
    public TransactionLog logTransactionCompleted(Transaction transaction, String processorTransactionId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("processorTransactionId", processorTransactionId);
        metadata.put("settledAt", transaction.getSettledAt() != null ? 
                     transaction.getSettledAt().toString() : LocalDateTime.now().toString());
        
        return log(transaction, "Transaction completed successfully", metadata);
    }

    /**
     * Log transaction failure
     */
    @Transactional
    public TransactionLog logTransactionFailed(Transaction transaction, String errorMessage, Exception e) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("errorMessage", errorMessage);
        metadata.put("errorType", e != null ? e.getClass().getSimpleName() : "Unknown");
        metadata.put("failedAt", LocalDateTime.now().toString());
        
        if (e != null && e.getMessage() != null) {
            metadata.put("errorDetail", e.getMessage());
        }
        
        return log(transaction, "Transaction failed: " + errorMessage, metadata);
    }

    /**
     * Log refund processing
     */
    @Transactional
    public TransactionLog logRefundProcessed(Transaction originalTransaction, 
                                            Transaction refundTransaction, 
                                            String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("refundTransactionId", refundTransaction.getId());
        metadata.put("refundReference", refundTransaction.getTransactionReference());
        metadata.put("refundAmount", refundTransaction.getAmount());
        metadata.put("reason", reason);
        
        return log(originalTransaction, "Refund processed", metadata);
    }

    /**
     * Log reversal of transaction
     */
    @Transactional
    public TransactionLog logTransactionReversed(Transaction transaction, String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason", reason);
        metadata.put("reversedAt", LocalDateTime.now().toString());
        
        return log(transaction, "Transaction reversed", metadata, TransactionStatus.REVERSED);
    }

    /**
     * Log fraud detection alert
     */
    @Transactional
    public TransactionLog logFraudAlert(Transaction transaction, String alertType, int riskScore) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alertType", alertType);
        metadata.put("riskScore", riskScore);
        metadata.put("alertTime", LocalDateTime.now().toString());
        
        return log(transaction, "Fraud alert: " + alertType, metadata);
    }

    /**
     * Log authorization attempt
     */
    @Transactional
    public TransactionLog logAuthorizationAttempt(Transaction transaction, 
                                                 String authorizationCode, 
                                                 boolean success) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("authorizationCode", authorizationCode);
        metadata.put("success", success);
        metadata.put("attemptTime", LocalDateTime.now().toString());
        
        String message = success ? "Authorization successful" : "Authorization failed";
        return log(transaction, message, metadata);
    }

    /**
     * Get all logs for a transaction (returns List)
     */
    public List<TransactionLog> getTransactionLogs(String transactionId) {
        return transactionLogRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId);
    }

    /**
     * Get logs for a transaction by reference (returns List)
     */
    public List<TransactionLog> getTransactionLogsByReference(String transactionReference) {
        return transactionLogRepository.findByTransactionReferenceOrderByCreatedAtDesc(transactionReference);
    }

    /**
     * Get paginated logs for a transaction (returns Page)
     */
    public Page<TransactionLog> getTransactionLogsPaginated(String transactionId, Pageable pageable) {
        return transactionLogRepository.findByTransactionId(transactionId, pageable);
    }

    /**
     * Get logs by status (returns List)
     */
    public List<TransactionLog> getLogsByStatus(TransactionStatus status) {
        return transactionLogRepository.findByNewStatus(status);
    }

    /**
     * Get logs within date range (returns List)
     */
    public List<TransactionLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return transactionLogRepository.findByCreatedAtBetween(start, end);
    }

    /**
     * Get logs with specific message pattern - Returns List
     */
    public List<TransactionLog> searchLogsByMessage(String keyword) {
        return transactionLogRepository.findByMessageContainingIgnoreCase(keyword);
    }

    /**
     * Get logs with specific message pattern with pagination - Returns Page
     */
    public Page<TransactionLog> searchLogsByMessagePaginated(String keyword, Pageable pageable) {
        return transactionLogRepository.findByMessageContainingIgnoreCase(keyword, pageable);
    }

    /**
     * Delete old logs (for data retention)
     */
    @Transactional
    public void deleteOldLogs(LocalDateTime olderThan) {
        long deletedCount = transactionLogRepository.deleteByCreatedAtBefore(olderThan);
        log.info("Deleted {} transaction logs older than {}", deletedCount, olderThan);
    }

    /**
     * Get current status from transaction
     */
    private TransactionStatus getCurrentStatus(Transaction transaction) {
        return transaction != null ? transaction.getStatus() : null;
    }

    /**
     * Get who made the change
     */
    private String getChangedBy(Transaction transaction) {
        if (transaction != null && transaction.getUpdatedBy() != null) {
            return transaction.getUpdatedBy();
        }
        if (transaction != null && transaction.getCreatedBy() != null) {
            return transaction.getCreatedBy();
        }
        return "SYSTEM";
    }

    /**
     * Serialize metadata map to JSON string
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
            return "{\"error\": \"Failed to serialize metadata\"}";
        }
    }

    /**
     * Deserialize metadata JSON string to map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deserializeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize metadata", e);
            return new HashMap<>();
        }
    }

    /**
     * Get formatted log summary for transaction
     */
    public String getTransactionLogSummary(String transactionId) {
        List<TransactionLog> logs = getTransactionLogs(transactionId);
        
        if (logs.isEmpty()) {
            return "No logs found for transaction: " + transactionId;
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Transaction Log Summary for: ").append(transactionId).append("\n");
        summary.append("=".repeat(50)).append("\n");
        
        for (TransactionLog logEntry : logs) {
            summary.append(String.format("[%s] %s - %s\n", 
                logEntry.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                logEntry.getNewStatus() != null ? logEntry.getNewStatus() : "INFO",
                logEntry.getMessage()));
        }
        
        return summary.toString();
    }

    /**
     * Get statistics about logs
     */
    public Map<String, Object> getLogStatistics(LocalDateTime start, LocalDateTime end) {
        List<TransactionLog> logs = getLogsByDateRange(start, end);
        
        Map<String, Long> statusCounts = new HashMap<>();
        Map<String, Long> hourlyDistribution = new HashMap<>();
        
        for (TransactionLog logEntry : logs) {
            // Count by status
            if (logEntry.getNewStatus() != null) {
                statusCounts.merge(logEntry.getNewStatus().toString(), 1L, Long::sum);
            }
            
            // Hourly distribution
            String hour = logEntry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
            hourlyDistribution.merge(hour, 1L, Long::sum);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", logs.size());
        stats.put("dateRange", start + " to " + end);
        stats.put("statusDistribution", statusCounts);
        stats.put("hourlyDistribution", hourlyDistribution);
        
        return stats;
    }
}