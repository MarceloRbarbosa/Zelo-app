package br.com.zelo.platform.beneficiary;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@Service
public class BeneficiaryService {

	private final BeneficiaryRepository repository;

	public BeneficiaryService(BeneficiaryRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<BeneficiaryResponse> listActive() {
		return repository.findAllByStatusOrderByFullNameAsc(BeneficiaryStatus.ACTIVE)
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

	private Beneficiary findActive(Long id) {
		return repository.findByIdAndStatus(id, BeneficiaryStatus.ACTIVE)
			.orElseThrow(() -> new NotFoundException("Beneficiary " + id + " not found"));
	}

}
