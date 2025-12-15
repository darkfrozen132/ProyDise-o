package com.proyecto.backend.simulation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Servicio centralizado para envío de mensajes WebSocket/STOMP
 * 
 * Proporciona una abstracción limpia sobre SimpMessagingTemplate para:
 * 1. Evitar acoplamiento directo con Spring Messaging
 * 2. Centralizar logging y manejo de errores
 * 3. Facilitar testing con mocks
 * 
 * Arquitectura STOMP:
 * - Los clientes se subscriben a /topic/simulations/{sessionId}
 * - Este servicio envía mensajes a ese topic vía convertAndSend()
 * - Spring maneja automáticamente la serialización JSON
 * 
 * @author Sistema Package Planner
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    // Flag para habilitar/deshabilitar envío de mensajes (útil para testing)
    private volatile boolean habilitado = true;
    
    /**
     * Envía un mensaje a un topic específico vía STOMP
     * 
     * @param destination Topic de destino (ej: "/topic/simulations/550e8400-...")
     * @param payload Objeto a enviar (será serializado a JSON automáticamente)
     * @param <T> Tipo del payload
     */
    public <T> void sendMessage(String destination, T payload) {
        if (!habilitado) {
            log.debug("⏸️ WebSocket deshabilitado, mensaje no enviado a {}", destination);
            return;
        }
        
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("✅ Mensaje enviado a {}: {}", destination, payload.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("❌ Error enviando mensaje a {}: {}", destination, e.getMessage(), e);
            // No propagamos la excepción para evitar que el AG se detenga por fallos de WS
        }
    }
    
    /**
     * Alias de sendMessage para compatibilidad con código legacy
     */
    public <T> void enviarMensaje(String destination, T payload) {
        sendMessage(destination, payload);
    }
    
    /**
     * Envía un mensaje al topic de una sesión de simulación específica
     * 
     * @param sessionId ID de la sesión de simulación
     * @param payload Objeto a enviar (será serializado a JSON automáticamente)
     * @param <T> Tipo del payload
     */
    public <T> void sendToSimulation(String sessionId, T payload) {
        String destination = "/topic/simulations/" + sessionId;
        sendMessage(destination, payload);
    }
    
    /**
     * Construye la URL completa del topic para una sesión
     * 
     * @param sessionId ID de la sesión
     * @return URL del topic (ej: "/topic/simulations/550e8400-...")
     */
    public String getSimulationTopic(String sessionId) {
        return "/topic/simulations/" + sessionId;
    }
    
    // ============ MÉTODOS DE CONTROL ============
    
    /**
     * Activa el envío de mensajes WebSocket
     */
    public void activar() {
        this.habilitado = true;
        log.info("✅ WebSocketService activado");
    }
    
    /**
     * Desactiva el envío de mensajes WebSocket
     */
    public void desactivar() {
        this.habilitado = false;
        log.warn("⏸️ WebSocketService desactivado");
    }
    
    /**
     * Verifica si el servicio está habilitado
     * 
     * @return true si está habilitado
     */
    public boolean estaHabilitado() {
        return habilitado;
    }
}
