package br.com.zelo.platform.beneficiary;

import org.springframework.data.domain.Sort;

public record BeneficiarySearch(String name, String documentNumber, Long activityId, Sort.Direction order) {
}
