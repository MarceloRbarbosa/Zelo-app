package br.com.zelo.platform.beneficiary;

import java.time.LocalDate;

public record BeneficiaryResponse(
		Long id,
		String fullName,
		String documentNumber,
		LocalDate birthDate,
		String phoneNumber,
		String address,
		BeneficiaryStatus status) {

	static BeneficiaryResponse from(Beneficiary beneficiary) {
		return new BeneficiaryResponse(beneficiary.getId(), beneficiary.getFullName(),
				beneficiary.getDocumentNumber(), beneficiary.getBirthDate(), beneficiary.getPhoneNumber(),
				beneficiary.getAddress(), beneficiary.getStatus());
	}

}
