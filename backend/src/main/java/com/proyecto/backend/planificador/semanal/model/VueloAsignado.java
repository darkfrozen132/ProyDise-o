package com.proyecto.backend.planificador.semanal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa un vuelo asignado a un pedido en su ruta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VueloAsignado {

    // ID del vuelo
    private String vueloId;

    // Origen del vuelo
    private String origen;

    // Destino del vuelo
    private String destino;

    // Salida del vuelo
    private LocalDateTime salida;

    // Llegada del vuelo
    private LocalDateTime llegada;

    // Cantidad asignada en este vuelo
    private int cantidad;
}
