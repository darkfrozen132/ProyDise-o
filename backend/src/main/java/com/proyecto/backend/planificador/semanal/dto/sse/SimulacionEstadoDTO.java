package com.proyecto.backend.planificador.semanal.dto.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO que representa el estado actual de la simulación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionEstadoDTO {
    
    /**
     * Indica si hay una simulación activa
     */
    private boolean activa;
    
    /**
     * Fecha de inicio de la simulación
     */
    private LocalDate fechaInicio;
    
    /**
     * Fecha/hora de inicio de la simulación
     */
    private LocalDateTime inicioSimulacion;
    
    /**
     * Minuto actual de la ventana temporal
     */
    private int minutoActual;
    
    /**
     * Salto en minutos por cada tick
     */
    private int saltoMinutos;
    
    /**
     * Número del tick actual
     */
    private int tickActual;
    
    /**
     * Número total de clientes SSE conectados
     */
    private int clientesConectados;
    
    /**
     * Progreso de la simulación (0.0 a 1.0)
     */
    private double progreso;
    
    /**
     * Límite de minutos para la simulación (normalmente 1440 = 24 horas)
     */
    private int limiteMinutos;
}
