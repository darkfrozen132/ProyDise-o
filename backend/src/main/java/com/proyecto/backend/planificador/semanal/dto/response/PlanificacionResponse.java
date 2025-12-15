package com.proyecto.backend.planificador.semanal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response completo de la planificacion de rutas
 * Contiene toda la informacion necesaria para la visualizacion en el mapa
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionResponse {

    // Metadata de la ejecucion
    private MetadataPlanificacion metadata;

    // Pedidos procesados en este batch (para verificacion)
    private List<PedidoResumenDTO> pedidosProcesados = new ArrayList<>();

    // Estado de aeropuertos con ocupacion
    private List<AeropuertoEstadoDTO> aeropuertos = new ArrayList<>();

    // Vuelos planificados con sus rutas
    private List<VueloEnRutaDTO> vuelos = new ArrayList<>();

    // Rutas asignadas por pedido
    private List<RutaPlanificadaDTO> rutas = new ArrayList<>();

    /**
     * Resumen de un pedido para verificacion
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidoResumenDTO {
        private Long id;
        private String fecha;  // formato: yyyy-MM-dd HH:mm
        private String destino;
        private int cantidad;
        private String clienteId;
        private String estado;
    }

    /**
     * Metadata de la planificacion ejecutada
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetadataPlanificacion {

        // Fecha y hora de inicio del rango de pedidos
        private LocalDateTime fechaInicio;

        // Fecha y hora de fin del rango de pedidos
        private LocalDateTime fechaFin;

        // Rango en formato legible
        private String rangoDescripcion;

        // Factor K utilizado
        private int factorK;

        // Salto de consumo en minutos (Sc = K * Sa)
        private int saltoConsumoMinutos;

        // Salto del algoritmo en minutos (Sa)
        private int saltoAlgoritmoMinutos;

        // Pedidos procesados
        private int pedidosProcesados;

        // Pedidos entregados a tiempo
        private int pedidosATiempo;

        // Pedidos entregados tarde
        private int pedidosTarde;

        // Pedidos no entregados
        private int pedidosNoEntregados;

        // Valor de la funcion objetivo
        private double objetivo;

        // Tiempo de ejecucion del algoritmo en milisegundos
        private long tiempoEjecucionMs;

        // Numero de generaciones ejecutadas
        private int generacionesEjecutadas;

        /**
         * Calcula el porcentaje de pedidos entregados a tiempo
         *
         * @return Porcentaje de entregas a tiempo
         */
        public double calcularPorcentajeATiempo() {
            int totalEntregados = pedidosATiempo + pedidosTarde;
            if (totalEntregados == 0) {
                return 0.0;
            }
            return (pedidosATiempo * 100.0) / totalEntregados;
        }

        /**
         * Calcula el porcentaje de pedidos completados
         *
         * @return Porcentaje de pedidos completados
         */
        public double calcularPorcentajeCompletado() {
            if (pedidosProcesados == 0) {
                return 0.0;
            }
            int totalEntregados = pedidosATiempo + pedidosTarde;
            return (totalEntregados * 100.0) / pedidosProcesados;
        }
    }
}
