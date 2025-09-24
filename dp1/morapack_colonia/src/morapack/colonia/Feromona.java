package morapack.colonia;

/**
 * Maneja las feromonas del algoritmo de colonia de hormigas
 * Las feromonas representan la "memoria" de buenas soluciones pasadas
 */
public class Feromona {
    private double[][] matriz;
    private int tamano;
    private double valorInicial;
    private double factorEvaporacion;
    private double feromonaMinima;
    private double feromonaMaxima;
    
    public Feromona(int tamano, double valorInicial, double factorEvaporacion) {
        this.tamano = tamano;
        this.valorInicial = valorInicial;
        this.factorEvaporacion = factorEvaporacion;
        this.feromonaMinima = 0.01;
        this.feromonaMaxima = 10.0;
        
        inicializarMatriz();
    }
    
    /**
     * Inicializa la matriz de feromonas con valores uniformes
     */
    private void inicializarMatriz() {
        matriz = new double[tamano][tamano];
        for (int i = 0; i < tamano; i++) {
            for (int j = 0; j < tamano; j++) {
                matriz[i][j] = valorInicial;
            }
        }
    }
    
    /**
     * Obtiene el nivel de feromona entre dos puntos
     */
    public double obtenerFeromona(int origen, int destino) {
        if (origen >= 0 && origen < tamano && destino >= 0 && destino < tamano) {
            return matriz[origen][destino];
        }
        return valorInicial;
    }
    
    /**
     * Deposita feromona en un camino específico
     */
    public void depositarFeromona(int origen, int destino, double cantidad) {
        if (origen >= 0 && origen < tamano && destino >= 0 && destino < tamano) {
            matriz[origen][destino] += cantidad;
            // Aplicar límites
            if (matriz[origen][destino] > feromonaMaxima) {
                matriz[origen][destino] = feromonaMaxima;
            }
        }
    }
    
    /**
     * Actualiza las feromonas de una solución completa
     */
    public void actualizarFeromona(double[] solucion, double calidad) {
        if (solucion == null || calidad <= 0) return;
        
        double cantidad = 1.0 / calidad; // Mejor calidad = más feromona
        
        for (int i = 0; i < solucion.length - 1; i++) {
            int origen = (int) solucion[i];
            int destino = (int) solucion[i + 1];
            depositarFeromona(origen, destino, cantidad);
        }
    }
    
    /**
     * Evapora las feromonas (reduce gradualmente su intensidad)
     */
    public void evaporar() {
        for (int i = 0; i < tamano; i++) {
            for (int j = 0; j < tamano; j++) {
                matriz[i][j] *= (1.0 - factorEvaporacion);
                // Aplicar límite mínimo
                if (matriz[i][j] < feromonaMinima) {
                    matriz[i][j] = feromonaMinima;
                }
            }
        }
    }
    
    /**
     * Refuerza el mejor camino encontrado
     */
    public void reforzarMejorCamino(double[] mejorSolucion, double mejorCalidad) {
        if (mejorSolucion == null) return;
        
        double refuerzo = 2.0 / mejorCalidad; // Refuerzo extra para el mejor camino
        
        for (int i = 0; i < mejorSolucion.length - 1; i++) {
            int origen = (int) mejorSolucion[i];
            int destino = (int) mejorSolucion[i + 1];
            depositarFeromona(origen, destino, refuerzo);
        }
    }
    
    /**
     * Reinicia la matriz de feromonas
     */
    public void reiniciar() {
        inicializarMatriz();
    }
    
    /**
     * Obtiene estadísticas de las feromonas
     */
    public EstadisticasFeromona obtenerEstadisticas() {
        double suma = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int contador = 0;
        
        for (int i = 0; i < tamano; i++) {
            for (int j = 0; j < tamano; j++) {
                if (i != j) { // Excluir diagonal
                    double valor = matriz[i][j];
                    suma += valor;
                    contador++;
                    if (valor < min) min = valor;
                    if (valor > max) max = valor;
                }
            }
        }
        
        double promedio = contador > 0 ? suma / contador : 0.0;
        return new EstadisticasFeromona(min, max, promedio, contador);
    }
    
    // Getters y setters
    public int getTamano() { return tamano; }
    public double getFactorEvaporacion() { return factorEvaporacion; }
    public void setFactorEvaporacion(double factor) { this.factorEvaporacion = factor; }
    public void setLimites(double minimo, double maximo) {
        this.feromonaMinima = minimo;
        this.feromonaMaxima = maximo;
    }
    
    /**
     * Clase interna para estadísticas
     */
    public static class EstadisticasFeromona {
        public final double minimo;
        public final double maximo;
        public final double promedio;
        public final int totalCaminos;
        
        public EstadisticasFeromona(double minimo, double maximo, double promedio, int totalCaminos) {
            this.minimo = minimo;
            this.maximo = maximo;
            this.promedio = promedio;
            this.totalCaminos = totalCaminos;
        }
    }
}
