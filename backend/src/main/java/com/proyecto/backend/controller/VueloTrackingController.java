package com.proyecto.backend.controller;

import com.proyecto.backend.service.VueloTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Controlador REST para el tracking de vuelos en tiempo real mediante SSE.
 */
@RestController
@RequestMapping("/api/vuelos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class VueloTrackingController {

    private final VueloTrackingService vueloTrackingService;

    /**
     * GET /api/vuelos/stream
     * 
     * Endpoint SSE para recibir actualizaciones de coordenadas en tiempo real
     * 
     * @return SseEmitter para streaming de eventos
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamVueloCoordinates() {
        log.info("🔌 Nueva conexión SSE para tracking de vuelos");
        return vueloTrackingService.registrarCliente();
    }

    /**
     * POST /api/vuelos/tracking/iniciar
     * 
     * Inicia el streaming de coordenadas del vuelo Lima -> Berlín
     * 
     * @return Estado de la operación
     */
    @PostMapping("/tracking/iniciar")
    public ResponseEntity<?> iniciarTracking() {
        try {
            log.info("📥 Solicitud de iniciar tracking de vuelos");
            vueloTrackingService.iniciarStreaming();
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Tracking de vuelos iniciado exitosamente",
                "streaming", true,
                "clientesConectados", vueloTrackingService.getClientesConectados()
            ));
            
        } catch (Exception e) {
            log.error("❌ Error al iniciar tracking", e);
            return ResponseEntity
                .internalServerError()
                .body(Map.of("error", "Error al iniciar tracking: " + e.getMessage()));
        }
    }

    /**
     * POST /api/vuelos/tracking/detener
     * 
     * Detiene el streaming de coordenadas
     * 
     * @return Estado de la operación
     */
    @PostMapping("/tracking/detener")
    public ResponseEntity<?> detenerTracking() {
        try {
            log.info("📥 Solicitud de detener tracking de vuelos");
            vueloTrackingService.detenerStreaming();
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Tracking de vuelos detenido exitosamente",
                "streaming", false
            ));
            
        } catch (Exception e) {
            log.error("❌ Error al detener tracking", e);
            return ResponseEntity
                .internalServerError()
                .body(Map.of("error", "Error al detener tracking: " + e.getMessage()));
        }
    }

    /**
     * GET /api/vuelos/tracking/estado
     * 
     * Obtiene el estado actual del tracking
     * 
     * @return Estado del streaming
     */
    @GetMapping("/tracking/estado")
    public ResponseEntity<?> obtenerEstadoTracking() {
        return ResponseEntity.ok(Map.of(
            "streaming", vueloTrackingService.isStreaming(),
            "clientesConectados", vueloTrackingService.getClientesConectados()
        ));
    }

    /**
     * GET /api/vuelos/posicion-actual/{id}
     * 
     * Obtiene la posición actual del vuelo desde la BD
     * 
     * @param id ID de la ruta
     * @return Coordenadas actuales del vuelo
     */
    @GetMapping("/posicion-actual/{id}")
    public ResponseEntity<?> obtenerPosicionActual(@PathVariable Long id) {
        return vueloTrackingService.obtenerPosicionActual(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/vuelos/activos
     * 
     * Obtiene todos los vuelos actualmente en progreso
     * 
     * @return Lista de vuelos activos con sus coordenadas actuales
     */
    @GetMapping("/activos")
    public ResponseEntity<?> obtenerVuelosActivos() {
        return ResponseEntity.ok(vueloTrackingService.obtenerVuelosActivos());
    }
}
