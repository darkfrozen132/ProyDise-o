package com.proyecto.backend.planificador.semanal.dto.sse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para iniciar una simulación incremental
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IniciarSimulacionRequest {
    
    /**
     * Fecha de inicio de la planificación
     */
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    
    /**
     * Incremento en minutos por cada tick (Sa)
     * Por defecto: 5 minutos
     */
    @Min(value = 1, message = "El salto debe ser al menos 1 minuto")
    @Builder.Default
    private int saltoMinutos = 5;
    
    /**
     * Tamaño de la población del algoritmo genético
     */
    @Min(value = 10, message = "La población debe ser al menos 10")
    @Builder.Default
    private int tamanioPoblacion = 50;
    
    /**
     * Máximo de generaciones del algoritmo genético
     */
    @Min(value = 10, message = "Las generaciones deben ser al menos 10")
    @Builder.Default
    private int maxGeneraciones = 200;
}
