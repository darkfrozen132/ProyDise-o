package com.proyecto.backend.algoritmo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    // Numero de paquetes en este vuelo
    private int paquetes;

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
     * Calcula el porcentaje de ocupacion del vuelo
     *
     * @return Porcentaje de ocupacion (0-100)
     */
    public double calcularPorcentajeOcupacion() {
        if (capacidad == 0) {
            return 0.0;
        }
        return (paquetes * 100.0) / capacidad;
    }

    /**
     * Verifica si el vuelo esta lleno
     *
     * @return true si esta al 100% de capacidad
     */
    public boolean estaLleno() {
        return paquetes >= capacidad;
    }
}
