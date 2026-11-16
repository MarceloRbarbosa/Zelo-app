package br.com.zelo.platform.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfValidatorTest {

	@Test
	void acceptsCpfWithValidCheckDigits() {
		assertThat(CpfValidator.isValid("52998224725")).isTrue();
		assertThat(CpfValidator.isValid("01234567890")).isTrue();
	}

	@Test
	void rejectsWrongCheckDigitsRepeatedDigitsAndWrongLength() {
		assertThat(CpfValidator.isValid("52998224724")).isFalse();
		assertThat(CpfValidator.isValid("11111111111")).isFalse();
		assertThat(CpfValidator.isValid("5299822472")).isFalse();
		assertThat(CpfValidator.isValid("529.982.247-25")).isFalse();
	}

}
