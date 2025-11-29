package com.morapack.skyroute.simulation.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SimulationSegment(
        String flightId,
        String origin,
        String destination,
        LocalDate date,
        int quantity,
        Instant departureUtc,
        Instant arrivalUtc
) {}