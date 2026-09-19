package com.portfolio.ecommerce.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A product as returned by the API.")
public class ProductResponseDto {

    @Schema(example = "42")
    private Long id;

    @Schema(example = "14-inch Laptop")
    private String name;

    @Schema(example = "Lightweight laptop for everyday use")
    private String description;

    @Schema(example = "ELEC-LAPTOP-001")
    private String sku;

    @Schema(example = "999.99")
    private BigDecimal price;

    @Schema(example = "25")
    private Integer stockQuantity;

    @Schema(example = "1")
    private Long categoryId;

    @Schema(example = "Electronics")
    private String categoryName;

    @Schema(description = "Names of every tag attached to this product.", example = "[\"bestseller\", \"new-arrival\"]")
    private Set<String> tags;

    @Schema(description = "When this product was first created.", example = "2026-09-01T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "When this product was last updated.", example = "2026-09-15T08:22:11Z")
    private Instant updatedAt;

}
