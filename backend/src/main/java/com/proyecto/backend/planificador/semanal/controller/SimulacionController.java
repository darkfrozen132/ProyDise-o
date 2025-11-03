package com.proyecto.backend.planificador.semanal.controller;

import com.proyecto.backend.planificador.semanal.dto.sse.IniciarSimulacionRequest;
import com.proyecto.backend.planificador.semanal.dto.sse.SimulacionEstadoDTO;
import com.proyecto.backend.planificador.service.SimulacionOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST + SSE para simulación incremental de planificación
 * 
 * Endpoints:
 * - POST /api/simulacion/iniciar - Inicia una simulación incremental
 * - POST /api/simulacion/detener - Detiene la simulación actual
 * - GET /api/simulacion/estado - Obtiene el estado de la simulación
 * - GET /api/simulacion/stream - Stream SSE para recibir eventos en tiempo real
 */
@RestController
@RequestMapping("/api/simulacion")
@RequiredArgsConstructor
@Slf4j
public class SimulacionController {

    private final SimulacionOrchestrator simulacionOrchestrator;

    /**
     * Inicia una nueva simulación incremental
     * 
     * POST /api/simulacion/iniciar
     * 
     * Body ejemplo:
     * {
     *   "fecha": "2025-01-15",
     *   "saltoMinutos": 5,
     *   "tamanioPoblacion": 50,
     *   "maxGeneraciones": 200
     * }
     * 
     * @param request Parámetros de la simulación
     * @return Confirmación de inicio
     */
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciarSimulacion(@Valid @RequestBody IniciarSimulacionRequest request) {
        log.info("POST /api/simulacion/iniciar - fecha: {}, saltoMinutos: {}", 
            request.getFecha(), request.getSaltoMinutos());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar que no haya una simulación activa
            if (simulacionOrchestrator.estaActiva()) {
                response.put("success", false);
                response.put("error", "Ya hay una simulación activa. Detenla primero.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Iniciar simulación
            simulacionOrchestrator.iniciarSimulacion(
                request.getFecha(),
                request.getSaltoMinutos(),
                request.getTamanioPoblacion(),
                request.getMaxGeneraciones()
            );
            
            response.put("success", true);
            response.put("mensaje", "Simulación iniciada exitosamente");
            response.put("fecha", request.getFecha());
            response.put("saltoMinutos", request.getSaltoMinutos());
            response.put("instrucciones", "Conecta a GET /api/simulacion/stream para recibir eventos en tiempo real");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.error("Error de estado al iniciar simulación: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error al iniciar simulación", e);
            response.put("success", false);
            response.put("error", "Error interno al iniciar simulación");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Detiene la simulación actual
     * 
     * POST /api/simulacion/detener
     * 
     * @return Confirmación de detención
     */
    @PostMapping("/detener")
    public ResponseEntity<Map<String, Object>> detenerSimulacion() {
        log.info("POST /api/simulacion/detener");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!simulacionOrchestrator.estaActiva()) {
                response.put("success", false);
                response.put("mensaje", "No hay simulación activa para detener");
                return ResponseEntity.badRequest().body(response);
            }
            
            simulacionOrchestrator.detenerSimulacion();
            
            response.put("success", true);
            response.put("mensaje", "Simulación detenida exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al detener simulación", e);
            response.put("success", false);
            response.put("error", "Error interno al detener simulación");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Obtiene el estado actual de la simulación
     * 
     * GET /api/simulacion/estado
     * 
     * @return Estado de la simulación
     */
    @GetMapping("/estado")
    public ResponseEntity<SimulacionEstadoDTO> obtenerEstado() {
        log.info("GET /api/simulacion/estado");
        
        try {
            SimulacionEstadoDTO estado = simulacionOrchestrator.obtenerEstado();
            return ResponseEntity.ok(estado);
            
        } catch (Exception e) {
            log.error("Error al obtener estado de simulación", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Stream SSE para recibir eventos de simulación en tiempo real
     * 
     * GET /api/simulacion/stream
     * 
     * Eventos enviados:
     * - "estado": Estado inicial de la simulación
     * - "tick": Cada tick de la simulación con la planificación actualizada
     * - "finalizado": Cuando la simulación termina
     * 
     * Ejemplo de uso desde JavaScript:
     * ```javascript
     * const eventSource = new EventSource('/api/simulacion/stream');
     * 
     * eventSource.addEventListener('estado', (event) => {
     *   const estado = JSON.parse(event.data);
     *   console.log('Estado inicial:', estado);
     * });
     * 
     * eventSource.addEventListener('tick', (event) => {
     *   const tick = JSON.parse(event.data);
     *   console.log('Tick', tick.tick, '- Progreso:', tick.progreso);
     *   // Actualizar mapa con tick.planificacion
     * });
     * 
     * eventSource.addEventListener('finalizado', (event) => {
     *   console.log('Simulación completada');
     *   eventSource.close();
     * });
     * ```
     * 
     * @return SseEmitter para streaming de eventos
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        log.info("GET /api/simulacion/stream - Nuevo cliente conectado");
        
        try {
            SseEmitter emitter = simulacionOrchestrator.registrarCliente();
            
            log.info("Cliente SSE registrado exitosamente");
            return emitter;
            
        } catch (Exception e) {
            log.error("Error al registrar cliente SSE", e);
            
            // Crear un emitter de emergencia para enviar el error
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                    .name("error")
                    .data("Error al conectar con el stream de simulación"));
                errorEmitter.complete();
            } catch (Exception sendError) {
                log.error("Error enviando mensaje de error", sendError);
            }
            
            return errorEmitter;
        }
    }
    
    /**
     * Health check del servicio de simulación
     * 
     * GET /api/simulacion/health
     * 
     * @return Estado de salud del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("servicio", "Simulación Incremental SSE");
        health.put("simulacionActiva", simulacionOrchestrator.estaActiva());
        
        if (simulacionOrchestrator.estaActiva()) {
            SimulacionEstadoDTO estado = simulacionOrchestrator.obtenerEstado();
            health.put("tickActual", estado.getTickActual());
            health.put("progreso", String.format("%.1f%%", estado.getProgreso() * 100));
            health.put("clientesConectados", estado.getClientesConectados());
        }
        
        return ResponseEntity.ok(health);
    }
}
