package br.com.zelo.platform.assistance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.zelo.platform.beneficiary.BeneficiaryContact;
import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.shared.exception.NotFoundException;
import br.com.zelo.platform.shared.security.CurrentStaff;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

	private static final CurrentStaff COORDINATOR = new CurrentStaff("3", false);

	private static final LocalDate DATE = LocalDate.of(2026, 11, 14);

	@Mock
	private DeliveryRepository repository;

	@Mock
	private AssistanceItemService itemService;

	@Mock
	private BeneficiaryService beneficiaryService;

	@InjectMocks
	private DeliveryService service;

	@Test
	void createRecordsWhoDeliveredWithoutCheckingStock() {
		when(itemService.findActive(1L)).thenReturn(item());
		when(beneficiaryService.findActiveContacts(any()))
			.thenReturn(List.of(new BeneficiaryContact(10L, "Ana", "22900000001")));
		when(repository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DeliveryResponse response = service.create(new DeliveryRequest(1L, 10L, 2, DATE, null), COORDINATOR);

		assertThat(response.itemName()).isEqualTo("Cesta básica");
		assertThat(response.beneficiaryName()).isEqualTo("Ana");
		assertThat(response.quantity()).isEqualTo(2);
	}

	@Test
	void createRejectsInactiveOrUnknownBeneficiary() {
		when(itemService.findActive(1L)).thenReturn(item());
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of());

		assertThatThrownBy(() -> service.create(new DeliveryRequest(1L, 10L, 1, DATE, null), COORDINATOR))
			.isInstanceOf(NotFoundException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void historyKeepsDeliveriesOfInactiveBeneficiariesWithoutName() {
		when(repository.findAllByBeneficiaryIdOrderByDeliveredAtDescIdDesc(10L))
			.thenReturn(List.of(new Delivery(1L, 10L, 1, DATE, null, 3L)));
		when(itemService.names(any())).thenReturn(Map.of(1L, "Cesta básica"));
		when(beneficiaryService.findActiveContacts(any())).thenReturn(List.of());

		assertThat(service.listByBeneficiary(10L)).singleElement()
			.satisfies(delivery -> assertThat(delivery.beneficiaryName()).isNull());
	}

	private static AssistanceItem item() {
		AssistanceItem item = new AssistanceItem("Cesta básica", null);
		ReflectionTestUtils.setField(item, "id", 1L);
		return item;
	}

}
