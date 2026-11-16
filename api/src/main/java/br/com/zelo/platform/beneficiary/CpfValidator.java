package br.com.zelo.platform.beneficiary;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || isValid(value);
	}

	static boolean isValid(String cpf) {
		if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
			return false;
		}
		return checkDigit(cpf, 9) == cpf.charAt(9) - '0' && checkDigit(cpf, 10) == cpf.charAt(10) - '0';
	}

	// Weighted sum of the first `length` digits, weights from length + 1 down to 2.
	private static int checkDigit(String cpf, int length) {
		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum += (cpf.charAt(i) - '0') * (length + 1 - i);
		}
		int rest = (sum * 10) % 11;
		return rest == 10 ? 0 : rest;
	}

}
