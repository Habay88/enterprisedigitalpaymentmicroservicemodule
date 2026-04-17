package com.edpp.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Rate Limiter Configuration - Defines different key resolvers for rate limiting
 * 
 * Rate limiting prevents abuse by limiting the number of requests from a client.
 * 
 * Types of Rate Limiters:
 * 1. Token Bucket: Allows bursts up to a limit, then refills at a steady rate
 * 2. Leaky Bucket: Smooths out bursts to a constant rate
 * 3. Fixed Window: Limits requests in a fixed time window
 * 4. Sliding Window: More accurate than fixed window
 * 
 * This implementation uses Token Bucket with Redis for distributed counting.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Primary key resolver - Based on Tenant ID
     * 
     * Each tenant (bank) gets their own rate limit bucket.
     * Allows different rate limits for different tenants.
     * 
     * Example:
     * - Premium tenant: 1000 requests/minute
     * - Standard tenant: 100 requests/minute
     * - Free tier: 10 requests/minute
     */
    @Bean
    @Primary
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
            return Mono.just(tenantId != null ? tenantId : "default");
        };
    }

    /**
     * User-based key resolver
     * 
     * Each user gets their own rate limit.
     * Prevents a single user from overwhelming the system.
     * 
     * Extracted from JWT token after authentication.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getAttribute("userId");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }

    /**
     * IP-based key resolver
     * 
     * Rate limits based on client IP address.
     * Useful as a fallback when authentication isn't available.
     * 
     * Works for:
     * - Unauthenticated endpoints
     * - API key authentication
     * - DDoS protection
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                               .getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }

    /**
     * API Key-based key resolver
     * 
     * Rate limits based on merchant API key.
     * Different rate limits for different merchant tiers:
     * - Enterprise: 10000 requests/minute
     * - Pro: 1000 requests/minute
     * - Free: 100 requests/minute
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            return Mono.just(apiKey != null ? apiKey : "unknown");
        };
    }
}