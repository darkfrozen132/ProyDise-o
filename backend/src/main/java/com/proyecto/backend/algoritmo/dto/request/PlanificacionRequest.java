package com.proyecto.backend.algoritmo.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request para ejecutar la planificacion de rutas con el algoritmo genetico
 *
 * Parametros principales:
 * - fecha: fecha de inicio de la planificacion (hora 00:00)
 * - K: factor de ampliacion temporal
 *   - K=1: operacion dia a dia
 *   - K=14: simulacion 3 dias
 *   - K=75: simulacion hasta colapso
 *
 * Formula: Sc = K * Sa (rango de tiempo de pedidos a consumir)
 * Ejemplo: Si K=14 y Sa=5min, entonces Sc=70min de pedidos a procesar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionRequest {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @Min(value = 1, message = "K debe ser al menos 1")
    private int factorK;

    // Parametros opcionales del algoritmo genetico
    private ParametrosGenetico parametrosGenetico;

    /**
     * Parametros configurables del algoritmo genetico
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParametrosGenetico {

        // Tamanio de poblacion
        private Integer tamanioPoblacion = 50;

        // Numero maximo de generaciones
        private Integer maxGeneraciones = 200;

        // Probabilidad de cruce
        private Double probabilidadCruce = 0.8;

        // Probabilidad de mutacion
        private Double probabilidadMutacion = 0.05;

        // Numero de elite (mejores que pasan directamente)
        private Integer numeroElite = 4;

        // Limite de generaciones sin mejora
        private Integer limiteGeneracionesSinMejora = 40;

        // Salto del algoritmo en minutos (Sa)
        private Integer saltoAlgoritmoMinutos = 5;

        // Semilla para reproducibilidad (opcional)
        private Long semilla;

        /**
         * Calcula el salto de consumo (Sc) en minutos
         * Sc = K * Sa
         *
         * @param factorK Factor de ampliacion temporal
         * @return Salto de consumo en minutos
         */
        public int calcularSaltoConsumo(int factorK) {
            return factorK * saltoAlgoritmoMinutos;
        }
    }

    /**
     * Obtiene los parametros del genetico, usando valores por defecto si no se especifican
     *
     * @return Parametros del algoritmo genetico
     */
    public ParametrosGenetico getParametrosGenetico() {
        if (parametrosGenetico == null) {
            parametrosGenetico = new ParametrosGenetico();
        }
        return parametrosGenetico;
    }

    /**
     * Calcula el rango de consumo en minutos segun K y Sa
     *
     * @return Rango de consumo en minutos
     */
    public int calcularRangoConsumoMinutos() {
        return getParametrosGenetico().calcularSaltoConsumo(factorK);
    }
}
