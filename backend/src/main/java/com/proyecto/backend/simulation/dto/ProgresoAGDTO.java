package com.proyecto.backend.simulation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proyecto.backend.planificador.semanal.dto.response.PlanificacionResponseSimple;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para reportar el progreso del Algoritmo Genético en tiempo real
 * 
 * Se envía periódicamente vía STOMP al topic: /topic/simulations/{sessionId}
 * 
 * Campos en español según convención frontend (@JsonProperty)
 * 
 * @author Sistema Package Planner
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgresoAGDTO {
    
    // ===== IDENTIFICACIÓN =====
    
    /**
     * ID de la sesión de simulación
     */
    @JsonProperty("sessionId")
    private String sessionId;
    
    /**
     * Tipo de evento: "PROGRESO_AG", "SIMULACION_COMPLETADA", "ERROR"
     */
    @JsonProperty("tipo")
    private String tipo;
    
    // ===== PROGRESO DEL ALGORITMO GENÉTICO =====
    
    /**
     * Generación actual del AG (0 a maxGeneraciones)
     */
    @JsonProperty("generacion")
    private Integer generacion;
    
    /**
     * Número máximo de generaciones configuradas
     */
    @JsonProperty("maxGeneraciones")
    private Integer maxGeneraciones;
    
    /**
     * Porcentaje de progreso (0-100)
     */
    @JsonProperty("progreso")
    private Double progreso;
    
    /**
     * Fitness de la mejor solución en esta generación
     */
    @JsonProperty("mejorFitness")
    private Double mejorFitness;
    
    /**
     * Fitness promedio de la población actual
     */
    @JsonProperty("fitnessPromedio")
    private Double fitnessPromedio;
    
    // ===== MEJOR SOLUCIÓN =====
    
    /**
     * Representación simplificada de la mejor solución encontrada
     * Incluye rutas, métricas, etc.
     */
    @JsonProperty("solucion")
    private PlanificacionResponseSimple solucion;
    
    // ===== CONTEXTO TEMPORAL =====
    
    /**
     * Fecha/hora simulada actual (mundo virtual)
     */
    @JsonProperty("fechaSimulada")
    private LocalDateTime fechaSimulada;
    
    /**
     * Timestamp real de cuando se envió este mensaje
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // ===== MÉTRICAS ADICIONALES =====
    
    /**
     * Número de pedidos procesados hasta ahora
     */
    @JsonProperty("pedidosProcesados")
    private Integer pedidosProcesados;
    
    /**
     * Número total de pedidos en la simulación
     */
    @JsonProperty("pedidosTotales")
    private Integer pedidosTotales;
    
    /**
     * Mensaje adicional (opcional)
     */
    @JsonProperty("mensaje")
    private String mensaje;
}
