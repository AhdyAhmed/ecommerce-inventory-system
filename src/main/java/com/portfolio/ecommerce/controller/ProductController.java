package com.portfolio.ecommerce.controller;

import com.portfolio.ecommerce.dto.common.PageResponse;
import com.portfolio.ecommerce.dto.product.ProductRequestDto;
import com.portfolio.ecommerce.dto.product.ProductResponseDto;
import com.portfolio.ecommerce.dto.product.ProductSearchCriteria;
import com.portfolio.ecommerce.exception.ErrorResponse;
import com.portfolio.ecommerce.service.ProductService;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Deliberately thin - request/response translation and status codes only.
 * All actual logic (category/tag resolution, mapping) lives in the service
 * layer so it stays unit-testable without a running web context.
 *
 * The {@code @ApiResponses} on each method are hand-written, not inferred:
 * springdoc can only see the 2xx return type from the method signature - it
 * has no way to know a given endpoint can also produce a 404 via
 * {@link com.portfolio.ecommerce.exception.ResourceNotFoundException},
 * since that's thrown deep in the service layer, not declared on the
 * controller method. Documenting the error responses here is what makes
 * Swagger UI's "Responses" section for each endpoint actually complete.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog: CRUD, pagination, dynamic filtering, and low-stock queries.")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a product", description = "Creates a new product under an existing category, optionally attaching existing tags.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation failed (bad SKU format, blank name, non-positive price, etc.)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "categoryId (or one of the tagIds) does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductRequestDto request) {
        ProductResponseDto created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "No product with that ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDto> getById(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * page/size/sort are all query params handled automatically by Spring
     * Data's Pageable resolver, e.g. GET /api/products?page=0&size=10&sort=price,desc
     */
    @GetMapping
    @Operation(summary = "List all products", description = "Paginated, sortable listing of every product. Use /search instead for filtering.")
    public ResponseEntity<PageResponse<ProductResponseDto>> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(productService.getAll(pageable)));
    }

    /**
     * Dynamic, combinable filtering - any subset of name/categoryId/minPrice/
     * maxPrice/inStock may be supplied, e.g.
     * GET /api/products/search?categoryId=1&minPrice=20&maxPrice=200&inStock=true
     * Kept as a distinct endpoint from the plain {@code GET /api/products}
     * above rather than overloading it, so "list everything" and "filter"
     * stay two clearly separate, independently cacheable concerns.
     */
    @GetMapping("/search")
    @Operation(summary = "Search products with combinable filters",
            description = "Any subset of name/categoryId/minPrice/maxPrice/inStock may be supplied, in any combination. " +
                    "Omitted filters simply aren't applied - calling this with no params at all behaves like GET /api/products.")
    public ResponseEntity<PageResponse<ProductResponseDto>> search(
            @ParameterObject ProductSearchCriteria criteria,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(productService.search(criteria, pageable)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List products at or below a stock threshold", description = "Ordered ascending by stock quantity.")
    public ResponseEntity<List<ProductResponseDto>> getLowStock(
            @Parameter(description = "Products with stockQuantity <= this value are returned.", example = "10")
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(productService.getLowStock(threshold));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a product", description = "Full update (PUT semantics) - every field in the request body replaces the existing product's fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No product with that ID, or categoryId/tagIds don't exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDto> update(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "No product with that ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deliberately a dedicated action endpoint rather than an `active` field
     * on the create/update DTO (Day 12 edge case: ordering a discontinued
     * product). Folding it into the full-replace PUT body would make every
     * client sending a PUT responsible for remembering and re-sending the
     * current active status just to avoid accidentally flipping it - the
     * same "action endpoint" shape Orders already uses for /confirm and
     * /cancel.
     */
    @PostMapping("/{id}/discontinue")
    @Operation(summary = "Discontinue a product", description = "Marks the product unavailable for new orders. Existing orders that already reference it are unaffected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product discontinued"),
            @ApiResponse(responseCode = "404", description = "No product with that ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDto> discontinue(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(productService.discontinue(id));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate a discontinued product", description = "Reverses /discontinue.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product reactivated"),
            @ApiResponse(responseCode = "404", description = "No product with that ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDto> reactivate(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(productService.reactivate(id));
    }

}
