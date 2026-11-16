package br.com.zelo.platform.event;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

	private final EventService service;

	public EventController(EventService service) {
		this.service = service;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping
	public List<EventResponse> list(@RequestParam Instant from, @RequestParam Instant to) {
		return service.list(from, to);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'VOLUNTEER')")
	@GetMapping("/{id}")
	public EventResponse get(@PathVariable Long id) {
		return service.get(id);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PostMapping
	public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
		EventResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/v1/events/" + created.id())).body(created);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PutMapping("/{id}")
	public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
		return service.update(id, request);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PostMapping("/{id}/complete")
	public EventResponse complete(@PathVariable Long id) {
		return service.complete(id);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
	@PostMapping("/{id}/cancel")
	public EventResponse cancel(@PathVariable Long id) {
		return service.cancel(id);
	}

}
