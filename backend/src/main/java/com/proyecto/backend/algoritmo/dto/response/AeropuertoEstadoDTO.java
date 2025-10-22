package com.proyecto.backend.algoritmo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado de un aeropuerto con su ocupacion a lo largo del tiempo
 * Necesario para visualizacion en el mapa
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AeropuertoEstadoDTO {

    // Codigo ICAO del aeropuerto
    private String code;

    // Coordenadas geograficas
    private double lat;
    private double lng;

    // Nombre completo del aeropuerto
    private String name;

    // Region/continente
    private String region;

    // Pais
    private String country;

    // Es sede principal (stock ilimitado)
    private boolean isSede;

    // Capacidad del almacen ("ILIMITADO" para sedes o numero)
    private String capacity;

    // Ocupacion actual (numero de paquetes)
    private int packages;

    // Ocupacion del almacen a lo largo del tiempo
    private List<OcupacionPorTiempo> ocupacionPorMinuto = new ArrayList<>();

    /**
     * Snapshot de ocupacion en un momento especifico
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OcupacionPorTiempo {

        // Minuto UTC desde el inicio de la simulacion
        private int minutoUTC;

        // Numero de paquetes en el almacen
        private int paquetes;

        // Porcentaje de ocupacion (0-100)
        private double porcentajeOcupacion;
    }

    /**
     * Agrega un snapshot de ocupacion
     *
     * @param minutoUTC Minuto UTC desde inicio
     * @param paquetes Numero de paquetes
     * @param capacidadMaxima Capacidad maxima del almacen
     */
    public void agregarOcupacion(int minutoUTC, int paquetes, int capacidadMaxima) {
        double porcentaje = capacidadMaxima > 0 ? (paquetes * 100.0) / capacidadMaxima : 0.0;
        ocupacionPorMinuto.add(new OcupacionPorTiempo(minutoUTC, paquetes, porcentaje));
    }
}
