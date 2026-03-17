package com.edpp.identity.requestdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;



import com.edpp.identity.model.KycDetails;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycUpdateRequest {

    @NotBlank(message = "ID type is required")
    private String idType;

    @NotBlank(message = "ID number is required")
    private String idNumber;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime idExpiryDate;

    @Pattern(regexp = "^(PASSPORT|DRIVERS_LICENSE|NATIONAL_ID|VOTERS_CARD)$", 
             message = "Invalid document type")
    private String documentType;

    private String documentUrl;

    public KycDetails toKycDetails() {
        return KycDetails.builder()
                .idType(this.idType)
                .idNumber(this.idNumber)
                .idExpiryDate(this.idExpiryDate)
                .kycCompleted(true)
                .kycVerifiedAt(LocalDateTime.now())
                .build();
    }
}