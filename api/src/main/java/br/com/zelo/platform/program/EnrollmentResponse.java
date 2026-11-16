package br.com.zelo.platform.program;

import java.time.LocalDate;

public record EnrollmentResponse(
		Long beneficiaryId,
		String fullName,
		EnrollmentStatus status,
		LocalDate leftAt) {
}
