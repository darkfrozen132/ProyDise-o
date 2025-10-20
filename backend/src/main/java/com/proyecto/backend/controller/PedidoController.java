package com.proyecto.backend.controller;

import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    /**
     * Obtiene todos los pedidos
     * GET /api/pedidos
     */
    @GetMapping
    public ResponseEntity<List<Pedido>> obtenerTodos() {
        return ResponseEntity.ok(pedidoService.obtenerTodos());
    }

    /**
     * Carga pedidos desde el archivo de texto
     * IMPORTANTE: Limpia la BD antes de cargar automáticamente
     * POST /api/pedidos/cargar
     * NOTA: Debe estar ANTES de /{id} para evitar conflictos de rutas
     */
    @PostMapping("/cargar")
    public ResponseEntity<Map<String, Object>> cargarPedidos() {
        try {
            List<Pedido> pedidos = pedidoService.cargarDesdeArchivo();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Pedidos cargados exitosamente (BD limpiada automáticamente)");
            response.put("cantidad", pedidos.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al cargar pedidos");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Limpia todos los pedidos de la BD
     * DELETE /api/pedidos/limpiar
     * NOTA: Debe estar ANTES de /{id} para evitar conflictos de rutas
     */
    @DeleteMapping("/limpiar")
    public ResponseEntity<Map<String, Object>> limpiarPedidos() {
        try {
            pedidoService.limpiarPedidos();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Todos los pedidos han sido eliminados de la base de datos");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al limpiar pedidos");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene estadísticas de pedidos
     * GET /api/pedidos/estadisticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Long>> obtenerEstadisticas() {
        return ResponseEntity.ok(pedidoService.obtenerEstadisticas());
    }

    /**
     * Busca pedidos por estado
     * GET /api/pedidos/estado/{estado}
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Pedido>> buscarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(pedidoService.buscarPorEstado(estado));
    }

    /**
     * Busca pedidos por aeropuerto destino
     * GET /api/pedidos/destino/{aeropuertoId}
     */
    @GetMapping("/destino/{aeropuertoId}")
    public ResponseEntity<List<Pedido>> buscarPorDestino(@PathVariable String aeropuertoId) {
        return ResponseEntity.ok(pedidoService.buscarPorAeropuertoDestino(aeropuertoId));
    }

    /**
     * Busca pedidos por cliente
     * GET /api/pedidos/cliente/{clienteId}
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Pedido>> buscarPorCliente(@PathVariable String clienteId) {
        return ResponseEntity.ok(pedidoService.buscarPorCliente(clienteId));
    }

    /**
     * Busca pedidos por día
     * GET /api/pedidos/dia/{dia}
     */
    @GetMapping("/dia/{dia}")
    public ResponseEntity<List<Pedido>> buscarPorDia(@PathVariable int dia) {
        return ResponseEntity.ok(pedidoService.buscarPorDia(dia));
    }

    /**
     * Crea un nuevo pedido
     * POST /api/pedidos
     */
    @PostMapping
    public ResponseEntity<Pedido> crear(@Valid @RequestBody Pedido pedido) {
        try {
            Pedido creado = pedidoService.crear(pedido);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Actualiza un pedido existente
     * PUT /api/pedidos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Pedido> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Pedido pedido) {
        try {
            Pedido actualizado = pedidoService.actualizar(id, pedido);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Actualiza el estado de un pedido
     * PATCH /api/pedidos/{id}/estado
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Pedido> actualizarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String nuevoEstado = body.get("estado");
            if (nuevoEstado == null || nuevoEstado.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            Pedido actualizado = pedidoService.actualizarEstado(id, nuevoEstado);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Elimina un pedido
     * DELETE /api/pedidos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        try {
            pedidoService.eliminar(id);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Pedido eliminado exitosamente");
            response.put("id", id);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Busca un pedido por ID
     * GET /api/pedidos/{id}
     * NOTA: Debe estar AL FINAL para que las rutas específicas se evalúen primero
     */
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscarPorId(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoService.buscarPorId(id);
            return ResponseEntity.ok(pedido);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
