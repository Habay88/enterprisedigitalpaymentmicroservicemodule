package com.edpp.identity.service;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.model.AuditEvent;
import com.edpp.identity.model.Customer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Value("${audit.kafka.topic:audit-events}")
    private String auditTopic;

    @Value("${audit.enabled:true}")
    private boolean auditEnabled;

    /**
     * Log customer onboarding event
     */
    @Async
    public void logCustomerOnboarding(Customer customer){
        if(!auditEnabled) return;

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CUSTOMER_ONBOARDING")
                .timestamp(LocalDateTime.now())
                .userId(customer.getId())
                .userEmail(customer.getEmail())
                .action("CREATE")
                .resource("CUSTOMER")
                .resourceId(customer.getCifNumber())
                .status("SUCCESS")
                .details(Map.of(
                        "customerType",customer.getCustomerType().toString(),
                        "riskRating", customer.getRiskRating().toString(),
                        "kycStatus", customer.getKycDetails() != null ?
                                customer.getKycDetails().isKycCompleted() : false
                        ))
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .build();
        sendAuditEvent(event);
    }
    /**
     * Log KYC update event
     */
    @Async
    public void logKycUpdate(Customer customer,String updateBy){
        if(!auditEnabled) return;
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("KYC_UPDATE")
                .timestamp(LocalDateTime.now())
                .userId(customer.getId())
                .userEmail(customer.getEmail())
                .action("UPDATE")
                .resource("KYC")
                .resourceId(customer.getCifNumber())
                .status("SUCCESS")
                .details(Map.of(
                        "kycCompleted",customer.getKycDetails() !=null ?
                                           customer.getKycDetails().isKycCompleted() : false,
                         "idType", customer.getKycDetails() != null ?
                                       customer.getKycDetails().getIdType() : null,
                        "verifiedBy", updatedBy

                ))
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .build();
        sendAuditEvent(event);

    }

    /**
     * Log status change event
     */
    @Async
    public void logStatusChange(String customerId, CustomerStatus  oldStatus,
                                CustomerStatus newStatus,String reason){
        if(!auditEnabled) return;
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("STATUS_CHANGE")
                .timestamp(LocalDateTime.now())
                .userId(customerId)
                .action("UPDATE")
                .resource("CUSTOMER_STATUS")
                .resourceId(customerId)
                .status("SUCCESS")
                .details(Map.of(
                        "oldStatus", oldStatus.toString(),
                        "newStatus", newStatus.toString(),
                        "reason", reason
                ))
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .build();
        sendAuditEvent(event);
    }
}
/**
 * Log failed login attempt
 */
@Async
public void logFailedLogin(String email,String reason){
    if(!auditEnabled) return;
    AuditEvent event = AuditEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("LOGIN_FAILED")
            .timestamp(LocalDateTime.now())
            .userEmail(email)
            .action("LOGIN")
            .resource("AUTH")
            .status("FAILED")
            .details(Map.of(
                    "reason",reason,
                    "attemptCount", 1
            ))
            .ipAddress(getClientIp())
            .userAgent(getUserAgent())
            .build();
    sendAuditEvent(event);
}
    /**
     * Log suspicious activity
     */
    @Async
public void logSuspiciousActivity(String customerId,String activity,
              Map<String,Object>details ){
        if(!auditEnabled) return;
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("SUSPICIOUS ACTIVITY")
                .timestamp(LocalDateTime.now())
                .userId(customerId)
                .action("ALERT")
                .resource("FRAUD")
                .resourceId(customerId)
                .status("WARNING")
                .details(Map.of(
                        "activity", activity,
                        "additionalInfo", details
                ))
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .build();
        sendAuditEvent(event);
        // Send alert for high-priority suspicious activities
        if (activity.contains("FRAUD") || activity.contains("UNAUTHORIZED")) {
            sendAlert(event);
        }
    }
    /**
     * Log data export (GDPR compliance)
     */
    @Async
public void logDataExport(String customerId,String exportedBy,String reason) {
        if (!auditEnabled) return;
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DATA_EXPORT")
                .timestamp(LocalDateTime.now())
                .userId(customerId)
                .action("EXPORT")
                .resource("PERSONAL_DATA")
                .resourceId(customerId)
                .status("SUCCESS")
                .details(Map.of(
                        "exportedBy", exportedBy,
                        "reason", reason,
                        "compliance", "GDPR"
                ))
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .build();

        sendAuditEvent(event);
    }
        /**
         * Send audit event to Kafka for persistent storage
         */
        private void sendAuditEvent(AuditEvent event){
            try{
                String eventJson = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(auditTopic, event.getEventId(), eventJson)
                        .whenComplete((result, ex)-> {
                            if (ex == null){
                                log.debug("Audit event sent successfully: {}",event.getEventId(), ex);
                                // Fallback to local logging
                                logAuditToFile(event);
                            }
                        });
            } catch (Exception e) {
                log.error("Error serializing audit event", e);
            }
        }
        /**
         * Fallback: log to file if Kafka is unavailable
         */
private void logAuditToFile(AuditEvent event){
            log.info("AUDIT: {} | {} | {} | {} | {}",
                    event.getTimestamp(),
                    event.getEventType(),
                    event.getUserId(),
                    event.getAction(),
                    event.getStatus()
            );
        }
        /**
         * Send alert for critical events
         */
        private void sendAlert(AuditEvent event){
            // will be sending email/sms pagerduty alert in production
            log.warn("ALERT: Critical audit event detected: {}", event);
        }
        /**
         * Get client IP address
         */
         private String getClientIp() {
            if (request == null) return "UNKNOWN";

            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String remoteAddr = request.getRemoteAddr();
            return remoteAddr != null ? remoteAddr : "UNKNOWN";
        }
        


}