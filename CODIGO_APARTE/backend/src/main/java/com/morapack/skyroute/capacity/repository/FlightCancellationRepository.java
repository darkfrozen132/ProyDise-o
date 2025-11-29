package com.morapack.skyroute.capacity.repository;

import com.morapack.skyroute.models.FlightCancellation;
import com.morapack.skyroute.models.FlightCancellationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightCancellationRepository extends JpaRepository<FlightCancellation, FlightCancellationId> {
    List<FlightCancellation> findByIdFlightId(String flightId);
}
