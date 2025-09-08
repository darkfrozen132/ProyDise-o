package colonia.algoritmo;

import colonia.*;
import colonia.Colonia.EstadisticasColonia;
import colonia.Feromona.EstadisticasFeromona;
import colonia.Heuristica.EstadisticasHeuristica;

/**
 * Implementación del Algoritmo de Colonia de Hormigas (ACO)
 * Algoritmo de optimización inspirado en el comportamiento de las hormigas
 */
public class AlgoritmoColoniaHormigas {
    
    // Parámetros del algoritmo
    private int numeroHormigas;
    private int numeroIteraciones;
    private double factorFeromona;        // Alpha: importancia de feromona
    private double factorHeuristico;      // Beta: importancia de heurística
    private double factorEvaporacion;     // Rho: tasa de evaporación
    private double valorInicialFeromona;
    
    // Componentes del algoritmo
    private Colonia colonia;
    private Feromona feromona;
    private Heuristica heuristica;
    
    // Estadísticas y control
    private boolean debug;
    private int iteracionSinMejora;
    private int maxIteracionesSinMejora;
    
    public AlgoritmoColoniaHormigas() {
        // Valores por defecto
        this.numeroHormigas = 50;
        this.numeroIteraciones = 1000;
        this.factorFeromona = 1.0;
        this.factorHeuristico = 2.0;
        this.factorEvaporacion = 0.1;
        this.valorInicialFeromona = 1.0;
        this.debug = false;
        this.maxIteracionesSinMejora = 100;
        this.iteracionSinMejora = 0;
    }
    
    /**
     * Configura los parámetros del algoritmo
     */
    public void configurar(int hormigas, int iteraciones, double alpha, double beta, 
                          double rho, double feromonaInicial) {
        this.numeroHormigas = hormigas;
        this.numeroIteraciones = iteraciones;
        this.factorFeromona = alpha;
        this.factorHeuristico = beta;
        this.factorEvaporacion = rho;
        this.valorInicialFeromona = feromonaInicial;
    }
    
    /**
     * Inicializa los componentes del algoritmo
     */
    public void inicializar(int tamanoProblem, Class<? extends Hormiga> tipoHormiga, 
                           Heuristica heuristicaProblema) {
        // Crear colonia
        this.colonia = new Colonia(numeroHormigas);
        this.colonia.inicializar(tipoHormiga);
        
        // Crear matriz de feromonas
        this.feromona = new Feromona(tamanoProblem, valorInicialFeromona, factorEvaporacion);
        
        // Configurar heurística
        this.heuristica = heuristicaProblema;
        this.heuristica.setFactorImportancia(factorHeuristico);
        
        this.iteracionSinMejora = 0;
    }
    
    /**
     * Ejecuta el algoritmo completo
     */
    public ResultadoACO ejecutar() {
        if (colonia == null || feromona == null || heuristica == null) {
            throw new IllegalStateException("Debe inicializar el algoritmo antes de ejecutar");
        }
        
        mostrarConfiguracion();
        
        long tiempoInicio = System.currentTimeMillis();
        Hormiga mejorGlobal = null;
        double mejorCalidadGlobal = Double.MAX_VALUE;
        
        for (int iteracion = 1; iteracion <= numeroIteraciones; iteracion++) {
            // 1. Construcción de soluciones
            colonia.construirSoluciones(feromona, heuristica);
            
            // 2. Actualizar mejor solución
            boolean mejoraEncontrada = colonia.actualizarMejorSolucion(iteracion);
            
            if (mejoraEncontrada) {
                mejorGlobal = colonia.getMejorHormiga();
                mejorCalidadGlobal = colonia.getMejorCalidad();
                iteracionSinMejora = 0;
            } else {
                iteracionSinMejora++;
            }
            
            // 3. Actualización de feromonas
            actualizarFeromonas();
            
            // 4. Mostrar progreso
            if (debug && iteracion % 100 == 0) {
                mostrarProgreso(iteracion);
            }
            
            // 5. Criterio de parada temprana
            if (iteracionSinMejora >= maxIteracionesSinMejora) {
                if (debug) {
                    System.out.printf("Parada temprana en iteración %d (sin mejora por %d iteraciones)%n", 
                                     iteracion, maxIteracionesSinMejora);
                }
                break;
            }
        }
        
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        
        return new ResultadoACO(mejorGlobal, mejorCalidadGlobal, colonia.getIteracionMejorEncontrada(), 
                               tiempoTotal, numeroIteraciones - iteracionSinMejora);
    }
    
    /**
     * Actualiza las feromonas después de cada iteración
     */
    private void actualizarFeromonas() {
        // 1. Evaporación
        feromona.evaporar();
        
        // 2. Depositar feromona de todas las hormigas
        for (Hormiga hormiga : colonia.getHormigas()) {
            if (hormiga.isSolucionCompleta()) {
                feromona.actualizarFeromona(hormiga.getSolucion(), hormiga.getCalidad());
            }
        }
        
        // 3. Reforzar mejor camino global
        if (colonia.getMejorHormiga() != null) {
            feromona.reforzarMejorCamino(colonia.getMejorHormiga().getSolucion(), 
                                       colonia.getMejorCalidad());
        }
    }
    
    /**
     * Muestra la configuración del algoritmo
     */
    private void mostrarConfiguracion() {
        if (debug) {
            System.out.println("=== CONFIGURACIÓN ALGORITMO COLONIA DE HORMIGAS ===");
            System.out.printf("Número de hormigas: %d%n", numeroHormigas);
            System.out.printf("Iteraciones máximas: %d%n", numeroIteraciones);
            System.out.printf("Factor feromona (α): %.2f%n", factorFeromona);
            System.out.printf("Factor heurístico (β): %.2f%n", factorHeuristico);
            System.out.printf("Evaporación (ρ): %.2f%n", factorEvaporacion);
            System.out.printf("Feromona inicial: %.2f%n", valorInicialFeromona);
            System.out.println("==================================================");
        }
    }
    
    /**
     * Muestra el progreso del algoritmo
     */
    private void mostrarProgreso(int iteracion) {
        EstadisticasColonia stats = colonia.calcularEstadisticas();
        System.out.printf("Iteración %d: Mejor=%.6f, Promedio=%.6f, Sin mejora=%d%n",
                         iteracion, stats.mejorCalidad, stats.calidadPromedio, iteracionSinMejora);
    }
    
    /**
     * Obtiene estadísticas detalladas del algoritmo
     */
    public EstadisticasACO obtenerEstadisticas() {
        if (colonia == null || feromona == null || heuristica == null) {
            return null;
        }
        
        EstadisticasColonia statsColonia = colonia.calcularEstadisticas();
        EstadisticasFeromona statsFeromona = feromona.obtenerEstadisticas();
        EstadisticasHeuristica statsHeuristica = heuristica.obtenerEstadisticas(feromona.getTamano());
        
        return new EstadisticasACO(statsColonia, statsFeromona, statsHeuristica, iteracionSinMejora);
    }
    
    // Getters y setters
    public void setDebug(boolean debug) { this.debug = debug; }
    public void setMaxIteracionesSinMejora(int max) { this.maxIteracionesSinMejora = max; }
    
    public int getNumeroHormigas() { return numeroHormigas; }
    public int getNumeroIteraciones() { return numeroIteraciones; }
    public double getFactorFeromona() { return factorFeromona; }
    public double getFactorHeuristico() { return factorHeuristico; }
    public double getFactorEvaporacion() { return factorEvaporacion; }
    
    public Colonia getColonia() { return colonia; }
    public Feromona getFeromona() { return feromona; }
    public Heuristica getHeuristica() { return heuristica; }
    
    /**
     * Clase para el resultado del algoritmo
     */
    public static class ResultadoACO {
        public final Hormiga mejorSolucion;
        public final double mejorCalidad;
        public final int iteracionEncontrada;
        public final long tiempoEjecucion;
        public final int iteracionesEjecutadas;
        
        public ResultadoACO(Hormiga solucion, double calidad, int iteracion, 
                           long tiempo, int iteracionesTotal) {
            this.mejorSolucion = solucion;
            this.mejorCalidad = calidad;
            this.iteracionEncontrada = iteracion;
            this.tiempoEjecucion = tiempo;
            this.iteracionesEjecutadas = iteracionesTotal;
        }
        
        @Override
        public String toString() {
            return String.format("ACO[Calidad=%.6f, Iteración=%d, Tiempo=%dms]",
                               mejorCalidad, iteracionEncontrada, tiempoEjecucion);
        }
    }
    
    /**
     * Clase para estadísticas completas del algoritmo
     */
    public static class EstadisticasACO {
        public final EstadisticasColonia colonia;
        public final EstadisticasFeromona feromona;
        public final EstadisticasHeuristica heuristica;
        public final int iteracionesSinMejora;
        
        public EstadisticasACO(EstadisticasColonia colonia, EstadisticasFeromona feromona,
                              EstadisticasHeuristica heuristica, int sinMejora) {
            this.colonia = colonia;
            this.feromona = feromona;
            this.heuristica = heuristica;
            this.iteracionesSinMejora = sinMejora;
        }
    }
}
