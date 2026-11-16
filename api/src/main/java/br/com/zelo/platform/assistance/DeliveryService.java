package br.com.zelo.platform.assistance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.NotFoundException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class DeliveryService {

	private final DeliveryRepository repository;

	private final AssistanceItemService itemService;

	private final BeneficiaryService beneficiaryService;

	public DeliveryService(DeliveryRepository repository, AssistanceItemService itemService,
			BeneficiaryService beneficiaryService) {
		this.repository = repository;
		this.itemService = itemService;
		this.beneficiaryService = beneficiaryService;
	}

	@Transactional(readOnly = true)
	public List<DeliveryResponse> listByPeriod(LocalDate from, LocalDate to) {
		ReceiptService.checkPeriod(from, to);
		return toResponses(repository.findAllByDeliveredAtBetweenOrderByDeliveredAtDescIdDesc(from, to));
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
		return toResponse(delivery, item.getName(), beneficiary.fullName());
	}

	// Deliveries to beneficiaries later inactivated stay in the history, without the name.
	private List<DeliveryResponse> toResponses(List<Delivery> deliveries) {
		Map<Long, String> items = itemService.names(deliveries.stream().map(Delivery::getItemId).toList());
		Map<Long, BeneficiaryContact> beneficiaries = beneficiaryService
			.findActiveContacts(deliveries.stream().map(Delivery::getBeneficiaryId).toList())
			.stream()
			.collect(Collectors.toMap(BeneficiaryContact::id, Function.identity()));
		return deliveries.stream().map(delivery -> {
			BeneficiaryContact beneficiary = beneficiaries.get(delivery.getBeneficiaryId());
			return toResponse(delivery, items.get(delivery.getItemId()),
					beneficiary == null ? null : beneficiary.fullName());
		}).toList();
	}

	private static DeliveryResponse toResponse(Delivery delivery, String itemName, String beneficiaryName) {
		return new DeliveryResponse(delivery.getId(), delivery.getItemId(), itemName, delivery.getBeneficiaryId(),
				beneficiaryName, delivery.getQuantity(), delivery.getDeliveredAt(), delivery.getNotes());
	}

}
