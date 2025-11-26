package com.proyecto.backend.simulation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Mensaje genérico para comunicación WebSocket
 * Soporta múltiples tipos de mensajes
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
@Builder
public record WebSocketMessage(
    @JsonProperty("tipo")
    MessageType type,

    @JsonProperty("mensaje")
    String message,

    @JsonProperty("data")
    Object data,

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp
) {

    public enum MessageType {
        CONEXION,
        PROGRESO,
        COMPLETADO,
        ERROR,
        CANCELADO,
        PAUSADO
    }

    /**
     * Crea un mensaje de conexión exitosa
     */
    public static WebSocketMessage connection(String message) {
        return WebSocketMessage.builder()
                .type(MessageType.CONEXION)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un mensaje de progreso
     */
    public static WebSocketMessage progress(SimulationSnapshot snapshot) {
        return WebSocketMessage.builder()
                .type(MessageType.PROGRESO)
                .message("Progreso de simulación")
                .data(snapshot)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un mensaje de completado
     */
    public static WebSocketMessage completed(SimulationSnapshot snapshot) {
        return WebSocketMessage.builder()
                .type(MessageType.COMPLETADO)
                .message("Simulación completada")
                .data(snapshot)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un mensaje de error
     */
    public static WebSocketMessage error(String errorMessage) {
        return WebSocketMessage.builder()
                .type(MessageType.ERROR)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un mensaje de cancelación
     */
    public static WebSocketMessage cancelled(String message) {
        return WebSocketMessage.builder()
                .type(MessageType.CANCELADO)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
