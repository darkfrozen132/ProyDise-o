package com.proyecto.backend.simulation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Snapshot inmutable del estado de una simulación
 * Optimizado para envío por WebSocket (sin sobrecarga de datos)
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
@Builder
public record SimulationSnapshot(
    @JsonProperty("simulationId")
    UUID simulationId,

    @JsonProperty("status")
    SimulationStatus status,

    @JsonProperty("tiempoSimulacionActual")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    LocalDateTime currentSimulationTime,

    @JsonProperty("proximoTiempo")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    LocalDateTime nextSimulationTime,

    @JsonProperty("avanceSimuladoMin")
    Integer advanceMinutes,

    @JsonProperty("ejecucionNumero")
    Integer iterationNumber,

    @JsonProperty("duracionRealMs")
    Long durationMs,

    @JsonProperty("processedCount")
    Integer processedOrders,

    @JsonProperty("totalOrders")
    Integer totalOrders,

    @JsonProperty("currentFitness")
    Double currentFitness,

    @JsonProperty("bestFitness")
    Double bestFitness,

    @JsonProperty("solucion")
    SolutionData solution,

    @JsonProperty("error")
    String errorMessage,

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp
) {

    /**
     * Estado de la simulación
     */
    public enum SimulationStatus {
        INITIALIZING,
        RUNNING,
        PAUSED,
        COMPLETED,
        CANCELLED,
        ERROR
    }

    /**
     * Datos de la solución actual
     */
    @Builder
    public record SolutionData(
        @JsonProperty("vuelos")
        List<SimulationRoute> routes,

        @JsonProperty("metadata")
        Metadata metadata
    ) {}

    /**
     * Ruta simplificada para visualización
     */
    @Builder
    public record SimulationRoute(
        @JsonProperty("id")
        Long id,

        @JsonProperty("codigoVuelo")
        String flightCode,

        @JsonProperty("origen")
        String origin,

        @JsonProperty("destino")
        String destination,

        @JsonProperty("fechaInicial")
        String departureDate,

        @JsonProperty("fechaFinal")
        String arrivalDate,

        @JsonProperty("horaSalida")
        String departureTime,

        @JsonProperty("horaLlegada")
        String arrivalTime,

        @JsonProperty("pedidos")
        List<OrderInfo> orders,

        @JsonProperty("cargaTotal")
        Integer totalLoad,

        @JsonProperty("capacidadMaxima")
        Integer maxCapacity,

        @JsonProperty("utilizacion")
        Double utilization
    ) {}

    /**
     * Información de un pedido asignado
     */
    @Builder
    public record OrderInfo(
        @JsonProperty("idPedido")
        Long orderId,

        @JsonProperty("clienteId")
        String clientId,

        @JsonProperty("aeropuertoDestino")
        String destinationAirport,

        @JsonProperty("cantidadProductos")
        Integer quantity,

        @JsonProperty("fechaLimite")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime deadline
    ) {}

    /**
     * Metadata de la solución
     */
    @Builder
    public record Metadata(
        @JsonProperty("totalVuelos")
        Integer totalFlights,

        @JsonProperty("totalPedidos")
        Integer totalOrders,

        @JsonProperty("fitness")
        Double fitness
    ) {}

    /**
     * Crea un snapshot de error
     */
    public static SimulationSnapshot error(UUID simulationId, String errorMessage) {
        return SimulationSnapshot.builder()
                .simulationId(simulationId)
                .status(SimulationStatus.ERROR)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un snapshot de inicialización
     */
    public static SimulationSnapshot initializing(UUID simulationId, Integer totalOrders) {
        return SimulationSnapshot.builder()
                .simulationId(simulationId)
                .status(SimulationStatus.INITIALIZING)
                .totalOrders(totalOrders)
                .processedOrders(0)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un snapshot de completado
     */
    public static SimulationSnapshot completed(UUID simulationId, Integer totalOrders, Double finalFitness) {
        return SimulationSnapshot.builder()
                .simulationId(simulationId)
                .status(SimulationStatus.COMPLETED)
                .totalOrders(totalOrders)
                .processedOrders(totalOrders)
                .currentFitness(finalFitness)
                .bestFitness(finalFitness)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
