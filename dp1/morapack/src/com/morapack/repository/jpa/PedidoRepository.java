package com.morapack.repository.jpa;

import com.morapack.model.Pedido;
import com.morapack.model.EstadoPedido;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Repositorio JPA para la entidad Pedido
 * Cuando se integre Spring Boot, extenderá JpaRepository<Pedido, String>
 */
public interface PedidoRepository {
    
    // Métodos básicos de CRUD
    Pedido save(Pedido pedido);
    Optional<Pedido> findById(String id);
    List<Pedido> findAll();
    void deleteById(String id);
    boolean existsById(String id);
    long count();
    
    // Consultas personalizadas por estado
    List<Pedido> findByEstado(EstadoPedido estado);
    List<Pedido> findByEstadoIn(List<EstadoPedido> estados);
    long countByEstado(EstadoPedido estado);
    
    // Consultas por sede
    List<Pedido> findBySedeAsignadaId(String sedeId);
    List<Pedido> findBySedeAsignadaIdIsNull(); // Pedidos sin asignar
    
    // Consultas por cliente
    List<Pedido> findByClienteId(String clienteId);
    
    // Consultas por aeropuerto destino
    List<Pedido> findByAeropuertoDestinoId(String aeropuertoId);
    
    // Consultas por prioridad
    List<Pedido> findByPrioridad(int prioridad);
    List<Pedido> findByPrioridadLessThanEqual(int prioridad); // Alta prioridad
    
    // Consultas por fecha
    List<Pedido> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
    List<Pedido> findByFechaLimiteEntregaBefore(LocalDateTime fecha); // Pedidos vencidos o próximos a vencer
    List<Pedido> findByFechaLimiteEntregaAfter(LocalDateTime fecha);
    
    // Consultas combinadas
    List<Pedido> findByEstadoAndPrioridad(EstadoPedido estado, int prioridad);
    List<Pedido> findByEstadoAndSedeAsignadaId(EstadoPedido estado, String sedeId);
    List<Pedido> findByEstadoAndAeropuertoDestinoId(EstadoPedido estado, String aeropuertoId);
    
    // Consultas para optimización
    List<Pedido> findByEstadoAndFechaLimiteEntregaAfter(EstadoPedido estado, LocalDateTime fecha);
    List<Pedido> findPedidosParaOptimizacion(); // Pedidos PENDIENTES factibles
}
