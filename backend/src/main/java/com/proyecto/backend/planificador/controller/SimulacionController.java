package com.proyecto.backend.planificador.controller;

import com.proyecto.backend.planificador.dto.SimulacionEstadoDTO;
import com.proyecto.backend.planificador.service.SimulacionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controlador REST para gestionar la simulación en tiempo real
 * 
 * Endpoints disponibles:
 * - POST /api/simulacion/iniciar  - Inicia la simulación
 * - POST /api/simulacion/pausar   - Pausa la simulación
 * - POST /api/simulacion/reanudar - Reanuda la simulación pausada
 * - POST /api/simulacion/detener  - Detiene y resetea la simulación
 * - GET  /api/simulacion/estado   - Obtiene el estado actual
 * - GET  /api/simulacion/stream   - Stream SSE en tiempo real
 */
@RestController
@RequestMapping("/api/simulacion")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SimulacionController {

    private final SimulacionOrchestrator orchestrator;
    
    // ⭐ Lista de clientes conectados para SSE (thread-safe)
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * POST /api/simulacion/iniciar
     * 
     * Inicia una nueva simulación
     * 
     * @return Estado de la simulación después de iniciar
     */
    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciarSimulacion() {
        try {
            log.info("📥 Solicitud de iniciar simulación recibida");
            orchestrator.iniciar();
            
            SimulacionEstadoDTO estado = orchestrator.getEstadoDTO();
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Simulación iniciada exitosamente");
            response.put("estado", estado);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.warn("⚠️ No se pudo iniciar simulación: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ Error al iniciar simulación", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno al iniciar simulación: " + e.getMessage()));
        }
    }

    /**
     * POST /api/simulacion/pausar
     * 
     * Pausa la simulación actual
     * 
     * @return Estado de la simulación después de pausar
     */
    @PostMapping("/pausar")
    public ResponseEntity<?> pausarSimulacion() {
        try {
            log.info("📥 Solicitud de pausar simulación recibida");
            orchestrator.pausar();
            
            SimulacionEstadoDTO estado = orchestrator.getEstadoDTO();
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Simulación pausada exitosamente");
            response.put("estado", estado);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.warn("⚠️ No se pudo pausar simulación: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ Error al pausar simulación", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno al pausar simulación: " + e.getMessage()));
        }
    }

    /**
     * POST /api/simulacion/reanudar
     * 
     * Reanuda una simulación pausada
     * 
     * @return Estado de la simulación después de reanudar
     */
    @PostMapping("/reanudar")
    public ResponseEntity<?> reanudarSimulacion() {
        try {
            log.info("📥 Solicitud de reanudar simulación recibida");
            orchestrator.reanudar();
            
            SimulacionEstadoDTO estado = orchestrator.getEstadoDTO();
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Simulación reanudada exitosamente");
            response.put("estado", estado);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.warn("⚠️ No se pudo reanudar simulación: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ Error al reanudar simulación", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno al reanudar simulación: " + e.getMessage()));
        }
    }

    /**
     * POST /api/simulacion/detener
     * 
     * Detiene completamente la simulación y resetea el estado
     * 
     * @return Mensaje de confirmación
     */
    @PostMapping("/detener")
    public ResponseEntity<?> detenerSimulacion() {
        try {
            log.info("📥 Solicitud de detener simulación recibida");
            orchestrator.detener();
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Simulación detenida y reseteada exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al detener simulación", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno al detener simulación: " + e.getMessage()));
        }
    }

    /**
     * GET /api/simulacion/estado
     * 
     * Obtiene el estado actual de la simulación (una sola vez)
     * 
     * @return DTO con el estado completo de la simulación
     */
    @GetMapping("/estado")
    public ResponseEntity<SimulacionEstadoDTO> obtenerEstado() {
        try {
            log.debug("📥 Solicitud de estado de simulación recibida");
            SimulacionEstadoDTO estado = orchestrator.getEstadoDTO();
            return ResponseEntity.ok(estado);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener estado de simulación", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * ⭐ GET /api/simulacion/stream
     * 
     * Stream SSE en tiempo real: devuelve constantemente el tiempo simulado y tiempo real
     * 
     * Uso en frontend:
     * const eventSource = new EventSource('http://localhost:8080/api/simulacion/stream');
     * eventSource.onmessage = (event) => {
     *   const data = JSON.parse(event.data);
     *   console.log('Hora simulada:', data.horaSimulada);
     *   console.log('Tiempo real (ms):', data.tiempoRealTranscurridoMs);
     * };
     * 
     * @return SseEmitter que envía JSON cada tick (cada segundo)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTiempos() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Sin timeout
        
        emitters.add(emitter);
        log.info("📡 Cliente conectado al stream SSE. Total clientes: {}", emitters.size());
        
        // Limpiar cuando se desconecta
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("� Cliente desconectado del stream SSE. Total clientes: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.warn("⏱️ Timeout del stream SSE. Total clientes: {}", emitters.size());
        });
        emitter.onError((e) -> {
            emitters.remove(emitter);
            log.error("❌ Error en stream SSE: {}", e.getMessage());
        });
        
        // Enviar estado inicial inmediatamente
        try {
            emitter.send(orchestrator.getEstadoDTO());
            log.debug("📤 Estado inicial enviado al cliente");
        } catch (IOException e) {
            log.error("❌ Error al enviar estado inicial", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * ⭐ Método interno para broadcast del estado a todos los clientes conectados
     * Este método debe ser llamado desde SimulacionOrchestrator después de cada tick
     * 
     * @param estado Estado actual de la simulación
     */
    public void broadcastEstado(SimulacionEstadoDTO estado) {
        if (emitters.isEmpty()) {
            return; // No hay clientes conectados
        }
        
        log.debug("📤 Broadcasting estado a {} clientes", emitters.size());
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(estado);
            } catch (IOException e) {
                log.warn("❌ Error al enviar a cliente, removiendo: {}", e.getMessage());
                emitters.remove(emitter); // Remover clientes desconectados
            }
        }
    }
}
