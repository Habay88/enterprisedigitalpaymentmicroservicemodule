package com.edpp.transaction.util;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request-scoped context holder for current request information
 * Provides access to request-specific data throughout the transaction processing
 */
@Component
@RequestScope
@Data
@Slf4j
public class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> CORRELATION_ID_HOLDER = new ThreadLocal<>();
    
    private String requestId;
    private String tenantId;
    private String userId;
    private String userEmail;
    private String userAgent;
    private String clientIp;
    private String sessionId;
    private LocalDateTime requestStartTime;
    private String correlationId;
    private String authToken;

    public RequestContext() {
        this.requestId = generateRequestId();
        this.requestStartTime = LocalDateTime.now();
        this.tenantId = TenantContext.getTenantId();
        this.correlationId = generateCorrelationId();
        
        // Set thread-local values for static access
        REQUEST_ID_HOLDER.set(this.requestId);
        CORRELATION_ID_HOLDER.set(this.correlationId);
        
        log.debug("RequestContext created - RequestId: {}, TenantId: {}", requestId, tenantId);
    }

    /**
     * Static method to get current request ID from thread-local
     */
    public static String getCurrentRequestId() {
        return REQUEST_ID_HOLDER.get();
    }
    
    /**
     * Static method to get current correlation ID from thread-local
     */
    public static String getCurrentCorrelationId() {
        return CORRELATION_ID_HOLDER.get();
    }
    
    /**
     * Instance method to get request ID (non-static)
     */
    public String getRequestId() {
        return this.requestId;
    }
    
    /**
     * Instance method to get correlation ID (non-static)
     */
    public String getCorrelationId() {
        return this.correlationId;
    }

    /**
     * Generate unique request ID
     */
    private String generateRequestId() {
        return "REQ_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Generate correlation ID for distributed tracing
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Check if request has valid tenant context
     */
    public boolean hasValidTenant() {
        return tenantId != null && !tenantId.isEmpty();
    }

    /**
     * Get request duration in milliseconds
     */
    public long getRequestDuration() {
        if (requestStartTime == null) {
            return 0;
        }
        return java.time.Duration.between(requestStartTime, LocalDateTime.now()).toMillis();
    }

    /**
     * Create a copy of the context for async operations
     */
    public RequestContext copy() {
        RequestContext copy = new RequestContext();
        copy.setRequestId(this.requestId);
        copy.setTenantId(this.tenantId);
        copy.setUserId(this.userId);
        copy.setUserEmail(this.userEmail);
        copy.setUserAgent(this.userAgent);
        copy.setClientIp(this.clientIp);
        copy.setSessionId(this.sessionId);
        copy.setRequestStartTime(this.requestStartTime);
        copy.setCorrelationId(this.correlationId);
        copy.setAuthToken(this.authToken);
        return copy;
    }

    /**
     * Clear sensitive data after request completion
     */
    public void clear() {
        this.authToken = null;
        this.userId = null;
        this.userEmail = null;
        REQUEST_ID_HOLDER.remove();
        CORRELATION_ID_HOLDER.remove();
        log.debug("RequestContext cleared - RequestId: {}", requestId);
    }

    /**
     * Get masked client IP for logging
     */
    public String getMaskedClientIp() {
        if (clientIp == null || clientIp.isEmpty()) {
            return "UNKNOWN";
        }
        // Mask last octet of IP
        String[] parts = clientIp.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
        }
        return clientIp;
    }

    /**
     * Validate request has required headers
     */
    public boolean hasRequiredHeaders() {
        return tenantId != null && !tenantId.isEmpty();
    }

    /**
     * Get request summary for logging
     */
    public String getRequestSummary() {
        return String.format("RequestId: %s, Tenant: %s, User: %s, IP: %s, Duration: %dms",
                requestId, tenantId, userId != null ? userId : "ANONYMOUS",
                getMaskedClientIp(), getRequestDuration());
    }
}