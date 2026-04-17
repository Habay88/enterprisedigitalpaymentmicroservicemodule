package com.edpp.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Service - Handles JWT token validation and claims extraction
 * 
 * JWT (JSON Web Token) structure:
 * Header: Algorithm and token type
 * Payload: Claims (user data, expiration, etc.)
 * Signature: Verifies token integrity
 * 
 * Why JWT for authentication?
 * - Stateless (no session storage needed)
 * - Self-contained (user info in token)
 * - Secure (cryptographically signed)
 * - Scalable (works across multiple instances)
 */
@Service
public class JwtService {

    @Value("${jwt.secret:your-256-bit-secret-key-for-jwt-signing-please-change-in-production}")
    private String secret;

    /**
     * Get signing key for JWT verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validate JWT token
     * Checks:
     * - Signature is valid
     * - Token hasn't expired
     * - Token format is correct
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract all claims from token
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Extract user ID from token (subject claim)
     */
    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract user email from token (custom claim)
     */
    public String getUserEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * Extract tenant ID from token (custom claim)
     */
    public String getTenantIdFromToken(String token) {
        return getClaims(token).get("tenantId", String.class);
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getClaims(token).getExpiration();
        return expiration.before(new Date());
    }
}