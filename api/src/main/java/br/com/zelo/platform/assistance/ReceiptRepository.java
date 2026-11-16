package br.com.zelo.platform.assistance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

	List<Receipt> findAllByReceivedAtBetweenOrderByReceivedAtDescIdDesc(LocalDate from, LocalDate to);

	@Query("select r.itemId as itemId, sum(r.quantity) as quantity from Receipt r group by r.itemId")
	List<ItemQuantity> sumByItem();

}
