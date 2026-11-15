package br.com.zelo.platform.program;

import java.time.LocalDate;

public record AttendanceBatchResponse(
		Long activityId,
		LocalDate attendanceDate,
		int registered,
		int present,
		int absent) {
}
