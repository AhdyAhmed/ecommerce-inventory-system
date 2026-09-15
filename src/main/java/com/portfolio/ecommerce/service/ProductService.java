package com.portfolio.ecommerce.service;

import com.portfolio.ecommerce.dto.product.ProductRequestDto;
import com.portfolio.ecommerce.dto.product.ProductResponseDto;
import com.portfolio.ecommerce.dto.product.ProductSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponseDto create(ProductRequestDto request);

    ProductResponseDto getById(Long id);

    Page<ProductResponseDto> getAll(Pageable pageable);

    /**
     * Dynamic filtering: any combination of the fields on {@code criteria}
     * may be present, and only the ones that are get applied.
     */
    Page<ProductResponseDto> search(ProductSearchCriteria criteria, Pageable pageable);

    List<ProductResponseDto> getLowStock(int threshold);

    ProductResponseDto update(Long id, ProductRequestDto request);

    void delete(Long id);

}
