package morapack.main;

import morapack.colonia.algoritmo.AlgoritmoColoniaHormigas;
import morapack.core.problema.ProblemaTSP;
import morapack.core.solucion.Solucion;

/**
 * Clase principal para demostrar el funcionamiento del algoritmo
 * Ant Colony Optimization implementado en MoraPack Colonia v2.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== MoraPack Colonia v2 - Ant Colony Optimization ===");
        System.out.println("Demostrando el algoritmo con diferentes problemas TSP\n");

        // Ejecutar diferentes ejemplos
        ejecutarTSPPequeño();
        System.out.println("\n" + "=".repeat(60) + "\n");

        ejecutarTSPMedio();
        System.out.println("\n" + "=".repeat(60) + "\n");

        ejecutarTSPAleatorio();
    }

    /**
     * Ejecuta un TSP pequeño con 5 ciudades
     */
    private static void ejecutarTSPPequeño() {
        System.out.println(">> EJEMPLO 1: TSP Pequeño (5 ciudades)");

        // Crear matriz de distancias para 5 ciudades
        double[][] distancias = {
            {0, 10, 15, 20, 25},
            {10, 0, 35, 25, 30},
            {15, 35, 0, 30, 20},
            {20, 25, 30, 0, 15},
            {25, 30, 20, 15, 0}
        };

        String[] nombres = {"Madrid", "Barcelona", "Valencia", "Sevilla", "Bilbao"};

        // Crear problema TSP
        ProblemaTSP problema = new ProblemaTSP(distancias, nombres);
        System.out.println("Problema: " + problema.getDescripcion());
        System.out.println("Estadísticas: " + problema.getEstadisticasDistancias());

        // Configurar y ejecutar algoritmo
        AlgoritmoColoniaHormigas algoritmo = new AlgoritmoColoniaHormigas(problema, 10, 50, 0.1);

        long tiempoInicio = System.currentTimeMillis();
        Solucion mejorSolucion = algoritmo.ejecutar();
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

        // Mostrar resultados
        mostrarResultados(mejorSolucion, problema, tiempoTotal);

        // Comparar con solución greedy
        compararConGreedy(problema);
    }

    /**
     * Ejecuta un TSP medio con 10 ciudades
     */
    private static void ejecutarTSPMedio() {
        System.out.println(">> EJEMPLO 2: TSP Medio (10 ciudades)");

        // Generar TSP aleatorio de tamaño medio
        ProblemaTSP problema = ProblemaTSP.generarAleatorio(10, 100.0);
        System.out.println("Problema: " + problema.getDescripcion());
        System.out.println("Estadísticas: " + problema.getEstadisticasDistancias());

        // Configurar algoritmo con más hormigas y iteraciones
        AlgoritmoColoniaHormigas algoritmo = new AlgoritmoColoniaHormigas(problema, 20, 100, 0.15);

        long tiempoInicio = System.currentTimeMillis();
        Solucion mejorSolucion = algoritmo.ejecutar();
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

        // Mostrar resultados
        mostrarResultados(mejorSolucion, problema, tiempoTotal);

        // Aplicar mejora local 2-opt
        System.out.println("\nAplicando mejora local 2-opt...");
        Solucion solucionMejorada = problema.aplicar2Opt(mejorSolucion);
        double mejora = mejorSolucion.getFitness() - solucionMejorada.getFitness();
        System.out.printf("Fitness después de 2-opt: %.2f (mejora: %.2f)%n",
                         solucionMejorada.getFitness(), mejora);
    }

    /**
     * Ejecuta un TSP aleatorio más grande
     */
    private static void ejecutarTSPAleatorio() {
        System.out.println(">> EJEMPLO 3: TSP Aleatorio (15 ciudades)");

        // Generar TSP aleatorio más grande
        ProblemaTSP problema = ProblemaTSP.generarAleatorio(15, 150.0);
        System.out.println("Problema: " + problema.getDescripcion());
        System.out.println("Estadísticas: " + problema.getEstadisticasDistancias());

        // Probar diferentes configuraciones de parámetros
        double[] alfaValues = {0.5, 1.0, 2.0};
        double[] betaValues = {1.0, 2.0, 3.0};

        System.out.println("\nProbando diferentes configuraciones de parámetros:");

        double mejorFitness = Double.MAX_VALUE;
        Solucion mejorSolucionGlobal = null;
        String mejorConfiguracion = "";

        for (double alfa : alfaValues) {
            for (double beta : betaValues) {
                System.out.printf("Configuración alfa=%.1f, beta=%.1f: ", alfa, beta);

                // Crear problema con nuevos parámetros
                ProblemaTSP problemaConfig = (ProblemaTSP) problema.clonarConParametros(alfa, beta);

                // Ejecutar algoritmo
                AlgoritmoColoniaHormigas algoritmo = new AlgoritmoColoniaHormigas(
                    problemaConfig, 25, 80, 0.1);

                Solucion solucion = algoritmo.ejecutar();

                if (solucion != null && solucion.getFitness() < mejorFitness) {
                    mejorFitness = solucion.getFitness();
                    mejorSolucionGlobal = solucion;
                    mejorConfiguracion = String.format("alfa=%.1f, beta=%.1f", alfa, beta);
                }

                System.out.printf("Fitness: %.2f%n",
                                solucion != null ? solucion.getFitness() : Double.MAX_VALUE);
            }
        }

        System.out.println("\nMejor configuración encontrada: " + mejorConfiguracion);
        if (mejorSolucionGlobal != null) {
            System.out.printf("Mejor fitness global: %.2f%n", mejorFitness);
        }
    }

    /**
     * Muestra los resultados de la ejecución del algoritmo
     */
    private static void mostrarResultados(Solucion solucion, ProblemaTSP problema, long tiempo) {
        if (solucion == null) {
            System.out.println("No se encontró solución válida.");
            return;
        }

        System.out.println("\n--- RESULTADOS ---");
        System.out.printf("Tiempo de ejecución: %d ms%n", tiempo);
        System.out.printf("Fitness de la mejor solución: %.2f%n", solucion.getFitness());

        // Mostrar el tour con nombres de ciudades
        System.out.print("Tour encontrado: ");
        int[] tour = solucion.getSecuenciaComoArray();
        for (int i = 0; i < tour.length; i++) {
            if (i > 0) System.out.print(" -> ");
            System.out.print(problema.getNombreCiudad(tour[i]));
        }
        System.out.println(" -> " + problema.getNombreCiudad(tour[0])); // Retorno al origen

        // Verificar validez
        boolean esValida = problema.esSolucionValida(solucion);
        System.out.println("Solución válida: " + (esValida ? "SÍ" : "NO"));
    }

    /**
     * Compara la solución ACO con una solución greedy
     */
    private static void compararConGreedy(ProblemaTSP problema) {
        System.out.println("\n--- COMPARACIÓN CON ALGORITMO GREEDY ---");

        // Generar solución greedy (vecino más cercano)
        Solucion solucionGreedy = generarSolucionGreedy(problema);

        if (solucionGreedy != null) {
            System.out.printf("Fitness solución Greedy: %.2f%n", solucionGreedy.getFitness());
            System.out.println("Tour Greedy: " + solucionGreedy.toStringCompacto());
        } else {
            System.out.println("No se pudo generar solución greedy");
        }

        // Generar múltiples soluciones aleatorias para comparación
        System.out.println("\nComparación con soluciones aleatorias:");
        double sumaAleatorias = 0.0;
        int numAleatorias = 10;

        for (int i = 0; i < numAleatorias; i++) {
            Solucion aleatoria = problema.generarSolucionAleatoria();
            sumaAleatorias += aleatoria.getFitness();
        }

        double promedioAleatorias = sumaAleatorias / numAleatorias;
        System.out.printf("Fitness promedio de %d soluciones aleatorias: %.2f%n",
                         numAleatorias, promedioAleatorias);
    }

    /**
     * Genera una solución usando el algoritmo greedy del vecino más cercano
     */
    private static Solucion generarSolucionGreedy(ProblemaTSP problema) {
        int tamaño = problema.getTamaño();
        boolean[] visitados = new boolean[tamaño];
        Solucion solucion = new Solucion();

        // Empezar desde la ciudad 0
        int ciudadActual = 0;
        solucion.agregarNodo(ciudadActual);
        visitados[ciudadActual] = true;

        // Visitar las ciudades restantes
        for (int i = 1; i < tamaño; i++) {
            int siguienteCiudad = problema.getCiudadMasCercana(ciudadActual, visitados);
            if (siguienteCiudad == -1) {
                return null; // No se pudo completar el tour
            }

            solucion.agregarNodo(siguienteCiudad);
            visitados[siguienteCiudad] = true;
            ciudadActual = siguienteCiudad;
        }

        // Evaluar la solución
        problema.evaluarSolucion(solucion);

        return solucion;
    }

    /**
     * Método para ejecutar solo un ejemplo específico (útil para testing)
     */
    public static void ejecutarEjemploSimple() {
        System.out.println("=== Ejemplo Simple de ACO ===");

        // TSP de 4 ciudades
        double[][] distancias = {
            {0, 2, 9, 10},
            {1, 0, 6, 4},
            {15, 7, 0, 8},
            {6, 3, 12, 0}
        };

        ProblemaTSP problema = new ProblemaTSP(distancias);
        AlgoritmoColoniaHormigas algoritmo = new AlgoritmoColoniaHormigas(problema, 5, 20, 0.1);

        Solucion solucion = algoritmo.ejecutar();

        if (solucion != null) {
            System.out.printf("Mejor fitness encontrado: %.2f%n", solucion.getFitness());
            System.out.println("Tour: " + solucion.toStringCompacto());
        }
    }
}