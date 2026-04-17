package com.edpp.merchant.enums;

public enum VerificationStatus {

	   PENDING,      // Awaiting verification
	    IN_PROGRESS,  // Verification in progress
	    VERIFIED,     // Fully verified
	    REJECTED,     // Verification failed
	    FLAGGED       // Requires manual review
}
