package com.edpp.gateway.config;

import lombok.RequiredArgsConstructor;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import com.edpp.gateway.filter.AuthenticationFilter;

import feign.Request.HttpMethod;

/**
 * Gateway Configuration - Defines all routing rules
 * 
 * This class configures how requests are routed to different microservices.
 * 
 * Key Concepts:
 * - Routes: Define path patterns and destination URLs
 * - Predicates: Conditions for matching requests (path, method, header, etc.)
 * - Filters: Modify requests/responses before/after routing
 * - Circuit Breakers: Prevent cascading failures
 * - Retry: Automatic retry for transient failures
 * 
 * The routing order matters - more specific routes should come first.
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                /*
                 * IDENTITY SERVICE ROUTE
                 * Handles: Customer management, authentication, tenant management
                 * Paths: /api/v1/customers/**, /api/v1/auth/**, /api/v1/tenants/**
                 */
                .route("identity-service", r -> r
                        .path("/api/v1/customers/**", "/api/v1/auth/**", "/api/v1/tenants/**")
                        .filters(f -> f
                                // Circuit breaker prevents cascading failures
                                .circuitBreaker(config -> config
                                        .setName("identityServiceCB")
                                        .setFallbackUri("forward:/fallback/identity"))
                                // Retry transient failures (network issues, timeouts)
                                .retry(config -> config
                                        .setRetries(3)
                                        .setStatuses(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST))
                                // Strip prefix to match service path
                                .rewritePath("/api/v1/(?<segment>.*)", "/api/v1/${segment}"))
                        .uri("lb://identity-service")) // lb:// uses service discovery

                /*
                 * WALLET SERVICE ROUTE
                 * Handles: Account management, balance operations, statements
                 * Paths: /api/v1/wallets/**
                 */
                .route("wallet-service", r -> r
                        .path("/api/v1/wallets/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("walletServiceCB")
                                        .setFallbackUri("forward:/fallback/wallet"))
                                .retry(config -> config
                                        .setRetries(3))
                                // Rate limit per tenant
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(new TenantKeyResolver())))
                        .uri("lb://wallet-service"))

                /*
                 * TRANSACTION SERVICE ROUTE
                 * Handles: Payment processing, refunds, reversals
                 * Paths: /api/v1/transactions/**
                 */
                .route("transaction-service", r -> r
                        .path("/api/v1/transactions/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("transactionServiceCB")
                                        .setFallbackUri("forward:/fallback/transaction"))
                                .retry(config -> config
                                        .setRetries(3))
                                // Add custom header to identify gateway as source
                                .addRequestHeader("X-Request-Source", "gateway"))
                        .uri("lb://transaction-service"))

                /*
                 * LEDGER SERVICE ROUTE
                 * Handles: GL accounts, journal entries, financial reports
                 * Paths: /api/v1/ledger/**
                 */
                .route("ledger-service", r -> r
                        .path("/api/v1/ledger/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("ledgerServiceCB")
                                        .setFallbackUri("forward:/fallback/ledger")))
                        .uri("lb://ledger-service"))

                /*
                 * SETTLEMENT SERVICE ROUTE
                 * Handles: Batch settlements, bank transfers, reconciliation
                 * Paths: /api/v1/settlements/**, /api/v1/settlement/**
                 */
                .route("settlement-service", r -> r
                        .path("/api/v1/settlements/**", "/api/v1/settlement/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("settlementServiceCB")
                                        .setFallbackUri("forward:/fallback/settlement")))
                        .uri("lb://settlement-service"))

                /*
                 * MERCHANT SERVICE ROUTE
                 * Handles: Merchant onboarding, API keys, webhooks
                 * Paths: /api/v1/merchants/**
                 * Special: Uses API key resolver for rate limiting (merchants have different
                 * limits)
                 */
                .route("merchant-service", r -> r
                        .path("/api/v1/merchants/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("merchantServiceCB")
                                        .setFallbackUri("forward:/fallback/merchant"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(new ApiKeyResolver())))
                        .uri("lb://merchant-service"))

                /*
                 * OPENAPI ROUTE - For API documentation
                 * Serves Swagger UI and OpenAPI specs
                 */
                .route("openapi", r -> r
                        .path("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .uri("http://localhost:8081"))

                .build();
    }

    /**
     * Rate limiter bean for Redis-based distributed rate limiting
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 10 requests per second
        // burstCapacity: 20 requests in a burst
        return new RedisRateLimiter(10, 20, 1);
    }
}

/**
 * TenantKeyResolver - Extracts tenant ID from request header for rate limiting
 * 
 * This ensures each tenant gets their own rate limit counter.
 * Different tenants (banks) have different rate limits based on their
 * subscription.
 */
class TenantKeyResolver implements org.springframework.cloud.gateway.filter.ratelimit.KeyResolver {
    @Override
    public reactor.core.publisher.Mono<String> resolve(org.springframework.web.server.ServerWebExchange exchange) {
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        return reactor.core.publisher.Mono.just(tenantId != null ? tenantId : "default");
    }
}

/**
 * ApiKeyResolver - Extracts API key for merchant rate limiting
 * 
 * Merchants use API keys for authentication. Each merchant has their own rate
 * limit
 * based on their plan (Free, Pro, Enterprise).
 */
class ApiKeyResolver implements org.springframework.cloud.gateway.filter.ratelimit.KeyResolver {
    @Override
    public reactor.core.publisher.Mono<String> resolve(org.springframework.web.server.ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        return reactor.core.publisher.Mono.just(apiKey != null ? apiKey : "unknown");
    }
}