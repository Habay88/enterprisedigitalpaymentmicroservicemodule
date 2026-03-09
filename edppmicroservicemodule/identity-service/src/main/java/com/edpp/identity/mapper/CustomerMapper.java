package com.edpp.identity.mapper;

import com.edpp.identity.model.Customer;
import com.edpp.identity.model.KycDetails;
import com.edpp.identity.requestdto.CustomerRequest;
import com.edpp.identity.responsedto.CustomerResponse;
import com.edpp.identity.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CustomerMapper {

    /**
     * Converts CustomerRequest to Customer entity with tenant context
     */
    public Customer toEntity(CustomerRequest request){
        if(request == null){
            return null;
        }
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .bvn(request.getBvn())
                .nin(request.getNin())
                .customerType(request.getCustomerType())
                .dateOfBirth(request.getDateOfBirth())
                .taxId(request.getTaxId())
                .residentialAddress(request.getAddress())
                .build();
        // set tenant context
        customer.setTenantId(TenantContext.getTenantId());
        customer.setTenantSchema(TenantContext.getSchema());

        return customer;
    }
    /**
     * Converts Customer entity to CustomerResponse
     */
    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }

        return CustomerResponse.builder()
                .id(customer.getId())
                .cifNumber(customer.getCifNumber())
                .fullName(formatFullName(customer.getFirstName(), customer.getLastName()))
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .maskedBvn(maskBvn(customer.getBvn()))
                .maskedNin(maskNin(customer.getNin()))
                .bvnVerified(isBvnVerified(customer))
                .ninVerified(isNinVerified(customer))
                .customerType(customer.getCustomerType())
                .status(customer.getStatus())
                .riskRating(customer.getRiskRating())
                .kycCompleted(isKycCompleted(customer))
                .address(customer.getResidentialAddress())
                .tenantId(customer.getTenantId())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    /**
     * Converts Customer entity to CustomerResponse with custom message
     */
    public CustomerResponse toResponse(Customer customer, String message) {
        CustomerResponse response = toResponse(customer);
        if (response != null) {
            response.setMessage(message);
        }
        return response;
    }

    /**
     * Converts list of Customer entities to list of CustomerResponse
     */
    public List<CustomerResponse> toResponseList(Collection<Customer> customers) {
        if (customers == null || customers.isEmpty()) {
            return Collections.emptyList();
        }

        return customers.stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates existing Customer entity with non-null fields from CustomerRequest
     */
    public Customer updateEntity(Customer customer, CustomerRequest request) {
        if (customer == null || request == null) {
            return customer;
        }

        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            customer.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            customer.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getBvn() != null) {
            customer.setBvn(request.getBvn());
        }
        if (request.getNin() != null) {
            customer.setNin(request.getNin());
        }
        if (request.getCustomerType() != null) {
            customer.setCustomerType(request.getCustomerType());
        }
        if (request.getDateOfBirth() != null) {
            customer.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getTaxId() != null) {
            customer.setTaxId(request.getTaxId());
        }
        if (request.getAddress() != null) {
            customer.setResidentialAddress(request.getAddress());
        }

        return customer;
    }

    private String formatFullName(String firstName, String lastName) {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            fullName.append(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }
        return fullName.toString();
    }

    private String maskBvn(String bvn) {
        if (bvn == null || bvn.length() < 11) {
            return null;
        }
        return bvn.substring(0, 6) + "*****";
    }

    private String maskNin(String nin) {
        if (nin == null || nin.length() < 11) {
            return null;
        }
        return nin.substring(0, 6) + "*****";
    }

    private boolean isBvnVerified(Customer customer) {
        return customer.getBvnVerification() != null &&
                customer.getBvnVerification().isVerified();
    }

    private boolean isNinVerified(Customer customer) {
        return customer.getNinVerification() != null &&
                customer.getNinVerification().isVerified();
    }

    private boolean isKycCompleted(Customer customer) {
        KycDetails kycDetails = customer.getKycDetails();
        return kycDetails != null && kycDetails.isKycCompleted();
    }
}