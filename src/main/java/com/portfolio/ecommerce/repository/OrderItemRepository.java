package com.portfolio.ecommerce.repository;

import com.portfolio.ecommerce.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
