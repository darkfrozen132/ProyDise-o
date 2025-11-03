package com.proyecto.backend.planificador.semanal.dto.sse;

import com.proyecto.backend.planificador.semanal.dto.response.PlanificacionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para eventos de simulación enviados por SSE
 * 
 * Representa un "tick" de la simulación incremental:
 * - Tick 1: ventana [0-5 min]
 * - Tick 2: ventana [0-10 min]
 * - Tick 3: ventana [0-15 min]
 * ...
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoTickDTO {
    
    /**
     * Número del tick (1, 2, 3, ...)
     */
    private int tick;
    
    /**
     * Minuto actual de la ventana temporal
     * Ejemplo: tick=1, minutoActual=5 → ventana [0-5 min]
     */
    private int minutoActual;
    
    /**
     * Incremento en minutos por cada tick (Sa)
     */
    private int saltoMinutos;
    
    /**
     * Fecha de inicio de la planificación
     */
    private LocalDate fechaInicio;
    
    /**
     * Resultado de la planificación para este tick
     */
    private PlanificacionResponse planificacion;
    
    /**
     * Tiempo de ejecución del tick en milisegundos
     */
    private long tiempoEjecucionMs;
    
    /**
     * Indica si la simulación está completada
     */
    private boolean completada;
    
    /**
     * Progreso de la simulación (0.0 a 1.0)
     */
    private double progreso;
}
