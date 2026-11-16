package br.com.zelo.platform.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.zelo.platform.beneficiary.BeneficiaryService;
import br.com.zelo.platform.identity.StaffMemberService;
import br.com.zelo.platform.identity.StaffSummary;

@ExtendWith(MockitoExtension.class)
class DataRemovalServiceTest {

	@Mock
	private DataRemovalRepository repository;

	@Mock
	private BeneficiaryService beneficiaryService;

	@Mock
	private StaffMemberService staffMemberService;

	@InjectMocks
	private DataRemovalService service;

	@Test
	void anonymizesThenRecordsWhoFulfilledTheRequest() {
		when(repository.save(any(DataRemoval.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(staffMemberService.findSummaries(any())).thenReturn(List.of(new StaffSummary(1L, "Admin")));

		DataRemovalResponse response = service.remove(7L, new DataRemovalRequest("Pedido da mãe por escrito"), 1L);

		InOrder order = inOrder(beneficiaryService, repository);
		order.verify(beneficiaryService).anonymize(7L);
		order.verify(repository).save(any(DataRemoval.class));
		assertThat(response.beneficiaryId()).isEqualTo(7L);
		assertThat(response.removedBy()).isEqualTo("Admin");
		assertThat(response.note()).isEqualTo("Pedido da mãe por escrito");
	}

}
