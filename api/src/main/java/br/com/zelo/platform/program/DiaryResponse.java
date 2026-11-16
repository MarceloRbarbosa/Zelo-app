package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.util.List;

// Class diary: one row per enrolled beneficiary and one column per class date in the requested period.
public record DiaryResponse(
		Long activityId,
		String title,
		ActivityStatus status,
		List<LocalDate> dates,
		List<Student> students) {

	// attendanceRate is the share of presences over every attendance recorded, or null without records.
	public record Student(
			Long beneficiaryId,
			String fullName,
			EnrollmentStatus enrollmentStatus,
			LocalDate leftAt,
			Integer attendanceRate,
			boolean dropoutRisk,
			List<Mark> marks) {
	}

	public record Mark(LocalDate date, boolean present, String notes) {
	}

}
