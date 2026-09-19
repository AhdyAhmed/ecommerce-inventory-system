package com.portfolio.ecommerce.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Case-insensitive substring match against product name.", example = "laptop")
    private String name;

    @Schema(description = "Exact category ID to filter by.", example = "1")
    private Long categoryId;

    @Schema(description = "Minimum price, inclusive.", example = "50.00")
    private BigDecimal minPrice;

    @Schema(description = "Maximum price, inclusive.", example = "500.00")
    private BigDecimal maxPrice;

    @Schema(description = "true -> only in-stock products, false -> only out-of-stock, omitted -> no filter.", example = "true")
    private Boolean inStock;

}
