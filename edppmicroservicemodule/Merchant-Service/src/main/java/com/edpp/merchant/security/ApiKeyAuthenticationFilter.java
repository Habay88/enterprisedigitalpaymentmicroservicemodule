package com.edpp.merchant.security;

import com.edpp.merchant.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String publicKey = request.getHeader("X-API-Key");
        String secretKey = request.getHeader("X-API-Secret");

        if (publicKey != null && secretKey != null && apiKeyService.validateApiKey(publicKey, secretKey)) {
            String merchantId = apiKeyService.getMerchantIdFromApiKey(publicKey);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    merchantId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}