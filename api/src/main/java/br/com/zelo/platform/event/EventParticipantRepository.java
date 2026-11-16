package br.com.zelo.platform.event;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

	Optional<EventParticipant> findByEventIdAndBeneficiaryId(Long eventId, Long beneficiaryId);

	List<EventParticipant> findAllByEventId(Long eventId);

	boolean existsByEventIdAndBeneficiaryId(Long eventId, Long beneficiaryId);

}
