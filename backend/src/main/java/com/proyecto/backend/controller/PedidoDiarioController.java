package com.proyecto.backend.controller;

import com.proyecto.backend.dto.PedidoDiarioRequest;
import com.proyecto.backend.model.PedidoDiario;
import com.proyecto.backend.service.PedidoDiarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para gestionar pedidos diarios
 * Base URL: /api/pedidos-diarios
 */
@RestController
@RequestMapping("/api/pedidos-diarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PedidoDiarioController {

    private final PedidoDiarioService pedidoDiarioService;

    /**
     * Crea un nuevo pedido diario
     * POST /api/pedidos-diarios
     * 
     * Body ejemplo:
     * {
     *   "clienteId": "0054321",
     *   "aeropuertoDestinoId": "SEQM",
     *   "cantidadProductos": 145,
     *   "dia": 30,
     *   "mes": 1,
     *   "anio": 2015,
     *   "hora": 9,
     *   "minuto": 15
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearPedido(@Valid @RequestBody PedidoDiarioRequest request) {
        try {
            PedidoDiario pedido = pedidoDiarioService.crearPedido(request);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Pedido diario creado exitosamente");
            response.put("pedido", pedido);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al crear pedido diario");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene todos los pedidos diarios
     * GET /api/pedidos-diarios
     */
    @GetMapping
    public ResponseEntity<List<PedidoDiario>> obtenerTodos() {
        return ResponseEntity.ok(pedidoDiarioService.obtenerTodos());
    }

    /**
     * Obtiene un pedido por su ID
     * GET /api/pedidos-diarios/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return pedidoDiarioService.obtenerPorId(id)
            .map(pedido -> ResponseEntity.ok((Object) pedido))
            .orElseGet(() -> {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Pedido no encontrado");
                error.put("id", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            });
    }

    /**
     * Busca pedidos por aeropuerto destino
     * GET /api/pedidos-diarios/destino/{aeropuertoId}
     */
    @GetMapping("/destino/{aeropuertoId}")
    public ResponseEntity<List<PedidoDiario>> buscarPorDestino(@PathVariable String aeropuertoId) {
        return ResponseEntity.ok(pedidoDiarioService.buscarPorAeropuertoDestino(aeropuertoId));
    }

    /**
     * Busca pedidos por cliente
     * GET /api/pedidos-diarios/cliente/{clienteId}
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoDiario>> buscarPorCliente(@PathVariable String clienteId) {
        return ResponseEntity.ok(pedidoDiarioService.buscarPorCliente(clienteId));
    }

    /**
     * Busca pedidos por día
     * GET /api/pedidos-diarios/dia/{dia}
     */
    @GetMapping("/dia/{dia}")
    public ResponseEntity<List<PedidoDiario>> buscarPorDia(@PathVariable int dia) {
        return ResponseEntity.ok(pedidoDiarioService.buscarPorDia(dia));
    }

    /**
     * Elimina un pedido por su ID
     * DELETE /api/pedidos-diarios/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarPedido(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        if (pedidoDiarioService.eliminarPedido(id)) {
            response.put("mensaje", "Pedido diario eliminado exitosamente");
            response.put("id", id);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Pedido no encontrado");
            response.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Elimina todos los pedidos diarios
     * DELETE /api/pedidos-diarios/limpiar
     */
    @DeleteMapping("/limpiar")
    public ResponseEntity<Map<String, Object>> limpiarPedidos() {
        try {
            pedidoDiarioService.eliminarTodos();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Todos los pedidos diarios han sido eliminados");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al limpiar pedidos diarios");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene estadísticas de pedidos diarios
     * GET /api/pedidos-diarios/estadisticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPedidos", pedidoDiarioService.contarPedidos());
        return ResponseEntity.ok(stats);
    }
}
