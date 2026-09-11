package com.portfolio.ecommerce.service;

import com.portfolio.ecommerce.dto.product.ProductRequestDto;
import com.portfolio.ecommerce.dto.product.ProductResponseDto;

import java.util.List;

public interface ProductService {

    ProductResponseDto create(ProductRequestDto request);

    ProductResponseDto getById(Long id);

    List<ProductResponseDto> getAll();

    ProductResponseDto update(Long id, ProductRequestDto request);

    void delete(Long id);

}
