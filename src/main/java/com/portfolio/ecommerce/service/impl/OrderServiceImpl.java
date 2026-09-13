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
import com.portfolio.ecommerce.exception.ResourceNotFoundException;
import com.portfolio.ecommerce.mapper.OrderMapper;
import com.portfolio.ecommerce.repository.OrderRepository;
import com.portfolio.ecommerce.repository.ProductRepository;
import com.portfolio.ecommerce.repository.UserRepository;
import com.portfolio.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

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

            int requestedQuantity = itemRequest.getQuantity();
            if (product.getStockQuantity() < requestedQuantity) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '%s' (sku: %s): requested %d, available %d"
                                .formatted(product.getName(), product.getSku(), requestedQuantity, product.getStockQuantity()));
            }

            product.setStockQuantity(product.getStockQuantity() - requestedQuantity);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(requestedQuantity)
                    .unitPrice(product.getPrice())
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(calculateTotal(order));

        Order saved = orderRepository.save(order);
        return orderMapper.toResponseDto(saved);
    }

    @Override
    public OrderResponseDto getById(Long id) {
        return orderMapper.toResponseDto(findOrderOrThrow(id));
    }

    @Override
    public List<OrderResponseDto> getAll() {
        return orderRepository.findAll().stream()
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
