package br.com.zelo.platform.identity;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

	// Row lock so concurrent failed logins cannot overwrite each other's attempt count.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from StaffMember s where s.email = :email")
	Optional<StaffMember> findByEmailForUpdate(String email);

	boolean existsByEmail(String email);

	boolean existsByRole(StaffRole role);

	List<StaffMember> findAllByOrderByFullNameAsc();

	long countByStatus(StaffStatus status);

}
