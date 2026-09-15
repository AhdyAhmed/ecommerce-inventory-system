package com.portfolio.ecommerce.service.impl;

import com.portfolio.ecommerce.domain.Category;
import com.portfolio.ecommerce.domain.Product;
import com.portfolio.ecommerce.domain.Tag;
import com.portfolio.ecommerce.dto.product.ProductRequestDto;
import com.portfolio.ecommerce.dto.product.ProductResponseDto;
import com.portfolio.ecommerce.dto.product.ProductSearchCriteria;
import com.portfolio.ecommerce.exception.ResourceNotFoundException;
import com.portfolio.ecommerce.mapper.ProductMapper;
import com.portfolio.ecommerce.repository.CategoryRepository;
import com.portfolio.ecommerce.repository.ProductRepository;
import com.portfolio.ecommerce.repository.TagRepository;
import com.portfolio.ecommerce.service.ProductService;
import com.portfolio.ecommerce.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponseDto create(ProductRequestDto request) {
        Category category = resolveCategory(request.getCategoryId());
        Set<Tag> tags = resolveTags(request.getTagIds());

        Product product = productMapper.toEntity(request, category, tags);
        Product saved = productRepository.save(product);

        return productMapper.toResponseDto(saved);
    }

    @Override
    public ProductResponseDto getById(Long id) {
        Product product = findProductOrThrow(id);
        return productMapper.toResponseDto(product);
    }

    @Override
    public Page<ProductResponseDto> getAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toResponseDto);
    }

    @Override
    public Page<ProductResponseDto> search(ProductSearchCriteria criteria, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.fromCriteria(criteria);
        return productRepository.findAll(spec, pageable)
                .map(productMapper::toResponseDto);
    }

    @Override
    public List<ProductResponseDto> getLowStock(int threshold) {
        return productRepository.findLowStock(threshold).stream()
                .map(productMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponseDto update(Long id, ProductRequestDto request) {
        Product product = findProductOrThrow(id);
        Category category = resolveCategory(request.getCategoryId());
        Set<Tag> tags = resolveTags(request.getTagIds());

        productMapper.updateEntityFromDto(request, product, category, tags);
        Product saved = productRepository.save(product);

        return productMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private Set<Tag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Tag> foundTags = tagRepository.findAllById(tagIds);
        if (foundTags.size() != tagIds.size()) {
            throw new ResourceNotFoundException("One or more tags not found for ids: " + tagIds);
        }

        return new HashSet<>(foundTags);
    }

}
