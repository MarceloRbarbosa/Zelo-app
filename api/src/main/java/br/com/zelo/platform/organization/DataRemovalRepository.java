package br.com.zelo.platform.organization;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DataRemovalRepository extends JpaRepository<DataRemoval, Long> {

	List<DataRemoval> findAllByOrderByRemovedAtDesc();

}
