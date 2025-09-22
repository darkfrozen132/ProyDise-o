package genetico.algoritmo;

import genetico.Individuo;
import genetico.Poblacion;
import genetico.operators.cruce.OperadorCruce;
import genetico.operators.mutacion.OperadorMutacion;
import genetico.operators.seleccion.OperadorSeleccion;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementación del Algoritmo Genético
 */
public class AlgoritmoGenetico<T> {
    private OperadorSeleccion<T> operadorSeleccion;
    private OperadorCruce<T> operadorCruce;
    private OperadorMutacion<T> operadorMutacion;
    private double probabilidadCruce;
    private double probabilidadMutacion;
    private int tamañoPoblacion;
    private int numeroGeneraciones;
    private boolean elitismo;
    private int tamañoElite;
    private Random random = new Random();
    
    // Estadísticas
    private List<Double> fitnessPromedioPorGeneracion;
    private List<Double> fitnessMaximoPorGeneracion;
    
    public AlgoritmoGenetico(int tamañoPoblacion, int numeroGeneraciones) {
        this.tamañoPoblacion = tamañoPoblacion;
        this.numeroGeneraciones = numeroGeneraciones;
        this.probabilidadCruce = 0.8;
        this.probabilidadMutacion = 0.01;
        this.elitismo = true;
        this.tamañoElite = Math.max(1, tamañoPoblacion / 10);
        this.fitnessPromedioPorGeneracion = new ArrayList<>();
        this.fitnessMaximoPorGeneracion = new ArrayList<>();
    }
    
    /**
     * Ejecuta el algoritmo genético
     */
    public Individuo<T> ejecutar(Poblacion<T> poblacionInicial) {
        if (operadorSeleccion == null || operadorCruce == null || operadorMutacion == null) {
            throw new IllegalStateException("Los operadores deben estar configurados antes de ejecutar");
        }
        
        Poblacion<T> poblacionActual = poblacionInicial;
        
        System.out.println("=== INICIANDO ALGORITMO GENÉTICO ===");
        System.out.println("Tamaño población: " + tamañoPoblacion);
        System.out.println("Número generaciones: " + numeroGeneraciones);
        System.out.println("Probabilidad cruce: " + probabilidadCruce);
        System.out.println("Probabilidad mutación: " + probabilidadMutacion);
        System.out.println("Elitismo: " + (elitismo ? "Sí (" + tamañoElite + " individuos)" : "No"));
        System.out.println();
        
        for (int generacion = 0; generacion < numeroGeneraciones; generacion++) {
            // 1. Evaluación (calcular fitness)
            evaluarPoblacion(poblacionActual);
            
            // 2. Registrar estadísticas
            double fitnessPromedio = poblacionActual.getFitnessPromedio();
            double fitnessMaximo = poblacionActual.getFitnessMaximo();
            fitnessPromedioPorGeneracion.add(fitnessPromedio);
            fitnessMaximoPorGeneracion.add(fitnessMaximo);
            
            // 3. Mostrar progreso
            if (generacion % 10 == 0 || generacion == numeroGeneraciones - 1) {
                System.out.printf("Generación %d: Fitness promedio=%.2f, máximo=%.2f%n", 
                    generacion, fitnessPromedio, fitnessMaximo);
            }
            
            // 4. Crear nueva generación
            if (generacion < numeroGeneraciones - 1) {
                poblacionActual = crearNuevaGeneracion(poblacionActual);
            }
        }
        
        poblacionActual.ordenar();
        Individuo<T> mejorIndividuo = poblacionActual.getMejorIndividuo();
        
        System.out.println("\n=== ALGORITMO COMPLETADO ===");
        System.out.println("Mejor individuo: " + mejorIndividuo);
        
        return mejorIndividuo;
    }
    
    /**
     * Evalúa toda la población (calcula fitness)
     */
    private void evaluarPoblacion(Poblacion<T> poblacion) {
        for (Individuo<T> individuo : poblacion) {
            individuo.getFitness(); // Esto dispara el cálculo si no está calculado
        }
    }
    
    /**
     * Crea una nueva generación usando selección, cruce y mutación
     */
    private Poblacion<T> crearNuevaGeneracion(Poblacion<T> poblacionActual) {
        Poblacion<T> nuevaPoblacion = new Poblacion<>(tamañoPoblacion);
        
        // 1. Elitismo (conservar los mejores)
        if (elitismo) {
            poblacionActual.ordenar();
            for (int i = 0; i < tamañoElite && i < poblacionActual.getTamaño(); i++) {
                nuevaPoblacion.añadirIndividuo(poblacionActual.getIndividuo(i).clonar());
            }
        }
        
        // 2. Reproducción (cruce y mutación)
        while (!nuevaPoblacion.estaLlena()) {
            // Selección de padres
            Individuo<T> padre1 = operadorSeleccion.seleccionar(poblacionActual);
            Individuo<T> padre2 = operadorSeleccion.seleccionar(poblacionActual);
            
            Individuo<T>[] hijos;
            
            // Cruce
            if (random.nextDouble() < probabilidadCruce) {
                hijos = operadorCruce.cruzar(padre1, padre2);
            } else {
                @SuppressWarnings("unchecked")
                Individuo<T>[] hijosClonados = new Individuo[]{padre1.clonar(), padre2.clonar()};
                hijos = hijosClonados;
            }
            
            // Mutación y agregar a nueva población
            for (Individuo<T> hijo : hijos) {
                if (hijo != null) {
                    operadorMutacion.mutar(hijo, probabilidadMutacion);
                    if (!nuevaPoblacion.estaLlena()) {
                        nuevaPoblacion.añadirIndividuo(hijo);
                    }
                }
            }
        }
        
        return nuevaPoblacion;
    }
    
    // Getters y setters
    public void setOperadorSeleccion(OperadorSeleccion<T> operadorSeleccion) {
        this.operadorSeleccion = operadorSeleccion;
    }
    
    public void setOperadorCruce(OperadorCruce<T> operadorCruce) {
        this.operadorCruce = operadorCruce;
    }
    
    public void setOperadorMutacion(OperadorMutacion<T> operadorMutacion) {
        this.operadorMutacion = operadorMutacion;
    }
    
    public void setProbabilidadCruce(double probabilidadCruce) {
        this.probabilidadCruce = probabilidadCruce;
    }
    
    public void setProbabilidadMutacion(double probabilidadMutacion) {
        this.probabilidadMutacion = probabilidadMutacion;
    }
    
    public void setElitismo(boolean elitismo) {
        this.elitismo = elitismo;
    }
    
    public void setTamañoElite(int tamañoElite) {
        this.tamañoElite = tamañoElite;
    }
    
    public List<Double> getFitnessPromedioPorGeneracion() {
        return new ArrayList<>(fitnessPromedioPorGeneracion);
    }
    
    public List<Double> getFitnessMaximoPorGeneracion() {
        return new ArrayList<>(fitnessMaximoPorGeneracion);
    }
}
