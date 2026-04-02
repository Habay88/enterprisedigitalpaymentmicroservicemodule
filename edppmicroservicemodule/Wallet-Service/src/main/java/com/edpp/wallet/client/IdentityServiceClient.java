package com.edpp.wallet.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.edpp.wallet.dtoresponse.CustomerResponse;

@FeignClient(name = "identity-service", path = "/api/v1/customers")
public interface IdentityServiceClient {

    @GetMapping("/{customerId}/validate")
    CustomerValidationResponse validateCustomer(
            @PathVariable("customerId") String customerId,
            @RequestHeader("X-Tenant-ID") String tenantId);


    @GetMapping("/{customerId}")
    CustomerResponse getCustomer(
            @PathVariable("customerId") String customerId,
            @RequestHeader("X-Tenant-ID") String tenantId);
}

record CustomerValidationResponse(boolean valid, String customerId, String status, 
                                   String message, String riskRating, boolean kycCompleted) {}


