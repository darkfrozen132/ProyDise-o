package com.proyecto.backend.planificador.semanal.service;

import com.proyecto.backend.planificador.semanal.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Actualiza el estado de la simulacion cada segundo en tiempo real
 *
 * Responsabilidades:
 * - Avanzar el tiempo de simulacion segun el factor K
 * - Actualizar estado de vuelos (programado -> en vuelo -> aterrizado)
 * - Actualizar estado de pedidos (pendiente -> en transito -> entregado)
 * - Actualizar ocupacion de almacenes
 *
 * IMPORTANTE: Este servicio se ejecuta independientemente del AlgoritmoGenetico
 */
@Service
@Slf4j
public class StateUpdater {

    /**
     * Avanza el tiempo de simulacion y actualiza todos los estados
     *
     * @param state Estado de la simulacion
     * @param factorK Factor de ampliacion temporal (cuantos segundos de datos por segundo real)
     */
    public void avanzarTiempo(SimulationState state, int factorK) {

        // Calcular nuevo tiempo de simulacion
        // Si K=70, cada segundo real = 70 segundos en datos
        LocalDateTime nuevoTiempo = state.getTiempoSimulacion().plusSeconds(factorK);
        state.setTiempoSimulacion(nuevoTiempo);

        // Actualizar estados
        actualizarVuelos(state, nuevoTiempo);
        actualizarPedidos(state, nuevoTiempo);
        actualizarAlmacenes(state);

        // Log cada 60 segundos (1 minuto real)
        if (state.getTickActual() % 60 == 0) {
            log.debug("Tiempo simulacion: {} (K={}, tick={})",
                nuevoTiempo, factorK, state.getTickActual());
        }

        state.setTickActual(state.getTickActual() + 1);
    }

    /**
     * Actualiza el estado de todos los vuelos
     */
    private void actualizarVuelos(SimulationState state, LocalDateTime ahora) {

        for (VueloState vuelo : state.getVuelos().values()) {

            // Si el vuelo ya despego y no ha aterrizado
            if (ahora.isAfter(vuelo.getSalida()) && ahora.isBefore(vuelo.getLlegada())) {

                if (vuelo.getEstado() != EstadoVuelo.EN_VUELO) {
                    vuelo.setEstado(EstadoVuelo.EN_VUELO);
                    log.debug("Vuelo {} EN_VUELO: {} -> {}", vuelo.getId(), vuelo.getOrigen(), vuelo.getDestino());
                }

                // Calcular progreso (para animacion en front)
                long totalMinutos = ChronoUnit.MINUTES.between(vuelo.getSalida(), vuelo.getLlegada());
                long transcurridos = ChronoUnit.MINUTES.between(vuelo.getSalida(), ahora);
                vuelo.setProgreso((double) transcurridos / totalMinutos);
            }

            // Si el vuelo ya aterrizo
            else if (ahora.isAfter(vuelo.getLlegada())) {

                if (vuelo.getEstado() != EstadoVuelo.ATERRIZADO) {
                    vuelo.setEstado(EstadoVuelo.ATERRIZADO);
                    vuelo.setProgreso(1.0);

                    log.debug("Vuelo {} ATERRIZADO en {} - {} pedidos",
                        vuelo.getId(), vuelo.getDestino(), vuelo.getPedidosAbordo().size());

                    // Mover pedidos del vuelo al almacen de destino
                    moverPedidosAAlmacen(state, vuelo);
                }
            }
        }
    }

    /**
     * Mueve los pedidos de un vuelo aterrizado al almacen de destino
     */
    private void moverPedidosAAlmacen(SimulationState state, VueloState vuelo) {

        for (Long pedidoId : vuelo.getPedidosAbordo()) {
            PedidoState pedido = state.getPedidos().get(pedidoId);

            if (pedido == null) continue;

            // Actualizar ubicacion del pedido
            pedido.setUbicacionActual(vuelo.getDestino());

            // Verificar si es el destino final
            if (vuelo.getDestino().equals(pedido.getDestino())) {
                // Pedido entregado
                pedido.setEstado(EstadoPedido.ENTREGADO);
                pedido.setFechaEntregaReal(vuelo.getLlegada());
                pedido.setProgresoRuta(1.0);

                log.info("Pedido {} ENTREGADO en {} a las {}",
                    pedidoId, vuelo.getDestino(), vuelo.getLlegada());
            } else {
                // Pedido en almacen esperando conexion
                pedido.setEstado(EstadoPedido.EN_ALMACEN);
            }
        }

        // Limpiar lista de pedidos del vuelo
        vuelo.getPedidosAbordo().clear();
    }

    /**
     * Actualiza el estado de todos los pedidos
     */
    private void actualizarPedidos(SimulationState state, LocalDateTime ahora) {

        for (PedidoState pedido : state.getPedidos().values()) {

            // Saltar pedidos ya entregados
            if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
                continue;
            }

            // Si esta planificado, verificar si ya salio en su primer vuelo
            if (pedido.getEstado() == EstadoPedido.PLANIFICADO) {
                verificarSalidaPedido(state, pedido, ahora);
            }

            // Si esta en transito, actualizar su ubicacion
            else if (pedido.getEstado() == EstadoPedido.EN_TRANSITO ||
                     pedido.getEstado() == EstadoPedido.EN_ALMACEN) {
                actualizarUbicacionPedido(state, pedido, ahora);
            }
        }
    }

    /**
     * Verifica si un pedido planificado ya salio en su primer vuelo
     */
    private void verificarSalidaPedido(SimulationState state, PedidoState pedido, LocalDateTime ahora) {

        if (pedido.getRutaPlanificada().isEmpty()) {
            return;
        }

        VueloAsignado primerVuelo = pedido.getRutaPlanificada().get(0);
        VueloState vuelo = state.getVuelos().get(primerVuelo.getVueloId());

        if (vuelo == null) {
            return;
        }

        // Si el vuelo ya salio
        if (ahora.isAfter(vuelo.getSalida())) {
            pedido.setEstado(EstadoPedido.EN_TRANSITO);
            pedido.setUbicacionActual(vuelo.getId());

            // Agregar pedido a la lista de abordo del vuelo
            if (!vuelo.getPedidosAbordo().contains(pedido.getId())) {
                vuelo.getPedidosAbordo().add(pedido.getId());
            }

            log.debug("Pedido {} EN_TRANSITO en vuelo {}", pedido.getId(), vuelo.getId());
        }
    }

    /**
     * Actualiza la ubicacion de un pedido en transito
     */
    private void actualizarUbicacionPedido(SimulationState state, PedidoState pedido, LocalDateTime ahora) {

        if (pedido.getRutaPlanificada().isEmpty()) {
            return;
        }

        // Recorrer la ruta planificada para encontrar en que segmento esta
        for (int i = 0; i < pedido.getRutaPlanificada().size(); i++) {
            VueloAsignado vueloAsignado = pedido.getRutaPlanificada().get(i);
            VueloState vuelo = state.getVuelos().get(vueloAsignado.getVueloId());

            if (vuelo == null) continue;

            // Si el vuelo aun no ha salido, el pedido esta en almacen de origen
            if (ahora.isBefore(vuelo.getSalida())) {
                if (!pedido.getUbicacionActual().equals(vuelo.getOrigen())) {
                    pedido.setUbicacionActual(vuelo.getOrigen());
                    pedido.setEstado(EstadoPedido.EN_ALMACEN);
                }
                break;
            }

            // Si el vuelo esta en el aire
            else if (ahora.isAfter(vuelo.getSalida()) && ahora.isBefore(vuelo.getLlegada())) {
                if (!pedido.getUbicacionActual().equals(vuelo.getId())) {
                    pedido.setUbicacionActual(vuelo.getId());
                    pedido.setEstado(EstadoPedido.EN_TRANSITO);

                    // Agregar a lista de abordo
                    if (!vuelo.getPedidosAbordo().contains(pedido.getId())) {
                        vuelo.getPedidosAbordo().add(pedido.getId());
                    }
                }

                // Calcular progreso en la ruta
                double progresoVuelo = (double) (i + vuelo.getProgreso()) / pedido.getRutaPlanificada().size();
                pedido.setProgresoRuta(progresoVuelo);
                break;
            }

            // Si es el ultimo vuelo y ya aterrizo
            else if (i == pedido.getRutaPlanificada().size() - 1 && ahora.isAfter(vuelo.getLlegada())) {
                if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
                    pedido.setUbicacionActual(vuelo.getDestino());
                    pedido.setEstado(EstadoPedido.ENTREGADO);
                    pedido.setFechaEntregaReal(vuelo.getLlegada());
                    pedido.setProgresoRuta(1.0);
                }
                break;
            }
        }
    }

    /**
     * Actualiza la ocupacion de los almacenes
     */
    private void actualizarAlmacenes(SimulationState state) {

        // Limpiar ocupacion actual
        for (AlmacenState almacen : state.getAlmacenes().values()) {
            almacen.getPedidosAlmacenados().clear();
            almacen.setOcupacionActual(0);
        }

        // Recalcular ocupacion basado en pedidos EN_ALMACEN
        for (PedidoState pedido : state.getPedidos().values()) {
            if (pedido.getEstado() == EstadoPedido.EN_ALMACEN) {
                String codigoAlmacen = pedido.getUbicacionActual();

                AlmacenState almacen = state.getAlmacenes().get(codigoAlmacen);
                if (almacen != null) {
                    almacen.getPedidosAlmacenados().add(pedido.getId());
                    almacen.setOcupacionActual(almacen.getOcupacionActual() + pedido.getCantidad());
                }
            }
        }
    }
}
