package com.proyecto.backend.algoritmo.core;

import com.proyecto.backend.model.Pedido;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Decodificador genetico para el algoritmo genetico
 *
 * Interpreta un cromosoma como prioridades de pedidos y genera una solucion
 * mediante asignacion greedy en orden de prioridad.
 *
 * Cromosoma:
 *   - Cada gen representa la prioridad de un pedido
 *   - Valores en rango [0, 1]
 *   - Mayor valor = mayor prioridad (se procesa primero)
 *
 * Proceso de decodificacion:
 *   1. Ordenar pedidos segun prioridades del cromosoma (descendente)
 *   2. Para cada pedido (en orden):
 *      - Buscar mejor ruta disponible (BFS)
 *      - Asignar capacidades de vuelos y almacenes
 *   3. Calcular metricas y fitness
 *   4. Retornar solucion
 */
@Slf4j
public class DecodificadorGenetico {

    private final WorldTemporal worldTemporal;
    private final ControladorAlmacenes controladorAlmacenes;
    private final CalculadorPlazos calculadorPlazos;
    private final BuscadorRutas buscadorRutas;

    /**
     * Constructor
     *
     * @param worldTemporal World temporal con vuelos expandidos
     * @param controladorAlmacenes Controlador de capacidad de almacenes
     */
    public DecodificadorGenetico(WorldTemporal worldTemporal, ControladorAlmacenes controladorAlmacenes) {
        this.worldTemporal = worldTemporal;
        this.controladorAlmacenes = controladorAlmacenes;
        this.calculadorPlazos = new CalculadorPlazos(worldTemporal);
        this.buscadorRutas = new BuscadorRutas(worldTemporal, controladorAlmacenes);
    }

    /**
     * Decodifica un cromosoma en una solucion
     *
     * @param cromosoma Cromosoma con prioridades de pedidos
     * @param pedidos Lista de pedidos a planificar
     * @return Solucion generada
     */
    public Solution decodificar(Chromosome cromosoma, List<Pedido> pedidos) {
        // Validar que el cromosoma tenga el numero correcto de genes
        if (cromosoma.getNumGenes() != pedidos.size()) {
            throw new IllegalArgumentException(
                String.format("Cromosoma tiene %d genes pero hay %d pedidos",
                    cromosoma.getNumGenes(), pedidos.size())
            );
        }

        Solution solucion = new Solution();

        log.debug("Decodificando cromosoma para {} pedidos", pedidos.size());

        // Ordenar pedidos segun prioridades del cromosoma (mayor prioridad primero)
        List<PedidoConPrioridad> pedidosOrdenados = ordenarPedidosPorPrioridad(cromosoma, pedidos);

        int rutasGeneradas = 0;
        int rutasFallidas = 0;

        // Procesar pedidos en orden de prioridad
        for (PedidoConPrioridad pedidoPriorizado : pedidosOrdenados) {
            Pedido pedido = pedidoPriorizado.pedido;

            try {
                List<SubRuta> subrutas = generarRutasPedido(pedido);

                if (!subrutas.isEmpty()) {
                    solucion.agregarRutas(pedido, subrutas);
                    rutasGeneradas++;
                } else {
                    log.debug("No se pudo generar ruta para pedido {} (prioridad: {:.3f})",
                            pedido.getId(), pedidoPriorizado.prioridad);
                    rutasFallidas++;
                    solucion.agregarRutas(pedido, new ArrayList<>());
                }

            } catch (Exception e) {
                log.error("Error generando ruta para pedido {}", pedido.getId(), e);
                rutasFallidas++;
                solucion.agregarRutas(pedido, new ArrayList<>());
            }
        }

        log.debug("Decodificacion completada: {} rutas generadas, {} fallidas",
                rutasGeneradas, rutasFallidas);

        // Calcular metricas y fitness
        solucion.calcularMetricasYFitness(calculadorPlazos);

        return solucion;
    }

    /**
     * Ordena pedidos segun prioridades del cromosoma
     *
     * @param cromosoma Cromosoma con prioridades
     * @param pedidos Lista de pedidos
     * @return Lista de pedidos ordenados por prioridad (descendente)
     */
    private List<PedidoConPrioridad> ordenarPedidosPorPrioridad(Chromosome cromosoma, List<Pedido> pedidos) {
        return IntStream.range(0, pedidos.size())
            .mapToObj(i -> new PedidoConPrioridad(pedidos.get(i), cromosoma.getGen(i)))
            .sorted(Comparator.comparingDouble((PedidoConPrioridad p) -> p.prioridad).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Genera rutas para un pedido especifico
     *
     * @param pedido Pedido a procesar
     * @return Lista de subrutas (normalmente 1)
     */
    private List<SubRuta> generarRutasPedido(Pedido pedido) {
        List<SubRuta> subrutas = new ArrayList<>();

        String destino = pedido.getAeropuertoDestinoId();
        int cantidad = pedido.getCantidadProductos();

        // Calcular dia relativo del pedido
        LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
        int diaRelativo = worldTemporal.calcularDiaRelativo(fechaPedido);

        if (diaRelativo < 0) {
            log.warn("Pedido {} fuera del horizonte temporal: {}", pedido.getId(), fechaPedido);
            return subrutas;
        }

        // Intentar generar ruta desde cada hub
        for (String hub : worldTemporal.getHubs()) {
            SubRuta subruta = buscadorRutas.buscarRuta(hub, destino, cantidad, diaRelativo);

            if (subruta != null) {
                subrutas.add(subruta);
                break; // Solo necesitamos una ruta
            }
        }

        return subrutas;
    }

    /**
     * Clase interna para asociar pedido con su prioridad
     */
    private static class PedidoConPrioridad {
        final Pedido pedido;
        final double prioridad;

        PedidoConPrioridad(Pedido pedido, double prioridad) {
            this.pedido = pedido;
            this.prioridad = prioridad;
        }
    }
}
