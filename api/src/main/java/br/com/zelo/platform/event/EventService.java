package br.com.zelo.platform.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.identity.StaffMemberService;
import br.com.zelo.platform.identity.StaffSummary;
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.ForbiddenException;
import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.exception.NotFoundException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class EventService {

	static final Duration MAX_PERIOD = Duration.ofDays(366);

	private final EventRepository repository;

	private final StaffMemberService staffMemberService;

	public EventService(EventRepository repository, StaffMemberService staffMemberService) {
		this.repository = repository;
		this.staffMemberService = staffMemberService;
	}

	// Every member sees every event, as in the calendar; changes are limited by role.
	@Transactional(readOnly = true)
	public List<EventResponse> list(Instant from, Instant to) {
		if (to.isBefore(from) || Duration.between(from, to).compareTo(MAX_PERIOD) > 0) {
			throw new InvalidRequestException("Period must start before it ends and span at most 366 days");
		}
		return toResponses(repository.findAllByEventDateBetweenOrderByEventDateAsc(from, to));
	}

	@Transactional(readOnly = true)
	public EventResponse get(Long id) {
		return toResponses(List.of(find(id))).getFirst();
	}

	@Transactional
	public EventResponse create(EventRequest request) {
		Event event = new Event(request.title(), request.description(), request.eventDate(), request.location(),
				staffMemberService.requireExisting(request.staffIds()));
		return toResponses(List.of(repository.save(event))).getFirst();
	}

	@Transactional
	public EventResponse update(Long id, EventRequest request) {
		Event event = findScheduled(id);
		event.update(request.title(), request.description(), request.eventDate(), request.location(),
				staffMemberService.requireExisting(request.staffIds()));
		return toResponses(List.of(event)).getFirst();
	}

	@Transactional
	public EventResponse complete(Long id) {
		Event event = findScheduled(id);
		event.complete();
		return toResponses(List.of(event)).getFirst();
	}

	@Transactional
	public EventResponse cancel(Long id) {
		Event event = findScheduled(id);
		event.cancel();
		return toResponses(List.of(event)).getFirst();
	}

	// Registration and attendance belong to admins, coordinators and the event's responsible volunteers,
	// and stop once the event is canceled.
	Event findForParticipants(Long id, CurrentStaff requester) {
		Event event = find(id);
		if (requester.volunteer() && !event.hasStaff(requester.id())) {
			throw new ForbiddenException("You are not responsible for event " + id);
		}
		return event;
	}

	private Event findScheduled(Long id) {
		Event event = find(id);
		if (event.getStatus() != EventStatus.SCHEDULED) {
			throw new ConflictException("Event " + id + " is " + event.getStatus().name().toLowerCase());
		}
		return event;
	}

	private Event find(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Event " + id + " not found"));
	}

	private List<EventResponse> toResponses(List<Event> events) {
		Set<Long> staffIds = events.stream()
			.flatMap(event -> event.getStaffIds().stream())
			.collect(Collectors.toSet());
		Map<Long, StaffSummary> staff = staffMemberService.findSummaries(staffIds)
			.stream()
			.collect(Collectors.toMap(StaffSummary::id, Function.identity()));
		return events.stream()
			.map(event -> EventResponse.from(event, event.getStaffIds().stream().map(staff::get).toList()))
			.toList();
	}

}
