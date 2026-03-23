package com.edpp.transaction.service;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.edpp.transaction.dtoresponse.CustomerLimitResponse;
import com.edpp.transaction.dtoresponse.CustomerValidationResponse;

@FeignClient(name = "identity-service", path = "/api/v1/customers")
public interface IdentityServiceClient {

    @GetMapping("/{customerId}/validate")
    CustomerValidationResponse validateCustomer(@PathVariable("customerId") String customerId,
                                                @RequestHeader("X-Tenant-ID") String tenantId);

    @GetMapping("/{customerId}/limits")
    CustomerLimitResponse getCustomerLimits(@PathVariable("customerId") String customerId,
                                            @RequestHeader("X-Tenant-ID") String tenantId);

    @GetMapping("/{customerId}/risk-rating")
    String getCustomerRiskRating(@PathVariable("customerId") String customerId,
                                  @RequestHeader("X-Tenant-ID") String tenantId);
}