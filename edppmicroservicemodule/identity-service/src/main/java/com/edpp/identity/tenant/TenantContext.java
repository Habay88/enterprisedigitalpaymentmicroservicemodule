package com.edpp.identity.tenant;

import lombok.extern.slf4j.Slf4j;
/**
 * Holds the current tenant context for the request thread
 */
@Slf4j
public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();
    public static void setTenantId(String tenantId) {
        log.debug("Setting tenantId: {} in thread: {}", tenantId, Thread.currentThread().getName());
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setSchema(String schema) {
        CURRENT_SCHEMA.set(schema);
    }

    public static String getSchema() {
        return CURRENT_SCHEMA.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_SCHEMA.remove();
    }
}
