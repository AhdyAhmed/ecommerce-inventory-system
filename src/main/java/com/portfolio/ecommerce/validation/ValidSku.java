package com.portfolio.ecommerce.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces the project's SKU convention: uppercase letters and digits,
 * grouped with single hyphens (e.g. "ELEC-LAPTOP-001"). Deliberately doesn't
 * flag blank values as invalid format - that's @NotBlank's job, so the two
 * annotations report distinct, non-overlapping error messages.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SkuFormatValidator.class)
public @interface ValidSku {

    String message() default "SKU must contain only uppercase letters, digits, and single hyphens (e.g. ELEC-LAPTOP-001)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
