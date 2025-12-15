package com.proyecto.backend.planificador.semanal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un pedido asignado a un vuelo
 * Usado en el response simplificado de planificacion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEnVueloDTO {

    /**
     * Identificador del pedido
     */
    private Long idPedido;

    /**
     * Cantidad de productos del pedido asignados a este vuelo
     */
    private Integer cantidad;
}
