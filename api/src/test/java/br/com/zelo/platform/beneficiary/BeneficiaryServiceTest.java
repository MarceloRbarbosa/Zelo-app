package br.com.zelo.platform.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

	private static final String DOCUMENT = "12345678901";

	@Mock
	private BeneficiaryRepository repository;

	@Mock
	private EnrollmentLookup enrollmentLookup;

	@InjectMocks
	private BeneficiaryService service;

	@Test
	void createSavesBeneficiaryAsActive() {
		when(repository.existsByDocumentNumber(DOCUMENT)).thenReturn(false);
		when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BeneficiaryResponse response = service.create(request(DOCUMENT));

		assertThat(response.fullName()).isEqualTo("Ana Souza");
		assertThat(response.status()).isEqualTo(BeneficiaryStatus.ACTIVE);
	}

	@Test
	void createRejectsDuplicateDocumentNumber() {
		when(repository.existsByDocumentNumber(DOCUMENT)).thenReturn(true);

		assertThatThrownBy(() -> service.create(request(DOCUMENT))).isInstanceOf(ConflictException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void createWithoutDocumentNumberSkipsUniquenessCheck() {
		when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BeneficiaryResponse response = service.create(request(null));

		assertThat(response.documentNumber()).isNull();
		verify(repository, never()).existsByDocumentNumber(any());
	}

	@Test
	void findByIdThrowsWhenBeneficiaryIsNotActive() {
		when(repository.findByIdAndStatus(1L, BeneficiaryStatus.ACTIVE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void updateRejectsDocumentNumberOfAnotherBeneficiary() {
		when(repository.findByIdAndStatus(1L, BeneficiaryStatus.ACTIVE)).thenReturn(Optional.of(beneficiary()));
		when(repository.existsByDocumentNumberAndIdNot(DOCUMENT, 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(1L, request(DOCUMENT))).isInstanceOf(ConflictException.class);
	}

	@Test
	void updateChangesBeneficiaryFields() {
		Beneficiary beneficiary = beneficiary();
		when(repository.findByIdAndStatus(1L, BeneficiaryStatus.ACTIVE)).thenReturn(Optional.of(beneficiary));
		when(repository.existsByDocumentNumberAndIdNot(DOCUMENT, 1L)).thenReturn(false);

		BeneficiaryResponse response = service.update(1L, request(DOCUMENT));

		assertThat(response.fullName()).isEqualTo("Ana Souza");
		assertThat(beneficiary.getDocumentNumber()).isEqualTo(DOCUMENT);
	}

	@Test
	void deactivateMarksBeneficiaryAsInactive() {
		Beneficiary beneficiary = beneficiary();
		when(repository.findByIdAndStatus(1L, BeneficiaryStatus.ACTIVE)).thenReturn(Optional.of(beneficiary));

		service.deactivate(1L);

		assertThat(beneficiary.getStatus()).isEqualTo(BeneficiaryStatus.INACTIVE);
	}

	@Test
	void searchByActivityWithoutEnrollmentsSkipsTheQuery() {
		when(enrollmentLookup.activeBeneficiaryIds(5L)).thenReturn(Set.of());

		assertThat(service.search(new BeneficiarySearch(null, null, 5L, Sort.Direction.ASC))).isEmpty();
		verify(repository, never()).findAll(any(Specification.class), any(Sort.class));
	}

	private static BeneficiaryRequest request(String documentNumber) {
		return new BeneficiaryRequest("Ana Souza", documentNumber, LocalDate.of(2012, 3, 10), "22999990000",
				null);
	}

	private static Beneficiary beneficiary() {
		return new Beneficiary("Old Name", null, LocalDate.of(2012, 3, 10), "22999990000", null);
	}

}
