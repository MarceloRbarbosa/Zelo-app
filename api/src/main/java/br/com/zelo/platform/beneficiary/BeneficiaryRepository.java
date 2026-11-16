package br.com.zelo.platform.beneficiary;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BeneficiaryRepository
		extends JpaRepository<Beneficiary, Long>, JpaSpecificationExecutor<Beneficiary> {

	Optional<Beneficiary> findByIdAndStatus(Long id, BeneficiaryStatus status);

	Optional<Beneficiary> findByIdAndStatusIn(Long id, Collection<BeneficiaryStatus> statuses);

	Optional<Beneficiary> findByDocumentNumber(String documentNumber);

	List<Beneficiary> findAllByIdInAndStatus(Collection<Long> ids, BeneficiaryStatus status);

	@Query("select b.id from Beneficiary b where b.id in :ids and b.status = :status")
	List<Long> findIdsByIdInAndStatus(Collection<Long> ids, BeneficiaryStatus status);

	long countByStatus(BeneficiaryStatus status);

}
