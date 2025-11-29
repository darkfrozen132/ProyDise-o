package com.morapack.skyroute.base.repository;

import com.morapack.skyroute.models.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightRepository extends JpaRepository<Flight, String> {}
