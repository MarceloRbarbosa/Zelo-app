package br.com.zelo.platform.beneficiary;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@Service
public class BeneficiaryService {

	private final BeneficiaryRepository repository;

	private final EnrollmentLookup enrollmentLookup;

	public BeneficiaryService(BeneficiaryRepository repository, EnrollmentLookup enrollmentLookup) {
		this.repository = repository;
		this.enrollmentLookup = enrollmentLookup;
	}

	// Every filter is optional; by activity means beneficiaries currently enrolled in it.
	@Transactional(readOnly = true)
	public List<BeneficiaryResponse> search(BeneficiarySearch search) {
		Specification<Beneficiary> spec = (root, query, cb) -> cb.equal(root.get("status"), BeneficiaryStatus.ACTIVE);
		if (search.name() != null && !search.name().isBlank()) {
			String pattern = "%" + escapeLike(search.name().trim().toLowerCase(Locale.ROOT)) + "%";
			spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("fullName")), pattern, '\\'));
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

	@Transactional(readOnly = true)
	public BeneficiaryResponse findById(Long id) {
		return BeneficiaryResponse.from(findActive(id));
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
		if (documentNumber != null && repository.existsByDocumentNumber(documentNumber)) {
			throw new ConflictException("Document number already registered");
		}
		Beneficiary beneficiary = new Beneficiary(request.fullName(), documentNumber, request.birthDate(),
				request.phoneNumber(), request.address());
		return BeneficiaryResponse.from(repository.save(beneficiary));
	}

	@Transactional
	public BeneficiaryResponse update(Long id, BeneficiaryRequest request) {
		Beneficiary beneficiary = findActive(id);
		String documentNumber = request.documentNumber();
		if (documentNumber != null && repository.existsByDocumentNumberAndIdNot(documentNumber, id)) {
			throw new ConflictException("Document number already registered");
		}
		beneficiary.update(request.fullName(), documentNumber, request.birthDate(), request.phoneNumber(),
				request.address());
		return BeneficiaryResponse.from(beneficiary);
	}

	@Transactional
	public void deactivate(Long id) {
		findActive(id).deactivate();
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
