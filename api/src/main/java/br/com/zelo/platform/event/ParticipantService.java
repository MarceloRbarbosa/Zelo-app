package br.com.zelo.platform.event;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.NotFoundException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class ParticipantService {

	private final EventParticipantRepository repository;

	private final EventService eventService;

	private final BeneficiaryService beneficiaryService;

	public ParticipantService(EventParticipantRepository repository, EventService eventService,
			BeneficiaryService beneficiaryService) {
		this.repository = repository;
		this.eventService = eventService;
		this.beneficiaryService = beneficiaryService;
	}

	@Transactional(readOnly = true)
	public List<ParticipantResponse> list(Long eventId, CurrentStaff requester) {
		eventService.findForParticipants(eventId, requester);
		List<EventParticipant> participants = repository.findAllByEventId(eventId);
		Map<Long, BeneficiaryContact> contacts = beneficiaryService
			.findActiveContacts(participants.stream().map(EventParticipant::getBeneficiaryId).toList())
			.stream()
			.collect(Collectors.toMap(BeneficiaryContact::id, Function.identity()));
		return participants.stream()
			.filter(participant -> contacts.containsKey(participant.getBeneficiaryId()))
			.map(participant -> toResponse(participant, contacts.get(participant.getBeneficiaryId())))
			.sorted(Comparator.comparing(ParticipantResponse::fullName))
			.toList();
	}

	@Transactional
	public ParticipantResponse register(Long eventId, ParticipantRequest request, CurrentStaff requester) {
		requireOpen(eventService.findForParticipants(eventId, requester));
		Long beneficiaryId = request.beneficiaryId();
		BeneficiaryContact contact = activeContact(beneficiaryId);
		if (repository.existsByEventIdAndBeneficiaryId(eventId, beneficiaryId)) {
			throw new ConflictException("Beneficiary " + beneficiaryId + " is already registered");
		}
		return toResponse(repository.saveAndFlush(new EventParticipant(eventId, beneficiaryId)), contact);
	}

	@Transactional
	public ParticipantResponse markAttendance(Long eventId, Long beneficiaryId, AttendanceMarkRequest request,
			CurrentStaff requester) {
		requireOpen(eventService.findForParticipants(eventId, requester));
		EventParticipant participant = find(eventId, beneficiaryId);
		participant.markAttendance(request.attended());
		return toResponse(participant, activeContact(beneficiaryId));
	}

	@Transactional
	public void unregister(Long eventId, Long beneficiaryId, CurrentStaff requester) {
		requireOpen(eventService.findForParticipants(eventId, requester));
		repository.delete(find(eventId, beneficiaryId));
	}

	private static void requireOpen(Event event) {
		if (event.getStatus() == EventStatus.CANCELED) {
			throw new ConflictException("Event " + event.getId() + " is canceled");
		}
	}

	private EventParticipant find(Long eventId, Long beneficiaryId) {
		return repository.findByEventIdAndBeneficiaryId(eventId, beneficiaryId)
			.orElseThrow(() -> new NotFoundException(
					"Beneficiary " + beneficiaryId + " is not registered in event " + eventId));
	}

	private BeneficiaryContact activeContact(Long beneficiaryId) {
		return beneficiaryService.findActiveContacts(Set.of(beneficiaryId))
			.stream()
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Beneficiary " + beneficiaryId + " not found"));
	}

	private static ParticipantResponse toResponse(EventParticipant participant, BeneficiaryContact contact) {
		return new ParticipantResponse(participant.getBeneficiaryId(), contact.fullName(),
				participant.getRegisteredAt(), participant.isAttended());
	}

}
