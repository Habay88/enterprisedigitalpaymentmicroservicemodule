package com.edpp.identity.service;

import com.edpp.identity.model.BvnVerification;
import com.edpp.identity.model.Customer;
import com.edpp.identity.model.NinVerification;
import com.edpp.identity.responsedto.NimcVerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor

public class IdentityValidationService {

    private static final Pattern BVN_PATTERN = Pattern.compile("^[0-9]{11}$");
    private static final Pattern NIN_PATTERN = Pattern.compile("^[0-9]{11}$");

    private final NibssIntegrationService nibssIntegrationService;
    /**
     * Validate BVN format and optionally verify with NIBSS
     */
    public boolean validateBvn(String bvn, boolean verfiyWithNibss){
        if(bvn == null || !BVN_PATTERN.matcher(bvn).matches()){
            throw new ValidationException("BVN must be exactly 11 digits")
        }
        if(verfiyWithNibss){
            return verfiyWithNibss(bvn);
        }
        return true;
    }
    /**
     * Validate NIN format and optionally verify with NIMC
     */
    public boolean validateNin(String nin, boolean verifyWithNimc) {
        if (nin == null || !NIN_PATTERN.matcher(nin).matches()) {
            throw new ValidationException("NIN must be exactly 11 digits");
        }

        if (verifyWithNimc) {
            return verifyNinWithNimc(nin);
        }

        return true;
    }
    /**
     * Verify BVN with NIBSS (Central BVN database)
     */
    private boolean verifyBvnWithNibss(String bvn) {
        try {
            // Call NIBSS API for BVN verification
            NibssVerificationResponse response = nibssIntegrationService.verifyBvn(bvn);

            if (response.isSuccessful()) {
                log.info("BVN verified successfully: {}", bvn.substring(0, 6) + "*****");
                return true;
            } else {
                log.warn("BVN verification failed: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying BVN with NIBSS", e);
            return false;
        }
    }
    /**
     * Verify NIN with NIMC (National Identity Management Commission)
     */
    private boolean verifyNinWithNimc(String nin) {
        try {
            // Call NIMC API for NIN verification
            NimcVerificationResponse response = nibssIntegrationService.verifyNin(nin);

            if (response.isSuccessful()) {
                log.info("NIN verified successfully: {}", nin.substring(0, 6) + "*****");
                return true;
            } else {
                log.warn("NIN verification failed: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying NIN with NIMC", e);
            return false;
        }
        /**
         * Perform comprehensive identity verification (BVN + NIN)
         */
        public Customer performIdentityVerification(Customer customer, String bvn, String nin) {
            log.info("Performing identity verification for customer: {}", customer.getEmail());

            // Validate and verify BVN
            if (bvn != null) {
                validateBvn(bvn, true);
                customer.setBvn(bvn);

                BvnVerification bvnVerification = BvnVerification.builder()
                        .verified(true)
                        .verifiedAt(LocalDateTime.now())
                        .verifiedBy("SYSTEM")
                        .verificationReference(UUID.randomUUID().toString())
                        .responseCode("00")
                        .responseMessage("BVN verified successfully")
                        .build();

                customer.setBvnVerification(bvnVerification);
            }

            // Validate and verify NIN
            if (nin != null) {
                validateNin(nin, true);
                customer.setNin(nin);

                NinVerification ninVerification = NinVerification.builder()
                        .verified(true)
                        .verifiedAt(LocalDateTime.now())
                        .verifiedBy("SYSTEM")
                        .verificationReference(UUID.randomUUID().toString())
                        .responseCode("00")
                        .responseMessage("NIN verified successfully")
                        .build();

                customer.setNinVerification(ninVerification);
            }

            return customer;
        }
    }

}


