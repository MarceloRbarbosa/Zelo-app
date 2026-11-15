package br.com.zelo.platform.beneficiary;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

	List<Beneficiary> findAllByStatusOrderByFullNameAsc(BeneficiaryStatus status);

	Optional<Beneficiary> findByIdAndStatus(Long id, BeneficiaryStatus status);

	boolean existsByDocumentNumber(String documentNumber);

	boolean existsByDocumentNumberAndIdNot(String documentNumber, Long id);

	@Query("select b.id from Beneficiary b where b.id in :ids and b.status = :status")
	List<Long> findIdsByIdInAndStatus(Collection<Long> ids, BeneficiaryStatus status);

}
