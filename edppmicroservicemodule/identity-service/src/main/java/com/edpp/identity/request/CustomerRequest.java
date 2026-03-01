package com.edpp.identity.request;

import com.edpp.identity.enums.CustomerType;
import com.edpp.identity.model.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    private String phoneNumber;

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @Past(message = "Date of birth must be in the past")
    private LocalDateTime dateOfBirth;

    private String taxId;

    @Valid
    private Address address;
}
