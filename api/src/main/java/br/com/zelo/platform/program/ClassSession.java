package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.time.LocalTime;

// One class of an activity on a given date, as shown in the calendar.
public record ClassSession(
		Long activityId,
		String title,
		ActivityStatus status,
		LocalDate date,
		LocalTime startTime,
		LocalTime endTime) {
}
