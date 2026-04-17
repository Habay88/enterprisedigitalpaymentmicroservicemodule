package com.edpp.gateway.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback Handler - Provides graceful degradation when services are unavailable
 * 
 * When a downstream service fails, these fallbacks return meaningful responses
 * instead of letting the request fail completely.
 * 
 * Benefits:
 * - Better user experience (custom error messages)
 * - Allows partial functionality when services are down
 * - Prevents cascading failures
 */
@Component
public class FallbackHandler {

    public Mono<ServerResponse> handleIdentityFallback(ServerRequest request) {
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Identity service temporarily unavailable",
                "status", "503",
                "fallback", "true",
                "message", "Please try again later"
            ));
    }

    public Mono<ServerResponse> handleWalletFallback(ServerRequest request) {
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Wallet service temporarily unavailable",
                "status", "503",
                "fallback", "true"
            ));
    }

    public Mono<ServerResponse> handleTransactionFallback(ServerRequest request) {
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Transaction service temporarily unavailable",
                "status", "503",
                "fallback", "true"
            ));
    }

    public Mono<ServerResponse> handleLedgerFallback(ServerRequest request) {
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Ledger service temporarily unavailable",
                "status", "503",
                "fallback", "true"
            ));
    }

    public Mono<ServerResponse> handleSettlementFallback(ServerRequest request) {
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Settlement service temporarily unavailable",
                "status", "503",
                "fallback", "true"
            ));
    }

    public Mono<ServerResponse> handleMerchantFallback(ServerRequest request) {
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "Merchant service temporarily unavailable",
                "status", "503",
                "fallback", "true"
            ));
    }
}