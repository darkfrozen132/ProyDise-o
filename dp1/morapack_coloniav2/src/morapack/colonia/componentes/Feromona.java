package morapack.colonia.componentes;

/**
 * Gestiona la matriz de feromonas del algoritmo ACO.
 * Las feromonas representan la información acumulada de buenas soluciones
 * encontradas por las hormigas anteriormente.
 */
public class Feromona {

    private double[][] matrizFeromona;
    private int tamaño;
    private double tasaEvaporacion;
    private double feromonaInicial;
    private double feromonaMinima;
    private double feromonaMaxima;

    // Constantes por defecto
    private static final double FEROMONA_INICIAL_DEFAULT = 0.1;
    private static final double TASA_EVAPORACION_DEFAULT = 0.1;
    private static final double FEROMONA_MINIMA_DEFAULT = 0.01;
    private static final double FEROMONA_MAXIMA_DEFAULT = 10.0;

    /**
     * Constructor con parámetros por defecto
     * @param tamaño Tamaño de la matriz (número de nodos)
     */
    public Feromona(int tamaño) {
        this(tamaño, FEROMONA_INICIAL_DEFAULT, TASA_EVAPORACION_DEFAULT,
             FEROMONA_MINIMA_DEFAULT, FEROMONA_MAXIMA_DEFAULT);
    }

    /**
     * Constructor completo
     * @param tamaño Tamaño de la matriz
     * @param feromonaInicial Valor inicial de feromona
     * @param tasaEvaporacion Tasa de evaporación (rho)
     * @param feromonaMinima Valor mínimo de feromona
     * @param feromonaMaxima Valor máximo de feromona
     */
    public Feromona(int tamaño, double feromonaInicial, double tasaEvaporacion,
                   double feromonaMinima, double feromonaMaxima) {
        this.tamaño = tamaño;
        this.feromonaInicial = feromonaInicial;
        this.tasaEvaporacion = tasaEvaporacion;
        this.feromonaMinima = feromonaMinima;
        this.feromonaMaxima = feromonaMaxima;

        inicializarMatriz();
    }

    /**
     * Inicializa la matriz de feromonas con valores iniciales
     */
    private void inicializarMatriz() {
        matrizFeromona = new double[tamaño][tamaño];

        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                matrizFeromona[i][j] = feromonaInicial;
            }
        }
    }

    /**
     * Obtiene el valor de feromona entre dos nodos
     * @param origen Nodo origen
     * @param destino Nodo destino
     * @return Valor de feromona
     */
    public double getFeromona(int origen, int destino) {
        if (origen < 0 || origen >= tamaño || destino < 0 || destino >= tamaño) {
            throw new IllegalArgumentException("Índices fuera de rango: " + origen + ", " + destino);
        }
        return matrizFeromona[origen][destino];
    }

    /**
     * Establece el valor de feromona entre dos nodos
     * @param origen Nodo origen
     * @param destino Nodo destino
     * @param valor Nuevo valor de feromona
     */
    public void setFeromona(int origen, int destino, double valor) {
        if (origen < 0 || origen >= tamaño || destino < 0 || destino >= tamaño) {
            throw new IllegalArgumentException("Índices fuera de rango: " + origen + ", " + destino);
        }

        // Aplicar límites
        valor = Math.max(feromonaMinima, Math.min(feromonaMaxima, valor));
        matrizFeromona[origen][destino] = valor;
    }

    /**
     * Aplica evaporación a toda la matriz de feromonas
     */
    public void evaporar() {
        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                double nuevoValor = (1.0 - tasaEvaporacion) * matrizFeromona[i][j];
                matrizFeromona[i][j] = Math.max(feromonaMinima, nuevoValor);
            }
        }
    }

    /**
     * Deposita feromona en una arista específica
     * @param origen Nodo origen
     * @param destino Nodo destino
     * @param cantidad Cantidad de feromona a depositar
     */
    public void depositarFeromona(int origen, int destino, double cantidad) {
        double valorActual = getFeromona(origen, destino);
        double nuevoValor = valorActual + cantidad;
        setFeromona(origen, destino, nuevoValor);
    }

    /**
     * Deposita feromona en un camino completo
     * @param camino Array con la secuencia de nodos del camino
     * @param cantidad Cantidad de feromona a depositar en cada arista
     */
    public void depositarFeromonaEnCamino(int[] camino, double cantidad) {
        for (int i = 0; i < camino.length - 1; i++) {
            depositarFeromona(camino[i], camino[i + 1], cantidad);
            // Para problemas simétricos, depositar en ambas direcciones
            depositarFeromona(camino[i + 1], camino[i], cantidad);
        }
    }

    /**
     * Reinicia la matriz de feromonas a valores iniciales
     */
    public void reiniciar() {
        inicializarMatriz();
    }

    /**
     * Obtiene una copia de la matriz de feromonas
     * @return Copia de la matriz
     */
    public double[][] getMatrizCopia() {
        double[][] copia = new double[tamaño][tamaño];
        for (int i = 0; i < tamaño; i++) {
            System.arraycopy(matrizFeromona[i], 0, copia[i], 0, tamaño);
        }
        return copia;
    }

    /**
     * Calcula la cantidad de feromona a depositar basada en la calidad de la solución
     * @param fitness Fitness de la solución (menor es mejor)
     * @param constanteQ Constante Q del algoritmo
     * @return Cantidad de feromona a depositar
     */
    public static double calcularCantidadFeromona(double fitness, double constanteQ) {
        if (fitness <= 0) {
            return constanteQ; // Evitar división por cero
        }
        return constanteQ / fitness;
    }

    // Getters y Setters
    public int getTamaño() {
        return tamaño;
    }

    public double getTasaEvaporacion() {
        return tasaEvaporacion;
    }

    public void setTasaEvaporacion(double tasaEvaporacion) {
        this.tasaEvaporacion = Math.max(0.0, Math.min(1.0, tasaEvaporacion));
    }

    public double getFeromonaMinima() {
        return feromonaMinima;
    }

    public void setFeromonaMinima(double feromonaMinima) {
        this.feromonaMinima = Math.max(0.0, feromonaMinima);
    }

    public double getFeromonaMaxima() {
        return feromonaMaxima;
    }

    public void setFeromonaMaxima(double feromonaMaxima) {
        this.feromonaMaxima = Math.max(feromonaMinima, feromonaMaxima);
    }
}