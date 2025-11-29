package com.morapack.skyroute.orders.dto;

import com.morapack.skyroute.models.OrderScope;

public record OrderCountResponse(OrderScope scope, long total) {}
