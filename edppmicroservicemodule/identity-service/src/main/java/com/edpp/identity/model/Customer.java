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
               @UniqueConstraint(columnNames = {"tenant_id", "nin"}),
               @UniqueConstraint(columnNames = {"tenant_id", "phone_number"})
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

    private String middleName;

    @Column(unique = true)
    private String email;

    @Column(name = "phone_number")
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

    @Embedded
    private Address residentialAddress;

    @Embedded
    private KycDetails kycDetails;

    @Enumerated(EnumType.STRING)
    private CustomerType customerType; // INDIVIDUAL, CORPORATE

    @Enumerated(EnumType.STRING)
    private CustomerStatus status; // ACTIVE, BLOCKED, CLOSED, PENDING_ACTIVATION, DEACTIVATED

    @Enumerated(EnumType.STRING)
    private RiskRating riskRating; // LOW, MEDIUM, HIGH, VERY_HIGH, UNASSESSED

    private LocalDateTime dateOfBirth;
    private String taxId; // SSN/EIN equivalent

    @Column(name = "blocked_reason")
    private String blockedReason;

    @Column(name = "deactivation_reason")
    private String deactivationReason;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private Set<Wallet> wallets = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;

    // Helper methods
    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }

    public boolean isActive() {
        return CustomerStatus.ACTIVE.equals(this.status);
    }

    public boolean isBlocked() {
        return CustomerStatus.BLOCKED.equals(this.status);
    }

    public boolean isDeactivated() {
        return CustomerStatus.DEACTIVATED.equals(this.status);
    }

    public boolean isPendingActivation() {
        return CustomerStatus.PENDING_ACTIVATION.equals(this.status);
    }

    public boolean hasCompletedKyc() {
        return kycDetails != null && kycDetails.isKycCompleted();
    }

    public boolean hasVerifiedBvn() {
        return bvnVerification != null && bvnVerification.isVerified();
    }

    public boolean hasVerifiedNin() {
        return ninVerification != null && ninVerification.isVerified();
    }
}