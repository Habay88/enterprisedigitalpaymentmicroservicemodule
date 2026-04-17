package com.edpp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Logging Filter - Logs all requests and responses
 * 
 * This filter provides comprehensive logging for:
 * - Request method, path, query parameters
 * - Response status code
 * - Request duration
 * - Unique request ID for tracing
 * 
 * Benefits:
 * - Debugging production issues
 * - Audit trail
 * - Performance monitoring
 * - Security forensics
 */
@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Generate unique request ID for tracing
        String requestId = UUID.randomUUID().toString();
        exchange.getAttributes().put("requestId", requestId);
        exchange.getAttributes().put("startTime", Instant.now());
        
        // Log request details
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String query = exchange.getRequest().getURI().getQuery();
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        
        log.info("Request: {} {} {} - Tenant: {} - RequestId: {}", 
                method, path, query != null ? "?" + query : "", 
                tenantId != null ? tenantId : "default", 
                requestId);
        
        // Process the request and log response after completion
        return chain.filter(exchange).doAfterTerminate(() -> {
            Instant start = (Instant) exchange.getAttributes().get("startTime");
            long duration = Instant.now().toEpochMilli() - start.toEpochMilli();
            int status = exchange.getResponse().getStatusCode() != null ? 
                         exchange.getResponse().getStatusCode().value() : 0;
            
            log.info("Response: {} - Status: {} - Duration: {}ms - RequestId: {}", 
                    path, status, duration, requestId);
        });
    }

    @Override
    public int getOrder() {
        return -150; // Run after authentication but before routing
    }
}