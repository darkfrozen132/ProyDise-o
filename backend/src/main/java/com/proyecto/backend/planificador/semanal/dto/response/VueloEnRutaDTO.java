package com.proyecto.backend.planificador.semanal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un vuelo planificado con su ruta geografica
 * Necesario para animar los aviones en el mapa
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VueloEnRutaDTO {

    // Identificador unico del vuelo: ORIGEN-DESTINO-DIA-HORA
    private String id;

    // Codigo ICAO del aeropuerto origen
    private String originCode;

    // Codigo ICAO del aeropuerto destino
    private String destinationCode;

    // Fecha y hora de salida (UTC)
    private LocalDateTime salida;

    // Fecha y hora de llegada (UTC)
    private LocalDateTime llegada;

    // Lista de pedidos en este vuelo
    private List<OrdenVuelo> orders = new ArrayList<>();

    // Capacidad maxima del vuelo
    private int capacidad;

    // Altitud de crucero (para visualizacion)
    private int altitude;

    // Velocidad en km/h (para animacion)
    private int speed;

    // Region de origen
    private String regionOrigin;

    // Region de destino
    private String regionDestination;

    // Coordenadas geograficas de la ruta
    private RutaGeografica ruta;

    /**
     * Pedido transportado en este vuelo
     * Corresponde al formato del JSON del SSE
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenVuelo {
        private String orderId;   // ID del pedido
        private int cantidad;     // Cantidad de productos
        
        /**
         * Getter legacy para quantity - usa getCantidad() en su lugar
         * @deprecated
         */
        @Deprecated
        public int getQuantity() {
            return cantidad;
        }
        
        /**
         * Setter legacy para quantity - usa setCantidad() en su lugar
         * @deprecated
         */
        @Deprecated
        public void setQuantity(int quantity) {
            this.cantidad = quantity;
        }
    }

    /**
     * Coordenadas geograficas del origen y destino
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RutaGeografica {

        private Coordenadas origin;
        private Coordenadas destination;

        /**
         * Coordenadas geograficas
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Coordenadas {
            private double lat;
            private double lng;
        }
    }

    /**
     * Calcula el total de paquetes en el vuelo
     *
     * @return Total de paquetes sumando todas las ordenes
     */
    public int getTotalPaquetes() {
        return orders.stream().mapToInt(OrdenVuelo::getCantidad).sum();
    }

    /**
     * Calcula el porcentaje de ocupacion del vuelo
     *
     * @return Porcentaje de ocupacion (0-100)
     */
    public double calcularPorcentajeOcupacion() {
        if (capacidad == 0) {
            return 0.0;
        }
        return (getTotalPaquetes() * 100.0) / capacidad;
    }

    /**
     * Verifica si el vuelo esta lleno
     *
     * @return true si esta al 100% de capacidad
     */
    public boolean estaLleno() {
        return getTotalPaquetes() >= capacidad;
    }
}
