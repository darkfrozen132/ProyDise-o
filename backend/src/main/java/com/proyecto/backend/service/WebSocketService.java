package com.proyecto.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Servicio simple para controlar el estado del WebSocket.
 * Solo tiene un booleano para activar/desactivar el envío de mensajes.
 */
@Service
public class WebSocketService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    
    private boolean habilitado = true;
    
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Activa el WebSocket
     */
    public void activar() {
        this.habilitado = true;
        logger.info("✅ WebSocket ACTIVADO");
    }
    
    /**
     * Desactiva el WebSocket
     */
    public void desactivar() {
        this.habilitado = false;
        logger.info("❌ WebSocket DESACTIVADO");
    }
    
    /**
     * Verifica si el WebSocket está habilitado
     */
    public boolean estaHabilitado() {
        return habilitado;
    }
    
    /**
     * Envía un mensaje si el WebSocket está habilitado
     */
    public void enviarMensaje(String canal, Object mensaje) {
        if (habilitado) {
            messagingTemplate.convertAndSend(canal, mensaje);
            logger.debug("📤 Mensaje enviado a {}: {}", canal, mensaje);
        } else {
            logger.debug("⏭️ WebSocket deshabilitado. Mensaje NO enviado a {}", canal);
        }
    }
    
    /**
     * Envía un estado simple al canal /topic/estado
     */
    public void enviarEstado(String mensaje) {
        if (habilitado) {
            Map<String, Object> estado = Map.of(
                "mensaje", mensaje,
                "timestamp", System.currentTimeMillis(),
                "activo", true
            );
            messagingTemplate.convertAndSend("/topic/estado", estado);
        }
    }
}
