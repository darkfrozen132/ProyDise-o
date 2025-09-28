package morapack.core.problema;

import morapack.core.solucion.Solucion;

/**
 * Implementación del Traveling Salesman Problem (TSP) para el algoritmo ACO.
 * El objetivo es encontrar el recorrido más corto que visite todas las ciudades
 * exactamente una vez y regrese al punto de origen.
 */
public class ProblemaTSP extends Problema {

    private double[][] matrizDistancias;
    private String[] nombresCiudades;
    private boolean esSimetrico;

    /**
     * Constructor con matriz de distancias
     * @param matrizDistancias Matriz de distancias entre ciudades
     */
    public ProblemaTSP(double[][] matrizDistancias) {
        this(matrizDistancias, null);
    }

    /**
     * Constructor completo
     * @param matrizDistancias Matriz de distancias entre ciudades
     * @param nombresCiudades Nombres de las ciudades (opcional)
     */
    public ProblemaTSP(double[][] matrizDistancias, String[] nombresCiudades) {
        super(matrizDistancias.length);

        validarMatrizDistancias(matrizDistancias);
        this.matrizDistancias = clonarMatriz(matrizDistancias);
        this.esSimetrico = verificarSimetria(matrizDistancias);

        if (nombresCiudades != null) {
            if (nombresCiudades.length != tamaño) {
                throw new IllegalArgumentException("El número de nombres debe coincidir con el tamaño de la matriz");
            }
            this.nombresCiudades = nombresCiudades.clone();
        } else {
            // Generar nombres por defecto
            this.nombresCiudades = new String[tamaño];
            for (int i = 0; i < tamaño; i++) {
                this.nombresCiudades[i] = "Ciudad_" + (i + 1);
            }
        }
    }

    /**
     * Constructor para generar TSP aleatorio
     * @param numeroCiudades Número de ciudades
     * @param rangoDistancia Rango máximo de distancias
     */
    public static ProblemaTSP generarAleatorio(int numeroCiudades, double rangoDistancia) {
        if (numeroCiudades < 3) {
            throw new IllegalArgumentException("El TSP requiere al menos 3 ciudades");
        }
        if (rangoDistancia <= 0) {
            throw new IllegalArgumentException("El rango de distancia debe ser positivo");
        }

        // Generar coordenadas aleatorias
        double[][] coordenadas = new double[numeroCiudades][2];
        for (int i = 0; i < numeroCiudades; i++) {
            coordenadas[i][0] = Math.random() * rangoDistancia;
            coordenadas[i][1] = Math.random() * rangoDistancia;
        }

        // Calcular matriz de distancias euclidianas
        double[][] distancias = new double[numeroCiudades][numeroCiudades];
        for (int i = 0; i < numeroCiudades; i++) {
            for (int j = 0; j < numeroCiudades; j++) {
                if (i == j) {
                    distancias[i][j] = 0;
                } else {
                    double dx = coordenadas[i][0] - coordenadas[j][0];
                    double dy = coordenadas[i][1] - coordenadas[j][1];
                    distancias[i][j] = Math.sqrt(dx * dx + dy * dy);
                }
            }
        }

        return new ProblemaTSP(distancias);
    }

    /**
     * Valida que la matriz de distancias sea correcta
     */
    private void validarMatrizDistancias(double[][] matriz) {
        if (matriz == null) {
            throw new IllegalArgumentException("La matriz de distancias no puede ser null");
        }
        if (matriz.length < 3) {
            throw new IllegalArgumentException("El TSP requiere al menos 3 ciudades");
        }

        for (int i = 0; i < matriz.length; i++) {
            if (matriz[i].length != matriz.length) {
                throw new IllegalArgumentException("La matriz de distancias debe ser cuadrada");
            }
            if (matriz[i][i] != 0) {
                throw new IllegalArgumentException("La distancia de una ciudad a sí misma debe ser 0");
            }
            for (int j = 0; j < matriz[i].length; j++) {
                if (i != j && (matriz[i][j] <= 0 || Double.isInfinite(matriz[i][j]) || Double.isNaN(matriz[i][j]))) {
                    throw new IllegalArgumentException("Distancia inválida en posición [" + i + "][" + j + "]: " + matriz[i][j]);
                }
            }
        }
    }

    /**
     * Verifica si la matriz es simétrica
     */
    private boolean verificarSimetria(double[][] matriz) {
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[i].length; j++) {
                if (Math.abs(matriz[i][j] - matriz[j][i]) > 1e-9) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Clona la matriz de distancias
     */
    private double[][] clonarMatriz(double[][] original) {
        double[][] copia = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            copia[i] = original[i].clone();
        }
        return copia;
    }

    @Override
    public double evaluarSolucion(Solucion solucion) {
        if (solucion == null || solucion.estaVacia()) {
            return Double.MAX_VALUE;
        }

        int[] secuencia = solucion.getSecuenciaComoArray();

        // Verificar que la solución tenga el tamaño correcto
        if (secuencia.length != tamaño) {
            return Double.MAX_VALUE;
        }

        double costoTotal = 0.0;

        // Calcular costo del recorrido
        for (int i = 0; i < secuencia.length - 1; i++) {
            costoTotal += matrizDistancias[secuencia[i]][secuencia[i + 1]];
        }

        // Agregar costo de retorno al origen (característica del TSP)
        costoTotal += matrizDistancias[secuencia[secuencia.length - 1]][secuencia[0]];

        // Actualizar fitness en la solución
        solucion.setFitness(costoTotal);

        return costoTotal;
    }

    @Override
    public double[][] getMatrizDistancias() {
        return clonarMatriz(matrizDistancias);
    }

    @Override
    public boolean esSolucionValida(Solucion solucion) {
        if (solucion == null || solucion.estaVacia()) {
            return false;
        }

        int[] secuencia = solucion.getSecuenciaComoArray();

        // Verificar tamaño
        if (secuencia.length != tamaño) {
            return false;
        }

        // Verificar que todos los nodos estén presentes exactamente una vez
        boolean[] visitados = new boolean[tamaño];
        for (int nodo : secuencia) {
            if (nodo < 0 || nodo >= tamaño) {
                return false; // Nodo fuera de rango
            }
            if (visitados[nodo]) {
                return false; // Nodo repetido
            }
            visitados[nodo] = true;
        }

        // Verificar que todos los nodos hayan sido visitados
        for (boolean visitado : visitados) {
            if (!visitado) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescripcion() {
        return String.format("TSP con %d ciudades (%s)",
                           tamaño,
                           esSimetrico ? "simétrico" : "asimétrico");
    }

    @Override
    public boolean esTipoTSP() {
        return true;
    }

    @Override
    public Problema clonarConParametros(double nuevaAlfa, double nuevaBeta) {
        ProblemaTSP clon = new ProblemaTSP(this.matrizDistancias, this.nombresCiudades);
        clon.setAlfa(nuevaAlfa);
        clon.setBeta(nuevaBeta);
        clon.setConstanteQ(this.getConstanteQ());
        return clon;
    }

    /**
     * Calcula la distancia total de un tour completo
     * @param tour Array con la secuencia del tour
     * @return Distancia total del tour
     */
    public double calcularDistanciaTour(int[] tour) {
        if (tour == null || tour.length != tamaño) {
            return Double.MAX_VALUE;
        }

        double distanciaTotal = 0.0;
        for (int i = 0; i < tour.length - 1; i++) {
            distanciaTotal += matrizDistancias[tour[i]][tour[i + 1]];
        }
        // Retorno al origen
        distanciaTotal += matrizDistancias[tour[tour.length - 1]][tour[0]];

        return distanciaTotal;
    }

    /**
     * Genera una solución aleatoria válida
     * @return Solución aleatoria
     */
    public Solucion generarSolucionAleatoria() {
        // Crear lista de ciudades
        java.util.List<Integer> ciudades = new java.util.ArrayList<>();
        for (int i = 0; i < tamaño; i++) {
            ciudades.add(i);
        }

        // Mezclar aleatoriamente
        java.util.Collections.shuffle(ciudades);

        // Crear solución
        Solucion solucion = new Solucion();
        for (int ciudad : ciudades) {
            solucion.agregarNodo(ciudad);
        }

        // Evaluar la solución
        evaluarSolucion(solucion);

        return solucion;
    }

    /**
     * Aplica mejora local 2-opt a una solución
     * @param solucion Solución a mejorar
     * @return Solución mejorada
     */
    public Solucion aplicar2Opt(Solucion solucion) {
        if (solucion == null || !esSolucionValida(solucion)) {
            return solucion;
        }

        Solucion mejorSolucion = solucion.clone();
        boolean mejoro = true;

        while (mejoro) {
            mejoro = false;

            for (int i = 1; i < tamaño - 1; i++) {
                for (int j = i + 1; j < tamaño; j++) {
                    Solucion candidato = mejorSolucion.clone();
                    candidato.aplicar2Opt(i, j);

                    double fitnessAnterior = mejorSolucion.getFitness();
                    double fitnessNuevo = evaluarSolucion(candidato);

                    if (fitnessNuevo < fitnessAnterior) {
                        mejorSolucion = candidato;
                        mejoro = true;
                    }
                }
            }
        }

        return mejorSolucion;
    }

    /**
     * Obtiene la ciudad más cercana a una dada
     * @param ciudadActual Ciudad de referencia
     * @param ciudadesDisponibles Array de ciudades disponibles
     * @return Índice de la ciudad más cercana
     */
    public int getCiudadMasCercana(int ciudadActual, boolean[] ciudadesDisponibles) {
        double distanciaMinima = Double.MAX_VALUE;
        int ciudadMasCercana = -1;

        for (int i = 0; i < tamaño; i++) {
            if (ciudadesDisponibles[i] && matrizDistancias[ciudadActual][i] < distanciaMinima) {
                distanciaMinima = matrizDistancias[ciudadActual][i];
                ciudadMasCercana = i;
            }
        }

        return ciudadMasCercana;
    }

    // Getters adicionales
    public String[] getNombresCiudades() {
        return nombresCiudades.clone();
    }

    public String getNombreCiudad(int indice) {
        if (indice < 0 || indice >= tamaño) {
            return "Ciudad_Desconocida";
        }
        return nombresCiudades[indice];
    }

    public boolean esSimetrico() {
        return esSimetrico;
    }

    /**
     * Obtiene estadísticas de la matriz de distancias
     * @return String con estadísticas
     */
    public String getEstadisticasDistancias() {
        double suma = 0.0;
        double minima = Double.MAX_VALUE;
        double maxima = Double.MIN_VALUE;
        int contador = 0;

        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                if (i != j) {
                    double distancia = matrizDistancias[i][j];
                    suma += distancia;
                    minima = Math.min(minima, distancia);
                    maxima = Math.max(maxima, distancia);
                    contador++;
                }
            }
        }

        double promedio = suma / contador;

        return String.format("Distancias - Min: %.2f, Max: %.2f, Promedio: %.2f",
                           minima, maxima, promedio);
    }
}