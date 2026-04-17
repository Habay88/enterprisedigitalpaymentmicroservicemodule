package com.edpp.gateway.config;

import com.edpp.gateway.filter.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration - JWT Authentication and Authorization
 * 
 * This configures security for the gateway using Spring Security WebFlux.
 * 
 * Key Concepts:
 * - JWT (JSON Web Token): Stateless authentication
 * - Public Endpoints: No authentication required (login, health, docs)
 * - Protected Endpoints: Require valid JWT or API key
 * - CORS: Cross-Origin Resource Sharing configuration
 * 
 * Why WebFlux Security?
 * - Reactive programming model
 * - Non-blocking I/O
 * - Better performance for gateway
 */
@Configuration
@EnableWebFluxSecurity  // Enables reactive security for WebFlux
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationFilter authenticationFilter;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/api/v1/auth/**",           // Login/register endpoints
        "/api/v1/merchants/webhook/**", // Webhooks (called by external systems)
        "/actuator/health",          // Health checks for k8s
        "/actuator/info",            // Service info
        "/v3/api-docs/**",           // OpenAPI specs
        "/swagger-ui/**",            // Swagger UI resources
        "/swagger-ui.html"
    );

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF - not needed for stateless APIs
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints - no authentication required
                .pathMatchers(PUBLIC_ENDPOINTS.toArray(new String[0])).permitAll()
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            
            // Add custom authentication filter before Spring Security's default
            .addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            
            .build();
    }

    /**
     * CORS Configuration - Allows cross-origin requests
     * 
     * Important for:
     * - Mobile apps calling from different domains
     * - Web applications hosted on different ports
     * - API testing tools (Postman, etc.)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins (configure specific domains in production)
        configuration.setAllowedOrigins(List.of("*"));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",      // JWT token
            "Content-Type",       // Request body format
            "X-Tenant-ID",        // Tenant identification
            "X-API-Key",          // Merchant API key
            "X-API-Secret"        // Merchant API secret
        ));
        
        // Headers exposed to client
        configuration.setExposedHeaders(List.of(
            "X-Request-ID",        // Request tracking ID
            "X-RateLimit-Remaining" // Remaining rate limit
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}