package com.proyecto.backend.simulation.controller;

import com.proyecto.backend.simulation.dto.DailyOperationResponse;
import com.proyecto.backend.simulation.dto.DailySimulationRequest;
import com.proyecto.backend.simulation.service.DailySimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para Operación DIARIA
 * 
 * Endpoint SÍNCRONO - llamas, procesa, devuelve resultado
 * No usa WebSocket, es una operación batch simple
 * 
 * Endpoints:
 * - POST /api/daily/run → Ejecutar operación diaria
 * - GET /api/daily/health → Health check
 * 
 * @author Sistema Package Planner
 * @version 2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/daily")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DailySimulationController {

    private final DailySimulationService dailySimulationService;

    /**
     * Ejecuta la operación diaria de forma SÍNCRONA
     * Procesa TODOS los pedidos de la tabla pedidos_diario
     * 
     * @param request Parámetros del AG (opcional, tiene defaults)
     * @return DailyOperationResponse con las rutas generadas
     */
    @PostMapping("/run")
    public ResponseEntity<DailyOperationResponse> ejecutarOperacionDiaria(
            @Valid @RequestBody(required = false) DailySimulationRequest request) {
        
        // Si no se envía request, usar defaults
        if (request == null) {
            request = DailySimulationRequest.builder().build();
        }
        
        log.info("🌅 [API] POST /api/daily/run - Población={}, Generaciones={}", 
                request.getTamanioPoblacion(), 
                request.getMaxGeneraciones());
        
        DailyOperationResponse response = dailySimulationService.ejecutarOperacionDiaria(request);
        
        if (response.isSuccess()) {
            log.info("✅ [API] Operación completada: {} rutas en {}ms", 
                    response.getTotalVuelos(), response.getTiempoProcesamiento());
            return ResponseEntity.ok(response);
        } else {
            log.warn("⚠️ [API] Operación fallida: {}", response.getMensaje());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Health check del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "DailyOperationService",
                "endpoint", "/api/daily/run",
                "method", "POST",
                "description", "Operación síncrona - procesa todos los pedidos de pedidos_diario",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
