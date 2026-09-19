package com.portfolio.ecommerce.dto.product;

import com.portfolio.ecommerce.validation.ValidSku;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for creating or fully replacing a product.")
public class ProductRequestDto {

    @Schema(description = "Product display name.", example = "14-inch Laptop", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Schema(description = "Optional free-text description.", example = "Lightweight laptop for everyday use")
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @Schema(description = "Stock keeping unit. Uppercase letters, digits and single internal hyphens only.",
            example = "ELEC-LAPTOP-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "SKU is required")
    @ValidSku
    private String sku;

    @Schema(description = "Unit price. Must be greater than zero.", example = "999.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @Schema(description = "Units currently in stock. Zero is allowed, negative is not.", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Schema(description = "ID of an existing category this product belongs to.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Schema(description = "Optional. IDs of existing tags to attach. Omitted or empty means \"no tags\".",
            example = "[1, 3]")
    private Set<Long> tagIds;

}
