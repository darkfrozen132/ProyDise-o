package com.morapack.skyroute.simulation.dto;

public record SimulationStatus(
        String simulationId,
        int processedOrders,
        int totalOrders,
        boolean completed,
        boolean cancelled,
        String error,
        SimulationSnapshot lastSnapshot
) {}
