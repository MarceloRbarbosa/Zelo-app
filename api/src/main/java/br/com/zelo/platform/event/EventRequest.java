package br.com.zelo.platform.event;

import java.time.Instant;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventRequest(
		@NotBlank @Size(max = 150) String title,
		String description,
		@NotNull Instant eventDate,
		@Size(max = 150) String location,
		@NotEmpty Set<@NotNull Long> staffIds) {
}
