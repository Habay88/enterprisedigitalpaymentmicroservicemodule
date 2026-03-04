package com.edpp.identity.dto;

import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Customer information response")
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
    
    @Schema(description = "Customer's phone number", example = "+1234567890")
    @JsonProperty("phone_number")
    private String phoneNumber;
    
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
    
    @Schema(description = "KYC verification details")
    @JsonProperty("kyc_details")
    private KycDetails kycDetails;
    
    @Schema(description = "Customer's address")
    private Address address;
    
    @Schema(description = "Account creation timestamp")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Message or additional info", example = "Customer retrieved successfully")
    private String message;
}
