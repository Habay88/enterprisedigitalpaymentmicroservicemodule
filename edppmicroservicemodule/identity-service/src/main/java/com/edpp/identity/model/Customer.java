package com.edpp.identity.model;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.CustomerType;
import com.edpp.identity.enums.RiskRating;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String cifNumber; // Customer Information File Number

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true)
    private String email;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private CustomerType customerType; // INDIVIDUAL, CORPORATE

    @Enumerated(EnumType.STRING)
    private CustomerStatus status; // ACTIVE, BLOCKED, CLOSED

    @Enumerated(EnumType.STRING)
    private RiskRating riskRating; // LOW, MEDIUM, HIGH

    private LocalDateTime dateOfBirth;
    private String taxId; // SSN/EIN equivalent

    @Embedded
    private Address residentialAddress;

    @Embedded
    private KycDetails kycDetails;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private Set<Wallet> wallets = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

}
