package com.proyecto.backend.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar progreso del algoritmo genético
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgresoAGDTO {
    
    /**
     * Generación actual
     */
    private int generacionActual;
    
    /**
     * Total de generaciones
     */
    private int totalGeneraciones;
    
    /**
     * Mejor fitness encontrado
     */
    private double mejorFitness;
    
    /**
     * Porcentaje de progreso (0-100)
     */
    private double porcentaje;
    
    /**
     * Tiempo transcurrido en ms
     */
    private long tiempoTranscurridoMs;
    
    /**
     * Tiempo estimado restante en ms
     */
    private long etaMs;
    
    /**
     * Número de pedidos procesados
     */
    private int pedidosProcesados;
    
    /**
     * Total de pedidos
     */
    private int totalPedidos;
    
    /**
     * Solución parcial con rutas asignadas (opcional)
     * Formato: Lista de vuelos con sus pedidos asignados
     */
    private Object solucion;
}
