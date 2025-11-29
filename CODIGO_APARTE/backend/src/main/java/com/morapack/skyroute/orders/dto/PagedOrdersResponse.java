package com.morapack.skyroute.orders.dto;

import com.morapack.skyroute.models.Order;

import java.util.List;

public record PagedOrdersResponse(
        List<Order> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
