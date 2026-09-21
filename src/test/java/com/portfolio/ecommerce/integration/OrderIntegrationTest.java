package com.portfolio.ecommerce.integration;

import com.portfolio.ecommerce.AbstractIntegrationTest;
import com.portfolio.ecommerce.domain.Category;
import com.portfolio.ecommerce.domain.Product;
import com.portfolio.ecommerce.domain.User;
import com.portfolio.ecommerce.dto.order.OrderItemRequestDto;
import com.portfolio.ecommerce.dto.order.OrderRequestDto;
import com.portfolio.ecommerce.repository.CategoryRepository;
import com.portfolio.ecommerce.repository.OrderRepository;
import com.portfolio.ecommerce.repository.ProductRepository;
import com.portfolio.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack: real HTTP dispatch -> OrderController -> OrderServiceImpl ->
 * OrderRepository/ProductRepository -> real Postgres.
 *
 * The rollback tests here are the actual point of Day 10. OrderServiceImplTest
 * (Day 9, mocked repositories) could only prove "insufficient stock throws
 * and save() is never called" - there was no real transaction to roll back,
 * so it could never prove anything about an item that failed *after* an
 * earlier item in the same request had already had its stock decremented.
 * These tests hit real Postgres, so re-reading a product's stock afterward
 * is a real claim about what's actually persisted, not just about what a
 * mock was told to expect.
 */
@DisplayName("Order API (integration)")
class OrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    private User alice;
    private Product laptop;
    private Product mouse;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(User.builder().fullName("Alice Johnson").email("alice@example.com").build());
        Category electronics = categoryRepository.save(Category.builder().name("Electronics").build());

        laptop = productRepository.save(Product.builder()
                .name("Laptop").sku("ELEC-LAPTOP-001")
                .price(new BigDecimal("1000.00")).stockQuantity(5)
                .category(electronics).build());

        mouse = productRepository.save(Product.builder()
                .name("Mouse").sku("ELEC-MOUSE-001")
                .price(new BigDecimal("25.00")).stockQuantity(3)
                .category(electronics).build());
    }

    @Test
    @DisplayName("happy path: order persists, total is computed, and stock is really decremented in Postgres")
    void createOrderPersistsAndDecrementsStock() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(alice.getId())
                .items(List.of(
                        OrderItemRequestDto.builder().productId(laptop.getId()).quantity(1).build(),
                        OrderItemRequestDto.builder().productId(mouse.getId()).quantity(2).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1050.00)) // 1000.00 + 2*25.00
                .andExpect(jsonPath("$.items.length()").value(2));

        // re-read from the database directly - this is what a mocked repository can't prove
        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity()).isEqualTo(4);
        assertThat(productRepository.findById(mouse.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("insufficient stock on the only item: 409, and stock is untouched in the database")
    void insufficientStockLeavesStockUntouched() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(alice.getId())
                .items(List.of(OrderItemRequestDto.builder().productId(laptop.getId()).quantity(999).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Insufficient stock")));

        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("real rollback: when a later item fails, an earlier item's already-decremented stock reverts too")
    void insufficientStockOnLaterItemRollsBackEarlierItemsStock() throws Exception {
        // laptop (qty 1) succeeds on its own and would decrement 5 -> 4 if committed;
        // mouse (qty 999) then fails. The whole request must roll back as one unit,
        // so the laptop's stock must be back to 5, not left at 4.
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(alice.getId())
                .items(List.of(
                        OrderItemRequestDto.builder().productId(laptop.getId()).quantity(1).build(),
                        OrderItemRequestDto.builder().productId(mouse.getId()).quantity(999).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity())
                .as("laptop stock must roll back to its original value, not stay decremented")
                .isEqualTo(5);
        assertThat(productRepository.findById(mouse.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("unknown user: 404, no order or stock changes persisted")
    void createOrderWithUnknownUserReturnsNotFound() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(999_999L)
                .items(List.of(OrderItemRequestDto.builder().productId(laptop.getId()).quantity(1).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("confirm then cancel: status transitions and stock is restored on cancel, against the real DB")
    void confirmThenCancelRestoresStock() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(alice.getId())
                .items(List.of(OrderItemRequestDto.builder().productId(laptop.getId()).quantity(2).build()))
                .build();

        String createJson = mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(createJson).get("id").asLong();

        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);

        mockMvc.perform(post("/api/orders/{id}/confirm", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity())
                .as("cancelling restores the reserved stock")
                .isEqualTo(5);

        // cancelling an already-cancelled order must be rejected, not double-restock
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict());

        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("edge case (Day 12): ordering a discontinued product returns 409, stock untouched")
    void orderingDiscontinuedProductReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/products/{id}/discontinue", laptop.getId()))
                .andExpect(status().isOk());

        OrderRequestDto request = OrderRequestDto.builder()
                .userId(alice.getId())
                .items(List.of(OrderItemRequestDto.builder().productId(laptop.getId()).quantity(1).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("discontinued")));

        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("edge case (Day 12): a negative quantity is rejected by bean validation before the service layer ever runs")
    void negativeQuantityReturnsValidationError() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(alice.getId())
                .items(List.of(OrderItemRequestDto.builder().productId(laptop.getId()).quantity(-1).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").exists());

        // nothing should have been touched - this never even reaches OrderServiceImpl
        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
        assertThat(orderRepository.count()).isZero();
    }

}
