package br.com.zelo.platform.program;

import java.util.HashSet;
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
public class ActivityService {

	private final ActivityRepository repository;

	private final StaffMemberService staffMemberService;

	public ActivityService(ActivityRepository repository, StaffMemberService staffMemberService) {
		this.repository = repository;
		this.staffMemberService = staffMemberService;
	}

	// Volunteers only see the activities they are responsible for.
	@Transactional(readOnly = true)
	public List<ActivityResponse> list(ActivityStatus status, CurrentStaff requester) {
		List<Activity> activities = requester.volunteer()
				? repository.findAllByStatusAndStaffId(status, requester.id())
				: repository.findAllByStatusOrderByTitleAsc(status);
		return toResponses(activities);
	}

	@Transactional(readOnly = true)
	public ActivityResponse get(Long id, CurrentStaff requester) {
		return toResponses(List.of(findAccessible(id, requester))).getFirst();
	}

	@Transactional
	public ActivityResponse create(ActivityRequest request) {
		Activity activity = new Activity(request.title(), request.description(), request.startDate(),
				schedules(request), staffMemberService.requireExisting(request.staffIds()));
		return toResponses(List.of(repository.save(activity))).getFirst();
	}

	@Transactional
	public ActivityResponse update(Long id, ActivityRequest request) {
		Activity activity = findActive(id);
		activity.update(request.title(), request.description(), request.startDate(), schedules(request),
				staffMemberService.requireExisting(request.staffIds()));
		return toResponses(List.of(activity)).getFirst();
	}

	@Transactional
	public ActivityResponse finish(Long id, ActivityFinishRequest request) {
		Activity activity = findActive(id);
		if (request.endDate().isBefore(activity.getStartDate())) {
			throw new InvalidRequestException("End date cannot be before the start date");
		}
		activity.finish(request.endDate());
		return toResponses(List.of(activity)).getFirst();
	}

	Activity findAccessible(Long id, CurrentStaff requester) {
		Activity activity = find(id);
		if (requester.volunteer() && !activity.hasStaff(requester.id())) {
			throw new ForbiddenException("You are not responsible for activity " + id);
		}
		return activity;
	}

	// Finished activities are kept as history and no longer change.
	Activity findActive(Long id) {
		Activity activity = find(id);
		if (!activity.isActive()) {
			throw new ConflictException("Activity " + id + " is finished");
		}
		return activity;
	}

	private Activity find(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Activity " + id + " not found"));
	}

	private static List<ActivitySchedule> schedules(ActivityRequest request) {
		Set<String> slots = new HashSet<>();
		return request.schedules().stream().map(schedule -> {
			if (!schedule.endTime().isAfter(schedule.startTime())) {
				throw new InvalidRequestException("Schedule end time must be after its start time");
			}
			if (!slots.add(schedule.dayOfWeek() + " " + schedule.startTime())) {
				throw new InvalidRequestException("Schedule repeated on " + schedule.dayOfWeek() + " at "
						+ schedule.startTime());
			}
			return new ActivitySchedule(schedule.dayOfWeek(), schedule.startTime(), schedule.endTime());
		}).toList();
	}

	private List<ActivityResponse> toResponses(List<Activity> activities) {
		Set<Long> staffIds = activities.stream()
			.flatMap(activity -> activity.getStaffIds().stream())
			.collect(Collectors.toSet());
		Map<Long, StaffSummary> staff = staffMemberService.findSummaries(staffIds)
			.stream()
			.collect(Collectors.toMap(StaffSummary::id, Function.identity()));
		return activities.stream()
			.map(activity -> ActivityResponse.from(activity,
					activity.getStaffIds().stream().map(staff::get).toList()))
			.toList();
	}

}
