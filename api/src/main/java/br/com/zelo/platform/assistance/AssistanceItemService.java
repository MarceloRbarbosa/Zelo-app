package br.com.zelo.platform.assistance;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@Service
public class AssistanceItemService {

	private final AssistanceItemRepository repository;

	private final ReceiptRepository receiptRepository;

	private final DeliveryRepository deliveryRepository;

	public AssistanceItemService(AssistanceItemRepository repository, ReceiptRepository receiptRepository,
			DeliveryRepository deliveryRepository) {
		this.repository = repository;
		this.receiptRepository = receiptRepository;
		this.deliveryRepository = deliveryRepository;
	}

	@Transactional(readOnly = true)
	public List<ItemResponse> list() {
		Map<Long, Long> received = totals(receiptRepository.sumByItem());
		Map<Long, Long> delivered = totals(deliveryRepository.sumByItem());
		return repository.findAllByOrderByNameAsc()
			.stream()
			.map(item -> toResponse(item, received.getOrDefault(item.getId(), 0L),
					delivered.getOrDefault(item.getId(), 0L)))
			.toList();
	}

	@Transactional
	public ItemResponse create(ItemRequest request) {
		String name = request.name().trim();
		if (repository.existsByNameIgnoreCase(name)) {
			throw new ConflictException("Item " + name + " already exists");
		}
		AssistanceItem item = new AssistanceItem(name, request.description());
		if (Boolean.FALSE.equals(request.active())) {
			item.update(name, request.description(), false);
		}
		return toResponse(repository.save(item), 0, 0);
	}

	@Transactional
	public ItemResponse update(Long id, ItemRequest request) {
		AssistanceItem item = find(id);
		String name = request.name().trim();
		if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("Item " + name + " already exists");
		}
		item.update(name, request.description(), !Boolean.FALSE.equals(request.active()));
		Map<Long, Long> received = totals(receiptRepository.sumByItem());
		Map<Long, Long> delivered = totals(deliveryRepository.sumByItem());
		return toResponse(item, received.getOrDefault(id, 0L), delivered.getOrDefault(id, 0L));
	}

	AssistanceItem findActive(Long id) {
		AssistanceItem item = find(id);
		if (!item.isActive()) {
			throw new ConflictException("Item " + id + " is inactive");
		}
		return item;
	}

	Map<Long, String> names(Collection<Long> ids) {
		return repository.findAllById(ids)
			.stream()
			.collect(Collectors.toMap(AssistanceItem::getId, AssistanceItem::getName));
	}

	private AssistanceItem find(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Item " + id + " not found"));
	}

	private static Map<Long, Long> totals(List<ItemQuantity> quantities) {
		return quantities.stream().collect(Collectors.toMap(ItemQuantity::getItemId, ItemQuantity::getQuantity));
	}

	private static ItemResponse toResponse(AssistanceItem item, long received, long delivered) {
		return new ItemResponse(item.getId(), item.getName(), item.getDescription(), item.isActive(), received,
				delivered, received - delivered);
	}

}
