package br.com.zelo.platform.program;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

// leftAt is required when the status is LEFT and ignored when it is ACTIVE.
public record EnrollmentStatusRequest(@NotNull EnrollmentStatus status, LocalDate leftAt) {
}
