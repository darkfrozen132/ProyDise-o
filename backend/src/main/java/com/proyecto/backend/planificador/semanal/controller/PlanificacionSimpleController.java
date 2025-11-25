package com.proyecto.backend.planificador.semanal.controller;

import com.proyecto.backend.planificador.semanal.dto.request.PlanificacionRequest;
import com.proyecto.backend.planificador.semanal.dto.response.PlanificacionResponseSimple;
import com.proyecto.backend.planificador.semanal.service.AlgoritmoGeneticoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller REST para planificación semanal SIMPLIFICADO
 * 
 * Este endpoint ejecuta la planificación de forma síncrona y devuelve
 * todos los vuelos de una sola vez.
 */
@RestController
@RequestMapping("/api/planificacion")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PlanificacionSimpleController {
    
    private final AlgoritmoGeneticoService algoritmoService;
    
    /**
     * Ejecuta planificación semanal de forma SIMPLE
     * 
     * @param fecha Fecha inicial (YYYY-MM-DD)
     * @param factorK Factor de amplificación temporal (default: 5)
     * @return Todos los vuelos planificados
     */
    @PostMapping("/ejecutar-simple")
    public ResponseEntity<?> ejecutarPlanificacionSimple(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "5") Integer factorK,
            @RequestParam(defaultValue = "20") Integer tamanioPoblacion,
            @RequestParam(defaultValue = "20") Integer maxGeneraciones,
            @RequestParam(defaultValue = "10") Integer limiteGeneracionesSinMejora
    ) {
        try {
            log.info("═══════════════════════════════════════════════════════════");
            log.info("🚀 PLANIFICACIÓN SIMPLE INICIADA");
            log.info("📅 Fecha: {}", fecha);
            log.info("⚙️ Factor K: {}", factorK);
            log.info("📊 Población: {}", tamanioPoblacion);
            log.info("🧬 Generaciones: {}", maxGeneraciones);
            log.info("═══════════════════════════════════════════════════════════");
            
            // Validar fecha
            if (fecha.getYear() < 2025) {
                fecha = LocalDate.of(2025, 1, 1);
                log.warn("⚠️ Fecha ajustada a: {}", fecha);
            }
            
            // Crear request con los parámetros
            PlanificacionRequest.ParametrosGenetico parametros = new PlanificacionRequest.ParametrosGenetico();
            parametros.setTamanioPoblacion(tamanioPoblacion);
            parametros.setMaxGeneraciones(maxGeneraciones);
            parametros.setLimiteGeneracionesSinMejora(limiteGeneracionesSinMejora);
            
            PlanificacionRequest request = new PlanificacionRequest(
                    fecha,
                    factorK,
                    parametros
            );
            
            long inicio = System.currentTimeMillis();
            
            // Ejecutar planificación SÍNCRONA
            PlanificacionResponseSimple resultado = algoritmoService.planificarSimple(request);
            
            long duracion = System.currentTimeMillis() - inicio;
            
            log.info("═══════════════════════════════════════════════════════════");
            log.info("✅ PLANIFICACIÓN COMPLETADA");
            log.info("⏱️ Duración: {} ms ({} segundos)", duracion, duracion / 1000);
            log.info("✈️ Vuelos generados: {}", resultado.getTotalVuelos());
            log.info("📦 Pedidos planificados: {}", resultado.getTotalPedidos());
            log.info("═══════════════════════════════════════════════════════════");
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error en planificación simple", e);
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of(
                            "error", "Error al ejecutar planificación",
                            "mensaje", e.getMessage()
                    ));
        }
    }
}
