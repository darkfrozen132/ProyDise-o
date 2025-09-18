package com.morapack.service;

import com.morapack.model.Pedido;
import com.morapack.model.EstadoPedido;
import com.morapack.repository.jpa.PedidoRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Servicio para la gestión de pedidos
 * Preparado para ser anotado con @Service cuando se integre Spring Boot
 */
public class PedidoService {
    
    private final PedidoRepository pedidoRepository;
    
    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }
    
    // Operaciones CRUD básicas
    public Pedido crearPedido(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("El pedido no puede ser nulo");
        }
        
        if (pedido.getId() != null && pedidoRepository.existsById(pedido.getId())) {
            throw new IllegalArgumentException("Ya existe un pedido con el ID: " + pedido.getId());
        }
        
        // Validaciones de negocio
        validarPedido(pedido);
        
        return pedidoRepository.save(pedido);
    }
    
    public Optional<Pedido> obtenerPedidoPorId(String id) {
        return pedidoRepository.findById(id);
    }
    
    public List<Pedido> obtenerTodosPedidos() {
        return pedidoRepository.findAll();
    }
    
    public Pedido actualizarPedido(Pedido pedido) {
        if (pedido == null || pedido.getId() == null) {
            throw new IllegalArgumentException("El pedido y su ID no pueden ser nulos");
        }
        
        if (!pedidoRepository.existsById(pedido.getId())) {
            throw new IllegalArgumentException("No existe un pedido con el ID: " + pedido.getId());
        }
        
        validarPedido(pedido);
        return pedidoRepository.save(pedido);
    }
    
    public void eliminarPedido(String id) {
        if (!pedidoRepository.existsById(id)) {
            throw new IllegalArgumentException("No existe un pedido con el ID: " + id);
        }
        
        pedidoRepository.deleteById(id);
    }
    
    // Operaciones de negocio específicas
    public Pedido asignarSedeAPedido(String pedidoId, String sedeId) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        if (!pedidoOpt.isPresent()) {
            throw new IllegalArgumentException("No existe un pedido con el ID: " + pedidoId);
        }
        
        Pedido pedido = pedidoOpt.get();
        
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden asignar pedidos en estado PENDIENTE");
        }
        
        pedido.setSedeAsignadaId(sedeId);
        pedido.setEstado(EstadoPedido.ASIGNADO);
        
        return pedidoRepository.save(pedido);
    }
    
    public Pedido cambiarEstadoPedido(String pedidoId, EstadoPedido nuevoEstado) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        if (!pedidoOpt.isPresent()) {
            throw new IllegalArgumentException("No existe un pedido con el ID: " + pedidoId);
        }
        
        Pedido pedido = pedidoOpt.get();
        
        // Validar transiciones de estado permitidas
        if (!esTransicionValidaEstado(pedido.getEstado(), nuevoEstado)) {
            throw new IllegalStateException(String.format("Transición de estado no válida: %s -> %s", 
                                                         pedido.getEstado(), nuevoEstado));
        }
        
        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }
    
    // Consultas específicas del negocio
    public List<Pedido> obtenerPedidosPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado);
    }
    
    public List<Pedido> obtenerPedidosPendientes() {
        return pedidoRepository.findByEstado(EstadoPedido.PENDIENTE);
    }
    
    public List<Pedido> obtenerPedidosAsignados() {
        return pedidoRepository.findByEstado(EstadoPedido.ASIGNADO);
    }
    
    public List<Pedido> obtenerPedidosPorSede(String sedeId) {
        return pedidoRepository.findBySedeAsignadaId(sedeId);
    }
    
    public List<Pedido> obtenerPedidosSinAsignar() {
        return pedidoRepository.findBySedeAsignadaIdIsNull();
    }
    
    public List<Pedido> obtenerPedidosVencidos() {
        return pedidoRepository.findByFechaLimiteEntregaBefore(LocalDateTime.now());
    }
    
    public List<Pedido> obtenerPedidosAltaPrioridad() {
        return pedidoRepository.findByPrioridad(1);
    }
    
    public List<Pedido> obtenerPedidosParaOptimizacion() {
        // Pedidos pendientes que aún son factibles
        return pedidoRepository.findByEstadoAndFechaLimiteEntregaAfter(
            EstadoPedido.PENDIENTE, LocalDateTime.now()
        );
    }
    
    // Estadísticas
    public long contarPedidosPorEstado(EstadoPedido estado) {
        return pedidoRepository.countByEstado(estado);
    }
    
    public EstadisticasPedidos obtenerEstadisticas() {
        long pendientes = contarPedidosPorEstado(EstadoPedido.PENDIENTE);
        long asignados = contarPedidosPorEstado(EstadoPedido.ASIGNADO);
        long enRuta = contarPedidosPorEstado(EstadoPedido.EN_RUTA);
        long entregados = contarPedidosPorEstado(EstadoPedido.ENTREGADO);
        long cancelados = contarPedidosPorEstado(EstadoPedido.CANCELADO);
        long total = pedidoRepository.count();
        
        return new EstadisticasPedidos(pendientes, asignados, enRuta, entregados, cancelados, total);
    }
    
    // Métodos auxiliares privados
    private void validarPedido(Pedido pedido) {
        if (pedido.getClienteId() == null || pedido.getClienteId().trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del cliente es obligatorio");
        }
        
        if (pedido.getAeropuertoDestinoId() == null || pedido.getAeropuertoDestinoId().trim().isEmpty()) {
            throw new IllegalArgumentException("El aeropuerto destino es obligatorio");
        }
        
        if (pedido.getCantidadProductos() <= 0) {
            throw new IllegalArgumentException("La cantidad de productos debe ser mayor a 0");
        }
        
        if (pedido.getPrioridad() < 1 || pedido.getPrioridad() > 3) {
            throw new IllegalArgumentException("La prioridad debe ser 1 (alta), 2 (media) o 3 (baja)");
        }
        
        if (pedido.getFechaLimiteEntrega() != null && 
            pedido.getFechaLimiteEntrega().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha límite de entrega no puede ser en el pasado");
        }
    }
    
    private boolean esTransicionValidaEstado(EstadoPedido estadoActual, EstadoPedido nuevoEstado) {
        switch (estadoActual) {
            case PENDIENTE:
                return nuevoEstado == EstadoPedido.ASIGNADO || nuevoEstado == EstadoPedido.CANCELADO;
            case ASIGNADO:
                return nuevoEstado == EstadoPedido.EN_RUTA || nuevoEstado == EstadoPedido.CANCELADO;
            case EN_RUTA:
                return nuevoEstado == EstadoPedido.ENTREGADO || nuevoEstado == EstadoPedido.CANCELADO;
            case ENTREGADO:
            case CANCELADO:
                return false; // Estados finales
            default:
                return false;
        }
    }
    
    /**
     * Clase interna para estadísticas de pedidos
     */
    public static class EstadisticasPedidos {
        private final long pendientes;
        private final long asignados;
        private final long enRuta;
        private final long entregados;
        private final long cancelados;
        private final long total;
        
        public EstadisticasPedidos(long pendientes, long asignados, long enRuta, 
                                 long entregados, long cancelados, long total) {
            this.pendientes = pendientes;
            this.asignados = asignados;
            this.enRuta = enRuta;
            this.entregados = entregados;
            this.cancelados = cancelados;
            this.total = total;
        }
        
        // Getters
        public long getPendientes() { return pendientes; }
        public long getAsignados() { return asignados; }
        public long getEnRuta() { return enRuta; }
        public long getEntregados() { return entregados; }
        public long getCancelados() { return cancelados; }
        public long getTotal() { return total; }
        
        @Override
        public String toString() {
            return String.format("EstadisticasPedidos{pendientes=%d, asignados=%d, enRuta=%d, entregados=%d, cancelados=%d, total=%d}",
                               pendientes, asignados, enRuta, entregados, cancelados, total);
        }
    }
}
