package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import br.com.zelo.platform.identity.StaffSummary;

public record ActivityResponse(
		Long id,
		String title,
		String description,
		ActivityStatus status,
		LocalDate startDate,
		LocalDate endDate,
		List<ActivitySchedule> schedules,
		List<StaffSummary> staff) {

	static ActivityResponse from(Activity activity, List<StaffSummary> staff) {
		List<ActivitySchedule> schedules = activity.getSchedules()
			.stream()
			.sorted(Comparator.comparing(ActivitySchedule::dayOfWeek).thenComparing(ActivitySchedule::startTime))
			.toList();
		List<StaffSummary> sortedStaff = staff.stream().sorted(Comparator.comparing(StaffSummary::fullName)).toList();
		return new ActivityResponse(activity.getId(), activity.getTitle(), activity.getDescription(),
				activity.getStatus(), activity.getStartDate(), activity.getEndDate(), schedules, sortedStaff);
	}

}
