package com.proyecto.backend.controller;

import com.proyecto.backend.model.PlanDeVuelo;
import com.proyecto.backend.service.PlanDeVueloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planesdevuelo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlanDeVueloController {

    private final PlanDeVueloService planDeVueloService;

    /**
     * Obtiene todos los planes de vuelo
     * GET /api/planesdevuelo
     */
    @GetMapping
    public ResponseEntity<List<PlanDeVuelo>> obtenerTodos() {
        return ResponseEntity.ok(planDeVueloService.obtenerTodos());
    }

    /**
     * Busca un plan de vuelo por ID
     * GET /api/planesdevuelo/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanDeVuelo> buscarPorId(@PathVariable Long id) {
        try {
            PlanDeVuelo planDeVuelo = planDeVueloService.buscarPorId(id);
            return ResponseEntity.ok(planDeVuelo);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Busca planes de vuelo por aeropuerto de origen
     * GET /api/planesdevuelo/origen/{codigoICAO}
     */
    @GetMapping("/origen/{codigoICAO}")
    public ResponseEntity<List<PlanDeVuelo>> buscarPorOrigen(@PathVariable String codigoICAO) {
        return ResponseEntity.ok(planDeVueloService.buscarPorOrigen(codigoICAO));
    }

    /**
     * Busca planes de vuelo por aeropuerto de destino
     * GET /api/planesdevuelo/destino/{codigoICAO}
     */
    @GetMapping("/destino/{codigoICAO}")
    public ResponseEntity<List<PlanDeVuelo>> buscarPorDestino(@PathVariable String codigoICAO) {
        return ResponseEntity.ok(planDeVueloService.buscarPorDestino(codigoICAO));
    }

    /**
     * Busca planes de vuelo entre dos aeropuertos
     * GET /api/planesdevuelo/ruta/{origen}/{destino}
     */
    @GetMapping("/ruta/{origen}/{destino}")
    public ResponseEntity<List<PlanDeVuelo>> buscarPorRuta(
            @PathVariable String origen,
            @PathVariable String destino) {
        return ResponseEntity.ok(planDeVueloService.buscarPorRuta(origen, destino));
    }

    /**
     * Crea un nuevo plan de vuelo
     * POST /api/planesdevuelo
     */
    @PostMapping
    public ResponseEntity<PlanDeVuelo> crear(@Valid @RequestBody PlanDeVuelo planDeVuelo) {
        try {
            PlanDeVuelo creado = planDeVueloService.crear(planDeVuelo);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Actualiza un plan de vuelo existente
     * PUT /api/planesdevuelo/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanDeVuelo> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlanDeVuelo planDeVuelo) {
        try {
            PlanDeVuelo actualizado = planDeVueloService.actualizar(id, planDeVuelo);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Elimina un plan de vuelo
     * DELETE /api/planesdevuelo/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        try {
            planDeVueloService.eliminar(id);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Plan de vuelo eliminado exitosamente");
            response.put("id", id);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Limpia todos los planes de vuelo de la BD
     * DELETE /api/planesdevuelo/limpiar
     */
    @DeleteMapping("/limpiar")
    public ResponseEntity<Map<String, Object>> limpiarPlanesDeVuelo() {
        try {
            planDeVueloService.limpiarPlanesDeVuelo();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Todos los planes de vuelo han sido eliminados de la base de datos");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al limpiar planes de vuelo");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Carga planes de vuelo desde el archivo de texto
     * POST /api/planesdevuelo/cargar
     */
    @PostMapping("/cargar")
    public ResponseEntity<Map<String, Object>> cargarPlanesDeVuelo() {
        try {
            List<PlanDeVuelo> planesDeVuelo = planDeVueloService.cargarDesdeArchivo();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Planes de vuelo cargados exitosamente");
            response.put("cantidad", planesDeVuelo.size());
            response.put("planesDeVuelo", planesDeVuelo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al cargar planes de vuelo");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Recarga planes de vuelo: Limpia la BD y carga desde el archivo
     * POST /api/planesdevuelo/recargar
     */
    @PostMapping("/recargar")
    public ResponseEntity<Map<String, Object>> recargarPlanesDeVuelo() {
        try {
            List<PlanDeVuelo> planesDeVuelo = planDeVueloService.recargarDesdeArchivo();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Planes de vuelo recargados exitosamente (BD limpiada y recargada)");
            response.put("cantidad", planesDeVuelo.size());
            response.put("planesDeVuelo", planesDeVuelo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al recargar planes de vuelo");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

}
