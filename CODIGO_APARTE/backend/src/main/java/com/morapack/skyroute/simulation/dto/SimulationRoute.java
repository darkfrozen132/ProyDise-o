package com.morapack.skyroute.simulation.dto;

import java.util.List;

public record SimulationRoute(
        int quantity,
        long slackMinutes,
        List<SimulationSegment> segments
) {}