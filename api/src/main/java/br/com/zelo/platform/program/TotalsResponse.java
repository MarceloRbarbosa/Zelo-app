package br.com.zelo.platform.program;

// Current totals of the organization; activeStaff is null for volunteers, who do not see the team.
public record TotalsResponse(
		long activeBeneficiaries,
		long activeActivities,
		long activeEnrollments,
		Long activeStaff) {
}
