package com.proyecto.backend.planificador.semanal.model;

/**
 * Estados posibles de entrega de un pedido
 */
public enum EstadoEntrega {
    /**
     * Pedido entregado dentro del plazo establecido
     */
    ENTREGADO_A_TIEMPO,

    /**
     * Pedido entregado pero fuera del plazo
     */
    ENTREGADO_TARDE,

    /**
     * Pedido no entregado (sin ruta asignada)
     */
    NO_ENTREGADO
}
