package br.com.zelo.platform.beneficiary;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

	List<Beneficiary> findAllByStatusOrderByFullNameAsc(BeneficiaryStatus status);

	Optional<Beneficiary> findByIdAndStatus(Long id, BeneficiaryStatus status);

	boolean existsByDocumentNumber(String documentNumber);

	boolean existsByDocumentNumberAndIdNot(String documentNumber, Long id);

}
