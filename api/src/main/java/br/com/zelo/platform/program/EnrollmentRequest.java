package br.com.zelo.platform.program;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(@NotNull Long beneficiaryId) {
}
