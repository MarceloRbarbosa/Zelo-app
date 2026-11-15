package br.com.zelo.platform.program;

import java.time.LocalDate;

public record ActivityResponse(
		Long id,
		String title,
		String description,
		String recurrencePattern,
		Long leadStaffId,
		ActivityStatus status,
		LocalDate startDate) {

	static ActivityResponse from(Activity activity) {
		return new ActivityResponse(activity.getId(), activity.getTitle(), activity.getDescription(),
				activity.getRecurrencePattern(), activity.getLeadStaffId(), activity.getStatus(),
				activity.getStartDate());
	}

}
