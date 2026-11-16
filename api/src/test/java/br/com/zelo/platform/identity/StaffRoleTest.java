package br.com.zelo.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StaffRoleTest {

	@Test
	void adminManagesEveryRoleIncludingAdmins() {
		assertThat(StaffRole.ROLE_ADMIN.canManage(StaffRole.ROLE_ADMIN)).isTrue();
		assertThat(StaffRole.ROLE_ADMIN.canManage(StaffRole.ROLE_COORDINATOR)).isTrue();
		assertThat(StaffRole.ROLE_ADMIN.canManage(StaffRole.ROLE_VOLUNTEER)).isTrue();
	}

	@Test
	void othersManageOnlyRolesBelowThem() {
		assertThat(StaffRole.ROLE_COORDINATOR.canManage(StaffRole.ROLE_VOLUNTEER)).isTrue();
		assertThat(StaffRole.ROLE_COORDINATOR.canManage(StaffRole.ROLE_COORDINATOR)).isFalse();
		assertThat(StaffRole.ROLE_COORDINATOR.canManage(StaffRole.ROLE_ADMIN)).isFalse();
		assertThat(StaffRole.ROLE_VOLUNTEER.canManage(StaffRole.ROLE_VOLUNTEER)).isFalse();
	}

}
