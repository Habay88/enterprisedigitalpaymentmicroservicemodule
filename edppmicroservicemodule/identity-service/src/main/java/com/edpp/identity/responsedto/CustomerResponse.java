package com.edpp.identity.responsedto;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.CustomerType;
import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.model.Address;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Customer information response with Nigerian identity details")
public class CustomerResponse {
    @Schema(description = "Customer unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Customer Information File Number", example = "CIF20240301123456")
    @JsonProperty("cif_number")
    private String cifNumber;

    @Schema(description = "Customer's full name", example = "John Doe")
    @JsonProperty("full_name")
    private String fullName;

    @Schema(description = "Customer's email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Customer's phone number", example = "+2348012345678")
    @JsonProperty("phone_number")
    private String phoneNumber;

    @Schema(description = "Bank Verification Number (masked)", example = "12345*****")
    @JsonProperty("bvn")
    private String maskedBvn;

    @Schema(description = "National Identification Number (masked)", example = "12345*****")
    @JsonProperty("nin")
    private String maskedNin;

    @Schema(description = "BVN verification status")
    @JsonProperty("bvn_verified")
    private boolean bvnVerified;

    @Schema(description = "NIN verification status")
    @JsonProperty("nin_verified")
    private boolean ninVerified;

    @Schema(description = "Customer type", example = "INDIVIDUAL")
    @JsonProperty("customer_type")
    private CustomerType customerType;

    @Schema(description = "Customer account status", example = "ACTIVE")
    private CustomerStatus status;

    @Schema(description = "Risk rating assigned to customer", example = "LOW")
    @JsonProperty("risk_rating")
    private RiskRating riskRating;

    @Schema(description = "Whether KYC process is completed", example = "true")
    @JsonProperty("kyc_completed")
    private boolean kycCompleted;

    @Schema(description = "Customer's address")
    private Address address;

    @Schema(description = "Tenant ID", example = "bank_a")
    @JsonProperty("tenant_id")
    private String tenantId;

    @Schema(description = "Account creation timestamp")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "Message or additional info", example = "Customer retrieved successfully")
    private String message;
}
