package com.proyecto.backend.controller;

import com.proyecto.backend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador temporal para operaciones administrativas
 * Puede ser eliminado en produccion
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final PedidoRepository pedidoRepository;

    /**
     * Actualiza todos los pedidos existentes para asignarles anio=2025 y mes=1
     * si tienen anio=0 o mes=0
     *
     * POST /api/admin/pedidos/actualizar-fecha
     */
    @PostMapping("/pedidos/actualizar-fecha")
    public ResponseEntity<Map<String, Object>> actualizarFechaPedidos() {
        log.info("POST /api/admin/pedidos/actualizar-fecha");

        Map<String, Object> resultado = new HashMap<>();

        try {
            var pedidos = pedidoRepository.findAll();
            int actualizados = 0;

            for (var pedido : pedidos) {
                boolean necesitaActualizacion = false;

                if (pedido.getAnio() == 0) {
                    pedido.setAnio(2025);
                    necesitaActualizacion = true;
                }

                if (pedido.getMes() == 0) {
                    pedido.setMes(1);
                    necesitaActualizacion = true;
                }

                if (necesitaActualizacion) {
                    pedidoRepository.save(pedido);
                    actualizados++;
                }
            }

            resultado.put("success", true);
            resultado.put("mensaje", "Pedidos actualizados exitosamente");
            resultado.put("totalPedidos", pedidos.size());
            resultado.put("pedidosActualizados", actualizados);

            log.info("Actualizados {} pedidos de {} totales", actualizados, pedidos.size());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Error al actualizar fechas de pedidos", e);
            resultado.put("success", false);
            resultado.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(resultado);
        }
    }

    /**
     * Obtiene estadisticas de pedidos por fecha
     *
     * GET /api/admin/pedidos/estadisticas
     */
    @GetMapping("/pedidos/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasPedidos() {
        log.info("GET /api/admin/pedidos/estadisticas");

        Map<String, Object> stats = new HashMap<>();

        try {
            var pedidos = pedidoRepository.findAll();

            long totalPedidos = pedidos.size();
            long pedidosPendientes = pedidos.stream()
                    .filter(p -> "PENDIENTE".equals(p.getEstado()))
                    .count();
            long pedidosSinFecha = pedidos.stream()
                    .filter(p -> p.getAnio() == 0 || p.getMes() == 0)
                    .count();

            // Agrupar por fecha
            Map<String, Long> porFecha = new HashMap<>();
            for (var pedido : pedidos) {
                String fecha = String.format("%04d-%02d-%02d",
                        pedido.getAnio(), pedido.getMes(), pedido.getDia());
                porFecha.merge(fecha, 1L, Long::sum);
            }

            stats.put("totalPedidos", totalPedidos);
            stats.put("pedidosPendientes", pedidosPendientes);
            stats.put("pedidosSinFecha", pedidosSinFecha);
            stats.put("pedidosPorFecha", porFecha);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error al obtener estadisticas", e);
            stats.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(stats);
        }
    }
}
