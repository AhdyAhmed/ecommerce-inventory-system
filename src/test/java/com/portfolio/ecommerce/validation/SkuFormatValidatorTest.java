package com.portfolio.ecommerce.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The validator has no dependency on jakarta.validation's runtime state - it
 * never reads from ConstraintValidatorContext - so this passes {@code null}
 * for it rather than mocking an interface no branch of the code touches.
 *
 * Deliberately doesn't re-test "blank is valid" as a format concern: that's
 * @NotBlank's job (see the class-level Javadoc on ValidSku), so this class
 * only asserts the regex itself.
 */
class SkuFormatValidatorTest {

    private final SkuFormatValidator validator = new SkuFormatValidator();

    @ParameterizedTest(name = "\"{0}\" is a valid SKU")
    @ValueSource(strings = {"ELEC-LAPTOP-001", "SKU1", "A-B-C-D", "12345"})
    void acceptsWellFormedSkus(String sku) {
        assertThat(validator.isValid(sku, null)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {
            "elec-laptop-001",  // lowercase
            "ELEC LAPTOP 001",  // spaces instead of hyphens
            "ELEC--LAPTOP",     // double hyphen
            "-ELEC-LAPTOP",     // leading hyphen
            "ELEC-LAPTOP-",     // trailing hyphen
            "ELEC_LAPTOP",      // underscore instead of hyphen
            "ELEC.LAPTOP"       // punctuation not in the allowed set
    })
    void rejectsMalformedSkus(String sku) {
        assertThat(validator.isValid(sku, null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void treatsBlankAsValidBecauseNotBlankOwnsPresence(String sku) {
        assertThat(validator.isValid(sku, null)).isTrue();
    }

}
