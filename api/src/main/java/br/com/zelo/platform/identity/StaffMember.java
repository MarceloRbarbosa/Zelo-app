package br.com.zelo.platform.identity;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_members")
public class StaffMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(name = "email", nullable = false, unique = true, length = 100)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "member_type", nullable = false, length = 20)
	private StaffMemberType memberType;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private StaffRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private StaffStatus status = StaffStatus.ACTIVE;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword;

	protected StaffMember() {
	}

	public StaffMember(String fullName, String email, String passwordHash, StaffMemberType memberType,
			StaffRole role) {
		this.fullName = fullName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.memberType = memberType;
		this.role = role;
	}

	// Members created by an admin get a provisional password and must replace it on first login.
	public static StaffMember withProvisionalPassword(String fullName, String email, String passwordHash,
			StaffMemberType memberType, StaffRole role) {
		StaffMember staff = new StaffMember(fullName, email, passwordHash, memberType, role);
		staff.mustChangePassword = true;
		return staff;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public boolean isActive() {
		return status == StaffStatus.ACTIVE;
	}

	public boolean isLocked(Instant now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	// Locks the account once maxAttempts consecutive failures are reached and starts a new count.
	public void registerFailedLogin(Instant now, int maxAttempts, Duration lockDuration) {
		failedLoginAttempts++;
		if (failedLoginAttempts >= maxAttempts) {
			lockedUntil = now.plus(lockDuration);
			failedLoginAttempts = 0;
		}
	}

	public void changePassword(String newPasswordHash) {
		this.passwordHash = newPasswordHash;
		this.mustChangePassword = false;
	}

	public void rename(String fullName) {
		this.fullName = fullName;
	}

	public void block() {
		this.status = StaffStatus.BLOCKED;
	}

	public void unblock() {
		this.status = StaffStatus.ACTIVE;
		this.failedLoginAttempts = 0;
		this.lockedUntil = null;
	}

	public void registerSuccessfulLogin() {
		failedLoginAttempts = 0;
		lockedUntil = null;
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public StaffMemberType getMemberType() {
		return memberType;
	}

	public StaffRole getRole() {
		return role;
	}

	public StaffStatus getStatus() {
		return status;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public boolean isMustChangePassword() {
		return mustChangePassword;
	}

}
