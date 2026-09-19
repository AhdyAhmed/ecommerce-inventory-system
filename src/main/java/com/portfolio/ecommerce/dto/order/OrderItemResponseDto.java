package com.portfolio.ecommerce.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A single line item within an order response, with a snapshot of the unit price at order time.")
public class OrderItemResponseDto {

    @Schema(example = "7")
    private Long id;

    @Schema(example = "1")
    private Long productId;

    @Schema(example = "14-inch Laptop")
    private String productName;

    @Schema(example = "2")
    private Integer quantity;

    @Schema(description = "Unit price at the time the order was placed - not the product's current price.", example = "999.99")
    private BigDecimal unitPrice;

    @Schema(description = "unitPrice * quantity.", example = "1999.98")
    private BigDecimal lineTotal;

}
