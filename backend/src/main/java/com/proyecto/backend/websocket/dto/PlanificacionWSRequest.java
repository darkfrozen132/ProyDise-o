package com.proyecto.backend.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request del cliente WebSocket para iniciar planificación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionWSRequest {
    
    /**
     * Acción a realizar: "iniciar", "pausar", "reanudar", "cancelar"
     */
    private String accion;
    
    /**
     * Fecha base de planificación (solo para "iniciar")
     */
    private LocalDate fecha;
    
    /**
     * Factor K de expansión temporal (solo para "iniciar")
     */
    private Integer factorK;
    
    /**
     * Tamaño de población del AG (opcional, default: 50)
     * Valores más bajos = más rápido pero menos preciso
     */
    private Integer tamanioPoblacion;
    
    /**
     * Número máximo de generaciones (opcional, default: 200)
     * Valores más bajos = más rápido pero menos preciso
     */
    private Integer maxGeneraciones;
    
    /**
     * Límite de generaciones sin mejora (opcional, default: 40)
     * Valores más bajos = converge más rápido
     */
    private Integer limiteGeneracionesSinMejora;
}
