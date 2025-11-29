package com.morapack.skyroute.base.controller;

import com.morapack.skyroute.models.Airport;
import com.morapack.skyroute.models.Flight;
import com.morapack.skyroute.base.service.AirportService;
import com.morapack.skyroute.base.service.FlightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/base")
public class BaseController {

    private final AirportService airportService;
    private final FlightService flightService;

    public BaseController(
            AirportService airportService,
            FlightService flightService
    ) {
        this.airportService = airportService;
        this.flightService = flightService;
    }

    @GetMapping("/airports")
    public List<BaseAirportResponse> getAirports() {
        return airportService.getAll()
                .stream()
                .map(BaseController::mapAirport)
                .toList();
    }

    @GetMapping("/flights")
    public List<BaseFlightResponse> getFlights() {
        return flightService.getAll()
                .stream()
                .map(BaseController::mapFlight)
                .toList();
    }

    private static BaseAirportResponse mapAirport(Airport airport) {
        return new BaseAirportResponse(
                airport.getCode(),
                airport.getName(),
                airport.getLatitude(),
                airport.getLongitude()
        );
    }

    private static BaseFlightResponse mapFlight(Flight flight) {
        return new BaseFlightResponse(
                flight.getId(),
                flight.getOriginCode(),
                flight.getDestinationCode(),
                flight.getDailyCapacity(),
                calculateDurationMinutes(flight.getDepLocal(), flight.getArrLocal())
        );
    }

    private static Long calculateDurationMinutes(LocalTime departure, LocalTime arrival) {
        if (departure == null || arrival == null) {
            return null;
        }
        long minutes = Duration.between(departure, arrival).toMinutes();
        if (minutes <= 0) {
            minutes += Duration.ofDays(1).toMinutes();
        }
        return minutes;
    }

    public record BaseAirportResponse(String id, String name, Double latitude, Double longitude) {}

    public record BaseFlightResponse(String id,
                                     String origin,
                                     String destination,
                                     Integer capacity,
                                     Long durationMinutes) {}

}
