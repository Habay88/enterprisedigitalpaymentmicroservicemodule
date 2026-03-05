package com.edpp.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.ValidationException;
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

}


