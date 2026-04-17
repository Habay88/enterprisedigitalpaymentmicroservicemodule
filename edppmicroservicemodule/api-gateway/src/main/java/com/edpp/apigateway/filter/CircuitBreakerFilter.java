package com.edpp.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit Breaker Filter - Prevents cascading failures
 * 
 * The circuit breaker pattern has three states:
 * 1. CLOSED: Normal operation, requests pass through
 * 2. OPEN: Service is failing, requests fail fast
 * 3. HALF-OPEN: Testing if service recovered
 * 
 * Why it's important:
 * - Prevents system overload when services are down
 * - Allows failed services time to recover
 * - Provides graceful degradation
 * - Reduces cascading failures
 * 
 * Circuit breaker triggers when:
 * - Failure rate exceeds threshold (e.g., 50%)
 * - Slow call rate exceeds threshold
 * - Minimum number of calls reached
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String serviceName = extractServiceName(exchange);
        CircuitBreaker circuitBreaker = circuitBreakers.computeIfAbsent(serviceName, 
            this::createCircuitBreaker);
        
        return chain.filter(exchange)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(throwable -> {
                log.error("Circuit breaker open for service: {}", serviceName, throwable);
                exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                exchange.getResponse().getHeaders().add("X-Circuit-Breaker", "open");
                return exchange.getResponse().setComplete();
            });
    }

    /**
     * Extract service name from request path
     */
    private String extractServiceName(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.contains("/api/v1/customers")) return "identity-service";
        if (path.contains("/api/v1/wallets")) return "wallet-service";
        if (path.contains("/api/v1/transactions")) return "transaction-service";
        if (path.contains("/api/v1/ledger")) return "ledger-service";
        if (path.contains("/api/v1/settlements")) return "settlement-service";
        if (path.contains("/api/v1/merchants")) return "merchant-service";
        return "unknown";
    }

    /**
     * Create circuit breaker with custom configuration
     */
    private CircuitBreaker createCircuitBreaker(String name) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            // Failure rate threshold (50% failures opens circuit)
            .failureRateThreshold(50)
            // Slow call rate threshold
            .slowCallRateThreshold(50)
            // Calls longer than 5 seconds are considered slow
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            // How long to wait before transitioning to half-open
            .waitDurationInOpenState(Duration.ofSeconds(30))
            // Number of calls in half-open state to test recovery
            .permittedNumberOfCallsInHalfOpenState(3)
            // Minimum number of calls before calculating failure rate
            .minimumNumberOfCalls(10)
            // Sliding window size for statistics
            .slidingWindowSize(20)
            .build();
        
        return CircuitBreaker.of(name, config);
    }

    @Override
    public int getOrder() {
        return -75;
    }
}