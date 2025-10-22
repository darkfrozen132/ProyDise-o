package com.proyecto.backend.algoritmo.core;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.model.PlanDeVuelo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodificador basico para generar rutas de forma greedy
 * Version inicial simple - sin optimizacion, sin verificar capacidades
 */
@Slf4j
public class DecodificadorBasico {

    private final World world;

    public DecodificadorBasico(World world) {
        this.world = world;
    }

    /**
     * Genera una solucion basica para una lista de pedidos
     *
     * @param pedidos Lista de pedidos a procesar
     * @return Solucion con rutas generadas
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

                    // Por ahora marcar todos como no entregados
                    // TODO: calcular si llega a tiempo
                    solucion.setPedidosNoEntregados(solucion.getPedidosNoEntregados() + 1);
                } else {
                    log.warn("No se pudo generar ruta para pedido {} a {}",
                            pedido.getId(), pedido.getAeropuertoDestinoId());
                    rutasFallidas++;
                    solucion.setPedidosNoEntregados(solucion.getPedidosNoEntregados() + 1);
                }

            } catch (Exception e) {
                log.error("Error generando ruta para pedido {}", pedido.getId(), e);
                rutasFallidas++;
                solucion.setPedidosNoEntregados(solucion.getPedidosNoEntregados() + 1);
            }
        }

        log.info("Rutas generadas: {}, fallidas: {}", rutasGeneradas, rutasFallidas);

        // Calcular objetivo basico
        solucion.setObjetivo(rutasGeneradas * 1.0);

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
        Aeropuerto aeropuertoDestino = world.getAeropuerto(destino);
        if (aeropuertoDestino == null) {
            log.warn("Aeropuerto destino {} no existe", destino);
            return subrutas;
        }

        // Intentar generar ruta desde cada hub
        for (String hub : world.getHubs()) {
            SubRuta subruta = buscarRutaDesdeHub(hub, destino, cantidad, pedido);

            if (subruta != null) {
                subrutas.add(subruta);
                break; // Solo necesitamos una ruta por ahora
            }
        }

        return subrutas;
    }

    /**
     * Busca una ruta desde un hub hasta el destino
     * Intenta: 1) Vuelo directo, 2) Con 1 escala
     *
     * @param hub Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param pedido Pedido original (para tiempos)
     * @return SubRuta o null si no se encuentra
     */
    private SubRuta buscarRutaDesdeHub(String hub, String destino, int cantidad, Pedido pedido) {
        // 1. Intentar vuelo directo
        SubRuta rutaDirecta = buscarVueloDirecto(hub, destino, cantidad, pedido);
        if (rutaDirecta != null) {
            return rutaDirecta;
        }

        // 2. Intentar con 1 escala
        SubRuta rutaConEscala = buscarConUnaEscala(hub, destino, cantidad, pedido);
        if (rutaConEscala != null) {
            return rutaConEscala;
        }

        return null;
    }

    /**
     * Busca un vuelo directo del hub al destino
     *
     * @param origen Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param pedido Pedido original
     * @return SubRuta o null
     */
    private SubRuta buscarVueloDirecto(String origen, String destino, int cantidad, Pedido pedido) {
        List<PlanDeVuelo> vuelosDesdeOrigen = world.getVuelosDesde(origen);

        for (PlanDeVuelo vuelo : vuelosDesdeOrigen) {
            if (vuelo.getAeropuertoDestino().equals(destino)) {
                // Encontramos vuelo directo!
                SubRuta subruta = new SubRuta(origen, cantidad);

                // Crear VueloUso (por ahora dia 0)
                // TODO: calcular el dia correcto basado en la fecha del pedido
                VueloUso vueloUso = new VueloUso();
                vueloUso.setPlanVuelo(vuelo);
                vueloUso.setIndiceDia(0);
                vueloUso.setSalidaUTC(0);  // TODO: calcular UTC correcto
                vueloUso.setLlegadaUTC(60); // TODO: calcular UTC correcto
                vueloUso.setCantidadAsignada(cantidad);

                subruta.agregarVuelo(vueloUso);

                log.debug("Ruta directa: {} -> {} ({})", origen, destino, vuelo.getCapacidadMaxima());
                return subruta;
            }
        }

        return null;
    }

    /**
     * Busca una ruta con una escala
     *
     * @param origen Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param pedido Pedido original
     * @return SubRuta o null
     */
    private SubRuta buscarConUnaEscala(String origen, String destino, int cantidad, Pedido pedido) {
        List<PlanDeVuelo> vuelosDesdeOrigen = world.getVuelosDesde(origen);

        // Para cada vuelo desde el origen
        for (PlanDeVuelo vuelo1 : vuelosDesdeOrigen) {
            String escala = vuelo1.getAeropuertoDestino();

            // No volver al origen
            if (escala.equals(origen)) continue;

            // Buscar vuelo desde la escala al destino
            List<PlanDeVuelo> vuelosDesdeEscala = world.getVuelosDesde(escala);

            for (PlanDeVuelo vuelo2 : vuelosDesdeEscala) {
                if (vuelo2.getAeropuertoDestino().equals(destino)) {
                    // Encontramos ruta con 1 escala!
                    SubRuta subruta = new SubRuta(origen, cantidad);

                    // Primer vuelo
                    VueloUso uso1 = new VueloUso();
                    uso1.setPlanVuelo(vuelo1);
                    uso1.setIndiceDia(0);
                    uso1.setSalidaUTC(0);
                    uso1.setLlegadaUTC(60);
                    uso1.setCantidadAsignada(cantidad);
                    subruta.agregarVuelo(uso1);

                    // Segundo vuelo
                    VueloUso uso2 = new VueloUso();
                    uso2.setPlanVuelo(vuelo2);
                    uso2.setIndiceDia(0);
                    uso2.setSalidaUTC(120);
                    uso2.setLlegadaUTC(180);
                    uso2.setCantidadAsignada(cantidad);
                    subruta.agregarVuelo(uso2);

                    log.debug("Ruta con escala: {} -> {} -> {}", origen, escala, destino);
                    return subruta;
                }
            }
        }

        return null;
    }
}
