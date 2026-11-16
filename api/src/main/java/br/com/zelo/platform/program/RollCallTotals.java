package br.com.zelo.platform.program;

import java.time.LocalDate;

// Attendance records of one activity on one date: a roll call that was done.
public interface RollCallTotals {

	Long getActivityId();

	LocalDate getAttendanceDate();

	Long getTotal();

	Long getPresent();

}
