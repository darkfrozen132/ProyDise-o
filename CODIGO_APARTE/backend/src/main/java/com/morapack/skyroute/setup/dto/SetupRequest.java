package com.morapack.skyroute.setup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetupRequest(
        List<AirportPayload> airports,
        List<FlightPayload> flights
) {

    public record AirportPayload(
            String code,
            String name,
            Integer gmtOffsetHours,
            Integer storageCapacity,
            String continent,
            Double latitude,
            Double longitude
    ) {}

    public record FlightPayload(
            String id,
            String originCode,
            String destinationCode,
            String departureTime,
            String arrivalTime,
            Integer dailyCapacity
    ) {
        public LocalTime depLocal() {
            return departureTime != null ? LocalTime.parse(departureTime) : null;
        }

        public LocalTime arrLocal() {
            return arrivalTime != null ? LocalTime.parse(arrivalTime) : null;
        }
    }
}
