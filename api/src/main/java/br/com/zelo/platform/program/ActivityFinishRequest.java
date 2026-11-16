package br.com.zelo.platform.program;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ActivityFinishRequest(@NotNull LocalDate endDate) {
}
