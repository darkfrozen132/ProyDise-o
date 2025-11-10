package com.proyecto.backend.planificador.semanal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un vuelo planificado en formato simplificado
 * Incluye fechas de salida y llegada, aeropuertos origen/destino y pedidos asignados
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VueloSimplificadoDTO {

    /**
     * Fecha y hora de salida del vuelo
     * Formato: yyyy-MM-dd HH:mm
     * Ejemplo: "2025-01-15 08:30"
     */
    private String fechaInicial;

    /**
     * Fecha y hora de llegada del vuelo
     * Formato: yyyy-MM-dd HH:mm
     * Ejemplo: "2025-01-15 14:45"
     */
    private String fechaFinal;

    /**
     * Codigo ICAO del aeropuerto de origen
     * Ejemplo: "SPIM" (Lima, Peru)
     */
    private String origenCodigoICAO;

    /**
     * Codigo ICAO del aeropuerto de destino
     * Ejemplo: "KJFK" (New York, USA)
     */
    private String destinoCodigoICAO;

    /**
     * Lista de pedidos asignados a este vuelo
     */
    private List<PedidoEnVueloDTO> pedidos = new ArrayList<>();

    /**
     * Agrega un pedido a la lista de pedidos del vuelo
     *
     * @param idPedido Identificador del pedido
     * @param cantidad Cantidad de productos
     */
    public void agregarPedido(Long idPedido, Integer cantidad) {
        pedidos.add(new PedidoEnVueloDTO(idPedido, cantidad));
    }

    /**
     * Calcula el total de paquetes en el vuelo
     *
     * @return Total de paquetes sumando todos los pedidos
     */
    public int getTotalPaquetes() {
        return pedidos.stream()
                .mapToInt(p -> p.getCantidad() != null ? p.getCantidad() : 0)
                .sum();
    }
}
