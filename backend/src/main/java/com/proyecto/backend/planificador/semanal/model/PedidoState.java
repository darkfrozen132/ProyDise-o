package com.proyecto.backend.planificador.semanal.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado de un pedido en la simulacion
 */
@Data
public class PedidoState {

    // ID del pedido
    private Long id;

    // Estado actual del pedido
    private EstadoPedido estado;

    // Ruta planificada (lista de vuelos asignados)
    private List<VueloAsignado> rutaPlanificada;

    // Fecha de creacion del pedido
    private LocalDateTime fechaCreacion;

    // Fecha estimada de entrega
    private LocalDateTime fechaEntregaEstimada;

    // Fecha real de entrega (cuando se entrega)
    private LocalDateTime fechaEntregaReal;

    // Ubicacion actual: codigo ICAO del aeropuerto o ID del vuelo
    private String ubicacionActual;

    // Destino del pedido
    private String destino;

    // Cantidad de productos
    private int cantidad;

    // Progreso en la ruta (0.0 a 1.0)
    private double progresoRuta;

    /**
     * Constructor
     */
    public PedidoState() {
        this.estado = EstadoPedido.PENDIENTE;
        this.rutaPlanificada = new ArrayList<>();
        this.progresoRuta = 0.0;
    }

    /**
     * Verifica si el pedido esta en transito (en vuelo o en almacen)
     */
    public boolean estaEnTransito() {
        return estado == EstadoPedido.EN_TRANSITO || estado == EstadoPedido.EN_ALMACEN;
    }

    /**
     * Verifica si el pedido fue entregado
     */
    public boolean fueEntregado() {
        return estado == EstadoPedido.ENTREGADO;
    }
}
