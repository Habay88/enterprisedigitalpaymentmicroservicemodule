package com.edpp.transaction.filter;
import com.edpp.transaction.util.TenantContext;
//import com.edpp.transaction.tenant.TenantContext;
import com.edpp.transaction.util.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class TenantIdentificationFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_PARAM = "tenantId";
    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    private final RequestContext requestContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract and set tenant ID
            String tenantId = extractTenantId(request);
            if (tenantId != null && !tenantId.isEmpty()) {
                TenantContext.setTenantId(tenantId);
                requestContext.setTenantId(tenantId);
                log.debug("Tenant identified: {} for path: {}", tenantId, request.getRequestURI());
            } else {
                log.debug("No tenant identified for request: {}", request.getRequestURI());
            }

            // Set correlation ID for tracing
            String correlationId = extractCorrelationId(request);
            TenantContext.setCorrelationId(correlationId);
            requestContext.setCorrelationId(correlationId);

            // Populate request context
            populateRequestContext(request);

            // Add response headers
            response.setHeader(REQUEST_ID_HEADER, requestContext.getRequestId());
            response.setHeader(CORRELATION_HEADER, correlationId);

            filterChain.doFilter(request, response);

        } finally {
            // Clear context after request completes
            TenantContext.clear();
            requestContext.clear();
        }
    }

    private String extractTenantId(HttpServletRequest request) {
        // Try header first
        String tenantId = request.getHeader(TENANT_HEADER);

        // Then try parameter
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = request.getParameter(TENANT_PARAM);
        }

        // Then try subdomain
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = extractFromSubdomain(request);
        }

        return tenantId;
    }

    private String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String extractFromSubdomain(HttpServletRequest request) {
        String host = request.getServerName();
        if (host != null && host.contains(".")) {
            String[] parts = host.split("\\.");
            if (parts.length > 2) {
                return parts[0];
            }
        }
        return null;
    }

    private void populateRequestContext(HttpServletRequest request) {
        requestContext.setTenantId(TenantContext.getTenantId());
        requestContext.setCorrelationId(TenantContext.getCorrelationId());
        requestContext.setClientIp(getClientIp(request));
        requestContext.setUserAgent(request.getHeader("User-Agent"));
        
        if (request.getSession() != null) {
            requestContext.setSessionId(request.getSession().getId());
        }
        
        // Extract user info from headers (if present)
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null) {
            requestContext.setUserId(userId);
        }
        
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        if (userEmail != null) {
            requestContext.setUserEmail(userEmail);
        }

        log.debug("Request context populated: {}", requestContext.getRequestSummary());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "UNKNOWN";
    }
}