package com.morapack.skyroute.simulation.dto;

import java.time.LocalDateTime;

public record SimulationStartRequest(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer windowMinutes
) {}
