package morapack.colonia;

/**
 * Maneja la información heurística del problema
 * Proporciona conocimiento específico del dominio para guiar a las hormigas
 */
public abstract class Heuristica {
    protected double factorImportancia; // Alpha: importancia de la heurística vs feromona
    
    public Heuristica(double factorImportancia) {
        this.factorImportancia = factorImportancia;
    }
    
    /**
     * Calcula el valor heurístico entre dos puntos/estados
     */
    public abstract double calcularValor(int origen, int destino);
    
    /**
     * Calcula valores heurísticos para todas las opciones disponibles
     */
    public double[] calcularValores(int origen, boolean[] opcionesDisponibles) {
        double[] valores = new double[opcionesDisponibles.length];
        
        for (int i = 0; i < opcionesDisponibles.length; i++) {
            if (opcionesDisponibles[i]) {
                valores[i] = calcularValor(origen, i);
            } else {
                valores[i] = 0.0;
            }
        }
        
        return valores;
    }
    
    /**
     * Normaliza los valores heurísticos entre 0 y 1
     */
    public double[] normalizar(double[] valores) {
        double max = 0.0;
        for (double valor : valores) {
            if (valor > max) max = valor;
        }
        
        if (max == 0.0) return valores;
        
        double[] normalizados = new double[valores.length];
        for (int i = 0; i < valores.length; i++) {
            normalizados[i] = valores[i] / max;
        }
        
        return normalizados;
    }
    
    /**
     * Combina valor heurístico con nivel de feromona para calcular deseabilidad
     */
    public double calcularDeseabilidad(double feromona, double heuristica, double factorFeromona) {
        if (heuristica <= 0) return 0.0;
        
        // Fórmula ACO: τ^α * η^β
        // donde τ = feromona, η = heurística, α = factorFeromona, β = factorImportancia
        return Math.pow(feromona, factorFeromona) * Math.pow(heuristica, factorImportancia);
    }
    
    /**
     * Calcula la matriz de deseabilidad completa
     */
    public double[][] calcularMatrizDeseabilidad(Feromona feromona, double factorFeromona) {
        int tamano = feromona.getTamano();
        double[][] matriz = new double[tamano][tamano];
        
        for (int i = 0; i < tamano; i++) {
            for (int j = 0; j < tamano; j++) {
                if (i != j) {
                    double valorFeromona = feromona.obtenerFeromona(i, j);
                    double valorHeuristico = calcularValor(i, j);
                    matriz[i][j] = calcularDeseabilidad(valorFeromona, valorHeuristico, factorFeromona);
                }
            }
        }
        
        return matriz;
    }
    
    /**
     * Obtiene estadísticas de los valores heurísticos
     */
    public EstadisticasHeuristica obtenerEstadisticas(int tamano) {
        double suma = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int contador = 0;
        
        for (int i = 0; i < tamano; i++) {
            for (int j = 0; j < tamano; j++) {
                if (i != j) {
                    double valor = calcularValor(i, j);
                    suma += valor;
                    contador++;
                    if (valor < min) min = valor;
                    if (valor > max) max = valor;
                }
            }
        }
        
        double promedio = contador > 0 ? suma / contador : 0.0;
        return new EstadisticasHeuristica(min, max, promedio, contador);
    }
    
    // Getters y setters
    public double getFactorImportancia() { return factorImportancia; }
    public void setFactorImportancia(double factor) { this.factorImportancia = factor; }
    
    /**
     * Clase interna para estadísticas
     */
    public static class EstadisticasHeuristica {
        public final double minimo;
        public final double maximo;
        public final double promedio;
        public final int totalValores;
        
        public EstadisticasHeuristica(double minimo, double maximo, double promedio, int totalValores) {
            this.minimo = minimo;
            this.maximo = maximo;
            this.promedio = promedio;
            this.totalValores = totalValores;
        }
    }
}
