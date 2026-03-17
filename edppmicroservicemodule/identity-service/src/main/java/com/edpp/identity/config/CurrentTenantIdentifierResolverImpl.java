package com.edpp.identity.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.edpp.identity.tenant.TenantContext;

public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {
    
    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : "public";
    }
    
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
