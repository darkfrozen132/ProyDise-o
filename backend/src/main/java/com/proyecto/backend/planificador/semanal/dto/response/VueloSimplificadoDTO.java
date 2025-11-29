package com.proyecto.backend.planificador.semanal.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un vuelo planificado en formato simplificado
 * Incluye fechas de salida y llegada, aeropuertos origen/destino y pedidos asignados
 * 
 * ACTUALIZADO: Ahora incluye campos UTC para visualización en mapas frontend
 */
@Data
@Builder
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
    @Builder.Default
    private List<PedidoEnVueloDTO> pedidos = new ArrayList<>();
    
    // ========== NUEVOS CAMPOS PARA FRONTEND ==========
    
    /**
     * Identificador único del vuelo
     * Formato: {ORIGEN}-{DESTINO}-{HORA}
     * Ejemplo: "LIM-MIA-0800"
     */
    @JsonProperty("flightId")
    private String flightId;
    
    /**
     * Fecha/hora de despegue en formato UTC ISO-8601
     * Ejemplo: "2025-01-15T13:00:00Z"
     */
    @JsonProperty("departureUtc")
    private String departureUtc;
    
    /**
     * Fecha/hora de aterrizaje en formato UTC ISO-8601
     * Ejemplo: "2025-01-15T19:30:00Z"
     */
    @JsonProperty("arrivalUtc")
    private String arrivalUtc;
    
    /**
     * Cantidad total de paquetes en el vuelo
     */
    @JsonProperty("quantity")
    private Integer quantity;
    
    /**
     * Holgura de tiempo en minutos
     * - Positivo: Vuelo llega a tiempo (minutos de margen)
     * - Negativo o 0: Vuelo retrasado
     */
    @JsonProperty("slackMinutes")
    private Integer slackMinutes;

    /**
     * Agrega un pedido a la lista de pedidos del vuelo
     *
     * @param idPedido Identificador del pedido
     * @param cantidad Cantidad de productos
     */
    public void agregarPedido(Long idPedido, Integer cantidad) {
        if (pedidos == null) {
            pedidos = new ArrayList<>();
        }
        pedidos.add(new PedidoEnVueloDTO(idPedido, cantidad));
    }

    /**
     * Calcula el total de paquetes en el vuelo
     *
     * @return Total de paquetes sumando todos los pedidos
     */
    public int getTotalPaquetes() {
        if (pedidos == null || pedidos.isEmpty()) {
            return quantity != null ? quantity : 0;
        }
        return pedidos.stream()
                .mapToInt(p -> p.getCantidad() != null ? p.getCantidad() : 0)
                .sum();
    }
}
