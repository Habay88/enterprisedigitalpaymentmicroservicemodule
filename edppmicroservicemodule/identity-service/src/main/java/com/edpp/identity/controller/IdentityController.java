package com.edpp.identity.controller;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.mapper.CustomerMapper;
import com.edpp.identity.model.Customer;
import com.edpp.identity.requestdto.CustomerRequest;
import com.edpp.identity.requestdto.KycUpdateRequest;
import com.edpp.identity.requestdto.RequestContext;
import com.edpp.identity.responsedto.CustomerResponse;
import com.edpp.identity.responsedto.PageResponse;
import com.edpp.identity.responsedto.ApiResponse;
import com.edpp.identity.service.AuditService;
import com.edpp.identity.service.IdentityService;
import com.edpp.identity.service.IdentityValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/customers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "APIs for managing customer information (CIF)")
public class IdentityController {

    private final IdentityService identityService;
    private final AuditService auditService;
    private final CustomerMapper customerMapper;
    private final IdentityValidationService validationService;
    private final RequestContext requestContext;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Onboard a new customer",
        description = "Creates a new customer record with CIF number and optional BVN/NIN verification"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Customer created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Customer already exists")
    })
    public ResponseEntity<ApiResponse<CustomerResponse>> onboardCustomer(
            @Valid @RequestBody CustomerRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        log.info("Onboarding new customer with email: {} for tenant: {}", 
                request.getEmail(), requestContext.getTenantId());

        // Validate BVN/NIN if provided
        validateIdentity(request);

        Customer customer = customerMapper.toEntity(request);
        customer.setCreatedBy(getUsername(currentUser));
        customer.setTenantId(requestContext.getTenantId());

        Customer savedCustomer = identityService.onboardCustomer(customer);
        auditService.logCustomerOnboarding(savedCustomer);

        CustomerResponse response = customerMapper.toResponse(savedCustomer);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CustomerResponse>builder()
                        .success(true)
                        .message("Customer onboarded successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .requestId(requestContext.getRequestId())
                        .build());
    }

    @GetMapping("/{cifNumber}")
    @Operation(summary = "Get customer by CIF number")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable String cifNumber) {
        log.debug("Fetching customer with CIF: {} for tenant: {}", 
                 cifNumber, requestContext.getTenantId());

        Customer customer = identityService.getCustomerByCif(cifNumber);
        CustomerResponse response = customerMapper.toResponse(customer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer retrieved successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @GetMapping("/bvn/{bvn}")
    @Operation(summary = "Get customer by BVN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByBvn(@PathVariable String bvn) {
        validationService.validateBvn(bvn, false);
        
        Customer customer = identityService.getCustomerByBvn(bvn);
        CustomerResponse response = customerMapper.toResponse(customer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer retrieved successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @GetMapping("/nin/{nin}")
    @Operation(summary = "Get customer by NIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByNin(@PathVariable String nin) {
        validationService.validateNin(nin, false);
        
        Customer customer = identityService.getCustomerByNin(nin);
        CustomerResponse response = customerMapper.toResponse(customer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer retrieved successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get customer by email")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByEmail(@PathVariable String email) {
        Customer customer = identityService.getCustomerByEmail(email);
        CustomerResponse response = customerMapper.toResponse(customer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer retrieved successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @GetMapping
    @Operation(summary = "Get all customers with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getAllCustomers(
            @PageableDefault(size = 20)
            @SortDefault.SortDefaults({
                @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        Page<Customer> customersPage = identityService.getAllCustomers(pageable);
        // FIXED: Use toResponse instead of toSimplifiedResponse
        Page<CustomerResponse> responsePage = customersPage.map(customer -> customerMapper.toResponse(customer));

        PageResponse<CustomerResponse> pageResponse = PageResponse.<CustomerResponse>builder()
                .content(responsePage.getContent())
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .last(responsePage.isLast())
                .first(responsePage.isFirst())
                .build();

        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerResponse>>builder()
                .success(true)
                .message("Customers retrieved successfully")
                .data(pageResponse)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers with filters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> searchCustomers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String searchTerm,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Customer> customers = identityService.searchCustomers(email, phone, status, searchTerm, pageable);
        // FIXED: Use toResponse instead of toSimplifiedResponse
        Page<CustomerResponse> responsePage = customers.map(customer -> customerMapper.toResponse(customer));

        PageResponse<CustomerResponse> pageResponse = PageResponse.<CustomerResponse>builder()
                .content(responsePage.getContent())
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerResponse>>builder()
                .success(true)
                .message("Search completed successfully")
                .data(pageResponse)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @PutMapping("/{cifNumber}/kyc")
    @Operation(summary = "Update KYC details")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateKyc(
            @PathVariable String cifNumber,
            @Valid @RequestBody KycUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        log.info("Updating KYC for customer: {}", cifNumber);

        String updatedBy = getUsername(currentUser);
        Customer updatedCustomer = identityService.updateKycStatus(
                cifNumber, 
                request.toKycDetails(), 
                updatedBy
        );

        auditService.logKycUpdate(updatedCustomer, updatedBy);

        CustomerResponse response = customerMapper.toResponse(updatedCustomer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("KYC updated successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @PostMapping("/{cifNumber}/block")
    @Operation(summary = "Block a customer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> blockCustomer(
            @PathVariable String cifNumber,
            @RequestParam String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        log.info("Blocking customer: {} with reason: {}", cifNumber, reason);

        String blockedBy = getUsername(currentUser);
        Customer blockedCustomer = identityService.blockCustomer(cifNumber, reason, blockedBy);

        auditService.logStatusChange(
                cifNumber,
                CustomerStatus.ACTIVE,
                CustomerStatus.BLOCKED,
                reason,
                blockedBy
        );

        CustomerResponse response = customerMapper.toResponse(blockedCustomer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer blocked successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @PostMapping("/{cifNumber}/unblock")
    @Operation(summary = "Unblock a customer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> unblockCustomer(
            @PathVariable String cifNumber,
            @RequestParam String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        log.info("Unblocking customer: {} with reason: {}", cifNumber, reason);

        String unblockedBy = getUsername(currentUser);
        Customer unblockedCustomer = identityService.unblockCustomer(cifNumber, reason, unblockedBy);

        auditService.logStatusChange(
                cifNumber,
                CustomerStatus.BLOCKED,
                CustomerStatus.ACTIVE,
                reason,
                unblockedBy
        );

        CustomerResponse response = customerMapper.toResponse(unblockedCustomer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer unblocked successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @PostMapping("/{cifNumber}/verify-bvn")
    @Operation(summary = "Verify BVN for customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> verifyBvn(
            @PathVariable String cifNumber,
            @RequestParam String bvn) {

        validationService.validateBvn(bvn, true);
        
        Customer verifiedCustomer = identityService.verifyBvn(cifNumber, bvn);
        CustomerResponse response = customerMapper.toResponse(verifiedCustomer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("BVN verified successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @PostMapping("/{cifNumber}/verify-nin")
    @Operation(summary = "Verify NIN for customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> verifyNin(
            @PathVariable String cifNumber,
            @RequestParam String nin) {

        validationService.validateNin(nin, true);
        
        Customer verifiedCustomer = identityService.verifyNin(cifNumber, nin);
        CustomerResponse response = customerMapper.toResponse(verifiedCustomer);

        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("NIN verified successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @GetMapping("/{cifNumber}/exists")
    @Operation(summary = "Check if customer exists")
    public ResponseEntity<ApiResponse<Boolean>> checkCustomerExists(@PathVariable String cifNumber) {
        boolean exists = identityService.customerExists(cifNumber);
        
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .data(exists)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get customer statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerStats() {
        Map<String, Object> stats = identityService.getCustomerStatistics();
        
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Statistics retrieved successfully")
                .data(stats)
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    @DeleteMapping("/{cifNumber}")
    @Operation(summary = "Deactivate customer account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(
            @PathVariable String cifNumber,
            @RequestParam String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser) {

        identityService.deactivateCustomer(cifNumber, reason, getUsername(currentUser));

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Customer deactivated successfully")
                .timestamp(LocalDateTime.now())
                .requestId(requestContext.getRequestId())
                .build());
    }

    private void validateIdentity(CustomerRequest request) {
        if (request.isVerifyIdentity()) {
            if (request.getBvn() != null) {
                validationService.validateBvn(request.getBvn(), true);
            }
            if (request.getNin() != null) {
                validationService.validateNin(request.getNin(), true);
            }
        }
    }

    private String getUsername(UserDetails userDetails) {
        return userDetails != null ? userDetails.getUsername() : "SYSTEM";
    }
}