package br.com.zelo.platform.beneficiary;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@Service
public class BeneficiaryService {

	private static final List<BeneficiaryStatus> VISIBLE = List.of(BeneficiaryStatus.ACTIVE, BeneficiaryStatus.INACTIVE);

	private final BeneficiaryRepository repository;

	private final EnrollmentLookup enrollmentLookup;

	private final BeneficiaryDocumentService documentService;

	public BeneficiaryService(BeneficiaryRepository repository, EnrollmentLookup enrollmentLookup,
			BeneficiaryDocumentService documentService) {
		this.repository = repository;
		this.enrollmentLookup = enrollmentLookup;
		this.documentService = documentService;
	}

	// Every filter is optional; by activity means beneficiaries currently enrolled in it.
	@Transactional(readOnly = true)
	public List<BeneficiaryResponse> search(BeneficiarySearch search) {
		List<BeneficiaryStatus> statuses = switch (search.status()) {
			case ACTIVE -> List.of(BeneficiaryStatus.ACTIVE);
			case INACTIVE -> List.of(BeneficiaryStatus.INACTIVE);
			case ALL -> VISIBLE;
		};
		Specification<Beneficiary> spec = (root, query, cb) -> root.get("status").in(statuses);
		if (search.name() != null && !search.name().isBlank()) {
			String pattern = "%" + escapeLike(stripAccents(search.name().trim().toLowerCase(Locale.ROOT))) + "%";
			spec = spec.and((root, query, cb) -> cb.like(
					cb.function("unaccent", String.class, cb.lower(root.get("fullName"))), pattern, '\\'));
		}
		if (search.documentNumber() != null && !search.documentNumber().isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("documentNumber"), search.documentNumber().trim()));
		}
		if (search.activityId() != null) {
			Set<Long> enrolled = enrollmentLookup.activeBeneficiaryIds(search.activityId());
			if (enrolled.isEmpty()) {
				return List.of();
			}
			spec = spec.and((root, query, cb) -> root.get("id").in(enrolled));
		}
		return repository.findAll(spec, Sort.by(search.order(), "fullName"))
			.stream()
			.map(BeneficiaryResponse::from)
			.toList();
	}

	// Inactive beneficiaries can still be opened, to be reactivated.
	@Transactional(readOnly = true)
	public BeneficiaryResponse findById(Long id) {
		return BeneficiaryResponse.from(findVisible(id));
	}

	@Transactional(readOnly = true)
	public Set<Long> findActiveIds(Collection<Long> ids) {
		return Set.copyOf(repository.findIdsByIdInAndStatus(ids, BeneficiaryStatus.ACTIVE));
	}

	@Transactional(readOnly = true)
	public List<BeneficiaryContact> findActiveContacts(Collection<Long> ids) {
		return repository.findAllByIdInAndStatus(ids, BeneficiaryStatus.ACTIVE)
			.stream()
			.map(beneficiary -> new BeneficiaryContact(beneficiary.getId(), beneficiary.getFullName(),
					beneficiary.getPhoneNumber()))
			.toList();
	}

	@Transactional
	public BeneficiaryResponse create(BeneficiaryRequest request) {
		String documentNumber = request.documentNumber();
		repository.findByDocumentNumber(documentNumber).ifPresent(BeneficiaryService::rejectDuplicate);
		Beneficiary beneficiary = new Beneficiary(request.fullName(), documentNumber, request.birthDate(),
				request.phoneNumber(), request.address());
		return BeneficiaryResponse.from(repository.save(beneficiary));
	}

	@Transactional
	public BeneficiaryResponse update(Long id, BeneficiaryRequest request) {
		Beneficiary beneficiary = findActive(id);
		String documentNumber = request.documentNumber();
		repository.findByDocumentNumber(documentNumber)
			.filter(existing -> !existing.getId().equals(id))
			.ifPresent(BeneficiaryService::rejectDuplicate);
		beneficiary.update(request.fullName(), documentNumber, request.birthDate(), request.phoneNumber(),
				request.address());
		return BeneficiaryResponse.from(beneficiary);
	}

	@Transactional
	public void deactivate(Long id) {
		findActive(id).deactivate();
	}

	@Transactional
	public BeneficiaryResponse reactivate(Long id) {
		Beneficiary beneficiary = findVisible(id);
		beneficiary.reactivate();
		return BeneficiaryResponse.from(beneficiary);
	}

	// Only on the beneficiary's (or guardian's) request; see the organization module.
	@Transactional
	public void anonymize(Long id) {
		findVisible(id).anonymize();
		documentService.removeAll(id);
	}

	// One CPF, one record: the client gets who already has it, so it can offer to reactivate.
	private static void rejectDuplicate(Beneficiary existing) {
		throw new ConflictException("Document number already registered", Map.of("beneficiaryId", existing.getId(),
				"fullName", existing.getFullName(), "status", existing.getStatus().name()));
	}

	private Beneficiary findVisible(Long id) {
		return repository.findByIdAndStatusIn(id, VISIBLE)
			.orElseThrow(() -> new NotFoundException("Beneficiary " + id + " not found"));
	}

	// The column goes through the database unaccent; the typed text is stripped the same way here.
	private static String stripAccents(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	// The name is matched literally: % and _ typed by the user are not wildcards.
	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private Beneficiary findActive(Long id) {
		return repository.findByIdAndStatus(id, BeneficiaryStatus.ACTIVE)
			.orElseThrow(() -> new NotFoundException("Beneficiary " + id + " not found"));
	}

}
