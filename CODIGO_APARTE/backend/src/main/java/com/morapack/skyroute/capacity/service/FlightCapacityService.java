package com.morapack.skyroute.capacity.service;

import com.morapack.skyroute.models.FlightCapacity;
import com.morapack.skyroute.models.FlightCapacity.FlightCapacityKey;
import com.morapack.skyroute.capacity.repository.FlightCapacityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FlightCapacityService {

    private final FlightCapacityRepository repository;

    public FlightCapacityService(FlightCapacityRepository repository) {
        this.repository = repository;
    }

    public List<FlightCapacity> getAll() {
        return repository.findAll();
    }

    public Optional<FlightCapacity> getById(FlightCapacityKey key) {
        return repository.findById(key);
    }

    public List<FlightCapacity> getByFlight(String flightId) {
        return repository.findByIdFlightId(flightId);
    }

    public List<FlightCapacity> getByDate(LocalDate date) {
        return repository.findByIdDate(date);
    }

    public FlightCapacity save(FlightCapacity capacity) {
        return repository.save(capacity);
    }

    public void delete(FlightCapacityKey key) {
        repository.deleteById(key);
    }
}
