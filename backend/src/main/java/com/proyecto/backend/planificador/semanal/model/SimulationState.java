package com.proyecto.backend.planificador.semanal.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado global de la simulacion en tiempo real
 *
 * Mantiene el estado mutable de:
 * - Pedidos (pendientes, planificados, en transito, entregados)
 * - Vuelos (capacidad usada, estado)
 * - Almacenes (ocupacion actual)
 *
 * Thread-safe: Usa ConcurrentHashMap para acceso concurrente
 */
@Data
@Slf4j
public class SimulationState {

    // Tiempo actual en el eje de datos (simulacion)
    private volatile LocalDateTime tiempoSimulacion;

    // Tiempo real de inicio de la simulacion
    private LocalDateTime tiempoRealInicio;

    // Factor K de ampliacion temporal
    private int factorK;

    // Estados de pedidos: Map<pedidoId, PedidoState>
    private Map<Long, PedidoState> pedidos;

    // Estados de vuelos: Map<vueloId, VueloState>
    private Map<String, VueloState> vuelos;

    // Estados de almacenes: Map<codigoICAO, AlmacenState>
    private Map<String, AlmacenState> almacenes;

    // Parametros del algoritmo genetico (ajustables dinamicamente)
    private ParametrosAG parametrosAG;

    // Alertas y metricas
    private boolean alertaRendimiento;
    private int tickActual;

    /**
     * Constructor
     */
    public SimulationState() {
        this.pedidos = new ConcurrentHashMap<>();
        this.vuelos = new ConcurrentHashMap<>();
        this.almacenes = new ConcurrentHashMap<>();
        this.parametrosAG = new ParametrosAG();
        this.alertaRendimiento = false;
        this.tickActual = 0;
    }

    /**
     * Inicializa el estado con la fecha base
     */
    public void inicializar(LocalDateTime fechaBase, int factorK) {
        this.tiempoSimulacion = fechaBase;
        this.tiempoRealInicio = LocalDateTime.now();
        this.factorK = factorK;

        log.info("SimulationState inicializado: tiempoBase={}, K={}", fechaBase, factorK);
    }

    /**
     * Obtiene la capacidad restante de un vuelo
     */
    public int getCapacidadRestanteVuelo(String vueloId, int capacidadMaxima) {
        VueloState vuelo = vuelos.get(vueloId);
        if (vuelo == null) {
            return capacidadMaxima;
        }
        return capacidadMaxima - vuelo.getCapacidadUsada();
    }

    /**
     * Reserva capacidad en un vuelo
     */
    public synchronized boolean reservarVuelo(String vueloId, int cantidad, int capacidadMaxima) {
        VueloState vuelo = vuelos.computeIfAbsent(vueloId, k -> {
            VueloState v = new VueloState();
            v.setId(vueloId);
            v.setCapacidadMaxima(capacidadMaxima);
            v.setCapacidadUsada(0);
            return v;
        });

        if (vuelo.getCapacidadUsada() + cantidad > capacidadMaxima) {
            return false;
        }

        vuelo.setCapacidadUsada(vuelo.getCapacidadUsada() + cantidad);
        return true;
    }

    /**
     * Obtiene el numero de pedidos por estado
     */
    public long contarPedidosPorEstado(EstadoPedido estado) {
        return pedidos.values().stream()
            .filter(p -> p.getEstado() == estado)
            .count();
    }

    /**
     * Parametros del algoritmo genetico
     */
    @Data
    public static class ParametrosAG {
        private int tamanioPoblacion = 20;
        private int maxGeneraciones = 20;
        private int limiteGeneracionesSinMejora = 10;
        private int eliteK = 4;
        private double probabilidadCruce = 0.8;
        private double probabilidadMutacion = 0.05;
    }
}
