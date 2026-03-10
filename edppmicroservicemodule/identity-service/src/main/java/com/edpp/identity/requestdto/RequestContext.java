package com.edpp.identity.requestdto;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.edpp.identity.tenant.TenantContext;

import java.util.UUID;

@Component
@RequestScope
public class RequestContext {
    
    private String requestId;
    private String tenantId;
    private String userId;
    private String userAgent;
    private String clientIp;

    public RequestContext() {
        this.requestId = UUID.randomUUID().toString();
        this.tenantId = TenantContext.getTenantId();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
}