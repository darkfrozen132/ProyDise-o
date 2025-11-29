package com.morapack.skyroute.capacity.controller;

import com.morapack.skyroute.base.repository.AirportRepository;
import com.morapack.skyroute.capacity.service.AirportOperationService;
import com.morapack.skyroute.models.Airport;
import com.morapack.skyroute.models.AirportOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/capacity/airports")
public class AirportOperationController {

    private final AirportOperationService airportOperationService;
    private final AirportRepository airportRepository;

    @Autowired
    public AirportOperationController(AirportOperationService airportOperationService,
                                      AirportRepository airportRepository) {
        this.airportOperationService = airportOperationService;
        this.airportRepository = airportRepository;
    }

    // ✅ Obtener todas las operaciones
    @GetMapping
    public ResponseEntity<List<AirportOperation>> getAll() {
        return ResponseEntity.ok(airportOperationService.getAllOperations());
    }
/* 
    // ✅ Obtener operaciones por código de aeropuerto
    @GetMapping("/airport/{airportCode}")
    public ResponseEntity<List<AirportOperation>> getByAirport(@PathVariable String airportCode) {
        Optional<Airport> airport = airportRepository.findById(airportCode);
        if (airport.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(airportOperationService.getOperationsByAirport(airport.get()));
    }

    // ✅ Registrar una nueva operación
    @PostMapping("/airport/{airportCode}/delta/{delta}")
    public ResponseEntity<AirportOperation> registerOperation(
            @PathVariable String airportCode,
            @PathVariable int delta) {
        Optional<Airport> airport = airportRepository.findById(airportCode);
        if (airport.isEmpty()) return ResponseEntity.notFound().build();

        AirportOperation op = airportOperationService.registerOperation(airport.get(), delta);
        return ResponseEntity.ok(op);
    }

    // ✅ Obtener operación por ID
    @GetMapping("/{id}")
    public ResponseEntity<AirportOperation> getById(@PathVariable Long id) {
        return airportOperationService.getOperationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Eliminar operación
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        airportOperationService.deleteOperation(id);
        return ResponseEntity.noContent().build();
    }
        */
}
