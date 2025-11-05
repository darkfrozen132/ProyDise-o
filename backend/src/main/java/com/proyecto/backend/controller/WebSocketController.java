package com.proyecto.backend.controller;

import com.proyecto.backend.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador WebSocket simple para pruebas.
 * 
 * Endpoints:
 * - GET /api/websocket/test?mensaje=Hola -> Envía mensaje via WebSocket
 * - GET /api/websocket/activar -> Activa el WebSocket
 * - GET /api/websocket/desactivar -> Desactiva el WebSocket
 * - GET /api/websocket/estado -> Consulta si está activo
 * - WebSocket: /app/enviar -> Recibe y responde mensajes
 */
@Controller
public class WebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    
    /**
     * Recibe mensajes del cliente WebSocket y responde
     * Cliente envía a: /app/enviar
     * Servidor responde en: /topic/estado
     */
    @MessageMapping("/enviar")
    @SendTo("/topic/estado")
    public Map<String, Object> recibirMensaje(Map<String, String> mensaje) {
        logger.info("📨 Mensaje WebSocket recibido: {}", mensaje);
        
        return Map.of(
            "recibido", mensaje.getOrDefault("mensaje", "sin mensaje"),
            "respuesta", "Mensaje procesado correctamente",
            "timestamp", LocalDateTime.now().toString()
        );
    }
    
    /**
     * Endpoint REST para probar WebSocket
     * GET /api/websocket/test?mensaje=Hola
     */
    @RestController
    @RequestMapping("/api/websocket")
    public static class WebSocketTestController {
        
        private final WebSocketService webSocketService;
        
        public WebSocketTestController(WebSocketService webSocketService) {
            this.webSocketService = webSocketService;
        }
        
        @GetMapping("/test")
        public Map<String, Object> enviarMensajePrueba(
                @RequestParam(defaultValue = "Test WebSocket") String mensaje) {
            
            Map<String, Object> payload = Map.of(
                "mensaje", mensaje,
                "timestamp", LocalDateTime.now().toString(),
                "estado", "OK"
            );
            
            webSocketService.enviarMensaje("/topic/estado", payload);
            
            return Map.of(
                "enviado", webSocketService.estaHabilitado(),
                "mensaje", mensaje,
                "canal", "/topic/estado",
                "habilitado", webSocketService.estaHabilitado()
            );
        }
        
        /**
         * Activa el WebSocket
         */
        @GetMapping("/activar")
        public Map<String, Object> activar() {
            webSocketService.activar();
            return Map.of(
                "accion", "activar",
                "habilitado", true,
                "mensaje", "WebSocket activado correctamente"
            );
        }
        
        /**
         * Desactiva el WebSocket
         */
        @GetMapping("/desactivar")
        public Map<String, Object> desactivar() {
            webSocketService.desactivar();
            return Map.of(
                "accion", "desactivar",
                "habilitado", false,
                "mensaje", "WebSocket desactivado"
            );
        }
        
        /**
         * Consulta el estado del WebSocket
         */
        @GetMapping("/estado")
        public Map<String, Object> consultarEstado() {
            return Map.of(
                "habilitado", webSocketService.estaHabilitado(),
                "timestamp", LocalDateTime.now().toString()
            );
        }
    }
}
