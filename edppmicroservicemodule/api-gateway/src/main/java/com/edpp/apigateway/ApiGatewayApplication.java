package com.edpp.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;

/**
 * API Gateway Application - The entry point for all client requests yes
 * 
 * This service acts as the single entry point for all microservices.
 * It handles:
 * 1. Request routing to appropriate services
 * 2. Authentication and authorization (JWT validation)
 * 3. Rate limiting per tenant/user/IP
 * 4. Request/response transformation
 * 5. Circuit breaking and fallbacks
 * 6. Distributed tracing and logging
 * 7. CORS handling
 * 
 * Why a Gateway is necessary:
 * - Provides a unified API surface for clients
 * - Centralizes cross-cutting concerns (security, logging, rate limiting)
 * - Enables service discovery and load balancing
 * - Allows protocol translation (HTTP to WebSocket, etc.)
 * - Simplifies client code (single endpoint instead of N endpoints)
 */
@SpringBootApplication
@EnableDiscoveryClient  // Enables service discovery with Eureka
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /**
     * Creates a Redis-based rate limiter for distributed rate limiting.
     * 
     * Parameters:
     * - replenishRate: Number of requests per second allowed
     * - burstCapacity: Maximum number of requests in a burst
     * - requestedTokens: Number of tokens consumed per request
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}