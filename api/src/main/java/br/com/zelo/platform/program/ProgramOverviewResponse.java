package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.util.List;

public record ProgramOverviewResponse(
		LocalDate from,
		LocalDate to,
		Integer attendanceRate,
		long activeEnrollments,
		long leftEnrollments,
		List<ActivityOverview> activities) {

	public record ActivityOverview(
			Long activityId,
			String title,
			ActivityStatus status,
			Integer attendanceRate,
			long activeEnrollments) {
	}

}
