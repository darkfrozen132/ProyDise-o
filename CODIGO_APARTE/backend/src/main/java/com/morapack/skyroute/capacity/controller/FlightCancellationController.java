package com.morapack.skyroute.capacity.controller;

import com.morapack.skyroute.base.repository.FlightRepository;
import com.morapack.skyroute.capacity.service.FlightCancellationService;
import com.morapack.skyroute.models.Flight;
import com.morapack.skyroute.models.FlightCancellation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/capacity/flights")
public class FlightCancellationController {

    private final FlightRepository flightRepository;
    private final FlightCancellationService cancellations;

    public FlightCancellationController(FlightRepository flightRepository,
                                        FlightCancellationService cancellations) {
        this.flightRepository = flightRepository;
        this.cancellations = cancellations;
    }

    @GetMapping("/{flightId}/cancellations")
    public List<FlightCancellation> list(@PathVariable String flightId) {
        return cancellations.findByFlight(flightId);
    }

    @PostMapping("/{flightId}/cancellations")
    public ResponseEntity<FlightCancellation> create(@PathVariable String flightId,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found: " + flightId));
        FlightCancellation created = cancellations.create(flight, date);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{flightId}/cancellations")
    public ResponseEntity<Void> delete(@PathVariable String flightId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        cancellations.delete(flightId, date);
        return ResponseEntity.noContent().build();
    }
}
