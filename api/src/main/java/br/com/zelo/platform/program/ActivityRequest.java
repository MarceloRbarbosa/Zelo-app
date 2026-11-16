package br.com.zelo.platform.program;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActivityRequest(
		@NotBlank @Size(max = 100) String title,
		@Size(max = 255) String description,
		@NotNull LocalDate startDate,
		@NotEmpty List<@Valid @NotNull Schedule> schedules,
		@NotEmpty Set<@NotNull Long> staffIds) {

	public record Schedule(
			@NotNull DayOfWeek dayOfWeek,
			@NotNull LocalTime startTime,
			@NotNull LocalTime endTime) {
	}

}
