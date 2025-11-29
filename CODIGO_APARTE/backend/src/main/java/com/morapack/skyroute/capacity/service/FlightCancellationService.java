package com.morapack.skyroute.capacity.service;

import com.morapack.skyroute.capacity.repository.FlightCancellationRepository;
import com.morapack.skyroute.models.Flight;
import com.morapack.skyroute.models.FlightCancellation;
import com.morapack.skyroute.models.FlightCancellationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FlightCancellationService {

    private final FlightCancellationRepository repository;

    public FlightCancellationService(FlightCancellationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FlightCancellation> findByFlight(String flightId) {
        return repository.findByIdFlightId(flightId);
    }

    @Transactional
    public FlightCancellation create(Flight flight, LocalDate date) {
        FlightCancellation cancellation = new FlightCancellation(flight, date);
        return repository.save(cancellation);
    }

    @Transactional
    public void delete(String flightId, LocalDate date) {
        repository.deleteById(new FlightCancellationId(flightId, date));
    }
}
