package com.portfolio.ecommerce.service.impl;

import com.portfolio.ecommerce.domain.Category;
import com.portfolio.ecommerce.domain.Order;
import com.portfolio.ecommerce.domain.OrderItem;
import com.portfolio.ecommerce.domain.Product;
import com.portfolio.ecommerce.domain.User;
import com.portfolio.ecommerce.domain.enums.OrderStatus;
import com.portfolio.ecommerce.dto.order.OrderItemRequestDto;
import com.portfolio.ecommerce.dto.order.OrderRequestDto;
import com.portfolio.ecommerce.dto.order.OrderResponseDto;
import com.portfolio.ecommerce.exception.InsufficientStockException;
import com.portfolio.ecommerce.exception.InvalidOrderStateException;
import com.portfolio.ecommerce.exception.ProductNotAvailableException;
import com.portfolio.ecommerce.exception.ResourceNotFoundException;
import com.portfolio.ecommerce.mapper.OrderMapper;
import com.portfolio.ecommerce.repository.OrderRepository;
import com.portfolio.ecommerce.repository.ProductRepository;
import com.portfolio.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pure Mockito unit tests for the order-creation and state-transition logic.
 * Note on "insufficient stock rolls back everything": at the unit level,
 * with mocked repositories, there's no real transaction to roll back - all
 * we can verify here is that the exception is thrown and orderRepository
 * .save(...) is never reached, so nothing is persisted. Whether stock
 * changes on *other* products already touched earlier in the loop actually
 * roll back at the database level is a Testcontainers concern (Day 10),
 * since that's a real transaction against a real Postgres instance.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderMapper orderMapper;

    private OrderServiceImpl orderService;

    private User user;
    private Product laptop;
    private Product mouse;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, userRepository, productRepository, orderMapper);

        user = User.builder().id(1L).fullName("Alice").email("alice@example.com").build();

        Category category = Category.builder().id(1L).name("Electronics").build();

        laptop = Product.builder()
                .id(1L).name("Laptop").sku("ELEC-LAPTOP-001")
                .price(new BigDecimal("1000.00")).stockQuantity(5)
                .category(category).build();

        mouse = Product.builder()
                .id(2L).name("Mouse").sku("ELEC-MOUSE-001")
                .price(new BigDecimal("25.00")).stockQuantity(3)
                .category(category).build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happy path: decrements stock per item and computes the total from unit-price snapshots")
        void createsOrderAndDecrementsStock() {
            OrderRequestDto request = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(
                            OrderItemRequestDto.builder().productId(1L).quantity(2).build(),   // 2 * 1000.00
                            OrderItemRequestDto.builder().productId(2L).quantity(3).build()))  // 3 * 25.00
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(productRepository.findById(1L)).willReturn(Optional.of(laptop));
            given(productRepository.findById(2L)).willReturn(Optional.of(mouse));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(orderMapper.toResponseDto(any(Order.class))).willReturn(OrderResponseDto.builder().id(99L).build());

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            OrderResponseDto result = orderService.create(request);

            assertThat(result.getId()).isEqualTo(99L);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("2075.00"); // 2000.00 + 75.00
            assertThat(savedOrder.getItems()).hasSize(2);

            // stock decremented in place on the managed entities
            assertThat(laptop.getStockQuantity()).isEqualTo(3); // 5 - 2
            assertThat(mouse.getStockQuantity()).isEqualTo(0);  // 3 - 3
        }

        @Test
        @DisplayName("not-found: unknown user ID throws before touching any product")
        void throwsWhenUserNotFound() {
            OrderRequestDto request = OrderRequestDto.builder()
                    .userId(404L)
                    .items(List.of(OrderItemRequestDto.builder().productId(1L).quantity(1).build()))
                    .build();
            given(userRepository.findById(404L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 404");

            verify(productRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("not-found: unknown product ID in the item list")
        void throwsWhenProductNotFound() {
            OrderRequestDto request = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(OrderItemRequestDto.builder().productId(999L).quantity(1).build()))
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found with id: 999");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("insufficient stock: requesting more than available throws and never reaches save()")
        void throwsWhenRequestedQuantityExceedsStock() {
            OrderRequestDto request = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(OrderItemRequestDto.builder().productId(1L).quantity(999).build()))
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(productRepository.findById(1L)).willReturn(Optional.of(laptop));

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Laptop")
                    .hasMessageContaining("requested 999")
                    .hasMessageContaining("available 5");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("insufficient stock on a later item: earlier items in the same request are never persisted")
        void doesNotPersistAnyItemWhenALaterOneFailsStockCheck() {
            OrderRequestDto request = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(
                            OrderItemRequestDto.builder().productId(1L).quantity(1).build(),    // fine on its own
                            OrderItemRequestDto.builder().productId(2L).quantity(999).build())) // fails
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(productRepository.findById(1L)).willReturn(Optional.of(laptop));
            given(productRepository.findById(2L)).willReturn(Optional.of(mouse));

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(InsufficientStockException.class);

            // save() is never reached regardless of how many items succeeded before the failure
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("edge case (Day 12): ordering a discontinued product throws before the stock check even runs")
        void throwsWhenProductIsDiscontinued() {
            laptop.setActive(false);
            OrderRequestDto request = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(OrderItemRequestDto.builder().productId(1L).quantity(1).build()))
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(productRepository.findById(1L)).willReturn(Optional.of(laptop));

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(ProductNotAvailableException.class)
                    .hasMessageContaining("Laptop")
                    .hasMessageContaining("discontinued");

            // stock must be untouched - the discontinued check runs before the stock decrement
            assertThat(laptop.getStockQuantity()).isEqualTo(5);
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("not-found")
        void throwsWhenNotFound() {
            given(orderRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("happy path: PENDING -> CONFIRMED")
        void confirmsPendingOrder() {
            Order order = Order.builder().id(5L).status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN).build();
            given(orderRepository.findById(5L)).willReturn(Optional.of(order));
            given(orderMapper.toResponseDto(order)).willReturn(OrderResponseDto.builder().id(5L).status(OrderStatus.CONFIRMED).build());

            OrderResponseDto result = orderService.confirm(5L);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("validation edge case: only PENDING orders can be confirmed")
        void throwsWhenOrderIsNotPending() {
            Order order = Order.builder().id(5L).status(OrderStatus.CANCELLED).totalAmount(BigDecimal.TEN).build();
            given(orderRepository.findById(5L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.confirm(5L))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("CANCELLED");
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("happy path: restocks every item's product and moves to CANCELLED")
        void cancelsOrderAndRestocksItems() {
            Order order = Order.builder().id(5L).status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN).build();
            OrderItem item1 = OrderItem.builder().product(laptop).quantity(2).unitPrice(laptop.getPrice()).build();
            OrderItem item2 = OrderItem.builder().product(mouse).quantity(1).unitPrice(mouse.getPrice()).build();
            order.addItem(item1);
            order.addItem(item2);

            given(orderRepository.findById(5L)).willReturn(Optional.of(order));
            given(orderMapper.toResponseDto(order)).willReturn(OrderResponseDto.builder().id(5L).status(OrderStatus.CANCELLED).build());

            orderService.cancel(5L);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(laptop.getStockQuantity()).isEqualTo(7); // 5 + 2
            assertThat(mouse.getStockQuantity()).isEqualTo(4);  // 3 + 1
        }

        @Test
        @DisplayName("validation edge case: an already-cancelled order can't be cancelled again (no double-restock)")
        void throwsWhenAlreadyCancelled() {
            Order order = Order.builder().id(5L).status(OrderStatus.CANCELLED).totalAmount(BigDecimal.TEN).build();
            given(orderRepository.findById(5L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancel(5L))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("already cancelled");

            // stock quantities must be untouched - nothing to restock on a rejected cancel
            assertThat(laptop.getStockQuantity()).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("getByUserEmailAndDateRange delegates straight to the repository and maps every result")
    void getByUserEmailAndDateRangeDelegates() {
        Order order = Order.builder().id(5L).status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN).build();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");
        given(orderRepository.findByUserEmailAndDateRange("alice@example.com", from, to)).willReturn(List.of(order));
        given(orderMapper.toResponseDto(order)).willReturn(OrderResponseDto.builder().id(5L).build());

        List<OrderResponseDto> result = orderService.getByUserEmailAndDateRange("alice@example.com", from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5L);
    }

}
