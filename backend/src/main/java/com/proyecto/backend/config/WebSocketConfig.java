package com.proyecto.backend.config;

import com.proyecto.backend.websocket.PlanificacionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Configuración de WebSocket
 * 
 * 1. STOMP + SockJS (existente):
 *    - Endpoint: ws://localhost:8000/ws
 *    - Canal: /topic/estado
 * 
 * 2. WebSocket nativo para planificación (nuevo):
 *    - Endpoint: ws://localhost:8080/ws/planificacion
 *    - Comunicación bidireccional JSON puro
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    private final PlanificacionWebSocketHandler planificacionHandler;

    // ============ STOMP CONFIG (existente) ============
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    // ============ WEBSOCKET NATIVO (nuevo) ============
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(planificacionHandler, "/ws/planificacion")
                .setAllowedOrigins("*"); // En producción: configurar CORS específico
    }
}
