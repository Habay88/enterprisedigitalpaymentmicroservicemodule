package com.edpp.transaction.config;

import com.edpp.transaction.util.RequestContext;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignClientConfig {

    private final RequestContext requestContext;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Propagate tenant ID from RequestContext
            String tenantId = requestContext.getTenantId();
            if (tenantId != null && !tenantId.isEmpty()) {
                requestTemplate.header("X-Tenant-ID", tenantId);
                log.debug("Adding tenant header: {}", tenantId);
            }
            
            // Use static method as fallback
            if (tenantId == null) {
                String staticTenantId = RequestContext.getCurrentTenantId();
                if (staticTenantId != null) {
                    requestTemplate.header("X-Tenant-ID", staticTenantId);
                }
            }

            // Propagate request ID for tracing
            String requestId = requestContext.getRequestId();
            if (requestId != null) {
                requestTemplate.header("X-Request-ID", requestId);
            } else {
                String staticRequestId = RequestContext.getCurrentRequestId();
                if (staticRequestId != null) {
                    requestTemplate.header("X-Request-ID", staticRequestId);
                }
            }

            // Propagate correlation ID
            String correlationId = requestContext.getCorrelationId();
            if (correlationId != null) {
                requestTemplate.header("X-Correlation-ID", correlationId);
            }
        };
    }

    @Bean
    public Retryer retryer() {
        // Custom retryer with exponential backoff
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}

class FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, feign.Response response) {
        if (response.status() >= 400 && response.status() <= 499) {
            return new RuntimeException("Client error: " + response.status());
        }
        if (response.status() >= 500) {
            return new RuntimeException("Server error: " + response.status());
        }
        return defaultDecoder.decode(methodKey, response);
    }
}