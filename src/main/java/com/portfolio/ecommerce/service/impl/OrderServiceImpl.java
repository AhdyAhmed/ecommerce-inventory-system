package com.portfolio.ecommerce.service.impl;

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
import com.portfolio.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    /**
     * Purely a logging threshold - "warn in the log when a decrement leaves
     * a product at or below this many units" - and unrelated to
     * ProductService.getLowStock(int threshold)'s caller-supplied query
     * parameter of the same name. Kept small and fixed here since it's just
     * an operational signal, not a business rule anyone configures per call.
     */
    private static final int LOW_STOCK_LOG_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    /**
     * Builds the order, decrementing stock and snapshotting each product's
     * price as it goes. The whole method runs in one transaction on purpose:
     * if item 3 of 5 fails on insufficient stock, the stock already
     * decremented for items 1 and 2 must roll back too, or the customer would
     * have been charged for products that were silently released back into
     * inventory. "All items succeed, or none do" is the only version of this
     * that's safe to expose as an API.
     *
     * Stock decrements happen via plain setters on managed entities
     * (`product.setStockQuantity(...)`) rather than an explicit save() call -
     * JPA's dirty checking flushes those changes automatically at commit,
     * same as the order and its items.
     */
    @Override
    @Transactional
    public OrderResponseDto create(OrderRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        for (OrderItemRequestDto itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + itemRequest.getProductId()));

            if (!product.isActive()) {
                throw new ProductNotAvailableException(
                        "Product '%s' (sku: %s) has been discontinued and can't be ordered"
                                .formatted(product.getName(), product.getSku()));
            }

            int requestedQuantity = itemRequest.getQuantity();
            if (product.getStockQuantity() < requestedQuantity) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '%s' (sku: %s): requested %d, available %d"
                                .formatted(product.getName(), product.getSku(), requestedQuantity, product.getStockQuantity()));
            }

            int remainingStock = product.getStockQuantity() - requestedQuantity;
            product.setStockQuantity(remainingStock);

            if (remainingStock <= LOW_STOCK_LOG_THRESHOLD) {
                log.warn("Low stock after order: productId={}, sku={}, remainingStock={}",
                        product.getId(), product.getSku(), remainingStock);
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(requestedQuantity)
                    .unitPrice(product.getPrice())
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(calculateTotal(order));

        Order saved = orderRepository.save(order);

        log.info("Order created: orderId={}, userId={}, itemCount={}, totalAmount={}",
                saved.getId(), user.getId(), saved.getItems().size(), saved.getTotalAmount());

        return orderMapper.toResponseDto(saved);
    }

    @Override
    public OrderResponseDto getById(Long id) {
        return orderMapper.toResponseDto(findOrderOrThrow(id));
    }

    @Override
    public Page<OrderResponseDto> getAll(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toResponseDto);
    }

    @Override
    public Page<OrderResponseDto> getByUserEmail(String email, Pageable pageable) {
        return orderRepository.findByUser_Email(email, pageable)
                .map(orderMapper::toResponseDto);
    }

    @Override
    public List<OrderResponseDto> getByUserEmailAndDateRange(String email, Instant from, Instant to) {
        return orderRepository.findByUserEmailAndDateRange(email, from, to).stream()
                .map(orderMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDto confirm(Long id) {
        Order order = findOrderOrThrow(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only PENDING orders can be confirmed (order %d is %s)".formatted(id, order.getStatus()));
        }
        order.setStatus(OrderStatus.CONFIRMED);
        log.info("Order confirmed: orderId={}", id);
        return orderMapper.toResponseDto(order);
    }

    /**
     * Cancelling releases the reserved stock back to each product. Allowed
     * from PENDING or CONFIRMED; a CANCELLED order can't be cancelled again,
     * which would double-restock it.
     */
    @Override
    @Transactional
    public OrderResponseDto cancel(Long id) {
        Order order = findOrderOrThrow(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order %d is already cancelled".formatted(id));
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order cancelled: orderId={}, itemsRestocked={}", id, order.getItems().size());
        return orderMapper.toResponseDto(order);
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private BigDecimal calculateTotal(Order order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
