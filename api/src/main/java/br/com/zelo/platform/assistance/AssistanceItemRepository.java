package br.com.zelo.platform.assistance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistanceItemRepository extends JpaRepository<AssistanceItem, Long> {

	List<AssistanceItem> findAllByOrderByNameAsc();

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

}
