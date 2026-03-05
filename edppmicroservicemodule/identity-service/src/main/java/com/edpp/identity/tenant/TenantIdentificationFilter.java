package com.edpp.identity.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantIdentificationFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_PARAM = "tenantId";
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Extract tenant ID from header or parameter
            String tenantId = extractTenantId(request);

            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                log.debug("Tenant identified: {} for path: {}", tenantId, request.getRequestURI());
            } else {
                // For public endpoints, we might not have a tenant
                log.debug("No tenant identified for request: {}", request.getRequestURI());
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clear the context after the request completes
            TenantContext.clear();
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

    private String extractFromSubdomain(HttpServletRequest request) {
        String host = request.getServerName();
        if (host != null && host.contains(".")) {
            // Extract subdomain (e.g., banka.localhost -> banka)
            String[] parts = host.split("\\.");
            if (parts.length > 2) {
                return parts[0];
            }
        }
        return null;
    }
}
