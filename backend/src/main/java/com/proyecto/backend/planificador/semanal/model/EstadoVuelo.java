package com.proyecto.backend.planificador.semanal.model;

/**
 * Estados posibles de un vuelo en la simulacion
 */
public enum EstadoVuelo {
    /**
     * Vuelo programado, aun no ha despegado
     */
    PROGRAMADO,

    /**
     * Vuelo en el aire
     */
    EN_VUELO,

    /**
     * Vuelo aterrizado
     */
    ATERRIZADO,

    /**
     * Vuelo cancelado
     */
    CANCELADO
}
