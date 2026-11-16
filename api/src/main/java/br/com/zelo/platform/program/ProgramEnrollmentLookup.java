package br.com.zelo.platform.program;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.EnrollmentLookup;

@Component
public class ProgramEnrollmentLookup implements EnrollmentLookup {

	private final EnrollmentRepository repository;

	public ProgramEnrollmentLookup(EnrollmentRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Set<Long> activeBeneficiaryIds(Long activityId) {
		return Set.copyOf(repository.findBeneficiaryIds(activityId, EnrollmentStatus.ACTIVE));
	}

}
