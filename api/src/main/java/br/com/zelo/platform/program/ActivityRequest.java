package br.com.zelo.platform.program;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActivityRequest(
		@NotBlank @Size(max = 100) String title,
		@Size(max = 255) String description,
		@Size(max = 50) String recurrencePattern,
		@NotNull LocalDate startDate) {
}
