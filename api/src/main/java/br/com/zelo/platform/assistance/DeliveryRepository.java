package br.com.zelo.platform.assistance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

	List<Delivery> findAllByDeliveredAtBetweenOrderByDeliveredAtDescIdDesc(LocalDate from, LocalDate to);

	List<Delivery> findAllByBeneficiaryIdOrderByDeliveredAtDescIdDesc(Long beneficiaryId);

	@Query("select d.itemId as itemId, sum(d.quantity) as quantity from Delivery d group by d.itemId")
	List<ItemQuantity> sumByItem();

}
