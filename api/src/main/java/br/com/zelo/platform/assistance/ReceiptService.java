package br.com.zelo.platform.assistance;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@Service
public class ReceiptService {

	static final int MAX_PERIOD_DAYS = 366;

	private final ReceiptRepository repository;

	private final AssistanceItemService itemService;

	public ReceiptService(ReceiptRepository repository, AssistanceItemService itemService) {
		this.repository = repository;
		this.itemService = itemService;
	}

	@Transactional(readOnly = true)
	public List<ReceiptResponse> list(LocalDate from, LocalDate to) {
		checkPeriod(from, to);
		List<Receipt> receipts = repository.findAllByReceivedAtBetweenOrderByReceivedAtDescIdDesc(from, to);
		Map<Long, String> names = itemService.names(receipts.stream().map(Receipt::getItemId).toList());
		return receipts.stream().map(receipt -> toResponse(receipt, names.get(receipt.getItemId()))).toList();
	}

	@Transactional
	public ReceiptResponse create(ReceiptRequest request, CurrentStaff requester) {
		AssistanceItem item = itemService.findActive(request.itemId());
		Receipt receipt = repository.save(new Receipt(item.getId(), request.quantity(), request.receivedAt(),
				request.donor(), request.notes(), requester.id()));
		return toResponse(receipt, item.getName());
	}

	static void checkPeriod(LocalDate from, LocalDate to) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_PERIOD_DAYS) {
			throw new InvalidRequestException("Period must start before it ends and span at most "
					+ MAX_PERIOD_DAYS + " days");
		}
	}

	private static ReceiptResponse toResponse(Receipt receipt, String itemName) {
		return new ReceiptResponse(receipt.getId(), receipt.getItemId(), itemName, receipt.getQuantity(),
				receipt.getReceivedAt(), receipt.getDonor(), receipt.getNotes());
	}

}
