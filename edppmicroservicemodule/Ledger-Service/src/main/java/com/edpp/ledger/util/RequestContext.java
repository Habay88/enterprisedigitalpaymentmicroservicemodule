package com.edpp.ledger.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request Context - Holds request-specific information for the current thread
 * 
 * This class provides tenant isolation and request tracing across the service.
 * It uses ThreadLocal to store context that can be accessed from any layer.
 */
@Component
@RequestScope
@Data
@Slf4j
public class RequestContext {

    private static final ThreadLocal<String> TENANT_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_EMAIL_HOLDER = new ThreadLocal<>();
    
    private String requestId;
    private String tenantId;
    private String userId;
    private String userEmail;
    private String clientIp;
    private String userAgent;
    private LocalDateTime requestStartTime;

    public RequestContext() {
        this.requestId = generateRequestId();
        this.requestStartTime = LocalDateTime.now();
        this.tenantId = TENANT_ID_HOLDER.get();
        REQUEST_ID_HOLDER.set(this.requestId);
    }

    /**
     * Get current tenant ID from thread-local (static access)
     */
    public static String getCurrentTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * Get current request ID from thread-local (static access)
     */
    public static String getCurrentRequestId() {
        return REQUEST_ID_HOLDER.get();
    }

    /**
     * Get current user ID from thread-local (static access)
     */
    public static String getCurrentUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * Get current user email from thread-local (static access)
     */
    public static String getCurrentUserEmail() {
        return USER_EMAIL_HOLDER.get();
    }

    /**
     * Set tenant ID in thread-local
     */
    public static void setCurrentTenantId(String tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * Set user ID in thread-local
     */
    public static void setCurrentUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * Set user email in thread-local
     */
    public static void setCurrentUserEmail(String userEmail) {
        USER_EMAIL_HOLDER.set(userEmail);
    }

    /**
     * Instance method to get tenant ID
     */
    public String getTenantId() {
        return this.tenantId != null ? this.tenantId : TENANT_ID_HOLDER.get();
    }

    /**
     * Instance method to get user ID
     */
    public String getUserId() {
        return this.userId != null ? this.userId : USER_ID_HOLDER.get();
    }

    /**
     * Generate unique request ID
     */
    private String generateRequestId() {
        return "REQ_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Clear all thread-local values
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
        REQUEST_ID_HOLDER.remove();
        USER_ID_HOLDER.remove();
        USER_EMAIL_HOLDER.remove();
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
     * Get masked client IP for logging
     */
    public String getMaskedClientIp() {
        if (clientIp == null || clientIp.isEmpty()) {
            return "UNKNOWN";
        }
        String[] parts = clientIp.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
        }
        return clientIp;
    }

    /**
     * Get request summary for logging
     */
    public String getRequestSummary() {
        return String.format("RequestId: %s, Tenant: %s, User: %s, IP: %s, Duration: %dms",
                requestId, getTenantId(), getUserId() != null ? getUserId() : "ANONYMOUS",
                getMaskedClientIp(), getRequestDuration());
    }
}