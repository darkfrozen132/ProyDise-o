package morapack.genetico;

import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.planificacion.RutaCompleta;
import morapack.planificacion.PlanificadorConexiones;
import java.util.*;

/**
 * Algoritmo Genético con planificación completa de rutas
 */
public class AlgoritmoGeneticoRutas {
    
    private final List<Pedido> pedidos;
    private final List<Vuelo> vuelos;
    private final PlanificadorConexiones planificador;
    private final int tamanoPoblacion;
    private final double probabilidadCruce;
    private final double probabilidadMutacion;
    private final Random random;
    
    public AlgoritmoGeneticoRutas(List<Pedido> pedidos, List<Vuelo> vuelos, 
                                 int tamanoPoblacion, double probabilidadCruce, double probabilidadMutacion) {
        this.pedidos = pedidos;
        this.vuelos = vuelos;
        this.planificador = new PlanificadorConexiones(vuelos);
        this.tamanoPoblacion = tamanoPoblacion;
        this.probabilidadCruce = probabilidadCruce;
        this.probabilidadMutacion = probabilidadMutacion;
        this.random = new Random();
    }
    
    /**
     * Ejecuta el algoritmo genético completo
     */
    public IndividuoRutasCompletas ejecutar(int generaciones) {
        System.out.println("🧬 INICIANDO ALGORITMO GENÉTICO CON RUTAS COMPLETAS");
        System.out.println("📊 Población: " + tamanoPoblacion + " | Generaciones: " + generaciones);
        System.out.println("🎯 Cruce: " + (probabilidadCruce * 100) + "% | Mutación: " + (probabilidadMutacion * 100) + "%");
        
        // 1. INICIALIZACIÓN: Crear población inicial
        List<IndividuoRutasCompletas> poblacion = inicializarPoblacion();
        
        // 2. EVALUACIÓN inicial
        evaluarPoblacion(poblacion);
        
        IndividuoRutasCompletas mejorGlobal = Collections.max(poblacion, 
                                           Comparator.comparingDouble(IndividuoRutasCompletas::getFitness));
        
        System.out.println("🎯 Fitness inicial del mejor: " + mejorGlobal.getFitness());
        
        // 3. EVOLUCIÓN por generaciones
        for (int gen = 0; gen < generaciones; gen++) {
            List<IndividuoRutasCompletas> nuevaPoblacion = new ArrayList<>();
            
            // Elitismo: Mantener mejores individuos
            poblacion.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
            int elite = Math.max(1, tamanoPoblacion / 10); // 10% elite
            for (int i = 0; i < elite; i++) {
                nuevaPoblacion.add(poblacion.get(i));
            }
            
            // Generar resto de población mediante cruce y mutación
            while (nuevaPoblacion.size() < tamanoPoblacion) {
                // Selección por torneo
                IndividuoRutasCompletas padre1 = seleccionTorneo(poblacion);
                IndividuoRutasCompletas padre2 = seleccionTorneo(poblacion);
                
                // Cruce
                IndividuoRutasCompletas hijo = padre1.cruzarCon(padre2, probabilidadCruce);
                
                // Mutación
                hijo.mutar(probabilidadMutacion);
                
                nuevaPoblacion.add(hijo);
            }
            
            poblacion = nuevaPoblacion;
            evaluarPoblacion(poblacion);
            
            // Actualizar mejor global
            IndividuoRutasCompletas mejorGeneracion = Collections.max(poblacion, 
                                                    Comparator.comparingDouble(IndividuoRutasCompletas::getFitness));
            
            if (mejorGeneracion.getFitness() > mejorGlobal.getFitness()) {
                mejorGlobal = mejorGeneracion;
            }
            
            // Mostrar progreso cada 10 generaciones
            if ((gen + 1) % 10 == 0 || gen == 0) {
                System.out.printf("📈 Gen %3d: Mejor=%.0f | Promedio=%.0f%n", 
                                gen + 1, mejorGeneracion.getFitness(), 
                                poblacion.stream().mapToDouble(IndividuoRutasCompletas::getFitness).average().orElse(0));
            }
        }
        
        System.out.println("🏆 MEJOR SOLUCIÓN ENCONTRADA:");
        System.out.println(mejorGlobal.obtenerDescripcion());
        
        return mejorGlobal;
    }
    
    /**
     * Inicializa población con rutas aleatorias
     */
    private List<IndividuoRutasCompletas> inicializarPoblacion() {
        List<IndividuoRutasCompletas> poblacion = new ArrayList<>();
        
        for (int i = 0; i < tamanoPoblacion; i++) {
            IndividuoRutasCompletas individuo = new IndividuoRutasCompletas(pedidos, vuelos);
            individuo.inicializarAleatorio();
            poblacion.add(individuo);
        }
        
        return poblacion;
    }
    
    /**
     * Evalúa fitness de toda la población
     */
    private void evaluarPoblacion(List<IndividuoRutasCompletas> poblacion) {
        for (IndividuoRutasCompletas individuo : poblacion) {
            individuo.evaluarFitness();
        }
    }
    
    /**
     * Selección por torneo
     */
    private IndividuoRutasCompletas seleccionTorneo(List<IndividuoRutasCompletas> poblacion) {
        int tamanoTorneo = 5;
        IndividuoRutasCompletas mejor = null;
        
        for (int i = 0; i < tamanoTorneo; i++) {
            IndividuoRutasCompletas candidato = poblacion.get(random.nextInt(poblacion.size()));
            if (mejor == null || candidato.getFitness() > mejor.getFitness()) {
                mejor = candidato;
            }
        }
        
        return mejor;
    }
}
