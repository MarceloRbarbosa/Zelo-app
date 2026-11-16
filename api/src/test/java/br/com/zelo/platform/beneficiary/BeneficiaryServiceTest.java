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
import org.springframework.test.util.ReflectionTestUtils;

import br.com.zelo.platform.shared.exception.ConflictException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

	private static final String DOCUMENT = "52998224725";

	private static final List<BeneficiaryStatus> VISIBLE = List.of(BeneficiaryStatus.ACTIVE, BeneficiaryStatus.INACTIVE);

	@Mock
	private BeneficiaryRepository repository;

	@Mock
	private EnrollmentLookup enrollmentLookup;

	@Mock
	private BeneficiaryDocumentService documentService;

	@InjectMocks
	private BeneficiaryService service;

	@Test
	void createSavesBeneficiaryAsActive() {
		when(repository.findByDocumentNumber(DOCUMENT)).thenReturn(Optional.empty());
		when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BeneficiaryResponse response = service.create(request(DOCUMENT));

		assertThat(response.fullName()).isEqualTo("Ana Souza");
		assertThat(response.status()).isEqualTo(BeneficiaryStatus.ACTIVE);
	}

	@Test
	void createRejectsDuplicateCpfTellingWhoHasIt() {
		Beneficiary existing = beneficiary(7L);
		existing.deactivate();
		when(repository.findByDocumentNumber(DOCUMENT)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.create(request(DOCUMENT))).isInstanceOfSatisfying(ConflictException.class,
				ex -> assertThat(ex.getProperties()).containsEntry("beneficiaryId", 7L)
					.containsEntry("fullName", "Old Name")
					.containsEntry("status", "INACTIVE"));
		verify(repository, never()).save(any());
	}

	@Test
	void findByIdAlsoReturnsInactiveBeneficiaries() {
		Beneficiary inactive = beneficiary(1L);
		inactive.deactivate();
		when(repository.findByIdAndStatusIn(1L, VISIBLE)).thenReturn(Optional.of(inactive));

		assertThat(service.findById(1L).status()).isEqualTo(BeneficiaryStatus.INACTIVE);
	}

	@Test
	void findByIdThrowsForAnonymizedOrMissingBeneficiary() {
		when(repository.findByIdAndStatusIn(1L, VISIBLE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void updateRejectsCpfOfAnotherBeneficiary() {
		when(repository.findByIdAndStatus(1L, BeneficiaryStatus.ACTIVE)).thenReturn(Optional.of(beneficiary(1L)));
		when(repository.findByDocumentNumber(DOCUMENT)).thenReturn(Optional.of(beneficiary(2L)));

		assertThatThrownBy(() -> service.update(1L, request(DOCUMENT))).isInstanceOf(ConflictException.class);
	}

	@Test
	void updateKeepsOwnCpf() {
		Beneficiary beneficiary = beneficiary(1L);
		when(repository.findByIdAndStatus(1L, BeneficiaryStatus.ACTIVE)).thenReturn(Optional.of(beneficiary));
		when(repository.findByDocumentNumber(DOCUMENT)).thenReturn(Optional.of(beneficiary));

		BeneficiaryResponse response = service.update(1L, request(DOCUMENT));

		assertThat(response.fullName()).isEqualTo("Ana Souza");
		assertThat(beneficiary.getDocumentNumber()).isEqualTo(DOCUMENT);
	}

	@Test
	void deactivateAndReactivate() {
		Beneficiary beneficiary = beneficiary(1L);
		when(repository.findByIdAndStatus(1L, BeneficiaryStatus.ACTIVE)).thenReturn(Optional.of(beneficiary));
		when(repository.findByIdAndStatusIn(1L, VISIBLE)).thenReturn(Optional.of(beneficiary));

		service.deactivate(1L);
		assertThat(beneficiary.getStatus()).isEqualTo(BeneficiaryStatus.INACTIVE);

		service.reactivate(1L);
		assertThat(beneficiary.getStatus()).isEqualTo(BeneficiaryStatus.ACTIVE);
	}

	@Test
	void anonymizeErasesPersonalDataAndDocuments() {
		Beneficiary beneficiary = beneficiary(1L);
		when(repository.findByIdAndStatusIn(1L, VISIBLE)).thenReturn(Optional.of(beneficiary));

		service.anonymize(1L);

		assertThat(beneficiary.getStatus()).isEqualTo(BeneficiaryStatus.ANONYMIZED);
		assertThat(beneficiary.getFullName()).isEqualTo("Dados removidos (LGPD)");
		assertThat(beneficiary.getDocumentNumber()).isNull();
		assertThat(beneficiary.getBirthDate()).isNull();
		assertThat(beneficiary.getPhoneNumber()).isNull();
		assertThat(beneficiary.getAddress()).isNull();
		verify(documentService).removeAll(1L);
	}

	@Test
	void searchByActivityWithoutEnrollmentsSkipsTheQuery() {
		when(enrollmentLookup.activeBeneficiaryIds(5L)).thenReturn(Set.of());

		assertThat(service.search(new BeneficiarySearch(null, null, 5L, Sort.Direction.ASC,
				BeneficiarySearch.Status.ACTIVE))).isEmpty();
		verify(repository, never()).findAll(any(Specification.class), any(Sort.class));
	}

	private static BeneficiaryRequest request(String documentNumber) {
		return new BeneficiaryRequest("Ana Souza", documentNumber, LocalDate.of(2012, 3, 10), "22999990000",
				null);
	}

	private static Beneficiary beneficiary(Long id) {
		Beneficiary beneficiary = new Beneficiary("Old Name", "11144477735", LocalDate.of(2012, 3, 10), "22999990000",
				"Rua A, 10");
		ReflectionTestUtils.setField(beneficiary, "id", id);
		return beneficiary;
	}

}
