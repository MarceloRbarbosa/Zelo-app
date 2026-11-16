package br.com.zelo.platform.program;

// Attendance records of one activity in a period: how many were taken and how many were present.
public interface RollCallTotals {

	Long getActivityId();

	Long getTotal();

	Long getPresent();

}
