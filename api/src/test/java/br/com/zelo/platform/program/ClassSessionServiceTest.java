package br.com.zelo.platform.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.zelo.platform.shared.exception.InvalidRequestException;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTest {

	// Monday
	private static final LocalDate FROM = LocalDate.of(2026, 11, 2);

	private static final LocalDate TO = LocalDate.of(2026, 11, 8);

	@Mock
	private ActivityRepository repository;

	@InjectMocks
	private ClassSessionService service;

	@Test
	void expandsEachScheduleIntoDatedSessionsSortedByDateAndTime() {
		Activity capoeira = activity(1L, "Capoeira", new ActivitySchedule(DayOfWeek.WEDNESDAY, time(14), time(15)),
				new ActivitySchedule(DayOfWeek.MONDAY, time(16), time(17)));
		Activity reforco = activity(2L, "Reforço", new ActivitySchedule(DayOfWeek.MONDAY, time(9), time(10)));
		when(repository.findAllOverlapping(FROM, TO)).thenReturn(List.of(capoeira, reforco));

		List<ClassSession> sessions = service.list(FROM, TO);

		assertThat(sessions).extracting(ClassSession::title, ClassSession::date, ClassSession::startTime)
			.containsExactly(tuple("Reforço", FROM, time(9)),
					tuple("Capoeira", FROM, time(16)),
					tuple("Capoeira", LocalDate.of(2026, 11, 4), time(14)));
	}

	@Test
	void rejectsInvertedPeriod() {
		assertThatThrownBy(() -> service.list(TO, FROM)).isInstanceOf(InvalidRequestException.class);
	}

	private static Activity activity(Long id, String title, ActivitySchedule... schedules) {
		Activity activity = new Activity(title, null, FROM, List.of(schedules), Set.of());
		ReflectionTestUtils.setField(activity, "id", id);
		return activity;
	}

	private static LocalTime time(int hour) {
		return LocalTime.of(hour, 0);
	}

}
