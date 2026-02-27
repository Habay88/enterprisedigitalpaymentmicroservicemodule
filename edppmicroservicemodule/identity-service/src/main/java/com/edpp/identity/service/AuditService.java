package com.edpp.identity.service;

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
}
