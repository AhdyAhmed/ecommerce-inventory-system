package com.portfolio.ecommerce.service;

import com.portfolio.ecommerce.dto.order.OrderRequestDto;
import com.portfolio.ecommerce.dto.order.OrderResponseDto;

import java.util.List;

public interface OrderService {

    OrderResponseDto create(OrderRequestDto request);

    OrderResponseDto getById(Long id);

    List<OrderResponseDto> getAll();

    OrderResponseDto confirm(Long id);

    OrderResponseDto cancel(Long id);

}
