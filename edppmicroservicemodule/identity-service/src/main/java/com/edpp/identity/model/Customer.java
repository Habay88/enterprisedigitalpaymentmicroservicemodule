package com.edpp.identity.model;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.CustomerType;
import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.tenant.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers",
       uniqueConstraints = {
               @UniqueConstraint(columnNames = {"tenant_id", "cif_number"}),
               @UniqueConstraint(columnNames = {"tenant_id", "email"}),
               @UniqueConstraint(columnNames = {"tenant_id", "bvn"}),
               @UniqueConstraint(columnNames = {"tenant_id", "nin"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Customer extends TenantAwareEntity {

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
    // Our Nigerian-specific identification
    @Column(name = "bvn", length = 11)
    private String bvn; // Bank Verification Number (11 digits)

    @Column(name = "nin", length = 11)
    private String nin; // National Identification Number (11 digits)

    @Embedded
    private BvnVerification bvnVerification;

    @Embedded
    private NinVerification ninVerification;


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
