package com.proyecto.backend.algoritmo.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Subruta que mueve un bloque de productos desde un hub hasta el destino
 * Un pedido puede dividirse en multiples subrutas desde diferentes hubs
 */
@Data
public class SubRuta {

    // Hub de origen (SPIM, EBCI, UBBB)
    private String hubOrigen;

    // Secuencia de vuelos que componen la subruta
    private List<VueloUso> vuelos;

    // Cantidad de productos que viajan en esta subruta
    private int cantidad;

    // Minuto UTC de llegada al destino final
    private int llegadaUTC;

    /**
     * Constructor
     */
    public SubRuta() {
        this.vuelos = new ArrayList<>();
    }

    /**
     * Constructor completo
     *
     * @param hubOrigen Hub de origen
     * @param cantidad Cantidad de productos
     */
    public SubRuta(String hubOrigen, int cantidad) {
        this.hubOrigen = hubOrigen;
        this.cantidad = cantidad;
        this.vuelos = new ArrayList<>();
    }

    /**
     * Agrega un vuelo a la subruta
     *
     * @param vuelo Vuelo a agregar
     */
    public void agregarVuelo(VueloUso vuelo) {
        this.vuelos.add(vuelo);
        // Actualizar llegada con el ultimo vuelo
        this.llegadaUTC = vuelo.getLlegadaUTC();
    }

    /**
     * Obtiene el numero de escalas (vuelos - 1)
     *
     * @return Numero de escalas
     */
    public int getNumeroEscalas() {
        return Math.max(0, vuelos.size() - 1);
    }

    /**
     * Obtiene el aeropuerto destino final
     *
     * @return Codigo ICAO del destino
     */
    public String getDestinoFinal() {
        if (vuelos.isEmpty()) {
            return null;
        }
        return vuelos.get(vuelos.size() - 1).getDestino();
    }

    /**
     * Verifica si la subruta es directa (sin escalas)
     *
     * @return true si es vuelo directo
     */
    public boolean esDirecto() {
        return vuelos.size() == 1;
    }
}
