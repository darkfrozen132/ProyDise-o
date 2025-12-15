package com.proyecto.backend.planificador.semanal.model;

/**
 * Estados posibles de un pedido en la simulacion
 */
public enum EstadoPedido {
    /**
     * Pedido cargado pero aun no planificado
     */
    PENDIENTE,

    /**
     * Pedido planificado (tiene ruta asignada) pero aun no ha salido
     */
    PLANIFICADO,

    /**
     * Pedido en vuelo (en transito)
     */
    EN_TRANSITO,

    /**
     * Pedido en almacen esperando conexion
     */
    EN_ALMACEN,

    /**
     * Pedido entregado en destino final
     */
    ENTREGADO
}
