package com.proyecto.backend.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para operación diaria (síncrona)
 * Contiene el resultado del procesamiento de todos los pedidos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyOperationResponse {

    /**
     * Si la operación fue exitosa
     */
    private boolean success;

    /**
     * Mensaje descriptivo
     */
    private String mensaje;

    /**
     * Total de pedidos en base de datos
     */
    private int totalPedidos;

    /**
     * Pedidos válidos (con fecha >= ahora)
     */
    private int pedidosValidos;

    /**
     * Pedidos descartados por fecha pasada
     */
    private int pedidosDescartados;

    /**
     * Pedidos que fueron asignados a rutas
     */
    private int pedidosAsignados;

    /**
     * Pedidos sin ruta (no se pudo asignar)
     */
    private int pedidosSinRuta;

    /**
     * Total de vuelos/rutas generados
     */
    private int totalVuelos;

    /**
     * Fitness de la mejor solución encontrada
     */
    private double mejorFitness;

    /**
     * Tiempo de procesamiento en milisegundos
     */
    private long tiempoProcesamiento;

    /**
     * Hora del servidor usada como referencia para filtrar pedidos
     */
    private LocalDateTime horaServidorUsada;

    /**
     * Fecha/hora de inicio de los pedidos
     */
    private LocalDateTime fechaInicioPedidos;

    /**
     * Fecha/hora final de los pedidos
     */
    private LocalDateTime fechaFinPedidos;

    /**
     * Lista de rutas generadas (resumen)
     */
    private List<RutaResumen> rutas;

    /**
     * Resumen de una ruta generada
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RutaResumen {
        private Long pedidoId;
        private String clienteId;
        private String destino;
        private int cantidadProductos;
        private List<String> aeropuertosRuta; // ej: ["SPIM", "SCEL", "SBBR"]
        private int totalTramos;
    }
}
