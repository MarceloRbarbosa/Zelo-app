package br.com.zelo.platform.event;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import br.com.zelo.platform.identity.StaffSummary;

public record EventResponse(
		Long id,
		String title,
		String description,
		Instant eventDate,
		String location,
		EventStatus status,
		List<StaffSummary> staff) {

	static EventResponse from(Event event, List<StaffSummary> staff) {
		return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getEventDate(),
				event.getLocation(), event.getStatus(),
				staff.stream().sorted(Comparator.comparing(StaffSummary::fullName)).toList());
	}

}
