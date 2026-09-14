package com.portfolio.ecommerce.service;

import com.portfolio.ecommerce.dto.order.OrderRequestDto;
import com.portfolio.ecommerce.dto.order.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OrderService {

    OrderResponseDto create(OrderRequestDto request);

    OrderResponseDto getById(Long id);

    Page<OrderResponseDto> getAll(Pageable pageable);

    Page<OrderResponseDto> getByUserEmail(String email, Pageable pageable);

    List<OrderResponseDto> getByUserEmailAndDateRange(String email, Instant from, Instant to);

    OrderResponseDto confirm(Long id);

    OrderResponseDto cancel(Long id);

}
