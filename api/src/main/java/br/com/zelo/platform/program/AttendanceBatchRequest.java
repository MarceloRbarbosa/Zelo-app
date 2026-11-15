package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record AttendanceBatchRequest(
		@NotNull Long activityId,
		@NotNull @PastOrPresent LocalDate attendanceDate,
		@NotEmpty List<@Valid @NotNull Entry> entries) {

	public record Entry(
			@NotNull Long beneficiaryId,
			@NotNull Boolean present,
			@Size(max = 255) String notes) {
	}

}
