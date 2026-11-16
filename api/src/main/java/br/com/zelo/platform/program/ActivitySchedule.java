package br.com.zelo.platform.program;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record ActivitySchedule(
		@Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 9) DayOfWeek dayOfWeek,
		@Column(name = "start_time", nullable = false) LocalTime startTime,
		@Column(name = "end_time", nullable = false) LocalTime endTime) {
}
