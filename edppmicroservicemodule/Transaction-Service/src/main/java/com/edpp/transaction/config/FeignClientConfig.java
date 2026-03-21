package com.edpp.transaction.config;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Propagate tenant ID
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                requestTemplate.header("X-Tenant-ID", tenantId);
            }

            // Propagate authorization header
            String authorization = getAuthorizationHeader();
            if (authorization != null) {
                requestTemplate.header("Authorization", authorization);
            }

            // Propagate request ID for tracing
            String requestId = RequestContext.getRequestId();
            if (requestId != null) {
                requestTemplate.header("X-Request-ID", requestId);
            }
        };
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    private String getAuthorizationHeader() {
        // Extract JWT token from security context
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            return "Bearer " + authentication.getCredentials();
        }
        return null;
    }
}

class FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, feign.Response response) {
        if (response.status() >= 400 && response.status() <= 499) {
            return new TransactionException("Client error: " + response.status());
        }
        if (response.status() >= 500) {
            return new ProcessorException("Server error: " + response.status());
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
