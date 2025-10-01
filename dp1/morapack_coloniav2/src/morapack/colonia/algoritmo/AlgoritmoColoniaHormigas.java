package morapack.colonia.algoritmo;

import morapack.colonia.componentes.Feromona;
import morapack.colonia.componentes.Heuristica;
import morapack.colonia.componentes.Hormiga;
import morapack.core.problema.Problema;
import morapack.core.problema.ProblemaMoraPack;
import morapack.core.solucion.Solucion;
import morapack.core.solucion.SolucionMoraPack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementación del algoritmo Ant Colony Optimization (ACO).
 * Gestiona una colonia de hormigas que colaboran para encontrar
 * soluciones óptimas a problemas de optimización combinatoria.
 */
public class AlgoritmoColoniaHormigas {

    // Componentes principales
    private Problema problema;
    private Feromona feromona;
    private Heuristica heuristica;
    private List<Hormiga> colonia;

    // Parámetros del algoritmo
    private int numeroHormigas;
    private int maxIteraciones;
    private double tasaEvaporacion;
    private boolean convergenciaHabilitada;
    private int iteracionesSinMejora;
    private double umbralConvergencia;

    // Estado del algoritmo
    private Solucion mejorSolucionGlobal;
    private Solucion mejorSolucionIteracion;
    private int iteracionActual;
    private boolean algoritmoPausado;
    private boolean algoritmoTerminado;
    private List<EstadisticasIteracion> historialEstadisticas;

    // Configuración optimizada para problemas de logística
    private static final int NUMERO_HORMIGAS_DEFAULT = 15; // Más hormigas para mayor diversidad
    private static final int MAX_ITERACIONES_DEFAULT = 150; // Más iteraciones para problemas complejos
    private static final double TASA_EVAPORACION_DEFAULT = 0.15; // Mayor evaporación para evitar estancamiento
    private static final int MAX_ITERACIONES_SIN_MEJORA = 20; // Más paciencia para problemas logísticos
    private static final double UMBRAL_CONVERGENCIA_DEFAULT = 0.001;

    /**
     * Constructor con parámetros por defecto
     * @param problema El problema a resolver
     */
    public AlgoritmoColoniaHormigas(Problema problema) {
        this(problema, NUMERO_HORMIGAS_DEFAULT, MAX_ITERACIONES_DEFAULT, TASA_EVAPORACION_DEFAULT);
    }

    /**
     * Constructor completo
     * @param problema El problema a resolver
     * @param numeroHormigas Número de hormigas en la colonia
     * @param maxIteraciones Máximo número de iteraciones
     * @param tasaEvaporacion Tasa de evaporación de feromonas
     */
    public AlgoritmoColoniaHormigas(Problema problema, int numeroHormigas,
                                  int maxIteraciones, double tasaEvaporacion) {
        if (problema == null) {
            throw new IllegalArgumentException("El problema no puede ser null");
        }
        if (numeroHormigas <= 0) {
            throw new IllegalArgumentException("Número de hormigas debe ser positivo");
        }
        if (maxIteraciones <= 0) {
            throw new IllegalArgumentException("Máximo de iteraciones debe ser positivo");
        }
        if (tasaEvaporacion < 0 || tasaEvaporacion > 1) {
            throw new IllegalArgumentException("Tasa de evaporación debe estar entre 0 y 1");
        }

        this.problema = problema;
        this.numeroHormigas = numeroHormigas;
        this.maxIteraciones = maxIteraciones;
        this.tasaEvaporacion = tasaEvaporacion;

        // Configuración por defecto
        this.convergenciaHabilitada = true;
        this.iteracionesSinMejora = 0;
        this.umbralConvergencia = UMBRAL_CONVERGENCIA_DEFAULT;
        this.historialEstadisticas = new ArrayList<>();

        inicializar();
    }

    /**
     * Inicializa todos los componentes del algoritmo
     */
    private void inicializar() {
        // Crear matriz de feromonas
        this.feromona = new Feromona(problema.getTamaño(), 0.1, tasaEvaporacion, 0.01, 10.0);

        // Crear información heurística específica para MoraPack
        if (problema instanceof ProblemaMoraPack) {
            this.heuristica = new Heuristica((ProblemaMoraPack) problema);
        } else {
            // Fallback para compatibilidad hacia atrás
            double[][] distancias = problema.getMatrizDistancias();
            // Crear heurística básica manualmente para TSP genérico
            throw new UnsupportedOperationException("Solo se soporta ProblemaMoraPack en esta versión");
        }

        // Crear colonia de hormigas
        this.colonia = new ArrayList<>();
        for (int i = 0; i < numeroHormigas; i++) {
            colonia.add(new Hormiga(i));
        }

        // Inicializar estado
        this.mejorSolucionGlobal = null;
        this.mejorSolucionIteracion = null;
        this.iteracionActual = 0;
        this.algoritmoPausado = false;
        this.algoritmoTerminado = false;
    }

    /**
     * Ejecuta el algoritmo completo
     * @return La mejor solución encontrada
     */
    public Solucion ejecutar() {
        System.out.println("Iniciando algoritmo ACO...");
        System.out.println("Problema: " + problema.getDescripcion());
        System.out.println("Parámetros: " + numeroHormigas + " hormigas, " + maxIteraciones + " iteraciones");

        long tiempoInicio = System.currentTimeMillis();

        while (!debeTerminar()) {
            if (!algoritmoPausado) {
                ejecutarIteracion();
                iteracionActual++;

                // Mostrar progreso cada 10 iteraciones
                if (iteracionActual % 10 == 0) {
                    mostrarProgreso();
                }
            } else {
                // Pequeña pausa si el algoritmo está pausado
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long tiempoEjecucion = System.currentTimeMillis() - tiempoInicio;
        algoritmoTerminado = true;

        System.out.println("\nAlgoritmo completado:");
        System.out.println("Tiempo de ejecución: " + tiempoEjecucion + " ms");
        System.out.println("Iteraciones: " + iteracionActual);
        if (mejorSolucionGlobal != null) {
            System.out.println("Mejor solución: " + mejorSolucionGlobal.getFitness());
        }

        return mejorSolucionGlobal;
    }

    /**
     * Ejecuta una sola iteración del algoritmo
     */
    public void ejecutarIteracion() {
        // 1. Reiniciar hormigas
        for (Hormiga hormiga : colonia) {
            hormiga.reiniciar();
        }

        // 2. Construcción de soluciones
        List<Solucion> solucionesIteracion = new ArrayList<>();
        for (Hormiga hormiga : colonia) {
            Solucion solucion = hormiga.construirSolucion(problema, feromona, heuristica);
            if (problema.esSolucionValida(solucion)) {
                solucionesIteracion.add(solucion);
            }
        }

        // 3. Actualizar mejor solución de la iteración
        mejorSolucionIteracion = encontrarMejorSolucion(solucionesIteracion);

        // 4. Actualizar mejor solución global
        if (mejorSolucionGlobal == null ||
            (mejorSolucionIteracion != null &&
             mejorSolucionIteracion.getFitness() > mejorSolucionGlobal.getFitness())) {

            mejorSolucionGlobal = mejorSolucionIteracion.clone();
            iteracionesSinMejora = 0;
        } else {
            iteracionesSinMejora++;
        }

        // 5. Evaporación de feromonas
        feromona.evaporar();

        // 6. Deposición de feromonas
        depositarFeromonas(solucionesIteracion);

        // 7. Registrar estadísticas
        registrarEstadisticas(solucionesIteracion);
    }

    /**
     * Encuentra la mejor solución de una lista
     */
    private Solucion encontrarMejorSolucion(List<Solucion> soluciones) {
        if (soluciones.isEmpty()) {
            return null;
        }

        Solucion mejor = soluciones.get(0);
        for (Solucion solucion : soluciones) {
            if (solucion.getFitness() > mejor.getFitness()) {
                mejor = solucion;
            }
        }
        return mejor;
    }

    /**
     * Deposita feromonas según las soluciones encontradas
     * Adaptado para el modelo de entregas parciales de MoraPack
     */
    private void depositarFeromonas(List<Solucion> soluciones) {
        if (problema instanceof ProblemaMoraPack) {
            depositarFeromonasMoraPack(soluciones);
        } else {
            depositarFeromonasTSP(soluciones);
        }
    }

    /**
     * Estrategia de depositación específica para MoraPack
     */
    private void depositarFeromonasMoraPack(List<Solucion> soluciones) {
        // Estrategia diversificada: mejor 30% deposita feromona
        soluciones.sort((s1, s2) -> Double.compare(s2.getFitness(), s1.getFitness()));
        int numElite = Math.max(1, (int) (soluciones.size() * 0.3));

        for (int i = 0; i < numElite; i++) {
            SolucionMoraPack solucion = (SolucionMoraPack) soluciones.get(i);
            double factorElite = 1.0 - (i * 0.2); // Decreciente para diversidad

            depositarFeromonaPorPedidos(solucion, factorElite);
        }

        // Refuerzo de la mejor solución global (pero más conservador)
        if (mejorSolucionGlobal != null && ThreadLocalRandom.current().nextDouble() < 0.05) {
            depositarFeromonaPorPedidos((SolucionMoraPack) mejorSolucionGlobal, 0.3);
        }
    }

    /**
     * Deposita feromona basada en los pedidos y rutas de una solución
     */
    private void depositarFeromonaPorPedidos(SolucionMoraPack solucion, double factor) {
        double cantidadBase = Feromona.calcularCantidadFeromona(
            solucion.getFitness(), problema.getConstanteQ()) * factor;

        // Para cada pedido en la solución
        for (Integer idPedido : solucion.getRutasPorPedido().keySet()) {
            List<SolucionMoraPack.RutaProducto> rutas = solucion.getRutasProducto(idPedido);

            for (SolucionMoraPack.RutaProducto ruta : rutas) {
                // Bonus por entregas que cumplen plazo
                double bonusPlazo = ruta.cumplePlazo() ? 1.2 : 0.8;

                // Bonus por eficiencia de la entrega
                double bonusEficiencia = ruta.esEntregaParcial() ?
                    (1.0 + ruta.porcentajeCompletado() * 0.3) : 1.1;

                double cantidadFeromona = cantidadBase * bonusPlazo * bonusEficiencia;

                // Crear camino de feromona para esta ruta
                int[] caminoRuta = construirCaminoDeRuta(ruta);
                feromona.depositarFeromonaEnCamino(caminoRuta, cantidadFeromona);
            }
        }
    }

    /**
     * Construye un camino de feromona para una ruta específica
     */
    private int[] construirCaminoDeRuta(SolucionMoraPack.RutaProducto ruta) {
        // Simplificación: usar índices de aeropuertos para la matriz de feromonas
        // En un sistema más complejo, esto mapearía rutas a índices de feromona
        List<String> aeropuertos = new ArrayList<>();
        aeropuertos.add(ruta.getAeropuertoOrigen());

        for (SolucionMoraPack.SegmentoVuelo segmento : ruta.getSegmentos()) {
            if (!aeropuertos.contains(segmento.getAeropuertoDestino())) {
                aeropuertos.add(segmento.getAeropuertoDestino());
            }
        }

        // Convertir a índices (simplificado)
        int[] camino = new int[aeropuertos.size()];
        for (int i = 0; i < aeropuertos.size(); i++) {
            camino[i] = Math.abs(aeropuertos.get(i).hashCode()) % problema.getTamaño();
        }
        return camino;
    }

    /**
     * Fallback para problemas TSP tradicionales
     */
    private void depositarFeromonasTSP(List<Solucion> soluciones) {
        if (mejorSolucionIteracion != null) {
            double cantidadFeromona = Feromona.calcularCantidadFeromona(
                mejorSolucionIteracion.getFitness(), problema.getConstanteQ());

            int[] secuencia = mejorSolucionIteracion.getSecuenciaComoArray();
            feromona.depositarFeromonaEnCamino(secuencia, cantidadFeromona);
        }
    }

    /**
     * Registra estadísticas de la iteración actual
     */
    private void registrarEstadisticas(List<Solucion> soluciones) {
        if (soluciones.isEmpty()) {
            return;
        }

        double sumFitness = 0.0;
        double mejorFitness = Double.MIN_VALUE;
        double peorFitness = Double.MAX_VALUE;

        // Estadísticas adicionales para MoraPack
        int totalPedidos = 0;
        int pedidosCompletos = 0;
        int entregasParciales = 0;
        double promedioEntregasPorPedido = 0.0;

        for (Solucion solucion : soluciones) {
            double fitness = solucion.getFitness();
            sumFitness += fitness;
            mejorFitness = Math.max(mejorFitness, fitness);
            peorFitness = Math.min(peorFitness, fitness);

            // Estadísticas específicas de MoraPack
            if (solucion instanceof SolucionMoraPack) {
                SolucionMoraPack solMP = (SolucionMoraPack) solucion;
                totalPedidos += solMP.getRutasPorPedido().size();

                for (Integer idPedido : solMP.getRutasPorPedido().keySet()) {
                    if (solMP.pedidoCompleto(idPedido)) {
                        pedidosCompletos++;
                    }
                    List<SolucionMoraPack.RutaProducto> rutas = solMP.getRutasProducto(idPedido);
                    if (rutas.size() > 1) {
                        entregasParciales++;
                    }
                    promedioEntregasPorPedido += rutas.size();
                }
            }
        }

        double fitnessPromedio = sumFitness / soluciones.size();

        if (totalPedidos > 0) {
            promedioEntregasPorPedido /= totalPedidos;
        }

        EstadisticasIteracion stats = new EstadisticasIteracion(
            iteracionActual, mejorFitness, peorFitness, fitnessPromedio,
            mejorSolucionGlobal != null ? mejorSolucionGlobal.getFitness() : Double.MIN_VALUE,
            totalPedidos, pedidosCompletos, entregasParciales, promedioEntregasPorPedido
        );

        historialEstadisticas.add(stats);
    }

    /**
     * Verifica si el algoritmo debe terminar
     */
    private boolean debeTerminar() {
        if (iteracionActual >= maxIteraciones) {
            return true;
        }

        if (convergenciaHabilitada && iteracionesSinMejora >= MAX_ITERACIONES_SIN_MEJORA) {
            System.out.println("Convergencia alcanzada después de " + iteracionesSinMejora + " iteraciones sin mejora");
            return true;
        }

        return algoritmoTerminado;
    }

    /**
     * Muestra el progreso del algoritmo
     */
    private void mostrarProgreso() {
        System.out.printf("Iteración %d/%d - Mejor fitness: %.4f - Sin mejora: %d%n",
            iteracionActual, maxIteraciones,
            mejorSolucionGlobal != null ? mejorSolucionGlobal.getFitness() : Double.MIN_VALUE,
            iteracionesSinMejora);
    }

    /**
     * Pausa el algoritmo
     */
    public void pausar() {
        this.algoritmoPausado = true;
    }

    /**
     * Reanuda el algoritmo
     */
    public void reanudar() {
        this.algoritmoPausado = false;
    }

    /**
     * Termina el algoritmo prematuramente
     */
    public void terminar() {
        this.algoritmoTerminado = true;
    }

    /**
     * Reinicia el algoritmo
     */
    public void reiniciar() {
        inicializar();
    }

    // Getters y Setters
    public Solucion getMejorSolucionGlobal() {
        return mejorSolucionGlobal;
    }

    public Solucion getMejorSolucionIteracion() {
        return mejorSolucionIteracion;
    }

    public int getIteracionActual() {
        return iteracionActual;
    }

    public boolean estaPausado() {
        return algoritmoPausado;
    }

    public boolean estaTerminado() {
        return algoritmoTerminado;
    }

    public List<EstadisticasIteracion> getHistorialEstadisticas() {
        return new ArrayList<>(historialEstadisticas);
    }

    public void setNumeroHormigas(int numeroHormigas) {
        if (numeroHormigas > 0) {
            this.numeroHormigas = numeroHormigas;
        }
    }

    public void setMaxIteraciones(int maxIteraciones) {
        if (maxIteraciones > 0) {
            this.maxIteraciones = maxIteraciones;
        }
    }

    public void setConvergenciaHabilitada(boolean habilitada) {
        this.convergenciaHabilitada = habilitada;
    }

    /**
     * Clase interna para almacenar estadísticas de cada iteración
     * Incluye métricas específicas para problemas de logística
     */
    public static class EstadisticasIteracion {
        public final int iteracion;
        public final double mejorFitness;
        public final double peorFitness;
        public final double fitnessPromedio;
        public final double mejorFitnessGlobal;

        // Estadísticas específicas de MoraPack
        public final int totalPedidos;
        public final int pedidosCompletos;
        public final int entregasParciales;
        public final double promedioEntregasPorPedido;

        // Constructor básico (compatibilidad hacia atrás)
        public EstadisticasIteracion(int iteracion, double mejorFitness, double peorFitness,
                                   double fitnessPromedio, double mejorFitnessGlobal) {
            this(iteracion, mejorFitness, peorFitness, fitnessPromedio, mejorFitnessGlobal,
                 0, 0, 0, 0.0);
        }

        // Constructor completo con estadísticas MoraPack
        public EstadisticasIteracion(int iteracion, double mejorFitness, double peorFitness,
                                   double fitnessPromedio, double mejorFitnessGlobal,
                                   int totalPedidos, int pedidosCompletos, int entregasParciales,
                                   double promedioEntregasPorPedido) {
            this.iteracion = iteracion;
            this.mejorFitness = mejorFitness;
            this.peorFitness = peorFitness;
            this.fitnessPromedio = fitnessPromedio;
            this.mejorFitnessGlobal = mejorFitnessGlobal;
            this.totalPedidos = totalPedidos;
            this.pedidosCompletos = pedidosCompletos;
            this.entregasParciales = entregasParciales;
            this.promedioEntregasPorPedido = promedioEntregasPorPedido;
        }

        public double tasaCompletitud() {
            return totalPedidos > 0 ? (double) pedidosCompletos / totalPedidos : 0.0;
        }

        public double tasaEntregasParciales() {
            return totalPedidos > 0 ? (double) entregasParciales / totalPedidos : 0.0;
        }

        @Override
        public String toString() {
            if (totalPedidos > 0) {
                return String.format("Iter %d: fitness=%.2f (prom=%.2f, global=%.2f) | " +
                                   "Pedidos: %d/%d completos (%.1f%%) | " +
                                   "Parciales: %d (%.1f%%) | Entregas/Pedido: %.1f",
                    iteracion, mejorFitness, fitnessPromedio, mejorFitnessGlobal,
                    pedidosCompletos, totalPedidos, tasaCompletitud() * 100,
                    entregasParciales, tasaEntregasParciales() * 100,
                    promedioEntregasPorPedido);
            } else {
                return String.format("Iter %d: mejor=%.4f, promedio=%.4f, global=%.4f",
                    iteracion, mejorFitness, fitnessPromedio, mejorFitnessGlobal);
            }
        }
    }
}