package com.morapack.skyroute.orders.dto;

import java.util.List;

public record OrderBatchRequest(List<OrderRequest> orders) {}
