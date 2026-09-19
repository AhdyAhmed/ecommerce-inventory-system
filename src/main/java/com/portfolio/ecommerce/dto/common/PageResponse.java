package com.portfolio.ecommerce.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data's {@code Page}/{@code PageImpl} can be returned directly from
 * a controller, but Spring itself warns against it: {@code PageImpl} isn't
 * really meant to be a serialization target (no stable no-arg constructor
 * contract, and its JSON shape has changed across Spring Data versions).
 * Wrapping it in an explicit DTO keeps the API's JSON contract something
 * *this* project controls, not something that happens to fall out of
 * Spring Data's internals.
 */
@Getter
@Builder
@Schema(description = "A single page of results.")
public class PageResponse<T> {

    @Schema(description = "The items on this page.")
    private List<T> content;

    @Schema(description = "Zero-based page index.", example = "0")
    private int pageNumber;

    @Schema(description = "Requested page size.", example = "20")
    private int pageSize;

    @Schema(description = "Total number of items across every page.", example = "42")
    private long totalElements;

    @Schema(description = "Total number of pages.", example = "3")
    private int totalPages;

    @Schema(description = "Whether this is the last page.", example = "false")
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

}
