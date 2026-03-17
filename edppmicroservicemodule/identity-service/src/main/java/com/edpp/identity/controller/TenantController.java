package com.edpp.identity.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.edpp.identity.enums.TenantStatus;
import com.edpp.identity.model.Tenant;
import com.edpp.identity.service.TenantService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Management", description = "APIs for managing tenants (banks/fintechs)")
public class TenantController {
    
    private final TenantService tenantService;
    
    @PostMapping
    @Operation(summary = "Create a new tenant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tenant> createTenant(@Valid @RequestBody Tenant tenant) {
        log.info("Creating new tenant: {}", tenant.getName());
        Tenant createdTenant = tenantService.createTenant(tenant);
        return new ResponseEntity<>(createdTenant, HttpStatus.CREATED);
    }
    
    @GetMapping("/{tenantId}")
    @Operation(summary = "Get tenant by ID")
    public ResponseEntity<Tenant> getTenant(@PathVariable String tenantId) {
        Tenant tenant = tenantService.getTenantByTenantId(tenantId);
        return ResponseEntity.ok(tenant);
    }
    
    @GetMapping("/schema/{schemaName}")
    @Operation(summary = "Get tenant by schema name")
    public ResponseEntity<Tenant> getTenantBySchema(@PathVariable String schemaName) {
        Tenant tenant = tenantService.getTenantBySchemaName(schemaName);
        return ResponseEntity.ok(tenant);
    }
    
    @GetMapping("/domain/{domain}")
    @Operation(summary = "Get tenant by domain")
    public ResponseEntity<Tenant> getTenantByDomain(@PathVariable String domain) {
        Tenant tenant = tenantService.getTenantByDomain(domain);
        return ResponseEntity.ok(tenant);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get all active tenants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Tenant>> getAllActiveTenants() {
        List<Tenant> tenants = tenantService.getAllActiveTenants();
        return ResponseEntity.ok(tenants);
    }
    
    @PatchMapping("/{tenantId}/status")
    @Operation(summary = "Update tenant status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tenant> updateTenantStatus(
            @PathVariable String tenantId,
            @RequestParam TenantStatus status,
            @RequestParam(required = false) String reason) {
        
        Tenant updatedTenant = tenantService.updateTenantStatus(tenantId, status, reason);
        return ResponseEntity.ok(updatedTenant);
    }
    
    @GetMapping("/validate/{tenantId}")
    @Operation(summary = "Validate tenant access")
    public ResponseEntity<Boolean> validateTenantAccess(@PathVariable String tenantId) {
        boolean isValid = tenantService.validateTenantAccess(tenantId);
        return ResponseEntity.ok(isValid);
    }
}