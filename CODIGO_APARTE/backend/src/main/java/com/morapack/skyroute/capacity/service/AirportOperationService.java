package com.morapack.skyroute.capacity.service;

import com.morapack.skyroute.models.Airport;
import com.morapack.skyroute.models.AirportOperation;
import com.morapack.skyroute.capacity.repository.AirportOperationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AirportOperationService {

    private final AirportOperationRepository airportOperationRepository;

    @Autowired
    public AirportOperationService(AirportOperationRepository airportOperationRepository) {
        this.airportOperationRepository = airportOperationRepository;
    }

    // ✅ Registrar nueva operación (depósito o retiro)
    public AirportOperation registerOperation(Airport airport, int delta) {
        AirportOperation operation = new AirportOperation(airport, LocalDateTime.now(), delta);
        return airportOperationRepository.save(operation);
    }

    // ✅ Obtener todas las operaciones
    public List<AirportOperation> getAllOperations() {
        return airportOperationRepository.findAll();
    }

    // ✅ Buscar operación por ID
    public Optional<AirportOperation> getOperationById(Long id) {
        return airportOperationRepository.findById(id);
    }

    // ✅ Buscar operaciones de un aeropuerto
    public List<AirportOperation> getOperationsByAirport(Airport airport) {
        return airportOperationRepository.findByAirportOrderByTimestampDesc(airport);
    }

    // ✅ Eliminar operación
    public void deleteOperation(Long id) {
        airportOperationRepository.deleteById(id);
    }
}
