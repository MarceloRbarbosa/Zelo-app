package br.com.zelo.platform.program;

import java.time.LocalDate;

public record DropoutAlertResponse(
		Long activityId,
		String activityTitle,
		Long beneficiaryId,
		String beneficiaryName,
		String phoneNumber,
		long consecutiveAbsences,
		LocalDate lastAttendanceDate) {
}
