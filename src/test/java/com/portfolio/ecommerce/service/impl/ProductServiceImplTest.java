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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pure Mockito unit tests - no Spring context, no database. Repositories and
 * the mapper are mocked so each test exercises only ProductServiceImpl's own
 * branching (which category/tag lookups happen, which exception gets thrown,
 * what gets passed to save()), not JPA or Hibernate behavior. That's what
 * integration tests with Testcontainers (Day 10) are for.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ProductMapper productMapper;

    private ProductServiceImpl productService;

    private Category category;
    private Product product;
    private ProductRequestDto requestDto;
    private ProductResponseDto responseDto;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, categoryRepository, tagRepository, productMapper);

        category = Category.builder().id(1L).name("Electronics").build();

        product = Product.builder()
                .id(10L)
                .name("Laptop")
                .sku("ELEC-LAPTOP-001")
                .price(new BigDecimal("999.99"))
                .stockQuantity(15)
                .category(category)
                .tags(new HashSet<>())
                .build();

        requestDto = ProductRequestDto.builder()
                .name("Laptop")
                .sku("ELEC-LAPTOP-001")
                .price(new BigDecimal("999.99"))
                .stockQuantity(15)
                .categoryId(1L)
                .tagIds(Collections.emptySet())
                .build();

        responseDto = ProductResponseDto.builder().id(10L).name("Laptop").build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happy path: resolves category, saves, returns mapped response")
        void savesProductWhenCategoryAndTagsExist() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productMapper.toEntity(eq(requestDto), eq(category), anySet())).willReturn(product);
            given(productRepository.save(product)).willReturn(product);
            given(productMapper.toResponseDto(product)).willReturn(responseDto);

            ProductResponseDto result = productService.create(requestDto);

            assertThat(result).isEqualTo(responseDto);
            verify(productRepository).save(product);
            verifyNoInteractions(tagRepository); // empty tagIds should short-circuit before hitting the repo
        }

        @Test
        @DisplayName("happy path: resolves every requested tag and passes them through")
        void savesProductWithResolvedTags() {
            requestDto.setTagIds(Set.of(1L, 2L));
            Tag saleTag = Tag.builder().id(1L).name("sale").build();
            Tag newTag = Tag.builder().id(2L).name("new").build();

            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(tagRepository.findAllById(Set.of(1L, 2L))).willReturn(List.of(saleTag, newTag));
            given(productMapper.toEntity(eq(requestDto), eq(category), eq(Set.of(saleTag, newTag)))).willReturn(product);
            given(productRepository.save(product)).willReturn(product);
            given(productMapper.toResponseDto(product)).willReturn(responseDto);

            ProductResponseDto result = productService.create(requestDto);

            assertThat(result).isEqualTo(responseDto);
        }

        @Test
        @DisplayName("not-found: unknown category ID throws before touching the mapper")
        void throwsWhenCategoryNotFound() {
            given(categoryRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.create(requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 1");

            verifyNoInteractions(productMapper);
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("validation edge case: any missing tag ID fails the whole request, not just that tag")
        void throwsWhenAnyTagIdMissing() {
            requestDto.setTagIds(Set.of(1L, 2L));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            // only tag 1 exists; tag 2 doesn't come back from findAllById
            given(tagRepository.findAllById(Set.of(1L, 2L)))
                    .willReturn(List.of(Tag.builder().id(1L).name("sale").build()));

            assertThatThrownBy(() -> productService.create(requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("tags not found");

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("happy path")
        void returnsMappedProductWhenFound() {
            given(productRepository.findById(10L)).willReturn(Optional.of(product));
            given(productMapper.toResponseDto(product)).willReturn(responseDto);

            assertThat(productService.getById(10L)).isEqualTo(responseDto);
        }

        @Test
        @DisplayName("not-found")
        void throwsWhenNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Test
    @DisplayName("getAll delegates paging to the repository and maps each result")
    void getAllDelegatesToRepositoryWithPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product));
        given(productRepository.findAll(pageable)).willReturn(page);
        given(productMapper.toResponseDto(product)).willReturn(responseDto);

        Page<ProductResponseDto> result = productService.getAll(pageable);

        assertThat(result.getContent()).containsExactly(responseDto);
    }

    @Test
    @DisplayName("search builds a Specification from the criteria and delegates to findAll(Specification, Pageable)")
    void searchAppliesSpecificationAndPagination() {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setName("lap");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product));

        given(productRepository.findAll(any(Specification.class), eq(pageable))).willReturn(page);
        given(productMapper.toResponseDto(product)).willReturn(responseDto);

        Page<ProductResponseDto> result = productService.search(criteria, pageable);

        assertThat(result.getContent()).containsExactly(responseDto);
        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getLowStock maps every repository result")
    void getLowStockMapsRepositoryResults() {
        given(productRepository.findLowStock(10)).willReturn(List.of(product));
        given(productMapper.toResponseDto(product)).willReturn(responseDto);

        List<ProductResponseDto> result = productService.getLowStock(10);

        assertThat(result).containsExactly(responseDto);
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("happy path: applies changes onto the existing managed entity")
        void updatesExistingProduct() {
            given(productRepository.findById(10L)).willReturn(Optional.of(product));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productRepository.save(product)).willReturn(product);
            given(productMapper.toResponseDto(product)).willReturn(responseDto);

            ProductResponseDto result = productService.update(10L, requestDto);

            assertThat(result).isEqualTo(responseDto);
            verify(productMapper).updateEntityFromDto(eq(requestDto), eq(product), eq(category), anySet());
        }

        @Test
        @DisplayName("not-found: unknown product ID throws before resolving category/tags")
        void throwsWhenProductNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(999L, requestDto))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(categoryRepository);
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("happy path")
        void deletesWhenExists() {
            given(productRepository.existsById(10L)).willReturn(true);

            productService.delete(10L);

            verify(productRepository).deleteById(10L);
        }

        @Test
        @DisplayName("not-found: never calls deleteById for a nonexistent product")
        void throwsWhenNotFound() {
            given(productRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> productService.delete(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            verify(productRepository, never()).deleteById(any());
        }
    }

}
