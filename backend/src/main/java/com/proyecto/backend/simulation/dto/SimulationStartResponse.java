package com.proyecto.backend.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta al iniciar una simulación
 * 
 * Contiene el sessionId único que el cliente debe usar para:
 * 1. Subscribirse al topic STOMP: /topic/simulations/{sessionId}
 * 2. Controlar la simulación vía REST: /api/simulations/{sessionId}/cancel
 * 
 * @author Sistema Package Planner
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationStartResponse {
    
    /**
     * ID único de la sesión de simulación (UUID)
     * El cliente debe guardarlo para subscribirse al topic y controlar la simulación
     */
    private String sessionId;
    
    /**
     * Mensaje descriptivo del estado inicial
     * Ej: "Simulación iniciada correctamente"
     */
    private String mensaje;
    
    /**
     * Topic STOMP al que el cliente debe subscribirse
     * Ej: "/topic/simulations/550e8400-e29b-41d4-a716-446655440000"
     */
    private String topicUrl;
}
