package morapack.core.problema;

import morapack.core.solucion.Solucion;

/**
 * Clase abstracta que define la interfaz para problemas de optimización
 * que pueden ser resueltos con algoritmos de colonia de hormigas.
 */
public abstract class Problema {

    protected int tamaño;
    protected double alfa;    // Importancia de la feromona
    protected double beta;    // Importancia de la heurística
    protected double constanteQ;  // Constante para cálculo de feromonas

    // Valores por defecto de parámetros ACO
    protected static final double ALFA_DEFAULT = 1.0;
    protected static final double BETA_DEFAULT = 2.0;
    protected static final double Q_DEFAULT = 100.0;

    /**
     * Constructor con parámetros por defecto
     * @param tamaño Tamaño del problema (número de nodos/elementos)
     */
    public Problema(int tamaño) {
        this(tamaño, ALFA_DEFAULT, BETA_DEFAULT, Q_DEFAULT);
    }

    /**
     * Constructor completo
     * @param tamaño Tamaño del problema
     * @param alfa Parámetro alfa (importancia feromona)
     * @param beta Parámetro beta (importancia heurística)
     * @param constanteQ Constante Q para cálculo de feromonas
     */
    public Problema(int tamaño, double alfa, double beta, double constanteQ) {
        if (tamaño <= 0) {
            throw new IllegalArgumentException("El tamaño debe ser positivo");
        }
        if (alfa < 0 || beta < 0) {
            throw new IllegalArgumentException("Alfa y beta deben ser no negativos");
        }

        this.tamaño = tamaño;
        this.alfa = alfa;
        this.beta = beta;
        this.constanteQ = constanteQ;
    }

    /**
     * Evalúa la calidad de una solución (fitness).
     * Menor valor indica mejor solución.
     * @param solucion La solución a evaluar
     * @return El fitness de la solución
     */
    public abstract double evaluarSolucion(Solucion solucion);

    /**
     * Obtiene la matriz de distancias/costos entre nodos
     * @return Matriz de distancias
     */
    public abstract double[][] getMatrizDistancias();

    /**
     * Verifica si una solución es válida para este problema
     * @param solucion La solución a verificar
     * @return true si es válida, false en caso contrario
     */
    public abstract boolean esSolucionValida(Solucion solucion);

    /**
     * Obtiene una representación legible del problema
     * @return Descripción del problema
     */
    public abstract String getDescripcion();

    /**
     * Obtiene el nodo inicial por defecto (puede ser aleatorio)
     * @return Índice del nodo inicial
     */
    public int getNodoInicial() {
        return (int) (Math.random() * tamaño);
    }

    /**
     * Verifica si dos nodos están conectados
     * @param origen Nodo origen
     * @param destino Nodo destino
     * @return true si están conectados
     */
    public boolean estanConectados(int origen, int destino) {
        if (origen < 0 || origen >= tamaño || destino < 0 || destino >= tamaño) {
            return false;
        }
        double[][] distancias = getMatrizDistancias();
        return distancias[origen][destino] > 0 && distancias[origen][destino] < Double.POSITIVE_INFINITY;
    }

    /**
     * Obtiene la distancia entre dos nodos
     * @param origen Nodo origen
     * @param destino Nodo destino
     * @return Distancia entre los nodos
     */
    public double getDistancia(int origen, int destino) {
        if (origen < 0 || origen >= tamaño || destino < 0 || destino >= tamaño) {
            return Double.POSITIVE_INFINITY;
        }
        return getMatrizDistancias()[origen][destino];
    }

    /**
     * Calcula el costo total de una secuencia de nodos
     * @param secuencia Array con la secuencia de nodos
     * @return Costo total del recorrido
     */
    public double calcularCostoRecorrido(int[] secuencia) {
        if (secuencia == null || secuencia.length < 2) {
            return 0.0;
        }

        double costoTotal = 0.0;
        double[][] distancias = getMatrizDistancias();

        for (int i = 0; i < secuencia.length - 1; i++) {
            costoTotal += distancias[secuencia[i]][secuencia[i + 1]];
        }

        return costoTotal;
    }

    /**
     * Verifica si es un problema de tipo TSP (requiere retorno al origen)
     * @return true si es tipo TSP
     */
    public boolean esTipoTSP() {
        return false; // Por defecto no es TSP, las subclases pueden sobreescribir
    }

    /**
     * Obtiene los nodos vecinos de un nodo dado
     * @param nodo El nodo del cual obtener vecinos
     * @return Array con los índices de nodos vecinos
     */
    public int[] getVecinos(int nodo) {
        if (nodo < 0 || nodo >= tamaño) {
            return new int[0];
        }

        java.util.List<Integer> vecinos = new java.util.ArrayList<>();
        double[][] distancias = getMatrizDistancias();

        for (int i = 0; i < tamaño; i++) {
            if (i != nodo && distancias[nodo][i] > 0 && distancias[nodo][i] < Double.POSITIVE_INFINITY) {
                vecinos.add(i);
            }
        }

        return vecinos.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Crea una copia del problema con nuevos parámetros
     * @param nuevaAlfa Nuevo valor de alfa
     * @param nuevaBeta Nuevo valor de beta
     * @return Nueva instancia del problema
     */
    public abstract Problema clonarConParametros(double nuevaAlfa, double nuevaBeta);

    // Getters y Setters
    public int getTamaño() {
        return tamaño;
    }

    public double getAlfa() {
        return alfa;
    }

    public void setAlfa(double alfa) {
        if (alfa < 0) {
            throw new IllegalArgumentException("Alfa debe ser no negativo");
        }
        this.alfa = alfa;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        if (beta < 0) {
            throw new IllegalArgumentException("Beta debe ser no negativo");
        }
        this.beta = beta;
    }

    public double getConstanteQ() {
        return constanteQ;
    }

    public void setConstanteQ(double constanteQ) {
        this.constanteQ = constanteQ;
    }

    @Override
    public String toString() {
        return String.format("%s [tamaño=%d, α=%.2f, β=%.2f, Q=%.2f]",
                getClass().getSimpleName(), tamaño, alfa, beta, constanteQ);
    }
}