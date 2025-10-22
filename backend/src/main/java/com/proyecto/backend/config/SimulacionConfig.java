package com.proyecto.backend.config;

import com.proyecto.backend.planificador.controller.SimulacionController;
import com.proyecto.backend.planificador.service.SimulacionOrchestrator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para conectar el Orchestrator con el Controller
 * Necesario para evitar dependencia circular en SSE
 */
@Configuration
@RequiredArgsConstructor
public class SimulacionConfig {

    private final SimulacionOrchestrator orchestrator;
    private final SimulacionController controller;

    @PostConstruct
    public void init() {
        // Conectar el orchestrator con el controller para SSE
        orchestrator.setSimulacionController(controller);
    }
}
