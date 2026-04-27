package com.edpp.iso8583.client;

import com.edpp.iso8583.dto.AuthorizationRequest;
import com.edpp.iso8583.dto.AuthorizationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "transaction-service", path = "/api/v1/transactions")
public interface TransactionServiceClient {

    @PostMapping("/iso/authorize")
    AuthorizationResponse processAuthorization(
            @RequestBody AuthorizationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId
    );

    @PostMapping("/iso/reverse")
    void reverseTransaction(
            @RequestParam String stan,
            @RequestParam String reason,
            @RequestHeader("X-Tenant-ID") String tenantId
    );
}