package br.com.zelo.platform.program;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

	private final ActivityRepository repository;

	public ActivityService(ActivityRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<ActivityResponse> listActive() {
		return repository.findAllByStatusOrderByTitleAsc(ActivityStatus.ACTIVE)
			.stream()
			.map(ActivityResponse::from)
			.toList();
	}

	@Transactional
	public ActivityResponse create(ActivityRequest request) {
		Activity activity = new Activity(request.title(), request.description(), request.recurrencePattern(),
				request.startDate());
		return ActivityResponse.from(repository.save(activity));
	}

}
