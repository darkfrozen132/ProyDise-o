package morapack.colonia.componentes;

import morapack.core.solucion.Solucion;
import morapack.core.problema.Problema;

/**
 * Representa una hormiga en el algoritmo de colonia de hormigas.
 * Cada hormiga construye una solución siguiendo las reglas de probabilidad
 * basadas en feromonas y información heurística.
 */
public class Hormiga {

    private int id;
    private Solucion solucionActual;
    private double fitness;
    private boolean[] visitados;

    /**
     * Constructor de la hormiga
     * @param id Identificador único de la hormiga
     */
    public Hormiga(int id) {
        this.id = id;
        this.fitness = Double.MAX_VALUE;
    }

    /**
     * Construye una solución completa para el problema dado
     * @param problema El problema a resolver
     * @param feromona Matriz de feromonas
     * @param heuristica Información heurística
     * @return La solución construida
     */
    public Solucion construirSolucion(Problema problema, Feromona feromona, Heuristica heuristica) {
        inicializarSolucion(problema);

        while (!solucionCompleta(problema)) {
            int siguienteNodo = seleccionarSiguienteNodo(problema, feromona, heuristica);
            agregarNodoASolucion(siguienteNodo);
        }

        calcularFitness(problema);
        return solucionActual;
    }

    /**
     * Inicializa la solución y estructuras auxiliares
     */
    private void inicializarSolucion(Problema problema) {
        this.solucionActual = new Solucion();
        this.visitados = new boolean[problema.getTamaño()];
        // Seleccionar nodo inicial aleatorio
        int nodoInicial = (int) (Math.random() * problema.getTamaño());
        agregarNodoASolucion(nodoInicial);
    }

    /**
     * Verifica si la solución está completa
     */
    private boolean solucionCompleta(Problema problema) {
        return solucionActual.getTamaño() == problema.getTamaño();
    }

    /**
     * Selecciona el siguiente nodo usando regla de probabilidad ACO
     */
    private int seleccionarSiguienteNodo(Problema problema, Feromona feromona, Heuristica heuristica) {
        int nodoActual = solucionActual.getUltimoNodo();
        double[] probabilidades = calcularProbabilidades(nodoActual, problema, feromona, heuristica);

        return seleccionarNodoPorRuleta(probabilidades);
    }

    /**
     * Calcula las probabilidades de transición usando la fórmula ACO
     */
    private double[] calcularProbabilidades(int nodoActual, Problema problema,
                                          Feromona feromona, Heuristica heuristica) {
        double[] probabilidades = new double[problema.getTamaño()];
        double suma = 0.0;

        for (int j = 0; j < problema.getTamaño(); j++) {
            if (!visitados[j]) {
                double tau = feromona.getFeromona(nodoActual, j);
                double eta = heuristica.getHeuristica(nodoActual, j);
                probabilidades[j] = Math.pow(tau, problema.getAlfa()) * Math.pow(eta, problema.getBeta());
                suma += probabilidades[j];
            }
        }

        // Normalizar probabilidades
        for (int j = 0; j < problema.getTamaño(); j++) {
            if (!visitados[j]) {
                probabilidades[j] /= suma;
            }
        }

        return probabilidades;
    }

    /**
     * Selecciona un nodo usando selección por ruleta
     */
    private int seleccionarNodoPorRuleta(double[] probabilidades) {
        double r = Math.random();
        double acumulado = 0.0;

        for (int i = 0; i < probabilidades.length; i++) {
            acumulado += probabilidades[i];
            if (r <= acumulado) {
                return i;
            }
        }

        // Fallback: seleccionar el último nodo no visitado
        for (int i = 0; i < visitados.length; i++) {
            if (!visitados[i]) {
                return i;
            }
        }

        return -1; // No debería llegar aquí
    }

    /**
     * Agrega un nodo a la solución actual
     */
    private void agregarNodoASolucion(int nodo) {
        solucionActual.agregarNodo(nodo);
        visitados[nodo] = true;
    }

    /**
     * Calcula el fitness de la solución actual
     */
    private void calcularFitness(Problema problema) {
        this.fitness = problema.evaluarSolucion(solucionActual);
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public Solucion getSolucion() {
        return solucionActual;
    }

    public double getFitness() {
        return fitness;
    }

    public void reiniciar() {
        this.solucionActual = null;
        this.visitados = null;
        this.fitness = Double.MAX_VALUE;
    }
}