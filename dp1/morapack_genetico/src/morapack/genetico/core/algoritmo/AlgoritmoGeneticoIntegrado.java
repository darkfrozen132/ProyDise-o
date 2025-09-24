package morapack.genetico.core.algoritmo;

import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.planificacion.PlanificadorAvanzadoEscalas;
import morapack.planificacion.RutaCompleta;
import java.util.*;

/**
 * Algoritmo Genético Integrado con Planificación Completa Avanzada
 * Combina el algoritmo genético existente con planificación real de rutas usando múltiples escalas
 */
public class AlgoritmoGeneticoIntegrado {
    
    // Parámetros del algoritmo genético
    private final int tamanoPoblacion;
    private final int numeroGeneraciones;
    private final double probabilidadCruce;
    private final double probabilidadMutacion;
    private final boolean elitismo;
    private final int tamanoElite;
    
    // Datos del problema
    private final List<Pedido> pedidos;
    private final List<Vuelo> vuelos;
    private final PlanificadorAvanzadoEscalas planificador;
    private final PlanificadorAvanzadoEscalas planificadorAvanzado;
    
    // Control de ejecución
    private final Random random;
    private final long semillaBase;
    
    // Estadísticas
    private List<Double> fitnessPromedioPorGeneracion;
    private List<Double> fitnessMaximoPorGeneracion;
    private List<String> mejoresSolucionesPorGeneracion;
    
    public AlgoritmoGeneticoIntegrado(List<Pedido> pedidos, List<Vuelo> vuelos, 
                                     int tamanoPoblacion, int numeroGeneraciones) {
        this(pedidos, vuelos, tamanoPoblacion, numeroGeneraciones, System.nanoTime());
    }
    
    public AlgoritmoGeneticoIntegrado(List<Pedido> pedidos, List<Vuelo> vuelos, 
                                     int tamanoPoblacion, int numeroGeneraciones, long seed) {
        this.pedidos = pedidos;
        this.vuelos = vuelos;
        this.tamanoPoblacion = tamanoPoblacion;
        this.numeroGeneraciones = numeroGeneraciones;
        this.probabilidadCruce = 0.8;
        this.probabilidadMutacion = 0.1;
        this.elitismo = true;
        this.tamanoElite = Math.max(1, tamanoPoblacion / 10);        
        this.planificador = new PlanificadorAvanzadoEscalas(vuelos);
        this.planificadorAvanzado = new PlanificadorAvanzadoEscalas(vuelos);
        this.semillaBase = seed;
        this.random = new Random(seed);
        
        
        this.fitnessPromedioPorGeneracion = new ArrayList<>();
        this.fitnessMaximoPorGeneracion = new ArrayList<>();
        this.mejoresSolucionesPorGeneracion = new ArrayList<>();
    }
    
    /**
     * Ejecuta el algoritmo genético con planificación completa
     */
    public IndividuoIntegrado ejecutar() {
        System.out.println("Algoritmo Genético - Pedidos: " + pedidos.size() + ", Población: " + tamanoPoblacion + ", Generaciones: " + numeroGeneraciones);
        
        // 1. INICIALIZACIÓN
        List<IndividuoIntegrado> poblacion = inicializarPoblacion();
        evaluarPoblacion(poblacion);
        
        IndividuoIntegrado mejorGlobal = Collections.max(poblacion, 
                                      Comparator.comparingDouble(IndividuoIntegrado::getFitness));
        
        System.out.println("Fitness inicial: " + String.format("%.2f", mejorGlobal.getFitness()));
        
        // 2. EVOLUCIÓN
        for (int generacion = 0; generacion < numeroGeneraciones; generacion++) {
            List<IndividuoIntegrado> nuevaPoblacion = new ArrayList<>();
            
            // Elitismo
            if (elitismo) {
                poblacion.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
                for (int i = 0; i < tamanoElite; i++) {
                    nuevaPoblacion.add(poblacion.get(i).copiar());
                }
            }
            
            // Generar resto de población
            while (nuevaPoblacion.size() < tamanoPoblacion) {
                // Selección por torneo
                IndividuoIntegrado padre1 = seleccionTorneo(poblacion);
                IndividuoIntegrado padre2 = seleccionTorneo(poblacion);
                
                // Cruce
                IndividuoIntegrado hijo = cruzar(padre1, padre2);
                
                // Mutación
                mutar(hijo);
                
                nuevaPoblacion.add(hijo);
            }
            
            poblacion = nuevaPoblacion;
            evaluarPoblacion(poblacion);
            
            // Actualizar estadísticas
            double fitnessPromedio = poblacion.stream()
                    .mapToDouble(IndividuoIntegrado::getFitness)
                    .average().orElse(0.0);
            
            IndividuoIntegrado mejorGeneracion = Collections.max(poblacion, 
                                               Comparator.comparingDouble(IndividuoIntegrado::getFitness));
            
            fitnessPromedioPorGeneracion.add(fitnessPromedio);
            fitnessMaximoPorGeneracion.add(mejorGeneracion.getFitness());
            mejoresSolucionesPorGeneracion.add(mejorGeneracion.obtenerResumen());
            
            if (mejorGeneracion.getFitness() > mejorGlobal.getFitness()) {
                mejorGlobal = mejorGeneracion.copiar();
            }
            
            // Mostrar progreso
            if ((generacion + 1) % 10 == 0 || generacion == 0) {
                System.out.printf("Gen %3d: Fitness=%.2f%n", 
                                generacion + 1, mejorGeneracion.getFitness());
            }
        }
        
        System.out.println("Fitness final: " + String.format("%.2f", mejorGlobal.getFitness()));
        System.out.println("Rutas encontradas: " + mejorGlobal.contarRutasPlanificadas() + "/" + pedidos.size());
        
        return mejorGlobal;
    }
    
    /**
     * Inicializa población con planificación real de rutas
     */
    private List<IndividuoIntegrado> inicializarPoblacion() {
        List<IndividuoIntegrado> poblacion = new ArrayList<>();
        
        for (int i = 0; i < tamanoPoblacion; i++) {
            // Generar semilla derivada para cada individuo
            long semillaIndividuo = semillaBase + i;
            IndividuoIntegrado individuo = new IndividuoIntegrado(pedidos, planificador, planificadorAvanzado, semillaIndividuo);
            individuo.inicializarConPlanificacion();
            poblacion.add(individuo);
        }
        
        return poblacion;
    }
    
    /**
     * Evalúa fitness de toda la población
     */
    private void evaluarPoblacion(List<IndividuoIntegrado> poblacion) {
        for (IndividuoIntegrado individuo : poblacion) {
            individuo.evaluarFitness();
        }
    }
    
    /**
     * Selección por torneo
     */
    private IndividuoIntegrado seleccionTorneo(List<IndividuoIntegrado> poblacion) {
        int tamanoTorneo = 3;
        IndividuoIntegrado mejor = null;
        
        for (int i = 0; i < tamanoTorneo; i++) {
            IndividuoIntegrado candidato = poblacion.get(random.nextInt(poblacion.size()));
            if (mejor == null || candidato.getFitness() > mejor.getFitness()) {
                mejor = candidato;
            }
        }
        
        return mejor;
    }
    
    /**
     * Operador de cruce con planificación
     */
    private IndividuoIntegrado cruzar(IndividuoIntegrado padre1, IndividuoIntegrado padre2) {
        if (random.nextDouble() > probabilidadCruce) {
            return random.nextBoolean() ? padre1.copiar() : padre2.copiar();
        }
        
        // Usar semilla derivada para reproducibilidad en cruce
        long semillaHijo = semillaBase + random.nextLong();
        IndividuoIntegrado hijo = new IndividuoIntegrado(pedidos, planificador, planificadorAvanzado, semillaHijo);
        
        // Cruce uniforme de rutas
        for (int i = 0; i < pedidos.size(); i++) {
            if (random.nextBoolean()) {
                hijo.asignarRuta(i, padre1.getRuta(i));
            } else {
                hijo.asignarRuta(i, padre2.getRuta(i));
            }
        }
        
        return hijo;
    }
    
    /**
     * Operador de mutación con re-planificación
     */
    private void mutar(IndividuoIntegrado individuo) {
        for (int i = 0; i < pedidos.size(); i++) {
            if (random.nextDouble() < probabilidadMutacion) {
                // Re-planificar ruta para este pedido
                individuo.replanificarRuta(i);
            }
        }
    }
    
    // Getters para estadísticas
    public List<Double> getFitnessPromedioPorGeneracion() { return fitnessPromedioPorGeneracion; }
    public List<Double> getFitnessMaximoPorGeneracion() { return fitnessMaximoPorGeneracion; }
    public List<String> getMejoresSolucionesPorGeneracion() { return mejoresSolucionesPorGeneracion; }
}
