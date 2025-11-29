package com.morapack.skyroute.orders.dto;

import java.time.LocalDateTime;

public record OrderRequest(
        String id,
        String customerReference,
        String destinationAirportCode,
        Integer quantity,
        LocalDateTime creationLocal,
        Boolean projected
) {}
