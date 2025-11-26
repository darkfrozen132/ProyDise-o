package com.proyecto.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Configuración de WebSocket con STOMP para comunicación en tiempo real
 * 
 * Arquitectura:
 * - Protocolo: STOMP sobre WebSocket con SockJS fallback
 * - Broker: Simple in-memory broker (producción: RabbitMQ/ActiveMQ)
 * - Endpoints: /ws (STOMP), /topic/simulations/{id} (subscripción)
 * 
 * Flujo:
 * 1. Cliente conecta a ws://localhost:8000/ws
 * 2. Cliente se subscribe a /topic/simulations/{sessionId}
 * 3. Servidor envía actualizaciones automáticamente vía SimpMessagingTemplate
 * 
 * Ventajas STOMP vs WebSocket nativo:
 * - Protocolo estándar con ACK/NACK
 * - SockJS fallback automático (HTTP Long-Polling)
 * - Integración nativa con Spring
 * - Multiplexión de canales
 * 
 * @author Sistema Package Planner
 * @version 2.0
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura el broker de mensajes
     * 
     * - enableSimpleBroker("/topic"): Habilita broker simple en memoria
     * - setApplicationDestinationPrefixes("/app"): Prefijo para mensajes del cliente al servidor
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker simple para /topic (en producción: usar RabbitMQ o ActiveMQ)
        config.enableSimpleBroker("/topic");
        
        // Prefijo para mensajes del cliente (opcional, para futuro)
        config.setApplicationDestinationPrefixes("/app");
        
        log.info("✅ Broker STOMP configurado: /topic/* habilitado");
    }

    /**
     * Registra los endpoints STOMP
     * 
     * - Endpoint: /ws
     * - SockJS: Habilitado (fallback HTTP Long-Polling)
     * - CORS: Permitir todos los orígenes (desarrollo)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // ⚠️ En producción: especificar dominios
                .withSockJS(); // Habilitar SockJS fallback
        
        log.info("✅ Endpoint STOMP registrado: ws://localhost:8000/ws (SockJS habilitado)");
    }

    /**
     * Configura opciones de transporte (opcional)
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(512 * 1024) // 512KB por mensaje
                .setSendBufferSizeLimit(1024 * 1024) // 1MB buffer
                .setSendTimeLimit(20000); // 20 segundos timeout
    }
}
