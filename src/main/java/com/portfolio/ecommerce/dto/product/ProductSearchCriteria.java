package com.portfolio.ecommerce.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Bound from request query params on {@code GET /api/products/search}
 * (e.g. {@code ?categoryId=2&minPrice=10&maxPrice=100&inStock=true}).
 * Every field is optional and independent - any subset can be supplied, in
 * any combination, and {@link com.portfolio.ecommerce.specification.ProductSpecification}
 * only applies the filters that are actually present.
 *
 * Deliberately a plain field-only POJO rather than a record: Spring MVC's
 * {@code @ModelAttribute} binding populates query-param objects via setters,
 * not a canonical constructor.
 */
@Getter
@Setter
public class ProductSearchCriteria {

    /**
     * Case-insensitive substring match against product name.
     */
    private String name;

    private Long categoryId;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    /**
     * true -> stockQuantity > 0, false -> stockQuantity == 0, null -> no filter.
     */
    private Boolean inStock;

}
