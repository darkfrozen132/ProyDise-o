package com.proyecto.backend.controller;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.service.AeropuertoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aeropuertos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AeropuertoController {

    private final AeropuertoService aeropuertoService;

    /**
     * Carga aeropuertos desde el archivo de texto
     * POST /api/aeropuertos/cargar
     */
    @PostMapping("/cargar")
    public ResponseEntity<Map<String, Object>> cargarAeropuertos() {
        try {
            List<Aeropuerto> aeropuertos = aeropuertoService.cargarDesdeArchivo();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Aeropuertos cargados exitosamente");
            response.put("cantidad", aeropuertos.size());
            response.put("aeropuertos", aeropuertos);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al cargar aeropuertos");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene todos los aeropuertos
     * GET /api/aeropuertos
     */
    @GetMapping
    public ResponseEntity<List<Aeropuerto>> obtenerTodos() {
        return ResponseEntity.ok(aeropuertoService.obtenerTodos());
    }

    /**
     * Busca un aeropuerto por codigo ICAO
     * GET /api/aeropuertos/{codigo}
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<Aeropuerto> buscarPorCodigo(@PathVariable String codigo) {
        try {
            Aeropuerto aeropuerto = aeropuertoService.buscarPorCodigo(codigo);
            return ResponseEntity.ok(aeropuerto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint de prueba: Carga y retorna resumen de aeropuertos
     * POST /api/aeropuertos/test-carga
     */
    @PostMapping("/test-carga")
    public ResponseEntity<Map<String, Object>> testCarga() {
        try {
            List<Aeropuerto> aeropuertos = aeropuertoService.cargarDesdeArchivo();

            Map<String, Object> response = new HashMap<>();
            response.put("exitoso", true);
            response.put("totalCargados", aeropuertos.size());

            // Agrupar por continente
            Map<String, Long> porContinente = aeropuertos.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Aeropuerto::getContinente,
                    java.util.stream.Collectors.counting()
                ));
            response.put("aeropuertosPorContinente", porContinente);

            // Mostrar primeros 3 aeropuertos como muestra
            List<Map<String, Object>> muestra = aeropuertos.stream()
                .limit(3)
                .map(a -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("codigo", a.getCodigoICAO());
                    item.put("ciudad", a.getCiudad());
                    item.put("pais", a.getPais());
                    item.put("continente", a.getContinente());
                    item.put("coordenadas", String.format("%.4f, %.4f", a.getLatitud(), a.getLongitud()));
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
            response.put("muestra", muestra);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("exitoso", false);
            error.put("error", "Error al cargar aeropuertos");
            error.put("detalle", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Recarga aeropuertos: Limpia la BD y carga desde el archivo
     * POST /api/aeropuertos/recargar
     */
    @PostMapping("/recargar")
    public ResponseEntity<Map<String, Object>> recargarAeropuertos() {
        try {
            List<Aeropuerto> aeropuertos = aeropuertoService.recargarDesdeArchivo();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Aeropuertos recargados exitosamente (BD limpiada y recargada)");
            response.put("cantidad", aeropuertos.size());
            response.put("aeropuertos", aeropuertos);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al recargar aeropuertos");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Limpia todos los aeropuertos de la BD
     * DELETE /api/aeropuertos/limpiar
     */
    @DeleteMapping("/limpiar")
    public ResponseEntity<Map<String, Object>> limpiarAeropuertos() {
        try {
            aeropuertoService.limpiarAeropuertos();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Todos los aeropuertos han sido eliminados de la base de datos");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al limpiar aeropuertos");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

}
