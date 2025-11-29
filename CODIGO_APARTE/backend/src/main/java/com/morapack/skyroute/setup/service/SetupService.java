package com.morapack.skyroute.setup.service;

import com.morapack.skyroute.base.repository.AirportRepository;
import com.morapack.skyroute.base.repository.FlightRepository;
import com.morapack.skyroute.models.Airport;
import com.morapack.skyroute.models.Flight;
import com.morapack.skyroute.setup.dto.SetupRequest;
import com.morapack.skyroute.setup.dto.SetupResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.Set;

@Service
public class SetupService {

    private final AirportRepository airportRepository;
    private final FlightRepository flightRepository;

    public SetupService(AirportRepository airportRepository,
                        FlightRepository flightRepository) {
        this.airportRepository = airportRepository;
        this.flightRepository = flightRepository;
    }

    @Transactional
    public SetupResponse load(SetupRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        int airports = loadAirports(request);
        int flights = loadFlights(request);
        return new SetupResponse(airports, flights);
    }

    private int loadAirports(SetupRequest request) {
        if (request.airports() == null) {
            return 0;
        }
        int count = 0;
        for (SetupRequest.AirportPayload payload : request.airports()) {
            validateAirport(payload);
            Airport airport = airportRepository.findById(payload.code())
                    .orElseGet(Airport::new);
            airport.setCode(payload.code());
            airport.setName(payload.name());
            airport.setGmtOffsetHours(payload.gmtOffsetHours());
            airport.setStorageCapacity(payload.storageCapacity());
            airport.setContinent(payload.continent());
            airport.setLatitude(payload.latitude());
            airport.setLongitude(payload.longitude());
            airportRepository.save(airport);
            count++;
        }
        return count;
    }

    private int loadFlights(SetupRequest request) {
        if (request.flights() == null) {
            return 0;
        }
        int count = 0;
        for (SetupRequest.FlightPayload payload : request.flights()) {
            validateFlight(payload);
            Airport origin = airportRepository.findById(payload.originCode())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Origin airport not found: " + payload.originCode()));
            Airport destination = airportRepository.findById(payload.destinationCode())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Destination airport not found: " + payload.destinationCode()));

            String id = payload.id();
            LocalTime dep = payload.depLocal();
            LocalTime arr = payload.arrLocal();
            if (dep == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departure time is required for flight");
            }
            if (arr == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arrival time is required for flight");
            }
            if (id == null || id.isBlank()) {
                id = Flight.buildId(origin, destination, dep);
            }

            Flight flight = flightRepository.findById(id).orElseGet(Flight::new);
            flight.setId(id);
            flight.setOrigin(origin);
            flight.setDestination(destination);
            flight.setDepLocal(dep);
            flight.setArrLocal(arr);
            flight.setDailyCapacity(payload.dailyCapacity());
            flight.setCancelledDates(Set.of());
            flightRepository.save(flight);
            count++;
        }
        return count;
    }

    private void validateAirport(SetupRequest.AirportPayload payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Airport payload is null");
        }
        if (isBlank(payload.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Airport code is required");
        }
        if (isBlank(payload.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Airport name is required");
        }
        if (payload.gmtOffsetHours() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Airport gmtOffsetHours is required");
        }
        if (payload.storageCapacity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Airport storageCapacity is required");
        }
    }

    private void validateFlight(SetupRequest.FlightPayload payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flight payload is null");
        }
        if (isBlank(payload.originCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flight originCode is required");
        }
        if (isBlank(payload.destinationCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flight destinationCode is required");
        }
        if (payload.dailyCapacity() == null || payload.dailyCapacity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flight dailyCapacity must be non-negative");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
