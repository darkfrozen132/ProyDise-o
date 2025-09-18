package com.morapack.controller;

import com.morapack.model.Pedido;
import com.morapack.model.EstadoPedido;
import com.morapack.service.PedidoService;
import java.util.List;
import java.util.Optional;

/**
 * Controlador REST para la gestión de pedidos
 * Preparado para ser anotado con @RestController cuando se integre Spring Boot
 * 
 * Endpoints que se implementarían:
 * GET    /api/pedidos                    - Obtener todos los pedidos
 * GET    /api/pedidos/{id}               - Obtener pedido por ID
 * POST   /api/pedidos                    - Crear nuevo pedido
 * PUT    /api/pedidos/{id}               - Actualizar pedido
 * DELETE /api/pedidos/{id}               - Eliminar pedido
 * 
 * GET    /api/pedidos/estado/{estado}    - Pedidos por estado
 * GET    /api/pedidos/sede/{sedeId}      - Pedidos por sede
 * GET    /api/pedidos/pendientes         - Pedidos pendientes
 * GET    /api/pedidos/sin-asignar        - Pedidos sin asignar
 * GET    /api/pedidos/estadisticas       - Estadísticas de pedidos
 * 
 * POST   /api/pedidos/{id}/asignar-sede  - Asignar sede a pedido
 * PUT    /api/pedidos/{id}/estado        - Cambiar estado de pedido
 */
public class PedidoController {
    
    private final PedidoService pedidoService;
    
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }
    
    // @GetMapping("/api/pedidos")
    public List<Pedido> obtenerTodosPedidos() {
        return pedidoService.obtenerTodosPedidos();
    }
    
    // @GetMapping("/api/pedidos/{id}")
    public ResponseEntity<Pedido> obtenerPedidoPorId(String id) {
        Optional<Pedido> pedido = pedidoService.obtenerPedidoPorId(id);
        return pedido.isPresent() ? 
            ResponseEntity.ok(pedido.get()) : 
            ResponseEntity.notFound().build();
    }
    
    // @PostMapping("/api/pedidos")
    public ResponseEntity<Pedido> crearPedido(Pedido pedido) {
        try {
            Pedido pedidoCreado = pedidoService.crearPedido(pedido);
            return ResponseEntity.ok(pedidoCreado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // @PutMapping("/api/pedidos/{id}")
    public ResponseEntity<Pedido> actualizarPedido(String id, Pedido pedido) {
        try {
            pedido.setId(id);
            Pedido pedidoActualizado = pedidoService.actualizarPedido(pedido);
            return ResponseEntity.ok(pedidoActualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // @DeleteMapping("/api/pedidos/{id}")
    public ResponseEntity<Void> eliminarPedido(String id) {
        try {
            pedidoService.eliminarPedido(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // @GetMapping("/api/pedidos/estado/{estado}")
    public List<Pedido> obtenerPedidosPorEstado(EstadoPedido estado) {
        return pedidoService.obtenerPedidosPorEstado(estado);
    }
    
    // @GetMapping("/api/pedidos/sede/{sedeId}")
    public List<Pedido> obtenerPedidosPorSede(String sedeId) {
        return pedidoService.obtenerPedidosPorSede(sedeId);
    }
    
    // @GetMapping("/api/pedidos/pendientes")
    public List<Pedido> obtenerPedidosPendientes() {
        return pedidoService.obtenerPedidosPendientes();
    }
    
    // @GetMapping("/api/pedidos/sin-asignar")
    public List<Pedido> obtenerPedidosSinAsignar() {
        return pedidoService.obtenerPedidosSinAsignar();
    }
    
    // @GetMapping("/api/pedidos/alta-prioridad")
    public List<Pedido> obtenerPedidosAltaPrioridad() {
        return pedidoService.obtenerPedidosAltaPrioridad();
    }
    
    // @GetMapping("/api/pedidos/vencidos")
    public List<Pedido> obtenerPedidosVencidos() {
        return pedidoService.obtenerPedidosVencidos();
    }
    
    // @GetMapping("/api/pedidos/para-optimizacion")
    public List<Pedido> obtenerPedidosParaOptimizacion() {
        return pedidoService.obtenerPedidosParaOptimizacion();
    }
    
    // @GetMapping("/api/pedidos/estadisticas")
    public PedidoService.EstadisticasPedidos obtenerEstadisticas() {
        return pedidoService.obtenerEstadisticas();
    }
    
    // @PostMapping("/api/pedidos/{id}/asignar-sede")
    public ResponseEntity<Pedido> asignarSedeAPedido(String id, AsignarSedeRequest request) {
        try {
            Pedido pedido = pedidoService.asignarSedeAPedido(id, request.getSedeId());
            return ResponseEntity.ok(pedido);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // @PutMapping("/api/pedidos/{id}/estado")
    public ResponseEntity<Pedido> cambiarEstadoPedido(String id, CambiarEstadoRequest request) {
        try {
            Pedido pedido = pedidoService.cambiarEstadoPedido(id, request.getNuevoEstado());
            return ResponseEntity.ok(pedido);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * DTO para asignar sede a pedido
     */
    public static class AsignarSedeRequest {
        private String sedeId;
        
        public AsignarSedeRequest() {}
        
        public AsignarSedeRequest(String sedeId) {
            this.sedeId = sedeId;
        }
        
        public String getSedeId() { return sedeId; }
        public void setSedeId(String sedeId) { this.sedeId = sedeId; }
    }
    
    /**
     * DTO para cambiar estado de pedido
     */
    public static class CambiarEstadoRequest {
        private EstadoPedido nuevoEstado;
        
        public CambiarEstadoRequest() {}
        
        public CambiarEstadoRequest(EstadoPedido nuevoEstado) {
            this.nuevoEstado = nuevoEstado;
        }
        
        public EstadoPedido getNuevoEstado() { return nuevoEstado; }
        public void setNuevoEstado(EstadoPedido nuevoEstado) { this.nuevoEstado = nuevoEstado; }
    }
    
    /**
     * Simulación de ResponseEntity para cuando no se use Spring Boot
     */
    public static class ResponseEntity<T> {
        private final T body;
        private final int status;
        
        private ResponseEntity(T body, int status) {
            this.body = body;
            this.status = status;
        }
        
        public static <T> ResponseEntity<T> ok(T body) {
            return new ResponseEntity<>(body, 200);
        }
        
        public static <T> ResponseEntity<T> ok() {
            return new ResponseEntity<>(null, 200);
        }
        
        public static <T> ResponseEntity<T> notFound() {
            return new ResponseEntity<>(null, 404);
        }
        
        public static <T> ResponseEntity<T> badRequest() {
            return new ResponseEntity<>(null, 400);
        }
        
        public ResponseEntity<T> build() {
            return this;
        }
        
        public T getBody() { return body; }
        public int getStatus() { return status; }
    }
}
