package com.morapack.skyroute.simulation.dto;

import java.util.List;

public record SimulationOrderPlan(
        String orderId,
        long slackMinutes,
        List<SimulationRoute> routes
) {}