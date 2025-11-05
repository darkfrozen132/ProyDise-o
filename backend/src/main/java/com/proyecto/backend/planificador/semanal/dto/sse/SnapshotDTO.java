package com.proyecto.backend.planificador.semanal.dto.sse;

import com.proyecto.backend.planificador.semanal.model.EstadoPedido;
import com.proyecto.backend.planificador.semanal.model.EstadoVuelo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Snapshot del estado completo de la simulacion
 * Se envia al front cada segundo via SSE
 */
@Data
@Builder
public class SnapshotDTO {

    // Tiempo actual de simulacion (eje de datos)
    private LocalDateTime tiempoSimulacion;

    // Tick actual (contador de segundos)
    private int tick;

    // Factor K
    private int factorK;

    // Aeropuertos con su estado
    private List<AeropuertoSnapshotDTO> aeropuertos;

    // Vuelos activos (en vuelo o proximos)
    private List<VueloSnapshotDTO> vuelosActivos;

    // Pedidos con su estado
    private List<PedidoSnapshotDTO> pedidos;

    // Estadisticas globales
    private EstadisticasDTO estadisticas;

    @Data
    @Builder
    public static class AeropuertoSnapshotDTO {
        private String codigo;
        private String nombre;
        private double latitud;
        private double longitud;
        private int capacidadAlmacen;
        private int ocupacionActual;
        private int pedidosAlmacenados;
    }

    @Data
    @Builder
    public static class VueloSnapshotDTO {
        private String id;
        private String origen;
        private String destino;
        private LocalDateTime salida;
        private LocalDateTime llegada;
        private double progreso; // 0.0 a 1.0
        private EstadoVuelo estado;
        private int capacidadUsada;
        private int capacidadMaxima;
        private List<Long> pedidosAbordo;
    }

    @Data
    @Builder
    public static class PedidoSnapshotDTO {
        private Long id;
        private EstadoPedido estado;
        private String ubicacionActual; // Codigo ICAO o ID de vuelo
        private String destino;
        private double progresoRuta; // 0.0 a 1.0
        private int cantidad;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaEntregaEstimada;
    }

    @Data
    @Builder
    public static class EstadisticasDTO {
        private int totalPedidos;
        private int pedidosPendientes;
        private int pedidosPlanificados;
        private int pedidosEnTransito;
        private int pedidosEntregados;
        private int vuelosActivos;
        private int vuelosProgramados;
        private int vuelosEnVuelo;
        private int vuelosAterrizado;
    }
}
