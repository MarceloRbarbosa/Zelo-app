package br.com.zelo.platform.event;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

	List<Event> findAllByEventDateBetweenOrderByEventDateAsc(Instant from, Instant to);

}
