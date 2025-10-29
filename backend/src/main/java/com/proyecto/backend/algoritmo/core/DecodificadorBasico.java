package com.proyecto.backend.algoritmo.core;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodificador basico para generar rutas de forma greedy
 *
 * Mejoras v2:
 * - Usa WorldTemporal con instancias de vuelos expandidas
 * - Verifica capacidades reales de cada vuelo
 * - Calcula dia correcto del pedido
 * - Usa fechas/horas UTC correctas
 *
 * Mejoras v3:
 * - Verifica capacidad de almacenes con ControladorAlmacenes
 * - Usa slots de tiempo (60 minutos) para rastrear ocupacion
 * - Reserva capacidad en almacenes intermedios y destino
 * - Busqueda BFS para multiples escalas (refactorizada en BuscadorRutas)
 */
@Slf4j
public class DecodificadorBasico {

    private final WorldTemporal worldTemporal;
    private final ControladorAlmacenes controladorAlmacenes;
    private final CalculadorPlazos calculadorPlazos;
    private final BuscadorRutas buscadorRutas;

    public DecodificadorBasico(WorldTemporal worldTemporal, ControladorAlmacenes controladorAlmacenes) {
        this.worldTemporal = worldTemporal;
        this.controladorAlmacenes = controladorAlmacenes;
        this.calculadorPlazos = new CalculadorPlazos(worldTemporal);
        this.buscadorRutas = new BuscadorRutas(worldTemporal, controladorAlmacenes);
    }

    /**
     * Genera una solucion basica para una lista de pedidos
     *
     * @param pedidos Lista de pedidos a procesar
     * @return Solucion con rutas generadas y metricas calculadas
     */
    public Solution generarSolucion(List<Pedido> pedidos) {
        Solution solucion = new Solution();

        log.info("Iniciando generacion de rutas para {} pedidos", pedidos.size());

        int rutasGeneradas = 0;
        int rutasFallidas = 0;

        for (Pedido pedido : pedidos) {
            try {
                List<SubRuta> subrutas = generarRutasPedido(pedido);

                if (!subrutas.isEmpty()) {
                    solucion.agregarRutas(pedido, subrutas);
                    rutasGeneradas++;
                } else {
                    log.warn("No se pudo generar ruta para pedido {} a {}",
                            pedido.getId(), pedido.getAeropuertoDestinoId());
                    rutasFallidas++;
                    // Agregar pedido sin rutas (sera clasificado como NO_ENTREGADO)
                    solucion.agregarRutas(pedido, new ArrayList<>());
                }

            } catch (Exception e) {
                log.error("Error generando ruta para pedido {}", pedido.getId(), e);
                rutasFallidas++;
                // Agregar pedido sin rutas (sera clasificado como NO_ENTREGADO)
                solucion.agregarRutas(pedido, new ArrayList<>());
            }
        }

        log.info("Rutas generadas: {}, fallidas: {}", rutasGeneradas, rutasFallidas);

        // Calcular metricas de entrega y fitness
        solucion.calcularMetricasYFitness(calculadorPlazos);

        log.info("Metricas calculadas: {} a tiempo, {} tarde, {} no entregados, fitness = {}",
                solucion.getPedidosATiempo(), solucion.getPedidosTarde(),
                solucion.getPedidosNoEntregados(), String.format("%.2f", solucion.getObjetivo()));

        return solucion;
    }

    /**
     * Genera rutas para un pedido especifico
     * Estrategia: buscar desde el hub mas cercano al destino
     *
     * @param pedido Pedido a procesar
     * @return Lista de subrutas (normalmente 1)
     */
    private List<SubRuta> generarRutasPedido(Pedido pedido) {
        List<SubRuta> subrutas = new ArrayList<>();

        String destino = pedido.getAeropuertoDestinoId();
        int cantidad = pedido.getCantidadProductos();

        // Verificar que el destino exista
        Aeropuerto aeropuertoDestino = worldTemporal.getAeropuerto(destino);
        if (aeropuertoDestino == null) {
            log.warn("Aeropuerto destino {} no existe", destino);
            return subrutas;
        }

        // Calcular dia relativo del pedido
        LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
        int diaRelativo = worldTemporal.calcularDiaRelativo(fechaPedido);

        if (diaRelativo < 0) {
            log.warn("Pedido {} fuera del horizonte temporal: {}",
                    pedido.getId(), fechaPedido);
            return subrutas;
        }

        // Intentar generar ruta desde cada hub
        for (String hub : worldTemporal.getHubs()) {
            SubRuta subruta = buscarRutaDesdeHub(hub, destino, cantidad, diaRelativo);

            if (subruta != null) {
                subrutas.add(subruta);
                break; // Solo necesitamos una ruta por ahora
            }
        }

        return subrutas;
    }

    /**
     * Busca una ruta desde un hub hasta el destino
     * Delega la busqueda a BuscadorRutas
     *
     * @param hub Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param diaRelativo Dia relativo del pedido
     * @return SubRuta o null si no se encuentra
     */
    private SubRuta buscarRutaDesdeHub(String hub, String destino, int cantidad, int diaRelativo) {
        return buscadorRutas.buscarRuta(hub, destino, cantidad, diaRelativo);
    }

}
