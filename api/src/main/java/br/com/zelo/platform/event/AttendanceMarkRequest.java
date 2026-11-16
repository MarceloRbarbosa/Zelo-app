package br.com.zelo.platform.event;

import jakarta.validation.constraints.NotNull;

public record AttendanceMarkRequest(@NotNull Boolean attended) {
}
