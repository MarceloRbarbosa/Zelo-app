package br.com.zelo.platform.beneficiary;

import org.springframework.data.domain.Sort;

public record BeneficiarySearch(String name, String documentNumber, Long activityId, Sort.Direction order,
		Status status) {

	// ALL means active and inactive; anonymized records never appear in searches.
	public enum Status {
		ACTIVE, INACTIVE, ALL
	}

}
