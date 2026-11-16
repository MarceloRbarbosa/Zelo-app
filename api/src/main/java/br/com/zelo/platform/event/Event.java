package br.com.zelo.platform.event;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@Column(name = "description")
	private String description;

	@Column(name = "event_date", nullable = false)
	private Instant eventDate;

	@Column(name = "location", length = 150)
	private String location;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private EventStatus status = EventStatus.SCHEDULED;

	@ElementCollection
	@CollectionTable(name = "event_staff", joinColumns = @JoinColumn(name = "event_id"))
	@Column(name = "staff_id")
	private Set<Long> staffIds = new HashSet<>();

	protected Event() {
	}

	public Event(String title, String description, Instant eventDate, String location, Collection<Long> staffIds) {
		update(title, description, eventDate, location, staffIds);
	}

	public void update(String title, String description, Instant eventDate, String location,
			Collection<Long> staffIds) {
		this.title = title;
		this.description = description;
		this.eventDate = eventDate;
		this.location = location;
		this.staffIds.clear();
		this.staffIds.addAll(staffIds);
	}

	public void complete() {
		this.status = EventStatus.COMPLETED;
	}

	public void cancel() {
		this.status = EventStatus.CANCELED;
	}

	public boolean hasStaff(Long staffId) {
		return staffIds.contains(staffId);
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public Instant getEventDate() {
		return eventDate;
	}

	public String getLocation() {
		return location;
	}

	public EventStatus getStatus() {
		return status;
	}

	public Set<Long> getStaffIds() {
		return Set.copyOf(staffIds);
	}

}
