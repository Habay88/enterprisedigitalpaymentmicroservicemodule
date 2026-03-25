package com.edpp.transaction.util;

import com.edpp.transaction.tenant.TenantContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequestScope
@Data
@Slf4j
public class RequestContext {

    // ThreadLocal for static access
    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> CORRELATION_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_ID_HOLDER = new ThreadLocal<>();
    
    // Instance fields
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
        TENANT_ID_HOLDER.set(this.tenantId);
        
        log.debug("RequestContext created - RequestId: {}, TenantId: {}", requestId, tenantId);
    }

    /**
     * STATIC METHOD - Get current request ID from thread-local
     */
    public static String getCurrentRequestId() {
        return REQUEST_ID_HOLDER.get();
    }
    
    /**
     * STATIC METHOD - Get current correlation ID from thread-local
     */
    public static String getCurrentCorrelationId() {
        return CORRELATION_ID_HOLDER.get();
    }
    
    /**
     * STATIC METHOD - Get current tenant ID from thread-local
     */
    public static String getCurrentTenantId() {
        return TENANT_ID_HOLDER.get();
    }
    
    /**
     * INSTANCE METHOD - Get request ID
     */
    public String getRequestId() {
        return this.requestId;
    }
    
    /**
     * INSTANCE METHOD - Get correlation ID
     */
    public String getCorrelationId() {
        return this.correlationId;
    }
    
    /**
     * INSTANCE METHOD - Get tenant ID
     */
    public String getTenantId() {
        return this.tenantId;
    }

    private String generateRequestId() {
        return "REQ_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    public boolean hasValidTenant() {
        return tenantId != null && !tenantId.isEmpty();
    }

    public long getRequestDuration() {
        if (requestStartTime == null) {
            return 0;
        }
        return java.time.Duration.between(requestStartTime, LocalDateTime.now()).toMillis();
    }

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

    public void clear() {
        this.authToken = null;
        this.userId = null;
        this.userEmail = null;
        REQUEST_ID_HOLDER.remove();
        CORRELATION_ID_HOLDER.remove();
        TENANT_ID_HOLDER.remove();
        log.debug("RequestContext cleared - RequestId: {}", requestId);
    }

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

    public boolean hasRequiredHeaders() {
        return tenantId != null && !tenantId.isEmpty();
    }

    public String getRequestSummary() {
        return String.format("RequestId: %s, Tenant: %s, User: %s, IP: %s, Duration: %dms",
                requestId, tenantId, userId != null ? userId : "ANONYMOUS",
                getMaskedClientIp(), getRequestDuration());
    }
}