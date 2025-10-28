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
 */
@Slf4j
public class DecodificadorBasico {

    private final WorldTemporal worldTemporal;

    public DecodificadorBasico(WorldTemporal worldTemporal) {
        this.worldTemporal = worldTemporal;
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
     * Intenta: 1) Vuelo directo, 2) Con 1 escala
     *
     * @param hub Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param diaRelativo Dia relativo del pedido
     * @return SubRuta o null si no se encuentra
     */
    private SubRuta buscarRutaDesdeHub(String hub, String destino, int cantidad, int diaRelativo) {
        // 1. Intentar vuelo directo
        SubRuta rutaDirecta = buscarVueloDirecto(hub, destino, cantidad, diaRelativo);
        if (rutaDirecta != null) {
            return rutaDirecta;
        }

        // 2. Intentar con 1 escala
        SubRuta rutaConEscala = buscarConUnaEscala(hub, destino, cantidad, diaRelativo);
        if (rutaConEscala != null) {
            return rutaConEscala;
        }

        return null;
    }

    /**
     * Busca un vuelo directo del hub al destino con capacidad disponible
     *
     * @param origen Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param diaRelativo Dia relativo del pedido
     * @return SubRuta o null
     */
    private SubRuta buscarVueloDirecto(String origen, String destino, int cantidad, int diaRelativo) {
        // Buscar vuelos directos desde origen a destino en el dia especificado
        List<VueloInstancia> vuelosDirectos = worldTemporal.buscarVuelosDirectos(origen, destino, diaRelativo);

        for (VueloInstancia vuelo : vuelosDirectos) {
            // Verificar capacidad disponible
            if (vuelo.tieneCapacidad(cantidad)) {
                // Asignar capacidad
                if (vuelo.asignarCapacidad(cantidad)) {
                    // Crear subruta
                    SubRuta subruta = new SubRuta(origen, cantidad);

                    // Convertir VueloInstancia a VueloUso
                    VueloUso vueloUso = vuelo.toVueloUso();
                    vueloUso.setCantidadAsignada(cantidad);

                    subruta.agregarVuelo(vueloUso);

                    log.debug("Ruta directa encontrada: {} ({} / {} productos, {:.1f}% ocupacion)",
                            vuelo.getId(), vuelo.getCapacidadUsada(), vuelo.getCapacidadMaxima(),
                            vuelo.getPorcentajeOcupacion());

                    return subruta;
                }
            }
        }

        return null;
    }

    /**
     * Busca una ruta con una escala verificando capacidades
     *
     * @param origen Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param diaRelativo Dia relativo del pedido
     * @return SubRuta o null
     */
    private SubRuta buscarConUnaEscala(String origen, String destino, int cantidad, int diaRelativo) {
        // Obtener vuelos desde el origen en el dia especificado
        List<VueloInstancia> vuelosDesdeOrigen = worldTemporal.getVuelosDesde(origen, diaRelativo);

        // Para cada vuelo desde el origen
        for (VueloInstancia vuelo1 : vuelosDesdeOrigen) {
            String escala = vuelo1.getDestino();

            // No volver al origen
            if (escala.equals(origen)) continue;

            // No ir al destino final (ya se busco directo antes)
            if (escala.equals(destino)) continue;

            // Verificar capacidad del primer vuelo
            if (!vuelo1.tieneCapacidad(cantidad)) continue;

            // Buscar vuelos desde la escala al destino
            // Puede ser mismo dia o dia siguiente dependiendo de los horarios
            List<VueloInstancia> vuelosDesdeEscala = worldTemporal.getVuelosDesde(escala, diaRelativo);

            for (VueloInstancia vuelo2 : vuelosDesdeEscala) {
                if (!vuelo2.getDestino().equals(destino)) continue;

                // Verificar que vuelo2 sale DESPUES de que llega vuelo1
                if (vuelo2.getSalidaUTC().isBefore(vuelo1.getLlegadaUTC())) {
                    continue;
                }

                // Verificar capacidad del segundo vuelo
                if (!vuelo2.tieneCapacidad(cantidad)) continue;

                // Asignar capacidades
                if (vuelo1.asignarCapacidad(cantidad) && vuelo2.asignarCapacidad(cantidad)) {
                    // Crear subruta
                    SubRuta subruta = new SubRuta(origen, cantidad);

                    // Primer vuelo
                    VueloUso uso1 = vuelo1.toVueloUso();
                    uso1.setCantidadAsignada(cantidad);
                    subruta.agregarVuelo(uso1);

                    // Segundo vuelo
                    VueloUso uso2 = vuelo2.toVueloUso();
                    uso2.setCantidadAsignada(cantidad);
                    subruta.agregarVuelo(uso2);

                    log.debug("Ruta con escala: {} -> {} -> {} ({} y {})",
                            origen, escala, destino, vuelo1.getId(), vuelo2.getId());

                    return subruta;
                } else {
                    // Si fallo la asignacion, liberar lo que se asigno
                    vuelo1.liberarCapacidad(cantidad);
                }
            }

            // Intentar con dia siguiente si no encontro
            if (diaRelativo + 1 < worldTemporal.getNumeroDias()) {
                List<VueloInstancia> vuelosDiaSiguiente = worldTemporal.getVuelosDesde(escala, diaRelativo + 1);

                for (VueloInstancia vuelo2 : vuelosDiaSiguiente) {
                    if (!vuelo2.getDestino().equals(destino)) continue;

                    // Verificar capacidad
                    if (!vuelo2.tieneCapacidad(cantidad)) continue;

                    // Asignar capacidades
                    if (vuelo1.asignarCapacidad(cantidad) && vuelo2.asignarCapacidad(cantidad)) {
                        SubRuta subruta = new SubRuta(origen, cantidad);

                        VueloUso uso1 = vuelo1.toVueloUso();
                        uso1.setCantidadAsignada(cantidad);
                        subruta.agregarVuelo(uso1);

                        VueloUso uso2 = vuelo2.toVueloUso();
                        uso2.setCantidadAsignada(cantidad);
                        subruta.agregarVuelo(uso2);

                        log.debug("Ruta con escala (dia siguiente): {} -> {} -> {}",
                                origen, escala, destino);

                        return subruta;
                    } else {
                        vuelo1.liberarCapacidad(cantidad);
                    }
                }
            }
        }

        return null;
    }
}
