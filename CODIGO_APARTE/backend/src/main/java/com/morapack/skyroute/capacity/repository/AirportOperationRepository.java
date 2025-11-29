package com.morapack.skyroute.capacity.repository;

import com.morapack.skyroute.models.AirportOperation;
import com.morapack.skyroute.models.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirportOperationRepository extends JpaRepository<AirportOperation, Long> {

    // Buscar todas las operaciones de un aeropuerto
    List<AirportOperation> findByAirport(Airport airport);

    // Buscar todas las operaciones de un aeropuerto ordenadas por fecha
    List<AirportOperation> findByAirportOrderByTimestampDesc(Airport airport);
}
