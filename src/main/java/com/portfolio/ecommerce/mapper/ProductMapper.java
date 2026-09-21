package com.portfolio.ecommerce.mapper;

import com.portfolio.ecommerce.domain.Category;
import com.portfolio.ecommerce.domain.Product;
import com.portfolio.ecommerce.domain.Tag;
import com.portfolio.ecommerce.dto.product.ProductRequestDto;
import com.portfolio.ecommerce.dto.product.ProductResponseDto;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Hand-written on purpose rather than MapStruct/ModelMapper - the mapping is
 * simple enough that a generated mapper wouldn't save much, and keeping it
 * explicit makes the entity<->DTO boundary easy to read in one place.
 */
@Component
public class ProductMapper {

    public Product toEntity(ProductRequestDto dto, Category category, Set<Tag> tags) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .sku(dto.getSku())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .category(category)
                .tags(tags)
                .build();
    }

    /**
     * Applies request fields onto an already-managed entity, leaving id,
     * createdAt/updatedAt, and version untouched - those aren't the client's
     * to set.
     */
    public void updateEntityFromDto(ProductRequestDto dto, Product product, Category category, Set<Tag> tags) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSku(dto.getSku());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setCategory(category);
        product.setTags(tags);
    }

    public ProductResponseDto toResponseDto(Product product) {
        Set<String> tagNames = product.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .active(product.isActive())
                .tags(tagNames)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

}
