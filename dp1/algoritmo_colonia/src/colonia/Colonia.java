package colonia;

import java.util.List;
import java.util.ArrayList;

/**
 * Representa una colonia de hormigas para el algoritmo ACO
 * Maneja la población de hormigas y sus interacciones
 */
public class Colonia {
    private List<Hormiga> hormigas;
    private int numeroHormigas;
    private Hormiga mejorHormiga;
    private double mejorCalidad;
    private int iteracionMejorEncontrada;
    
    public Colonia(int numeroHormigas) {
        this.numeroHormigas = numeroHormigas;
        this.hormigas = new ArrayList<>();
        this.mejorCalidad = Double.MAX_VALUE; // Asumimos minimización
        this.iteracionMejorEncontrada = 0;
    }
    
    /**
     * Inicializa la colonia con hormigas del tipo especificado
     */
    public void inicializar(Class<? extends Hormiga> tipoHormiga) {
        hormigas.clear();
        
        try {
            for (int i = 0; i < numeroHormigas; i++) {
                Hormiga hormiga = tipoHormiga.newInstance();
                hormigas.add(hormiga);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al crear hormigas: " + e.getMessage());
        }
    }
    
    /**
     * Hace que todas las hormigas construyan sus soluciones
     */
    public void construirSoluciones(Feromona feromona, Heuristica heuristica) {
        for (Hormiga hormiga : hormigas) {
            hormiga.reiniciar();
            hormiga.construirSolucion(feromona, heuristica);
            hormiga.evaluarSolucion();
        }
    }
    
    /**
     * Actualiza la mejor solución encontrada
     */
    public boolean actualizarMejorSolucion(int iteracion) {
        boolean mejoraEncontrada = false;
        
        for (Hormiga hormiga : hormigas) {
            if (hormiga.getCalidad() < mejorCalidad) { // Minimización
                mejorCalidad = hormiga.getCalidad();
                mejorHormiga = clonarHormiga(hormiga);
                iteracionMejorEncontrada = iteracion;
                mejoraEncontrada = true;
            }
        }
        
        return mejoraEncontrada;
    }
    
    /**
     * Clona una hormiga para preservar la mejor solución
     */
    private Hormiga clonarHormiga(Hormiga original) {
        try {
            Hormiga clon = original.getClass().newInstance();
            clon.setCalidad(original.getCalidad());
            clon.setSolucionCompleta(original.isSolucionCompleta());
            return clon;
        } catch (Exception e) {
            return original; // Fallback
        }
    }
    
    /**
     * Obtiene la hormiga con mejor calidad de la iteración actual
     */
    public Hormiga obtenerMejorHormigaIteracion() {
        if (hormigas.isEmpty()) return null;
        
        Hormiga mejor = hormigas.get(0);
        for (Hormiga hormiga : hormigas) {
            if (hormiga.getCalidad() < mejor.getCalidad()) {
                mejor = hormiga;
            }
        }
        
        return mejor;
    }
    
    /**
     * Calcula estadísticas de la colonia
     */
    public EstadisticasColonia calcularEstadisticas() {
        if (hormigas.isEmpty()) {
            return new EstadisticasColonia(0.0, 0.0, 0.0, 0.0, 0);
        }
        
        double suma = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int solucionesCompletas = 0;
        
        for (Hormiga hormiga : hormigas) {
            double calidad = hormiga.getCalidad();
            suma += calidad;
            
            if (calidad < min) min = calidad;
            if (calidad > max) max = calidad;
            
            if (hormiga.isSolucionCompleta()) {
                solucionesCompletas++;
            }
        }
        
        double promedio = suma / hormigas.size();
        
        // Calcular desviación estándar
        double sumaCuadrados = 0.0;
        for (Hormiga hormiga : hormigas) {
            double diferencia = hormiga.getCalidad() - promedio;
            sumaCuadrados += diferencia * diferencia;
        }
        double desviacion = Math.sqrt(sumaCuadrados / hormigas.size());
        
        return new EstadisticasColonia(min, max, promedio, desviacion, solucionesCompletas);
    }
    
    /**
     * Reinicia todas las hormigas
     */
    public void reiniciarHormigas() {
        for (Hormiga hormiga : hormigas) {
            hormiga.reiniciar();
        }
    }
    
    // Getters
    public List<Hormiga> getHormigas() { return new ArrayList<>(hormigas); }
    public int getNumeroHormigas() { return numeroHormigas; }
    public Hormiga getMejorHormiga() { return mejorHormiga; }
    public double getMejorCalidad() { return mejorCalidad; }
    public int getIteracionMejorEncontrada() { return iteracionMejorEncontrada; }
    
    // Setters
    public void setNumeroHormigas(int numero) {
        this.numeroHormigas = numero;
    }
    
    @Override
    public String toString() {
        return String.format("Colonia[%d hormigas, Mejor=%.6f en iteración %d]", 
                           numeroHormigas, mejorCalidad, iteracionMejorEncontrada);
    }
    
    /**
     * Clase interna para estadísticas de la colonia
     */
    public static class EstadisticasColonia {
        public final double mejorCalidad;
        public final double peorCalidad;
        public final double calidadPromedio;
        public final double desviacionEstandar;
        public final int solucionesCompletas;
        
        public EstadisticasColonia(double mejor, double peor, double promedio, 
                                 double desviacion, int completas) {
            this.mejorCalidad = mejor;
            this.peorCalidad = peor;
            this.calidadPromedio = promedio;
            this.desviacionEstandar = desviacion;
            this.solucionesCompletas = completas;
        }
        
        @Override
        public String toString() {
            return String.format("Stats[Mejor=%.4f, Peor=%.4f, Prom=%.4f, Desv=%.4f, Completas=%d]",
                               mejorCalidad, peorCalidad, calidadPromedio, desviacionEstandar, solucionesCompletas);
        }
    }
}
