package br.com.zelo.platform.beneficiary;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// A Brazilian CPF: 11 digits with valid check digits. Null is left to @NotBlank.
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cpf {

	String message() default "must be a valid CPF";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
