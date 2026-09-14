package com.portfolio.ecommerce.dto.common;

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
public class PageResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
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
