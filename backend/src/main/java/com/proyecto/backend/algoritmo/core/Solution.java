package com.proyecto.backend.algoritmo.core;

import com.proyecto.backend.model.Pedido;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solucion completa de planificacion de rutas
 * Resultado de decodificar un cromosoma del algoritmo genetico
 */
@Data
public class Solution {

    // Pesos de la funcion fitness (configurables)
    public static final double PESO_PEDIDO_A_TIEMPO = 100.0;      // Recompensa por entrega a tiempo
    public static final double PESO_PEDIDO_TARDE = -50.0;         // Penalizacion por entrega tarde
    public static final double PESO_PEDIDO_NO_ENTREGADO = -200.0; // Penalizacion fuerte por no entrega
    public static final double PESO_VIOLACION_CAPACIDAD = -1000.0; // Penalizacion critica

    // Rutas planificadas por pedido
    private Map<Pedido, List<SubRuta>> rutas;

    // Uso de capacidad por vuelo (key: identificador unico del vuelo)
    private Map<String, Integer> capacidadUsada;

    // Metricas de la solucion
    private int pedidosATiempo;
    private int pedidosTarde;
    private int pedidosNoEntregados;
    private int violacionesCapacidad;
    private int holguraPromedioMinutos;

    // Valor de la funcion objetivo (fitness)
    private double objetivo;

    /**
     * Constructor
     */
    public Solution() {
        this.rutas = new HashMap<>();
        this.capacidadUsada = new HashMap<>();
        this.pedidosATiempo = 0;
        this.pedidosTarde = 0;
        this.pedidosNoEntregados = 0;
        this.violacionesCapacidad = 0;
        this.holguraPromedioMinutos = 0;
        this.objetivo = 0.0;
    }

    /**
     * Agrega las subrutas de un pedido
     *
     * @param pedido Pedido
     * @param subrutas Lista de subrutas planificadas
     */
    public void agregarRutas(Pedido pedido, List<SubRuta> subrutas) {
        this.rutas.put(pedido, new ArrayList<>(subrutas));
    }

    /**
     * Obtiene las subrutas de un pedido
     *
     * @param pedido Pedido
     * @return Lista de subrutas o lista vacia si no existe
     */
    public List<SubRuta> getRutas(Pedido pedido) {
        return rutas.getOrDefault(pedido, new ArrayList<>());
    }

    /**
     * Registra el uso de capacidad de un vuelo
     *
     * @param vueloId Identificador del vuelo
     * @param cantidad Cantidad usada
     */
    public void registrarUsoCapacidad(String vueloId, int cantidad) {
        capacidadUsada.merge(vueloId, cantidad, Integer::sum);
    }

    /**
     * Obtiene la cantidad usada de un vuelo
     *
     * @param vueloId Identificador del vuelo
     * @return Cantidad usada
     */
    public int getCapacidadUsada(String vueloId) {
        return capacidadUsada.getOrDefault(vueloId, 0);
    }

    /**
     * Calcula el total de pedidos procesados
     * Suma de todos los estados: a tiempo + tarde + no entregados
     *
     * @return Numero total de pedidos procesados
     */
    public int getTotalPedidos() {
        return pedidosATiempo + pedidosTarde + pedidosNoEntregados;
    }

    /**
     * Calcula el total de pedidos entregados (a tiempo + tarde)
     *
     * @return Numero de pedidos entregados
     */
    public int getPedidosEntregados() {
        return pedidosATiempo + pedidosTarde;
    }

    /**
     * Calcula el porcentaje de pedidos entregados a tiempo
     *
     * @return Porcentaje (0-100)
     */
    public double getPorcentajeATiempo() {
        int entregados = getPedidosEntregados();
        if (entregados == 0) {
            return 0.0;
        }
        return (pedidosATiempo * 100.0) / entregados;
    }

    /**
     * Calcula el porcentaje de completitud
     *
     * @return Porcentaje de pedidos completados (0-100)
     */
    public double getPorcentajeCompletitud() {
        int total = getTotalPedidos();
        if (total == 0) {
            return 0.0;
        }
        return (getPedidosEntregados() * 100.0) / total;
    }

    /**
     * Calcula las metricas de entrega usando el CalculadorPlazos
     * Clasifica cada pedido como: a tiempo, tarde, o no entregado
     *
     * @param calculador Calculador de plazos de entrega
     */
    public void calcularMetricas(CalculadorPlazos calculador) {
        // Reiniciar contadores
        this.pedidosATiempo = 0;
        this.pedidosTarde = 0;
        this.pedidosNoEntregados = 0;

        // Clasificar cada pedido
        for (Map.Entry<Pedido, List<SubRuta>> entry : rutas.entrySet()) {
            Pedido pedido = entry.getKey();
            List<SubRuta> subrutas = entry.getValue();

            EstadoEntrega estado = calculador.calcularEstadoEntrega(pedido, subrutas);

            switch (estado) {
                case ENTREGADO_A_TIEMPO:
                    pedidosATiempo++;
                    break;
                case ENTREGADO_TARDE:
                    pedidosTarde++;
                    break;
                case NO_ENTREGADO:
                    pedidosNoEntregados++;
                    break;
            }
        }
    }

    /**
     * Calcula la funcion fitness (objetivo) de la solucion
     *
     * Formula:
     *   fitness = w1 * pedidos_a_tiempo + w2 * pedidos_tarde + w3 * pedidos_no_entregados + w4 * violaciones
     *
     * Donde:
     *   w1 = +100 (recompensa por entrega a tiempo)
     *   w2 = -50  (penalizacion media por entrega tarde)
     *   w3 = -200 (penalizacion fuerte por no entrega)
     *   w4 = -1000 (penalizacion critica por violacion de capacidad)
     *
     * Objetivo: MAXIMIZAR fitness
     *
     * Ejemplos:
     *   - 10 pedidos a tiempo, 0 tarde, 0 no entregados: fitness = 1000
     *   - 8 a tiempo, 2 tarde, 0 no entregados: fitness = 800 - 100 = 700
     *   - 5 a tiempo, 2 tarde, 3 no entregados: fitness = 500 - 100 - 600 = -200
     *   - 10 a tiempo con 1 violacion: fitness = 1000 - 1000 = 0 (invalida)
     */
    public void calcularFitness() {
        this.objetivo =
            (pedidosATiempo * PESO_PEDIDO_A_TIEMPO) +
            (pedidosTarde * PESO_PEDIDO_TARDE) +
            (pedidosNoEntregados * PESO_PEDIDO_NO_ENTREGADO) +
            (violacionesCapacidad * PESO_VIOLACION_CAPACIDAD);
    }

    /**
     * Calcula metricas y fitness en un solo paso
     *
     * @param calculador Calculador de plazos de entrega
     */
    public void calcularMetricasYFitness(CalculadorPlazos calculador) {
        calcularMetricas(calculador);
        calcularFitness();
    }

    /**
     * Verifica si la solucion es factible (sin violaciones criticas)
     *
     * @return true si no hay violaciones de capacidad
     */
    public boolean esFactible() {
        return violacionesCapacidad == 0;
    }

    /**
     * Genera un resumen de la solucion
     *
     * @return String con metricas principales
     */
    public String getResumen() {
        return String.format(
            "Solution[pedidos=%d, aTiempo=%d, tarde=%d, noEntregados=%d, " +
            "violaciones=%d, objetivo=%.2f, completitud=%.1f%%, puntualidad=%.1f%%]",
            getTotalPedidos(), pedidosATiempo, pedidosTarde, pedidosNoEntregados,
            violacionesCapacidad, objetivo, getPorcentajeCompletitud(), getPorcentajeATiempo()
        );
    }
}
