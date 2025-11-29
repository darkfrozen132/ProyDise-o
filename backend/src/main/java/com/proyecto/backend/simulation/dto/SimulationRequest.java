package com.proyecto.backend.simulation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO para solicitar el inicio de una simulación
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
@Data
public class SimulationRequest {

    @JsonProperty("accion")
    private String action; // Opcional: solo para compatibilidad con frontend legacy

    @NotNull(message = "La fecha de inicio es obligatoria")
    @JsonProperty("fecha")
    private LocalDate startDate;

    /**
     * Hora de inicio de la simulación (formato HH:mm)
     * Si no se especifica, se usa 00:00 por defecto
     */
    @JsonFormat(pattern = "HH:mm")
    @JsonProperty("hora")
    private LocalTime startTime = LocalTime.of(0, 0);

    @NotNull(message = "El factor K es obligatorio")
    @Min(value = 1, message = "El factor K debe ser al menos 1")
    @JsonProperty("factorK")
    private Integer factorK;

    // Parámetros opcionales del Algoritmo Genético
    @Min(value = 5, message = "El tamaño de población debe ser al menos 5")
    @JsonProperty("tamanioPoblacion")
    private Integer populationSize = 20;

    @Min(value = 5, message = "El máximo de generaciones debe ser al menos 5")
    @JsonProperty("maxGeneraciones")
    private Integer maxGenerations = 20;

    @Min(value = 1, message = "El límite de generaciones sin mejora debe ser al menos 1")
    @JsonProperty("limiteGeneracionesSinMejora")
    private Integer stagnationLimit = 10;

    /**
     * Valida que la acción sea "iniciar"
     */
    public boolean isStartAction() {
        return "iniciar".equalsIgnoreCase(action);
    }

    /**
     * Valida que la acción sea "cancelar"
     */
    public boolean isCancelAction() {
        return "cancelar".equalsIgnoreCase(action);
    }

    /**
     * Valida que la acción sea "pausar"
     */
    public boolean isPauseAction() {
        return "pausar".equalsIgnoreCase(action);
    }

    /**
     * Valida que la acción sea "reanudar"
     */
    public boolean isResumeAction() {
        return "reanudar".equalsIgnoreCase(action);
    }

    /**
     * Obtiene la fecha y hora de inicio combinadas
     * Si no se especificó hora, usa 00:00
     * 
     * @return LocalDateTime con fecha + hora de inicio
     */
    public LocalDateTime getStartDateTime() {
        LocalTime time = (startTime != null) ? startTime : LocalTime.of(0, 0);
        return LocalDateTime.of(startDate, time);
    }
}
