package com.morapack.skyroute.capacity.repository;

import com.morapack.skyroute.models.FlightCapacity;
import com.morapack.skyroute.models.FlightCapacity.FlightCapacityKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FlightCapacityRepository extends JpaRepository<FlightCapacity, FlightCapacityKey> {

    List<FlightCapacity> findByIdFlightId(String flightId);

    List<FlightCapacity> findByIdDate(LocalDate date);
}
