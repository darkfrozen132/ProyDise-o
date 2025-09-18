package com.morapack.repository.impl;

import com.morapack.model.Pedido;
import com.morapack.model.EstadoPedido;
import com.morapack.repository.jpa.PedidoRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del repositorio de pedidos
 * En una implementación real extendería JpaRepository
 */
public class PedidoRepositoryImpl implements PedidoRepository {
    
    private final Map<String, Pedido> pedidos = new HashMap<>();
    
    @Override
    public Pedido save(Pedido pedido) {
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }
    
    @Override
    public Optional<Pedido> findById(String id) {
        return Optional.ofNullable(pedidos.get(id));
    }
    
    @Override
    public List<Pedido> findAll() {
        return new ArrayList<>(pedidos.values());
    }
    
    @Override
    public void deleteById(String id) {
        pedidos.remove(id);
    }
    
    @Override
    public boolean existsById(String id) {
        return pedidos.containsKey(id);
    }
    
    @Override
    public long count() {
        return pedidos.size();
    }
    
    @Override
    public List<Pedido> findByEstado(EstadoPedido estado) {
        return pedidos.values().stream()
                .filter(p -> p.getEstado() == estado)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByEstadoIn(List<EstadoPedido> estados) {
        return pedidos.values().stream()
                .filter(p -> estados.contains(p.getEstado()))
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByEstado(EstadoPedido estado) {
        return pedidos.values().stream()
                .filter(p -> p.getEstado() == estado)
                .count();
    }
    
    @Override
    public List<Pedido> findBySedeAsignadaId(String sedeId) {
        return pedidos.values().stream()
                .filter(p -> sedeId.equals(p.getSedeAsignadaId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findBySedeAsignadaIdIsNull() {
        return pedidos.values().stream()
                .filter(p -> p.getSedeAsignadaId() == null)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByClienteId(String clienteId) {
        return pedidos.values().stream()
                .filter(p -> clienteId.equals(p.getClienteId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByAeropuertoDestinoId(String aeropuertoId) {
        return pedidos.values().stream()
                .filter(p -> aeropuertoId.equals(p.getAeropuertoDestinoId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByPrioridad(int prioridad) {
        return pedidos.values().stream()
                .filter(p -> p.getPrioridad() == prioridad)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByPrioridadLessThanEqual(int prioridad) {
        return pedidos.values().stream()
                .filter(p -> p.getPrioridad() <= prioridad)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin) {
        return pedidos.values().stream()
                .filter(p -> p.getFechaCreacion().isAfter(inicio) && p.getFechaCreacion().isBefore(fin))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByFechaLimiteEntregaBefore(LocalDateTime fecha) {
        return pedidos.values().stream()
                .filter(p -> p.getFechaLimiteEntrega().isBefore(fecha))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByFechaLimiteEntregaAfter(LocalDateTime fecha) {
        return pedidos.values().stream()
                .filter(p -> p.getFechaLimiteEntrega().isAfter(fecha))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByEstadoAndPrioridad(EstadoPedido estado, int prioridad) {
        return pedidos.values().stream()
                .filter(p -> p.getEstado() == estado && p.getPrioridad() == prioridad)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByEstadoAndSedeAsignadaId(EstadoPedido estado, String sedeId) {
        return pedidos.values().stream()
                .filter(p -> p.getEstado() == estado && sedeId.equals(p.getSedeAsignadaId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByEstadoAndAeropuertoDestinoId(EstadoPedido estado, String aeropuertoId) {
        return pedidos.values().stream()
                .filter(p -> p.getEstado() == estado && aeropuertoId.equals(p.getAeropuertoDestinoId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findByEstadoAndFechaLimiteEntregaAfter(EstadoPedido estado, LocalDateTime fecha) {
        return pedidos.values().stream()
                .filter(p -> p.getEstado() == estado && p.getFechaLimiteEntrega().isAfter(fecha))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> findPedidosParaOptimizacion() {
        LocalDateTime ahora = LocalDateTime.now();
        return pedidos.values().stream()
                .filter(p -> p.getEstado() == EstadoPedido.PENDIENTE)
                .filter(p -> p.getFechaLimiteEntrega().isAfter(ahora))
                .collect(Collectors.toList());
    }
}
