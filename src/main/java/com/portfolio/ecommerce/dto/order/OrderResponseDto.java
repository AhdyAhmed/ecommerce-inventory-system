package com.portfolio.ecommerce.dto.order;

import com.portfolio.ecommerce.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "An order as returned by the API.")
public class OrderResponseDto {

    @Schema(example = "99")
    private Long id;

    @Schema(example = "1")
    private Long userId;

    @Schema(example = "Alice Johnson")
    private String userFullName;

    @Schema(description = "PENDING on creation; moves to CONFIRMED or CANCELLED via the /confirm and /cancel endpoints.")
    private OrderStatus status;

    @Schema(description = "Sum of every line item's lineTotal.", example = "2075.00")
    private BigDecimal totalAmount;

    private List<OrderItemResponseDto> items;

    @Schema(example = "2026-09-19T10:15:30Z")
    private Instant createdAt;

    @Schema(example = "2026-09-19T10:15:30Z")
    private Instant updatedAt;

}
