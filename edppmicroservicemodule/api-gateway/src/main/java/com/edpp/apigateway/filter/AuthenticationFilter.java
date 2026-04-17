package com.edpp.gateway.filter;

import com.edpp.gateway.service.JwtService;
import com.edpp.gateway.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Authentication Filter - Validates JWT tokens and API keys
 * 
 * This is a GlobalFilter that runs on every request.
 * It checks for authentication credentials before allowing requests to proceed.
 * 
 * Authentication Methods:
 * 1. JWT (Bearer token) - For customers and internal users
 * 2. API Key + Secret - For merchants integrating with the platform
 * 
 * Flow:
 * 1. Check if path is public (no auth required)
 * 2. Check for API Key authentication (merchants)
 * 3. Check for JWT Bearer token (customers)
 * 4. Extract claims and add to request headers for downstream services
 * 5. Store context in RequestContext for correlation
 * 
 * Why a GlobalFilter?
 * - Runs on every request (like middleware)
 * - Can modify request/response
 * - Can short-circuit requests (return 401 before routing)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final RequestContext requestContext;

    // Public paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/merchants/webhook",
        "/actuator/health",
        "/actuator/info",
        "/v3/api-docs",
        "/swagger-ui"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public endpoints
        if (isPublicPath(path)) {
            log.debug("Public path accessed: {}", path);
            return chain.filter(exchange);
        }

        // Check for API Key authentication (Merchant integration)
        String apiKey = request.getHeaders().getFirst("X-API-Key");
        String apiSecret = request.getHeaders().getFirst("X-API-Secret");
        
        if (apiKey != null && apiSecret != null) {
            return authenticateWithApiKey(exchange, chain, apiKey, apiSecret);
        }

        // Check for JWT Bearer token (Customer authentication)
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid authorization header for path: {}", path);
            return unauthorized(exchange, "Missing or invalid authorization header");
        }

        String token = authHeader.substring(7);
        
        // Validate JWT token
        if (!jwtService.validateToken(token)) {
            log.warn("Invalid or expired token for path: {}", path);
            return unauthorized(exchange, "Invalid or expired token");
        }

        // Extract claims from JWT
        String userId = jwtService.getUserIdFromToken(token);
        String userEmail = jwtService.getUserEmailFromToken(token);
        String tenantId = jwtService.getTenantIdFromToken(token);

        // Store in request context for logging/tracing
        requestContext.setUserId(userId);
        requestContext.setUserEmail(userEmail);
        requestContext.setTenantId(tenantId);

        // Add user context to request headers for downstream services
        // This allows services to trust the gateway's authentication
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Email", userEmail)
                .header("X-Tenant-ID", tenantId)
                .header("X-Auth-Type", "JWT")
                .build();

        log.debug("Authenticated user: {} for tenant: {}", userEmail, tenantId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Authenticate using API Key (for merchants)
     * 
     * Merchants use API keys instead of JWT tokens.
     * Format: pk_xxx (public) and sk_xxx (secret)
     */
    private Mono<Void> authenticateWithApiKey(ServerWebExchange exchange, GatewayFilterChain chain,
                                               String apiKey, String apiSecret) {
        // Validate API key format
        if (apiKey.startsWith("pk_") && apiSecret.startsWith("sk_")) {
            ServerHttpRequest request = exchange.getRequest();
            String merchantId = extractMerchantId(apiKey);
            
            // Add merchant context to headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-Type", "API_KEY")
                    .header("X-Merchant-Id", merchantId)
                    .build();
            
            log.debug("Authenticated merchant: {}", merchantId);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }
        
        log.warn("Invalid API key format");
        return unauthorized(exchange, "Invalid API key");
    }

    /**
     * Extract merchant ID from API key
     * 
     * API key format: pk_live_MERCHANTID_RANDOM
     * This extracts the merchant ID portion
     */
    private String extractMerchantId(String apiKey) {
        // In production, lookup from database
        // For now, extract from the key format
        String[] parts = apiKey.split("_");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "unknown";
    }

    /**
     * Check if path is public (no authentication required)
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Return 401 Unauthorized response
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    /**
     * Order determines execution sequence
     * Lower numbers run first
     * -100 ensures this runs before other filters
     */
    @Override
    public int getOrder() {
        return -100;
    }
}