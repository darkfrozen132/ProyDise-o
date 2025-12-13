package com.proyecto.backend.simulation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitud de simulación DIARIA
 * A diferencia de SimulationRequest, NO requiere fecha porque procesa TODOS los pedidos
 * de la tabla pedidos_diario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySimulationRequest {

    /**
     * Factor K de velocidad de simulación (1-1000)
     * Determina qué tan rápido avanza el tiempo simulado
     */
    @Min(value = 1, message = "El factor K debe ser al menos 1")
    @Max(value = 1000, message = "El factor K no puede ser mayor a 1000")
    @Builder.Default
    private int factorK = 5;

    /**
     * Tamaño de población para el AG (2-100)
     */
    @Min(value = 2, message = "El tamaño de población debe ser al menos 2")
    @Max(value = 100, message = "El tamaño de población no puede ser mayor a 100")
    @Builder.Default
    private int tamanioPoblacion = 3;

    /**
     * Número máximo de generaciones del AG (1-500)
     */
    @Min(value = 1, message = "Debe haber al menos 1 generación")
    @Max(value = 500, message = "No puede haber más de 500 generaciones")
    @Builder.Default
    private int maxGeneraciones = 1;

    /**
     * Límite de generaciones sin mejora antes de parar (1-100)
     */
    @Min(value = 1, message = "Debe haber al menos 1 generación sin mejora")
    @Max(value = 100, message = "No puede ser mayor a 100")
    @Builder.Default
    private int limiteGeneracionesSinMejora = 1;
}
