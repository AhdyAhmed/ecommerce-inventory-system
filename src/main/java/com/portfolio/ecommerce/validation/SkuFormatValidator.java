package com.portfolio.ecommerce.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SkuFormatValidator implements ConstraintValidator<ValidSku, String> {

    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Z0-9]+(-[A-Z0-9]+)*$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // presence is @NotBlank's responsibility, not this validator's
            return true;
        }
        return SKU_PATTERN.matcher(value).matches();
    }

}
