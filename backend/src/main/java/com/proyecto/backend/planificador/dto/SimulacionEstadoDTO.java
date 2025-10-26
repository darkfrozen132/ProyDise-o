package com.proyecto.backend.planificador.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO para el estado de la simulación en tiempo real
 * Incluye tiempos y rutas de solución activas
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
    
    // ⭐ RUTAS DE SOLUCIÓN - Para graficar en el mapa
    private List<Map<String, Object>> rutasSolucion;  // Rutas activas en este momento
}
