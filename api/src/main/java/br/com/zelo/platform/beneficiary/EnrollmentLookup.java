package br.com.zelo.platform.beneficiary;

import java.util.Set;

// Implemented by the program module, so the search by activity does not make beneficiary depend on it.
public interface EnrollmentLookup {

	Set<Long> activeBeneficiaryIds(Long activityId);

}
