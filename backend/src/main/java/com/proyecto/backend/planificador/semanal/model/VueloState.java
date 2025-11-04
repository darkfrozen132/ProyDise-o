package com.proyecto.backend.planificador.semanal.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado de un vuelo en la simulacion
 */
@Data
public class VueloState {

    // ID del vuelo: ORIGEN-DESTINO-YYYYMMDD-HHMM
    private String id;

    // Aeropuerto de origen
    private String origen;

    // Aeropuerto de destino
    private String destino;

    // Fecha/hora de salida
    private LocalDateTime salida;

    // Fecha/hora de llegada
    private LocalDateTime llegada;

    // Capacidad maxima
    private int capacidadMaxima;

    // Capacidad usada
    private int capacidadUsada;

    // Estado del vuelo
    private EstadoVuelo estado;

    // Pedidos a bordo (IDs)
    private List<Long> pedidosAbordo;

    // Progreso del vuelo (0.0 a 1.0) para animacion
    private double progreso;

    /**
     * Constructor
     */
    public VueloState() {
        this.estado = EstadoVuelo.PROGRAMADO;
        this.pedidosAbordo = new ArrayList<>();
        this.capacidadUsada = 0;
        this.progreso = 0.0;
    }

    /**
     * Obtiene la capacidad restante
     */
    public int getCapacidadRestante() {
        return capacidadMaxima - capacidadUsada;
    }

    /**
     * Verifica si hay capacidad disponible
     */
    public boolean tieneCapacidad(int cantidad) {
        return getCapacidadRestante() >= cantidad;
    }
}
