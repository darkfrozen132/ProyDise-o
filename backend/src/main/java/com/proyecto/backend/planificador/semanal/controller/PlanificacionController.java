package com.proyecto.backend.planificador.semanal.controller;

import com.proyecto.backend.planificador.semanal.dto.request.PlanificacionRequest;
import com.proyecto.backend.planificador.semanal.dto.response.PlanificacionResponse;
import com.proyecto.backend.planificador.semanal.service.AlgoritmoGeneticoService;
import com.proyecto.backend.planificador.semanal.service.WorldCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el algoritmo genetico de planificacion de rutas SEMANAL
 */
@RestController
@RequestMapping("/api/planificacion/semanal")
@RequiredArgsConstructor
@Slf4j
public class PlanificacionController {

    private final AlgoritmoGeneticoService algoritmoGeneticoService;
    private final WorldCacheService worldCacheService;

    /**
     * Ejecuta la planificacion de rutas con el algoritmo genetico
     *
     * POST /api/planificacion
     *
     * Body ejemplo:
     * {
     *   "fecha": "2025-01-15",
     *   "factorK": 1,
     *   "parametrosGenetico": {
     *     "tamanioPoblacion": 50,
     *     "maxGeneraciones": 200
     *   }
     * }
     *
     * @param request Parametros de planificacion
     * @return Response con la planificacion completa
     */
    @PostMapping
    public ResponseEntity<PlanificacionResponse> planificar(@Valid @RequestBody PlanificacionRequest request) {
        log.info("POST /api/planificacion - fecha: {}, K: {}", request.getFecha(), request.getFactorK());

        try {
            PlanificacionResponse response = algoritmoGeneticoService.planificar(request);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.error("Error de estado: {}", e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("Error al ejecutar planificacion", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene el estado actual del World (cache)
     *
     * GET /api/planificacion/world/estado
     *
     * @return Estado del World con estadisticas
     */
    @GetMapping("/world/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstadoWorld() {
        log.info("GET /api/planificacion/world/estado");

        Map<String, Object> estado = new HashMap<>();

        try {
            if (!worldCacheService.estaInicializado()) {
                estado.put("inicializado", false);
                estado.put("mensaje", "World no ha sido inicializado");
                return ResponseEntity.ok(estado);
            }

            estado.put("inicializado", true);
            estado.put("estadisticas", worldCacheService.getEstadisticas());
            estado.put("ultimaActualizacion", worldCacheService.getUltimaActualizacion());
            estado.put("numeroAeropuertos", worldCacheService.getWorld().getAeropuertos().size());
            estado.put("numeroVuelos", worldCacheService.getWorld().getPlanesVuelo().size());
            estado.put("hubs", worldCacheService.getWorld().getHubs());

            return ResponseEntity.ok(estado);

        } catch (Exception e) {
            log.error("Error al obtener estado del World", e);
            estado.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(estado);
        }
    }

    /**
     * Refresca el World cargando datos nuevamente desde la BD
     *
     * POST /api/planificacion/world/refrescar
     *
     * @return Confirmacion de refresco
     */
    @PostMapping("/world/refrescar")
    public ResponseEntity<Map<String, Object>> refrescarWorld() {
        log.info("POST /api/planificacion/world/refrescar");

        Map<String, Object> resultado = new HashMap<>();

        try {
            worldCacheService.refrescar();

            resultado.put("success", true);
            resultado.put("mensaje", "World refrescado exitosamente");
            resultado.put("estadisticas", worldCacheService.getEstadisticas());
            resultado.put("ultimaActualizacion", worldCacheService.getUltimaActualizacion());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Error al refrescar World", e);
            resultado.put("success", false);
            resultado.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(resultado);
        }
    }

    /**
     * Health check del servicio de planificacion
     *
     * GET /api/planificacion/health
     *
     * @return Estado de salud del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("servicio", "Algoritmo Genetico - Planificacion de Rutas");
        health.put("worldInicializado", worldCacheService.estaInicializado());

        if (worldCacheService.estaInicializado()) {
            health.put("ultimaActualizacion", worldCacheService.getUltimaActualizacion());
        }

        return ResponseEntity.ok(health);
    }
}
