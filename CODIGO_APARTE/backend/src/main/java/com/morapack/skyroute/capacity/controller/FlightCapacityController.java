package com.morapack.skyroute.capacity.controller;

import com.morapack.skyroute.models.FlightCapacity;
import com.morapack.skyroute.capacity.service.FlightCapacityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/capacity/flights")
public class FlightCapacityController {

    private final FlightCapacityService service;

    public FlightCapacityController(FlightCapacityService service) {
        this.service = service;
    }

    @GetMapping
    public List<FlightCapacity> getAll() {
        return service.getAll();
    }

    /*@GetMapping("/{flightId}/{date}")
    public ResponseEntity<FlightCapacity> getById(
            @PathVariable String flightId,
            @PathVariable String date) {

        FlightCapacityKey key = new FlightCapacity().new FlightCapacityKey();
        key.setFlightId(flightId);
        key.setDate(LocalDate.parse(date));

        return service.getById(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/flight/{flightId}")
    public List<FlightCapacity> getByFlight(@PathVariable String flightId) {
        return service.getByFlight(flightId);
    }

    @GetMapping("/date/{date}")
    public List<FlightCapacity> getByDate(@PathVariable String date) {
        return service.getByDate(LocalDate.parse(date));
    }

    @PostMapping
    public FlightCapacity save(@RequestBody FlightCapacity capacity) {
        return service.save(capacity);
    }

    @DeleteMapping("/{flightId}/{date}")
    public ResponseEntity<Void> delete(
            @PathVariable String flightId,
            @PathVariable String date) {

        FlightCapacityKey key = new FlightCapacity().new FlightCapacityKey();
        key.setFlightId(flightId);
        key.setDate(LocalDate.parse(date));

        service.delete(key);
        return ResponseEntity.noContent().build();
    }*/
}
