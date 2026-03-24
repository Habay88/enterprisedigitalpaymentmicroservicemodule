package com.edpp.transaction.util;



import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local holder for tenant context
 * Ensures tenant isolation across threads
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();
    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    /**
     * Set tenant ID for current thread
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) {
            log.debug("Setting tenantId: {} in thread: {}", tenantId, Thread.currentThread().getName());
            CURRENT_TENANT.set(tenantId);
        } else {
            log.warn("Attempted to set null or empty tenantId");
        }
    }

    /**
     * Get tenant ID for current thread
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Set schema name for current thread
     */
    public static void setSchema(String schema) {
        if (schema != null && !schema.isEmpty()) {
            CURRENT_SCHEMA.set(schema);
        }
    }

    /**
     * Get schema name for current thread
     */
    public static String getSchema() {
        return CURRENT_SCHEMA.get();
    }

    /**
     * Set correlation ID for tracing
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            CORRELATION_ID.set(correlationId);
        }
    }

    /**
     * Get correlation ID
     */
    public static String getCorrelationId() {
        return CORRELATION_ID.get();
    }

    /**
     * Clear all tenant context for current thread
     */
    public static void clear() {
        log.debug("Clearing tenant context for thread: {}", Thread.currentThread().getName());
        CURRENT_TENANT.remove();
        CURRENT_SCHEMA.remove();
        CORRELATION_ID.remove();
    }

    /**
     * Check if tenant context is present
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Get current tenant or throw exception
     */
    public static String requireTenant() {
        String tenantId = getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available");
        }
        return tenantId;
    }
}