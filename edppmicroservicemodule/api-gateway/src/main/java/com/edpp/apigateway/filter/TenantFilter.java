package com.edpp.gateway.filter;

import com.edpp.gateway.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Tenant Filter - Identifies and routes requests to the correct tenant
 * 
 * Multi-tenancy allows multiple banks/fintechs to use the same platform
 * with complete data isolation.
 * 
 * Tenant identification methods:
 * 1. HTTP Header: X-Tenant-ID (explicit)
 * 2. Subdomain: banka.api.com -> tenant = "banka"
 * 3. Default: DEFAULT_TENANT (fallback)
 * 
 * Each tenant has:
 * - Separate database schema
 * - Isolated data
 * - Own configuration
 * - Own rate limits
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFilter implements GlobalFilter, Ordered {

    private final RequestContext requestContext;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Try to get tenant ID from header first
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        
        // If no header, try to extract from subdomain
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = extractFromSubdomain(exchange);
        }
        
        // Use default tenant if still not found
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "DEFAULT_TENANT";
            log.debug("No tenant identified, using default");
        }
        
        // Store in request context
        requestContext.setTenantId(tenantId);
        
        // Add tenant ID to request headers for downstream services
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header("X-Tenant-ID", tenantId)
                .build())
            .build();
        
        log.debug("Tenant identified: {}", tenantId);
        
        return chain.filter(mutatedExchange);
    }

    /**
     * Extract tenant ID from subdomain
     * 
     * Example: banka.api.edpp.com -> "banka"
     * This allows white-labeling for different banks
     */
    private String extractFromSubdomain(ServerWebExchange exchange) {
        String host = exchange.getRequest().getURI().getHost();
        if (host != null && host.contains(".")) {
            String[] parts = host.split("\\.");
            if (parts.length >= 2) {
                // Extract subdomain (first part of hostname)
                return parts[0];
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -200; // Run first (highest priority)
    }
}