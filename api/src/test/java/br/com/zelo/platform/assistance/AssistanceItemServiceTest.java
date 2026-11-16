package br.com.zelo.platform.assistance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.zelo.platform.shared.exception.ConflictException;

@ExtendWith(MockitoExtension.class)
class AssistanceItemServiceTest {

	@Mock
	private AssistanceItemRepository repository;

	@Mock
	private ReceiptRepository receiptRepository;

	@Mock
	private DeliveryRepository deliveryRepository;

	@InjectMocks
	private AssistanceItemService service;

	@Test
	void listShowsBalanceAsReceivedMinusDelivered() {
		when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(item(1L, true)));
		when(receiptRepository.sumByItem()).thenReturn(List.of(quantity(1L, 30)));
		when(deliveryRepository.sumByItem()).thenReturn(List.of(quantity(1L, 12)));

		assertThat(service.list()).containsExactly(new ItemResponse(1L, "Cesta básica", null, true, 30, 12, 18));
	}

	@Test
	void createRejectsNameAlreadyUsedIgnoringCase() {
		when(repository.existsByNameIgnoreCase("cesta básica")).thenReturn(true);

		assertThatThrownBy(() -> service.create(new ItemRequest(" cesta básica ", null, null)))
			.isInstanceOf(ConflictException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void inactiveItemTakesNoNewMovements() {
		when(repository.findById(1L)).thenReturn(Optional.of(item(1L, false)));

		assertThatThrownBy(() -> service.findActive(1L)).isInstanceOf(ConflictException.class);
	}

	private static AssistanceItem item(Long id, boolean active) {
		AssistanceItem item = new AssistanceItem("Cesta básica", null);
		item.update("Cesta básica", null, active);
		ReflectionTestUtils.setField(item, "id", id);
		return item;
	}

	private static ItemQuantity quantity(Long itemId, long quantity) {
		return new ItemQuantity() {

			@Override
			public Long getItemId() {
				return itemId;
			}

			@Override
			public Long getQuantity() {
				return quantity;
			}

		};
	}

}
