package com.proyecto.backend.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response del servidor WebSocket al cliente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanificacionWSResponse {
    
    /**
     * Tipo de mensaje: "conectado", "progreso", "completado", "error", "pausado", "cancelado"
     */
    private String tipo;
    
    /**
     * Mensaje descriptivo
     */
    private String mensaje;
    
    /**
     * Métricas del algoritmo genético (generación, fitness, tiempo, etc.)
     */
    private Map<String, Object> metricas;
    
    /**
     * Solución actual: vuelos con pedidos asignados
     */
    private Object solucion;
    
    /**
     * Datos adicionales (opcional) - usado para conexión, etc.
     */
    private Map<String, Object> datos;
}
