package morapack.colonia.componentes;

/**
 * Gestiona la información heurística del algoritmo ACO.
 * La información heurística guía a las hormigas hacia decisiones
 * localmente prometedoras, independiente de las feromonas.
 */
public class Heuristica {

    private double[][] matrizHeuristica;
    private int tamaño;
    private TipoHeuristica tipo;

    /**
     * Tipos de heurística disponibles
     */
    public enum TipoHeuristica {
        DISTANCIA_INVERSA,    // 1/distancia (para TSP)
        CAPACIDAD,            // Basada en capacidad disponible
        TIEMPO,               // Basada en tiempo de procesamiento
        COSTO,                // Basada en costo
        PERSONALIZADA         // Definida por el usuario
    }

    /**
     * Constructor con matriz heurística personalizada
     * @param matrizHeuristica Matriz de valores heurísticos precalculados
     */
    public Heuristica(double[][] matrizHeuristica) {
        this.matrizHeuristica = matrizHeuristica.clone();
        this.tamaño = matrizHeuristica.length;
        this.tipo = TipoHeuristica.PERSONALIZADA;
        validarMatriz();
    }

    /**
     * Constructor para heurística basada en distancias
     * @param matrizDistancias Matriz de distancias entre nodos
     * @param tipo Tipo de heurística a aplicar
     */
    public Heuristica(double[][] matrizDistancias, TipoHeuristica tipo) {
        this.tamaño = matrizDistancias.length;
        this.tipo = tipo;
        this.matrizHeuristica = new double[tamaño][tamaño];

        calcularHeuristicaSegunTipo(matrizDistancias);
        validarMatriz();
    }

    /**
     * Calcula la heurística según el tipo especificado
     */
    private void calcularHeuristicaSegunTipo(double[][] matrizBase) {
        switch (tipo) {
            case DISTANCIA_INVERSA:
                calcularDistanciaInversa(matrizBase);
                break;
            case CAPACIDAD:
                calcularHeuristicaCapacidad(matrizBase);
                break;
            case TIEMPO:
                calcularHeuristicaTiempo(matrizBase);
                break;
            case COSTO:
                calcularHeuristicaCosto(matrizBase);
                break;
            default:
                throw new IllegalArgumentException("Tipo de heurística no implementado: " + tipo);
        }
    }

    /**
     * Calcula heurística como inverso de la distancia (común en TSP)
     */
    private void calcularDistanciaInversa(double[][] distancias) {
        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                if (i == j) {
                    matrizHeuristica[i][j] = 0.0; // Sin auto-transición
                } else if (distancias[i][j] > 0) {
                    matrizHeuristica[i][j] = 1.0 / distancias[i][j];
                } else {
                    matrizHeuristica[i][j] = 0.0; // Distancia inválida
                }
            }
        }
    }

    /**
     * Calcula heurística basada en capacidad disponible
     */
    private void calcularHeuristicaCapacidad(double[][] capacidades) {
        double maxCapacidad = encontrarMaximo(capacidades);

        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                if (i == j) {
                    matrizHeuristica[i][j] = 0.0;
                } else {
                    // Normalizar capacidad (mayor capacidad = mayor atractivo)
                    matrizHeuristica[i][j] = capacidades[i][j] / maxCapacidad;
                }
            }
        }
    }

    /**
     * Calcula heurística basada en tiempo (menor tiempo = mayor atractivo)
     */
    private void calcularHeuristicaTiempo(double[][] tiempos) {
        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                if (i == j) {
                    matrizHeuristica[i][j] = 0.0;
                } else if (tiempos[i][j] > 0) {
                    matrizHeuristica[i][j] = 1.0 / tiempos[i][j];
                } else {
                    matrizHeuristica[i][j] = 0.0;
                }
            }
        }
    }

    /**
     * Calcula heurística basada en costo (menor costo = mayor atractivo)
     */
    private void calcularHeuristicaCosto(double[][] costos) {
        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                if (i == j) {
                    matrizHeuristica[i][j] = 0.0;
                } else if (costos[i][j] > 0) {
                    matrizHeuristica[i][j] = 1.0 / costos[i][j];
                } else {
                    matrizHeuristica[i][j] = 0.0;
                }
            }
        }
    }

    /**
     * Encuentra el valor máximo en una matriz
     */
    private double encontrarMaximo(double[][] matriz) {
        double maximo = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[i].length; j++) {
                if (matriz[i][j] > maximo) {
                    maximo = matriz[i][j];
                }
            }
        }
        return maximo;
    }

    /**
     * Valida que la matriz heurística sea válida
     */
    private void validarMatriz() {
        if (matrizHeuristica == null) {
            throw new IllegalArgumentException("La matriz heurística no puede ser null");
        }

        for (int i = 0; i < tamaño; i++) {
            if (matrizHeuristica[i].length != tamaño) {
                throw new IllegalArgumentException("La matriz heurística debe ser cuadrada");
            }
            for (int j = 0; j < tamaño; j++) {
                if (Double.isNaN(matrizHeuristica[i][j]) || matrizHeuristica[i][j] < 0) {
                    throw new IllegalArgumentException(
                        "Valor heurístico inválido en posición [" + i + "][" + j + "]: " + matrizHeuristica[i][j]);
                }
            }
        }
    }

    /**
     * Obtiene el valor heurístico entre dos nodos
     * @param origen Nodo origen
     * @param destino Nodo destino
     * @return Valor heurístico
     */
    public double getHeuristica(int origen, int destino) {
        if (origen < 0 || origen >= tamaño || destino < 0 || destino >= tamaño) {
            throw new IllegalArgumentException("Índices fuera de rango: " + origen + ", " + destino);
        }
        return matrizHeuristica[origen][destino];
    }

    /**
     * Establece un valor heurístico específico
     * @param origen Nodo origen
     * @param destino Nodo destino
     * @param valor Nuevo valor heurístico
     */
    public void setHeuristica(int origen, int destino, double valor) {
        if (origen < 0 || origen >= tamaño || destino < 0 || destino >= tamaño) {
            throw new IllegalArgumentException("Índices fuera de rango: " + origen + ", " + destino);
        }
        if (valor < 0 || Double.isNaN(valor)) {
            throw new IllegalArgumentException("Valor heurístico inválido: " + valor);
        }
        matrizHeuristica[origen][destino] = valor;
    }

    /**
     * Normaliza todos los valores heurísticos al rango [0,1]
     */
    public void normalizar() {
        double maximo = encontrarMaximo(matrizHeuristica);
        if (maximo > 0) {
            for (int i = 0; i < tamaño; i++) {
                for (int j = 0; j < tamaño; j++) {
                    matrizHeuristica[i][j] /= maximo;
                }
            }
        }
    }

    /**
     * Obtiene una copia de la matriz heurística
     * @return Copia de la matriz
     */
    public double[][] getMatrizCopia() {
        double[][] copia = new double[tamaño][tamaño];
        for (int i = 0; i < tamaño; i++) {
            System.arraycopy(matrizHeuristica[i], 0, copia[i], 0, tamaño);
        }
        return copia;
    }

    /**
     * Obtiene los nodos candidatos ordenados por valor heurístico
     * @param nodoActual Nodo desde el cual calcular
     * @param nodosDisponibles Array de nodos disponibles
     * @return Array de nodos ordenados por valor heurístico descendente
     */
    public int[] getNodosCandidatos(int nodoActual, boolean[] nodosDisponibles) {
        return java.util.stream.IntStream.range(0, tamaño)
                .filter(i -> nodosDisponibles[i])
                .boxed()
                .sorted((a, b) -> Double.compare(
                    getHeuristica(nodoActual, b),
                    getHeuristica(nodoActual, a)))
                .mapToInt(Integer::intValue)
                .toArray();
    }

    // Getters
    public int getTamaño() {
        return tamaño;
    }

    public TipoHeuristica getTipo() {
        return tipo;
    }
}