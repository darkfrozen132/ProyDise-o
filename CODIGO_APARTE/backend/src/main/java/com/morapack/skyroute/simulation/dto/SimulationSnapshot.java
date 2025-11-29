package com.morapack.skyroute.simulation.dto;

import java.time.Instant;
import java.util.List;

public record SimulationSnapshot(
        String simulationId,
        int processedOrders,
        int totalOrders,
        double fitness,
        Instant generatedAt,
        List<SimulationOrderPlan> orderPlans
) {}
