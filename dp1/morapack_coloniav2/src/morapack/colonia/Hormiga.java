package morapack.colonia;

/**
 * Clase abstracta que representa una hormiga en el algoritmo de colonia
 * Cada hormiga construye una solución siguiendo feromonas y heurísticas
 */
public abstract class Hormiga {
    protected double[] solucion;
    protected double calidad;
    protected boolean solucionCompleta;
    protected int posicionActual;
    
    public Hormiga() {
        this.calidad = 0.0;
        this.solucionCompleta = false;
        this.posicionActual = 0;
    }
    
    /**
     * Construye una solución completa paso a paso
     */
    public abstract void construirSolucion(Feromona feromona, Heuristica heuristica);
    
    /**
     * Selecciona el siguiente movimiento basado en probabilidades
     */
    protected abstract int seleccionarSiguienteMovimiento(double[] probabilidades);
    
    /**
     * Calcula la probabilidad de elegir cada opción disponible
     */
    protected abstract double[] calcularProbabilidades(Feromona feromona, Heuristica heuristica, 
                                                      boolean[] opcionesDisponibles);
    
    /**
     * Evalúa la calidad de la solución construida
     */
    public abstract void evaluarSolucion();
    
    /**
     * Reinicia la hormiga para una nueva construcción
     */
    public void reiniciar() {
        this.solucionCompleta = false;
        this.posicionActual = 0;
        this.calidad = 0.0;
        if (solucion != null) {
            for (int i = 0; i < solucion.length; i++) {
                solucion[i] = 0;
            }
        }
    }
    
    /**
     * Verifica si quedan opciones disponibles
     */
    protected abstract boolean[] obtenerOpcionesDisponibles();
    
    // Getters y setters
    public double[] getSolucion() { return solucion != null ? solucion.clone() : null; }
    public double getCalidad() { return calidad; }
    public boolean isSolucionCompleta() { return solucionCompleta; }
    public int getPosicionActual() { return posicionActual; }
    
    public void setCalidad(double calidad) { this.calidad = calidad; }
    public void setSolucionCompleta(boolean completa) { this.solucionCompleta = completa; }
    
    @Override
    public String toString() {
        return String.format("Hormiga[Calidad=%.6f, Completa=%s]", calidad, solucionCompleta);
    }
}
