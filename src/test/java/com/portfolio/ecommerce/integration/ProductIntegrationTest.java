package com.portfolio.ecommerce.integration;

import com.portfolio.ecommerce.AbstractIntegrationTest;
import com.portfolio.ecommerce.domain.Category;
import com.portfolio.ecommerce.domain.Product;
import com.portfolio.ecommerce.dto.product.ProductRequestDto;
import com.portfolio.ecommerce.repository.CategoryRepository;
import com.portfolio.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack: real HTTP dispatch (MockMvc) -> ProductController ->
 * ProductServiceImpl -> ProductRepository -> real Postgres (Testcontainers).
 * Where {@code ProductServiceImplTest} (Day 9) proved the service's own
 * branching with mocked repositories, this proves the whole chain actually
 * persists and reads back correctly against a real database, and that the
 * error response shape a client actually receives over HTTP matches what
 * {@code GlobalExceptionHandler} is supposed to produce.
 */
@DisplayName("Product API (integration)")
class ProductIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;

    @BeforeEach
    void setUp() {
        electronics = categoryRepository.save(Category.builder().name("Electronics").build());
    }

    @Test
    @DisplayName("full CRUD lifecycle: create -> read -> update -> delete -> 404")
    void fullCrudLifecycle() throws Exception {
        ProductRequestDto createRequest = ProductRequestDto.builder()
                .name("Mechanical Keyboard")
                .sku("ELEC-KEYBOARD-001")
                .price(new BigDecimal("129.00"))
                .stockQuantity(30)
                .categoryId(electronics.getId())
                .build();

        // create -> 201, Location header set, body contains the persisted id
        String createResponseJson = mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.categoryName").value("Electronics"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponseJson).get("id").asLong();

        // confirm it actually landed in Postgres, not just in the HTTP response
        assertThat(productRepository.findById(id)).isPresent();

        // read -> 200, same data
        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("ELEC-KEYBOARD-001"));

        // update -> 200, changed fields reflected
        ProductRequestDto updateRequest = ProductRequestDto.builder()
                .name("Mechanical Keyboard v2")
                .sku("ELEC-KEYBOARD-001")
                .price(new BigDecimal("139.00"))
                .stockQuantity(25)
                .categoryId(electronics.getId())
                .build();

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard v2"))
                .andExpect(jsonPath("$.price").value(139.00));

        assertThat(productRepository.findById(id).orElseThrow().getStockQuantity()).isEqualTo(25);

        // delete -> 204, then a re-read -> 404
        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());

        assertThat(productRepository.findById(id)).isEmpty();

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getById for a nonexistent product returns the standard error shape")
    void getByIdNotFoundReturnsStandardErrorShape() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("999999")))
                .andExpect(jsonPath("$.path").value("/api/products/999999"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("creating with invalid fields returns 400 with a fieldErrors entry per violation")
    void createWithInvalidFieldsReturnsValidationErrorShape() throws Exception {
        ProductRequestDto invalidRequest = ProductRequestDto.builder()
                .name("")                              // @NotBlank
                .sku("bad sku!")                        // @ValidSku
                .price(new BigDecimal("-5"))            // @Positive
                .stockQuantity(-1)                      // @PositiveOrZero
                .categoryId(null)                       // @NotNull
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.sku", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.price", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.stockQuantity", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.categoryId", notNullValue()));

        // nothing should have been persisted for a rejected request
        assertThat(productRepository.count()).isZero();
    }

    @Test
    @DisplayName("creating with an unknown categoryId returns 404, not 400 or 500")
    void createWithUnknownCategoryReturnsNotFound() throws Exception {
        ProductRequestDto request = ProductRequestDto.builder()
                .name("Orphan Product")
                .sku("MISC-ORPHAN-001")
                .price(new BigDecimal("10.00"))
                .stockQuantity(1)
                .categoryId(999_999L)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Category not found")));

        assertThat(productRepository.count()).isZero();
    }

    @Test
    @DisplayName("discontinue then reactivate, verified against the real DB, not just the HTTP response")
    void discontinueThenReactivate() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Mouse").sku("ELEC-MOUSE-001")
                .price(new BigDecimal("25.00")).stockQuantity(10)
                .category(electronics).build());

        assertThat(productRepository.findById(product.getId()).orElseThrow().isActive()).isTrue();

        mockMvc.perform(post("/api/products/{id}/discontinue", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(productRepository.findById(product.getId()).orElseThrow().isActive()).isFalse();

        mockMvc.perform(post("/api/products/{id}/reactivate", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        assertThat(productRepository.findById(product.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("discontinuing a nonexistent product returns 404")
    void discontinueNotFoundReturns404() throws Exception {
        mockMvc.perform(post("/api/products/{id}/discontinue", 999_999L))
                .andExpect(status().isNotFound());
    }

}
