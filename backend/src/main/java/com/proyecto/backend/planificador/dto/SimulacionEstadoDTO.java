package com.proyecto.backend.planificador.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para el estado de la simulación en tiempo real
 * Simplificado: solo devuelve tiempos (simulado y real)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionEstadoDTO {
    
    // ⭐ TIEMPOS - Lo más importante
    private LocalDateTime horaSimulada;          // Hora en la simulación
    private long tiempoRealTranscurridoMs;       // Milisegundos reales transcurridos
    
    // Estado básico
    private boolean activa;
    private int tickActual;
    private String estadoDescripcion;
    
    // Configuración (útil para el frontend)
    private double timeScale; // Horas simuladas por segundo real
}
