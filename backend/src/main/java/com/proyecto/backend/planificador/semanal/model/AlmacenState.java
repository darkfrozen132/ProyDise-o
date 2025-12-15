package com.proyecto.backend.planificador.semanal.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado de un almacen en la simulacion
 */
@Data
public class AlmacenState {

    // Codigo ICAO del aeropuerto
    private String codigoICAO;

    // Capacidad maxima del almacen (0 = ilimitado para hubs)
    private int capacidadMaxima;

    // Ocupacion actual
    private int ocupacionActual;

    // Pedidos actualmente almacenados (IDs)
    private List<Long> pedidosAlmacenados;

    /**
     * Constructor
     */
    public AlmacenState() {
        this.pedidosAlmacenados = new ArrayList<>();
        this.ocupacionActual = 0;
    }

    /**
     * Obtiene el espacio disponible
     */
    public int getEspacioDisponible() {
        if (capacidadMaxima == 0) {
            return Integer.MAX_VALUE; // Ilimitado
        }
        return capacidadMaxima - ocupacionActual;
    }

    /**
     * Verifica si hay espacio disponible
     */
    public boolean tieneEspacio(int cantidad) {
        return getEspacioDisponible() >= cantidad;
    }
}
