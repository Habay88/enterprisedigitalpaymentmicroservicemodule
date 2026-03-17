package com.edpp.identity.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider {
    
    private final DataSource dataSource;
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();
    
    public MultiTenantConnectionProviderImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        String tenantId = (String) tenantIdentifier;
        
        if (tenantId == null || tenantId.isEmpty()) {
            return dataSource.getConnection();
        }
        
        DataSource tenantDataSource = dataSourceMap.computeIfAbsent(tenantId, k -> {
            // Create tenant-specific DataSource
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:postgresql://localhost:5432/" + tenantId + "_db");
            ds.setUsername("postgres");
            ds.setPassword("password");
            ds.setDriverClassName("org.postgresql.Driver");
            return ds;
        });
        
        return tenantDataSource.getConnection();
    }
    
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    @Override
    public boolean supportsAggressiveRelease() {
        return true;
    }
    
    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }
    
    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}