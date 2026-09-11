package com.portfolio.ecommerce.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Used for both create (POST) and full update (PUT) - PUT semantics mean the
 * whole resource is replaced, so the same shape covers both.
 *
 * No Bean Validation annotations yet on purpose - @Valid / @NotBlank /
 * @Positive etc. land on Day 5 alongside global exception handling. Until
 * then, a malformed request (blank name, negative price, unknown category
 * ID) either slips through or surfaces as a raw 500.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private Long categoryId;

    /**
     * Optional. Omitted or empty means "no tags".
     */
    private Set<Long> tagIds;

}
