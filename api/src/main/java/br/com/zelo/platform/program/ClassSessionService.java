package br.com.zelo.platform.program;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.InvalidRequestException;

@Service
public class ClassSessionService {

	private final ActivityRepository repository;

	public ClassSessionService(ActivityRepository repository) {
		this.repository = repository;
	}

	// Every member sees every class in the calendar; finished activities show the classes they had.
	@Transactional(readOnly = true)
	public List<ClassSession> list(LocalDate from, LocalDate to) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= DiaryService.MAX_PERIOD_DAYS) {
			throw new InvalidRequestException("Period must start before it ends and span at most "
					+ DiaryService.MAX_PERIOD_DAYS + " days");
		}
		return repository.findAllOverlapping(from, to)
			.stream()
			.flatMap(activity -> activity.scheduledDates(from, to)
				.stream()
				.flatMap(date -> activity.getSchedules()
					.stream()
					.filter(schedule -> schedule.dayOfWeek() == date.getDayOfWeek())
					.map(schedule -> new ClassSession(activity.getId(), activity.getTitle(), activity.getStatus(),
							date, schedule.startTime(), schedule.endTime()))))
			.sorted(Comparator.comparing(ClassSession::date)
				.thenComparing(ClassSession::startTime)
				.thenComparing(ClassSession::title))
			.toList();
	}

}
