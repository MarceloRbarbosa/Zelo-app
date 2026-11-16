package br.com.zelo.platform.beneficiary;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryDocumentRepository extends JpaRepository<BeneficiaryDocument, Long> {

	List<BeneficiaryDocument> findAllByBeneficiaryIdOrderByUploadedAtDesc(Long beneficiaryId);

	Optional<BeneficiaryDocument> findByIdAndBeneficiaryId(Long id, Long beneficiaryId);

}
