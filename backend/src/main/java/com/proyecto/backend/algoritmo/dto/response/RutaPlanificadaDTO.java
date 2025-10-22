package com.proyecto.backend.algoritmo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ruta completa planificada para un pedido
 * Muestra como se mueven los productos desde el hub hasta el destino
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaPlanificadaDTO {

    // ID del pedido en la base de datos
    private Long pedidoId;

    // ID del cliente
    private String clienteId;

    // Codigo ICAO del aeropuerto destino
    private String destino;

    // Cantidad total de productos del pedido
    private int cantidad;

    // Estado final del pedido
    private EstadoPedido estado;

    // Fecha y hora de registro del pedido
    private LocalDateTime fechaPedido;

    // Fecha y hora limite de entrega
    private LocalDateTime fechaLimite;

    // Subrutas que componen la ruta completa
    private List<SubrutaDTO> subrutas = new ArrayList<>();

    /**
     * Estados posibles del pedido
     */
    public enum EstadoPedido {
        ENTREGADO_A_TIEMPO,
        ENTREGADO_TARDE,
        NO_ENTREGADO,
        EN_PROCESO
    }

    /**
     * Subruta desde un hub especifico
     * Un pedido puede tener multiples subrutas si se divide en varios envios
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubrutaDTO {

        // Hub de origen (SPIM, EBCI, UBBB)
        private String hub;

        // Cantidad de productos en esta subruta
        private int cantidad;

        // Fecha y hora de llegada al destino
        private LocalDateTime llegada;

        // Secuencia de vuelos (IDs de vuelos)
        private List<String> vuelos = new ArrayList<>();

        // Escalas realizadas (codigos ICAO de aeropuertos intermedios)
        private List<String> escalas = new ArrayList<>();
    }

    /**
     * Calcula el tiempo total de la ruta mas larga
     *
     * @return Duracion en minutos desde el pedido hasta la ultima llegada
     */
    public long calcularTiempoTotalMinutos() {
        if (subrutas.isEmpty() || fechaPedido == null) {
            return 0;
        }

        LocalDateTime ultimaLlegada = subrutas.stream()
                .map(SubrutaDTO::getLlegada)
                .max(LocalDateTime::compareTo)
                .orElse(fechaPedido);

        return java.time.Duration.between(fechaPedido, ultimaLlegada).toMinutes();
    }

    /**
     * Verifica si el pedido fue entregado completo
     *
     * @return true si la suma de cantidades de subrutas == cantidad total
     */
    public boolean esEntregaCompleta() {
        int totalEntregado = subrutas.stream()
                .mapToInt(SubrutaDTO::getCantidad)
                .sum();
        return totalEntregado >= cantidad;
    }
}
