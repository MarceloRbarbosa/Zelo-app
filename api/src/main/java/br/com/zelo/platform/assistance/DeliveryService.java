package br.com.zelo.platform.assistance;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.identity.StaffMemberService;
import br.com.zelo.platform.shared.exception.NotFoundException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class DeliveryService {

	private final DeliveryRepository repository;

	private final AssistanceItemService itemService;

	private final BeneficiaryService beneficiaryService;

	private final StaffMemberService staffMemberService;

	public DeliveryService(DeliveryRepository repository, AssistanceItemService itemService,
			BeneficiaryService beneficiaryService, StaffMemberService staffMemberService) {
		this.repository = repository;
		this.itemService = itemService;
		this.beneficiaryService = beneficiaryService;
		this.staffMemberService = staffMemberService;
	}

	// Optional filters within the period: item and part of the beneficiary's name (ignoring case and accents).
	@Transactional(readOnly = true)
	public List<DeliveryResponse> listByPeriod(LocalDate from, LocalDate to, Long itemId, String beneficiaryName) {
		ReceiptService.checkPeriod(from, to);
		String term = beneficiaryName == null || beneficiaryName.isBlank() ? null : normalize(beneficiaryName);
		return toResponses(repository.findAllByDeliveredAtBetweenOrderByDeliveredAtDescIdDesc(from, to))
			.stream()
			.filter(delivery -> itemId == null || itemId.equals(delivery.itemId()))
			.filter(delivery -> term == null
					|| delivery.beneficiaryName() != null && normalize(delivery.beneficiaryName()).contains(term))
			.toList();
	}

	// Full delivery history of one beneficiary.
	@Transactional(readOnly = true)
	public List<DeliveryResponse> listByBeneficiary(Long beneficiaryId) {
		return toResponses(repository.findAllByBeneficiaryIdOrderByDeliveredAtDescIdDesc(beneficiaryId));
	}

	// No frequency limit and no stock check: the item balance is shown, never enforced.
	@Transactional
	public DeliveryResponse create(DeliveryRequest request, CurrentStaff requester) {
		AssistanceItem item = itemService.findActive(request.itemId());
		BeneficiaryContact beneficiary = beneficiaryService.findActiveContacts(Set.of(request.beneficiaryId()))
			.stream()
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Beneficiary " + request.beneficiaryId() + " not found"));
		Delivery delivery = repository.save(new Delivery(item.getId(), beneficiary.id(), request.quantity(),
				request.deliveredAt(), request.notes(), requester.id()));
		return toResponse(delivery, item.getName(), beneficiary.fullName(),
				ReceiptService.staffNames(staffMemberService, List.of(requester.id())).get(requester.id()));
	}

	// Deliveries to beneficiaries later inactivated stay in the history, without the name.
	private List<DeliveryResponse> toResponses(List<Delivery> deliveries) {
		Map<Long, String> items = itemService.names(deliveries.stream().map(Delivery::getItemId).toList());
		Map<Long, BeneficiaryContact> beneficiaries = beneficiaryService
			.findActiveContacts(deliveries.stream().map(Delivery::getBeneficiaryId).toList())
			.stream()
			.collect(Collectors.toMap(BeneficiaryContact::id, Function.identity()));
		Map<Long, String> staff = ReceiptService.staffNames(staffMemberService,
				deliveries.stream().map(Delivery::getRecordedBy).distinct().toList());
		return deliveries.stream().map(delivery -> {
			BeneficiaryContact beneficiary = beneficiaries.get(delivery.getBeneficiaryId());
			return toResponse(delivery, items.get(delivery.getItemId()),
					beneficiary == null ? null : beneficiary.fullName(), staff.get(delivery.getRecordedBy()));
		}).toList();
	}

	private static String normalize(String value) {
		return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	private static DeliveryResponse toResponse(Delivery delivery, String itemName, String beneficiaryName,
			String recordedBy) {
		return new DeliveryResponse(delivery.getId(), delivery.getItemId(), itemName, delivery.getBeneficiaryId(),
				beneficiaryName, delivery.getQuantity(), delivery.getDeliveredAt(), delivery.getNotes(), recordedBy);
	}

}
