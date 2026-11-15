package br.com.zelo.platform.program;

import java.time.LocalDate;

public interface AbsenceStreak {

	Long getActivityId();

	Long getBeneficiaryId();

	Long getConsecutiveAbsences();

	LocalDate getLastAttendanceDate();

}
