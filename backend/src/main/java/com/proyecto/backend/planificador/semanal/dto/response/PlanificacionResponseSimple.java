package com.proyecto.backend.planificador.semanal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response simplificado de la planificacion de rutas
 * Contiene unicamente la lista de vuelos planificados con sus pedidos asignados
 *
 * Estructura:
 * {
 *   "vuelos": [
 *     {
 *       "fechaInicial": "2025-01-15 08:30",
 *       "fechaFinal": "2025-01-15 14:45",
 *       "origenCodigoICAO": "SPIM",
 *       "destinoCodigoICAO": "KJFK",
 *       "pedidos": [
 *         {"idPedido": 123, "cantidad": 50},
 *         {"idPedido": 456, "cantidad": 30}
 *       ]
 *     }
 *   ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionResponseSimple {

    /**
     * Lista de vuelos planificados con sus pedidos asignados
     */
    private List<VueloSimplificadoDTO> vuelos = new ArrayList<>();

    /**
     * Constructor de conveniencia para inicializar con una lista de vuelos
     *
     * @param vuelos Lista inicial de vuelos
     */
    public static PlanificacionResponseSimple conVuelos(List<VueloSimplificadoDTO> vuelos) {
        return new PlanificacionResponseSimple(vuelos);
    }

    /**
     * Agrega un vuelo a la lista
     *
     * @param vuelo Vuelo a agregar
     */
    public void agregarVuelo(VueloSimplificadoDTO vuelo) {
        vuelos.add(vuelo);
    }

    /**
     * Obtiene el numero total de vuelos planificados
     *
     * @return Numero de vuelos
     */
    public int getTotalVuelos() {
        return vuelos.size();
    }

    /**
     * Obtiene el numero total de pedidos asignados a todos los vuelos
     *
     * @return Numero total de pedidos
     */
    public int getTotalPedidos() {
        return vuelos.stream()
                .mapToInt(v -> v.getPedidos().size())
                .sum();
    }
}
