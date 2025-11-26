package com.proyecto.backend.simulation.controller;

import com.proyecto.backend.simulation.dto.SimulationRequest;
import com.proyecto.backend.simulation.dto.SimulationSnapshot;
import com.proyecto.backend.simulation.dto.SimulationStartResponse;
import com.proyecto.backend.simulation.service.SimulationService;
import com.proyecto.backend.simulation.service.WebSocketService;
import com.proyecto.backend.simulation.session.SimulationSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador REST para gestión de simulaciones
 * 
 * Endpoints:
 * - POST /api/simulations/start → Iniciar simulación
 * - POST /api/simulations/{id}/cancel → Cancelar simulación
 * - POST /api/simulations/{id}/pause → Pausar simulación
 * - POST /api/simulations/{id}/resume → Reanudar simulación
 * - GET /api/simulations/{id} → Obtener estado
 * - GET /api/simulations → Listar todas
 * 
 * Nota: Las actualizaciones en tiempo real se envían por WebSocket STOMP
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // ⚠️ En producción: especificar dominios
public class SimulationController {

    private final SimulationService simulationService;
    private final WebSocketService webSocketService;

    /**
     * Inicia una nueva simulación
     * 
     * @param request Configuración de la simulación
     * @return SimulationStartResponse con sessionId y topic URL
     */
    @PostMapping("/start")
    public ResponseEntity<SimulationStartResponse> startSimulation(
            @Valid @RequestBody SimulationRequest request) {
        
        try {
            UUID sessionId = simulationService.startSimulation(request);
            String topicUrl = webSocketService.getSimulationTopic(sessionId.toString());
            
            log.info("✅ Simulación iniciada: {} → Topic: {}", sessionId, topicUrl);
            
            SimulationStartResponse response = SimulationStartResponse.builder()
                    .sessionId(sessionId.toString())
                    .mensaje("Simulación iniciada exitosamente")
                    .topicUrl(topicUrl)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al iniciar simulación: {}", e.getMessage(), e);
            
            SimulationStartResponse errorResponse = SimulationStartResponse.builder()
                    .sessionId(null)
                    .mensaje("Error: " + e.getMessage())
                    .topicUrl(null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Cancela una simulación en ejecución
     * 
     * @param sessionId ID de la sesión
     * @return Confirmación de cancelación
     */
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<SimulationResponse> cancelSimulation(
            @PathVariable UUID sessionId) {
        
        boolean cancelled = simulationService.cancelSimulation(sessionId);
        
        if (cancelled) {
            return ResponseEntity.ok(SimulationResponse.success(
                    sessionId,
                    "Simulación cancelada exitosamente"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SimulationResponse.error("Simulación no encontrada"));
        }
    }

    /**
     * Pausa una simulación en ejecución
     * 
     * @param sessionId ID de la sesión
     * @return Confirmación de pausa
     */
    @PostMapping("/{sessionId}/pause")
    public ResponseEntity<SimulationResponse> pauseSimulation(
            @PathVariable UUID sessionId) {
        
        boolean paused = simulationService.pauseSimulation(sessionId);
        
        if (paused) {
            return ResponseEntity.ok(SimulationResponse.success(
                    sessionId,
                    "Simulación pausada exitosamente"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SimulationResponse.error("Simulación no encontrada"));
        }
    }

    /**
     * Reanuda una simulación pausada
     * 
     * @param sessionId ID de la sesión
     * @return Confirmación de reanudación
     */
    @PostMapping("/{sessionId}/resume")
    public ResponseEntity<SimulationResponse> resumeSimulation(
            @PathVariable UUID sessionId) {
        
        boolean resumed = simulationService.resumeSimulation(sessionId);
        
        if (resumed) {
            return ResponseEntity.ok(SimulationResponse.success(
                    sessionId,
                    "Simulación reanudada exitosamente"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SimulationResponse.error("Simulación no encontrada"));
        }
    }

    /**
     * Obtiene el estado actual de una simulación
     * 
     * @param sessionId ID de la sesión
     * @return Snapshot actual
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSimulationStatus(
            @PathVariable UUID sessionId) {
        
        SimulationSnapshot snapshot = simulationService.getSimulationStatus(sessionId);
        
        if (snapshot != null) {
            return ResponseEntity.ok(snapshot);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SimulationResponse.error("Simulación no encontrada"));
        }
    }

    /**
     * Lista todas las simulaciones activas
     * 
     * @return Map de sesiones activas
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listSimulations() {
        Map<UUID, SimulationSession> sessions = simulationService.getActiveSessions();
        
        Map<String, Object> response = Map.of(
                "totalSessions", sessions.size(),
                "sessions", sessions.entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey().toString(),
                                e -> Map.of(
                                        "sessionName", e.getValue().getSessionName(),
                                        "status", e.getValue().getCurrentStatus(),
                                        "createdAt", e.getValue().getCreatedAt(),
                                        "isRunning", e.getValue().isRunning(),
                                        "isPaused", e.getValue().isPaused()
                                )
                        ))
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check del servicio de simulaciones
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        int activeSessions = simulationService.getActiveSessions().size();
        
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "SimulationService",
                "activeSessions", activeSessions,
                "timestamp", System.currentTimeMillis()
        ));
    }

    // ============ DTO DE RESPUESTA ============

    /**
     * DTO genérico para respuestas de la API
     */
    public record SimulationResponse(
            boolean success,
            String message,
            UUID sessionId,
            String subscriptionTopic
    ) {
        public static SimulationResponse success(UUID sessionId, String message) {
            return new SimulationResponse(
                    true,
                    message,
                    sessionId,
                    "/topic/simulations/" + sessionId
            );
        }

        public static SimulationResponse error(String message) {
            return new SimulationResponse(
                    false,
                    message,
                    null,
                    null
            );
        }
    }
}
