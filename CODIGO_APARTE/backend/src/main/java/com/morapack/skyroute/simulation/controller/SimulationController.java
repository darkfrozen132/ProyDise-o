package com.morapack.skyroute.simulation.controller;

import com.morapack.skyroute.simulation.dto.SimulationStartRequest;
import com.morapack.skyroute.simulation.dto.SimulationStartResponse;
import com.morapack.skyroute.simulation.dto.SimulationStatus;
import com.morapack.skyroute.simulation.service.SimulationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    public ResponseEntity<SimulationStartResponse> start(@RequestBody SimulationStartRequest request) {
        SimulationStartResponse response = simulationService.startSimulation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{simulationId}/status")
    public SimulationStatus status(@PathVariable UUID simulationId) {
        return simulationService.getStatus(simulationId);
    }

    @DeleteMapping("/{simulationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID simulationId) {
        simulationService.cancel(simulationId);
    }
}
