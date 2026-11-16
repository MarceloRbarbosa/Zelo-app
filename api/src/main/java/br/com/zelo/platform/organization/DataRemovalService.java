package br.com.zelo.platform.organization;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.identity.StaffMemberService;
import br.com.zelo.platform.identity.StaffSummary;

@Service
public class DataRemovalService {

	private final DataRemovalRepository repository;

	private final BeneficiaryService beneficiaryService;

	private final StaffMemberService staffMemberService;

	public DataRemovalService(DataRemovalRepository repository, BeneficiaryService beneficiaryService,
			StaffMemberService staffMemberService) {
		this.repository = repository;
		this.beneficiaryService = beneficiaryService;
		this.staffMemberService = staffMemberService;
	}

	// Anonymizes the beneficiary (documents included) and keeps a record that the request was fulfilled.
	@Transactional
	public DataRemovalResponse remove(Long beneficiaryId, DataRemovalRequest request, Long staffId) {
		beneficiaryService.anonymize(beneficiaryId);
		DataRemoval removal = repository.save(new DataRemoval(beneficiaryId, staffId, request.note()));
		return toResponses(List.of(removal)).getFirst();
	}

	@Transactional(readOnly = true)
	public List<DataRemovalResponse> list() {
		return toResponses(repository.findAllByOrderByRemovedAtDesc());
	}

	private List<DataRemovalResponse> toResponses(List<DataRemoval> removals) {
		Map<Long, String> names = staffMemberService
			.findSummaries(removals.stream().map(DataRemoval::getRemovedBy).distinct().toList())
			.stream()
			.collect(Collectors.toMap(StaffSummary::id, StaffSummary::fullName));
		return removals.stream()
			.map(removal -> new DataRemovalResponse(removal.getId(), removal.getBeneficiaryId(),
					removal.getRemovedAt(), names.get(removal.getRemovedBy()), removal.getNote()))
			.toList();
	}

}
