package com.proyecto.backend.simulation.state;

import com.proyecto.backend.model.PedidoSemanal;
import com.proyecto.backend.planificador.semanal.model.EstadoPedido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gestiona el estado de pedidos EN MEMORIA por cada sesión de simulación.
 * 
 * Cada sesión tiene su propio conjunto de estados, completamente aislado.
 * Esto permite que múltiples simulaciones corran en paralelo sin conflictos.
 * 
 * NO modifica la base de datos - todo está en RAM.
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
@Slf4j
@Component
public class SessionStateManager {

    /**
     * Estados de pedidos por sesión
     * Map<sessionId, Map<pedidoId, PedidoSessionState>>
     */
    private final Map<String, Map<Long, PedidoSessionState>> sessionStates = new ConcurrentHashMap<>();

    /**
     * Crea una nueva sesión con todos los pedidos en estado PENDIENTE
     * 
     * @param sessionId ID de la sesión
     * @param pedidos Lista de pedidos a inicializar
     */
    public void inicializarSesion(String sessionId, List<PedidoSemanal> pedidos) {
        Map<Long, PedidoSessionState> estadosSesion = new ConcurrentHashMap<>();
        
        for (PedidoSemanal pedido : pedidos) {
            PedidoSessionState estado = new PedidoSessionState();
            estado.setPedidoId(pedido.getId());
            estado.setEstado(EstadoPedido.PENDIENTE);
            estado.setFechaDeadline(LocalDateTime.of(
                pedido.getAnio(), pedido.getMes(), pedido.getDia(),
                pedido.getHora(), pedido.getMinuto()
            ));
            estadosSesion.put(pedido.getId(), estado);
        }
        
        sessionStates.put(sessionId, estadosSesion);
        log.info("📦 Sesión {} inicializada con {} pedidos en estado PENDIENTE (RAM)", 
                 sessionId, pedidos.size());
    }

    /**
     * Obtiene los pedidos PENDIENTE de una sesión específica
     * 
     * @param sessionId ID de la sesión
     * @param pedidosTodos Todos los pedidos cargados de BD
     * @return Solo los pedidos que aún están PENDIENTE en esta sesión
     */
    public List<PedidoSemanal> filtrarPedidosPendientes(String sessionId, List<PedidoSemanal> pedidosTodos) {
        Map<Long, PedidoSessionState> estadosSesion = sessionStates.get(sessionId);
        
        if (estadosSesion == null) {
            log.warn("⚠️ Sesión {} no inicializada, retornando todos los pedidos", sessionId);
            return pedidosTodos;
        }
        
        return pedidosTodos.stream()
                .filter(p -> {
                    PedidoSessionState estado = estadosSesion.get(p.getId());
                    return estado != null && estado.getEstado() == EstadoPedido.PENDIENTE;
                })
                .collect(Collectors.toList());
    }

    /**
     * Marca pedidos como ASIGNADO/PLANIFICADO en una sesión específica
     * 
     * @param sessionId ID de la sesión
     * @param pedidosIds IDs de pedidos a marcar como asignados
     */
    public void marcarComoAsignados(String sessionId, Set<Long> pedidosIds) {
        Map<Long, PedidoSessionState> estadosSesion = sessionStates.get(sessionId);
        
        if (estadosSesion == null) {
            log.warn("⚠️ Sesión {} no inicializada, no se pueden marcar pedidos", sessionId);
            return;
        }
        
        for (Long pedidoId : pedidosIds) {
            PedidoSessionState estado = estadosSesion.get(pedidoId);
            if (estado != null) {
                estado.setEstado(EstadoPedido.PLANIFICADO);
                estado.setFechaAsignacion(LocalDateTime.now());
            }
        }
        
        log.debug("✅ Sesión {}: {} pedidos marcados como PLANIFICADO (RAM)", sessionId, pedidosIds.size());
    }

    /**
     * Marca un pedido como EN_TRANSITO
     */
    public void marcarEnTransito(String sessionId, Long pedidoId, String ubicacionActual) {
        Map<Long, PedidoSessionState> estadosSesion = sessionStates.get(sessionId);
        if (estadosSesion != null) {
            PedidoSessionState estado = estadosSesion.get(pedidoId);
            if (estado != null) {
                estado.setEstado(EstadoPedido.EN_TRANSITO);
                estado.setUbicacionActual(ubicacionActual);
            }
        }
    }

    /**
     * Marca un pedido como ENTREGADO
     */
    public void marcarEntregado(String sessionId, Long pedidoId) {
        Map<Long, PedidoSessionState> estadosSesion = sessionStates.get(sessionId);
        if (estadosSesion != null) {
            PedidoSessionState estado = estadosSesion.get(pedidoId);
            if (estado != null) {
                estado.setEstado(EstadoPedido.ENTREGADO);
                estado.setFechaEntregaReal(LocalDateTime.now());
            }
        }
    }

    /**
     * Obtiene el estado de un pedido en una sesión específica
     */
    public EstadoPedido getEstadoPedido(String sessionId, Long pedidoId) {
        Map<Long, PedidoSessionState> estadosSesion = sessionStates.get(sessionId);
        if (estadosSesion != null) {
            PedidoSessionState estado = estadosSesion.get(pedidoId);
            if (estado != null) {
                return estado.getEstado();
            }
        }
        return EstadoPedido.PENDIENTE; // Default
    }

    /**
     * Obtiene estadísticas de una sesión
     */
    public Map<EstadoPedido, Long> getEstadisticasSesion(String sessionId) {
        Map<Long, PedidoSessionState> estadosSesion = sessionStates.get(sessionId);
        if (estadosSesion == null) {
            return Map.of();
        }
        
        return estadosSesion.values().stream()
                .collect(Collectors.groupingBy(PedidoSessionState::getEstado, Collectors.counting()));
    }

    /**
     * Cuenta pedidos por estado en una sesión
     */
    public long contarPorEstado(String sessionId, EstadoPedido estado) {
        Map<Long, PedidoSessionState> estadosSesion = sessionStates.get(sessionId);
        if (estadosSesion == null) {
            return 0;
        }
        
        return estadosSesion.values().stream()
                .filter(p -> p.getEstado() == estado)
                .count();
    }

    /**
     * Limpia el estado de una sesión (cuando termina o se cancela)
     */
    public void limpiarSesion(String sessionId) {
        Map<Long, PedidoSessionState> removed = sessionStates.remove(sessionId);
        if (removed != null) {
            log.info("🧹 Sesión {} limpiada: {} estados de pedidos eliminados de RAM", 
                     sessionId, removed.size());
        }
    }

    /**
     * Verifica si una sesión existe
     */
    public boolean existeSesion(String sessionId) {
        return sessionStates.containsKey(sessionId);
    }

    /**
     * Obtiene el número de sesiones activas
     */
    public int getNumeroSesionesActivas() {
        return sessionStates.size();
    }

    /**
     * Estado de un pedido específico para una sesión (en RAM)
     */
    @lombok.Data
    public static class PedidoSessionState {
        private Long pedidoId;
        private EstadoPedido estado;
        private LocalDateTime fechaDeadline;
        private LocalDateTime fechaAsignacion;
        private LocalDateTime fechaEntregaReal;
        private String ubicacionActual;
        private String vueloActual;
        private double progresoRuta;
    }
}
