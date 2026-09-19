package com.portfolio.ecommerce.controller;

import com.portfolio.ecommerce.dto.common.PageResponse;
import com.portfolio.ecommerce.dto.order.OrderRequestDto;
import com.portfolio.ecommerce.dto.order.OrderResponseDto;
import com.portfolio.ecommerce.exception.ErrorResponse;
import com.portfolio.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Deliberately thin, same rationale as {@link ProductController} - see its
 * class-level Javadoc for why the {@code @ApiResponses} blocks below are
 * hand-written rather than inferred by springdoc.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement with stock control, state transitions (confirm/cancel), and order queries.")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an order",
            description = "Creates an order for an existing user from a list of product IDs and quantities. " +
                    "Computes the total from each product's current price, decrements stock per item, and starts the order as PENDING. " +
                    "Fails as a single unit - nothing is persisted and no stock is touched - if any product is unknown or any " +
                    "requested quantity exceeds current stock, even if earlier items in the same request would have succeeded on their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(responseCode = "400", description = "Validation failed (missing userId, empty items list, non-positive quantity, etc.)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "userId or a productId does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Requested quantity exceeds a product's current stock",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponseDto> create(@Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto created = orderService.create(request);
        return ResponseEntity.created(URI.create("/api/orders/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "No order with that ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponseDto> getById(
            @Parameter(description = "Order ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    /**
     * page/size/sort are all query params, e.g.
     * GET /api/orders?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    @Operation(summary = "List all orders", description = "Paginated, sortable listing of every order.")
    public ResponseEntity<PageResponse<OrderResponseDto>> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(orderService.getAll(pageable)));
    }

    @GetMapping("/by-user/{email}")
    @Operation(summary = "List a user's orders", description = "Paginated, newest first by default.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders returned (an empty page if the user has none, or doesn't exist)")
    })
    public ResponseEntity<PageResponse<OrderResponseDto>> getByUserEmail(
            @Parameter(description = "User's email", example = "alice@example.com") @PathVariable String email,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(orderService.getByUserEmail(email, pageable)));
    }

    @GetMapping("/search")
    @Operation(summary = "Find a user's orders within a date range", description = "Both from and to are inclusive, ISO-8601 instants.")
    public ResponseEntity<List<OrderResponseDto>> search(
            @Parameter(description = "User's email", example = "alice@example.com") @RequestParam String email,
            @Parameter(description = "Range start (inclusive), ISO-8601.", example = "2026-01-01T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Range end (inclusive), ISO-8601.", example = "2026-12-31T23:59:59Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(orderService.getByUserEmailAndDateRange(email, from, to));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm an order", description = "PENDING -> CONFIRMED. Rejected if the order isn't currently PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order confirmed"),
            @ApiResponse(responseCode = "404", description = "No order with that ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Order is not currently PENDING",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponseDto> confirm(
            @Parameter(description = "Order ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Moves the order to CANCELLED and restores stock for every line item. Rejected if already CANCELLED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled, stock restored"),
            @ApiResponse(responseCode = "404", description = "No order with that ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Order is already CANCELLED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponseDto> cancel(
            @Parameter(description = "Order ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }

}
