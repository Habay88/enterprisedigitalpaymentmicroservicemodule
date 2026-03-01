package com.edpp.identity.controller;

import com.edpp.identity.request.CustomerRequest;
import com.edpp.identity.response.CustomerResponse;
import com.edpp.identity.service.AuditService;
import com.edpp.identity.service.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name= "Customer Management", description="APIs FOR MANAGING CUSTOMER INFORMATION(CIF)")
public class IdentityController {
private final IdentityService identityService;
private final AuditService auditService;

    @PostMapping
    @Operation(summary = "Onboard a new customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Customer already exists")
    })
    public ResponseEntity<CustomerResponse> onBoardCustomer(@Valid @RequestBody CustomerRequest request)
    {
        log.info("Received customer onboarding request for email: {}",request.getEmail());
        
    }
}
