package com.edpp.identity.controller;


import com.edpp.identity.responsedto.CustomerResponse;
import com.edpp.identity.service.AuditService;
import com.edpp.identity.service.IdentityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "APIs for managing customer information (CIF)")
public class IdentityController {
    
    private final IdentityService identityService;
    private final AuditService auditService;
    

    }
