package br.com.zelo.platform.assistance;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.security.CurrentStaff;

// Restricted area: items, receipts and deliveries are handled by admins and coordinators only.
@RestController
@RequestMapping("/api/v1/assistance")
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
public class AssistanceController {

	private final AssistanceItemService itemService;

	private final ReceiptService receiptService;

	private final DeliveryService deliveryService;

	public AssistanceController(AssistanceItemService itemService, ReceiptService receiptService,
			DeliveryService deliveryService) {
		this.itemService = itemService;
		this.receiptService = receiptService;
		this.deliveryService = deliveryService;
	}

	@GetMapping("/items")
	public List<ItemResponse> listItems() {
		return itemService.list();
	}

	@PostMapping("/items")
	@ResponseStatus(HttpStatus.CREATED)
	public ItemResponse createItem(@Valid @RequestBody ItemRequest request) {
		return itemService.create(request);
	}

	@PutMapping("/items/{id}")
	public ItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
		return itemService.update(id, request);
	}

	@GetMapping("/receipts")
	public List<ReceiptResponse> listReceipts(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
		return receiptService.list(from, to);
	}

	@PostMapping("/receipts")
	@ResponseStatus(HttpStatus.CREATED)
	public ReceiptResponse createReceipt(@Valid @RequestBody ReceiptRequest request, Authentication authentication) {
		return receiptService.create(request, CurrentStaff.from(authentication));
	}

	// Either a period (from and to) or a beneficiary's whole history.
	@GetMapping("/deliveries")
	public List<DeliveryResponse> listDeliveries(@RequestParam(required = false) Long beneficiaryId,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long itemId, @RequestParam(required = false) String beneficiaryName) {
		if (beneficiaryId != null) {
			return deliveryService.listByBeneficiary(beneficiaryId);
		}
		if (from == null || to == null) {
			throw new InvalidRequestException(
					"Give beneficiaryId or both from and to");
		}
		return deliveryService.listByPeriod(from, to, itemId, beneficiaryName);
	}

	@PostMapping("/deliveries")
	@ResponseStatus(HttpStatus.CREATED)
	public DeliveryResponse createDelivery(@Valid @RequestBody DeliveryRequest request,
			Authentication authentication) {
		return deliveryService.create(request, CurrentStaff.from(authentication));
	}

}
