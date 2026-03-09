package com.edpp.identity.requestdto;

import com.edpp.identity.enums.CustomerType;
import com.edpp.identity.model.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer onboarding request payload with Nigerian identity support")
public class CustomerRequest {
    @Schema(description = "Customer's first name", example = "John", required = true)
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s-']+$", message = "First name contains invalid characters")
    private String firstName;

    @Schema(description = "Customer's last name", example = "Doe", required = true)
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s-']+$", message = "Last name contains invalid characters")
    private String lastName;

    @Schema(description = "Customer's email address", example = "john.doe@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Schema(description = "Customer's phone number", example = "+2348012345678")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Schema(description = "Bank Verification Number (11 digits)", example = "12345678901")
    @Pattern(regexp = "^[0-9]{11}$", message = "BVN must be exactly 11 digits")
    private String bvn;

    @Schema(description = "National Identification Number (11 digits)", example = "12345678901")
    @Pattern(regexp = "^[0-9]{11}$", message = "NIN must be exactly 11 digits")
    private String nin;

    @Schema(description = "Customer type", example = "INDIVIDUAL", required = true)
    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @Schema(description = "Customer's date of birth", example = "1990-01-01T00:00:00")
    @Past(message = "Date of birth must be in the past")
    private LocalDateTime dateOfBirth;

    @Schema(description = "Tax ID / Social Security Number", example = "123-45-6789")
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{4}$|^\\d{9}$", message = "Invalid tax ID format")
    private String taxId;

    @Schema(description = "Customer's residential address")
    @Valid
    private Address address;

    @Schema(description = "Whether to verify identity with NIBSS/NIMC", example = "true")
    @JsonProperty("verify_identity")
    private boolean verifyIdentity;

    @Schema(description = "Request ID for tracing", example = "req-123-456")
    @JsonProperty("request_id")
    private String requestId;

}
