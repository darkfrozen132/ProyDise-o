package morapack.optimizacion;

import morapack.modelo.*;
import morapack.genetico.IndividuoBase;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Optimizador específico para el problema MoraPack usando algoritmo genético
 */
public class OptimizadorMoraPack {
    private List<Aeropuerto> sedes;
    private List<Pedido> pedidos;
    
    // Parámetros del algoritmo genético
    private int tamanoPoblacion = 100;
    private int numeroGeneraciones = 1000;
    private double probabilidadCruce = 0.8;
    private double probabilidadMutacion = 0.1;
    
    public OptimizadorMoraPack(List<Aeropuerto> sedes, List<Pedido> pedidos) {
        this.sedes = sedes;
        this.pedidos = pedidos;
    }
    
    public void configurar(int tamanoPoblacion, int numeroGeneraciones, 
                          double probabilidadCruce, double probabilidadMutacion) {
        this.tamanoPoblacion = tamanoPoblacion;
        this.numeroGeneraciones = numeroGeneraciones;
        this.probabilidadCruce = probabilidadCruce;
        this.probabilidadMutacion = probabilidadMutacion;
    }
    
    public IndividuoMoraPack optimizar() {
        System.out.println("Iniciando algoritmo genético...");
        System.out.printf("Población: %d, Generaciones: %d, Pc: %.2f, Pm: %.2f%n",
                         tamanoPoblacion, numeroGeneraciones, probabilidadCruce, probabilidadMutacion);
        
        // Crear población inicial
        List<IndividuoMoraPack> poblacion = crearPoblacionInicial();
        evaluarPoblacion(poblacion);
        
        IndividuoMoraPack mejorGlobal = obtenerMejor(poblacion);
        double mejorFitnessInicial = mejorGlobal.getFitness();
        
        System.out.printf("Fitness inicial: %.6f%n", mejorFitnessInicial);
        
        // Evolución
        for (int generacion = 1; generacion <= numeroGeneraciones; generacion++) {
            List<IndividuoMoraPack> nuevaPoblacion = new ArrayList<>();
            
            // Elitismo: mantener el mejor
            nuevaPoblacion.add((IndividuoMoraPack) mejorGlobal.clonar());
            
            // Generar nueva población
            while (nuevaPoblacion.size() < tamanoPoblacion) {
                // Selección por torneo
                IndividuoMoraPack padre1 = seleccionTorneo(poblacion);
                IndividuoMoraPack padre2 = seleccionTorneo(poblacion);
                
                List<IndividuoMoraPack> hijos;
                
                // Cruce
                if (Math.random() < probabilidadCruce) {
                    hijos = IndividuoMoraPack.cruzar(padre1, padre2);
                } else {
                    hijos = new ArrayList<>();
                    hijos.add((IndividuoMoraPack) padre1.clonar());
                    hijos.add((IndividuoMoraPack) padre2.clonar());
                }
                
                // Mutación
                for (IndividuoMoraPack hijo : hijos) {
                    hijo.mutar(probabilidadMutacion);
                    if (nuevaPoblacion.size() < tamanoPoblacion) {
                        nuevaPoblacion.add(hijo);
                    }
                }
            }
            
            poblacion = nuevaPoblacion;
            evaluarPoblacion(poblacion);
            
            IndividuoMoraPack mejorActual = obtenerMejor(poblacion);
            if (mejorActual.getFitness() > mejorGlobal.getFitness()) {
                mejorGlobal = (IndividuoMoraPack) mejorActual.clonar();
            }
            
            // Mostrar progreso cada 100 generaciones
            if (generacion % 100 == 0) {
                double porcentajeMejora = ((mejorGlobal.getFitness() - mejorFitnessInicial) / mejorFitnessInicial) * 100;
                System.out.printf("Generación %d: Fitness=%.6f (%.2f%% mejora)%n", 
                                generacion, mejorGlobal.getFitness(), porcentajeMejora);
            }
        }
        
        double mejorFitnessFinal = mejorGlobal.getFitness();
        double porcentajeMejoraTotal = ((mejorFitnessFinal - mejorFitnessInicial) / mejorFitnessInicial) * 100;
        
        System.out.printf("Optimización completada. Mejora total: %.2f%%%n", porcentajeMejoraTotal);
        
        return mejorGlobal;
    }
    
    private List<IndividuoMoraPack> crearPoblacionInicial() {
        List<IndividuoMoraPack> poblacion = new ArrayList<>();
        
        for (int i = 0; i < tamanoPoblacion; i++) {
            IndividuoMoraPack individuo = new IndividuoMoraPack(pedidos, sedes);
            poblacion.add(individuo);
        }
        
        return poblacion;
    }
    
    private void evaluarPoblacion(List<IndividuoMoraPack> poblacion) {
        for (IndividuoMoraPack individuo : poblacion) {
            individuo.evaluar();
        }
    }
    
    private IndividuoMoraPack obtenerMejor(List<IndividuoMoraPack> poblacion) {
        IndividuoMoraPack mejor = poblacion.get(0);
        
        for (IndividuoMoraPack individuo : poblacion) {
            if (individuo.getFitness() > mejor.getFitness()) {
                mejor = individuo;
            }
        }
        
        return mejor;
    }
    
    private IndividuoMoraPack seleccionTorneo(List<IndividuoMoraPack> poblacion) {
        int tamanoTorneo = 3;
        Random random = new Random();
        
        IndividuoMoraPack ganador = poblacion.get(random.nextInt(poblacion.size()));
        
        for (int i = 1; i < tamanoTorneo; i++) {
            IndividuoMoraPack competidor = poblacion.get(random.nextInt(poblacion.size()));
            if (competidor.getFitness() > ganador.getFitness()) {
                ganador = competidor;
            }
        }
        
        return ganador;
    }
    
    /**
     * Genera una solución aleatoria para comparación
     */
    public IndividuoMoraPack generarSolucionAleatoria() {
        return new IndividuoMoraPack(pedidos, sedes);
    }
    
    /**
     * Compara la solución optimizada con una solución aleatoria
     */
    public void compararConSolucionAleatoria(IndividuoMoraPack solucionOptimizada) {
        IndividuoMoraPack solucionAleatoria = generarSolucionAleatoria();
        solucionAleatoria.evaluar();
        
        IndividuoMoraPack.EstadisticasSolucion statsOptimizada = solucionOptimizada.obtenerEstadisticas();
        IndividuoMoraPack.EstadisticasSolucion statsAleatoria = solucionAleatoria.obtenerEstadisticas();
        
        double mejoraPorcentual = ((statsAleatoria.costoTotal - statsOptimizada.costoTotal) / statsAleatoria.costoTotal) * 100;
        
        System.out.println("\n=== COMPARACIÓN CON SOLUCIÓN ALEATORIA ===");
        System.out.printf("Costo solución aleatoria: $%.2f%n", statsAleatoria.costoTotal);
        System.out.printf("Costo solución optimizada: $%.2f%n", statsOptimizada.costoTotal);
        System.out.printf("Mejora obtenida: %.2f%%%n", mejoraPorcentual);
        System.out.printf("Violaciones (aleatoria): %d%n", statsAleatoria.violacionesFactibilidad);
        System.out.printf("Violaciones (optimizada): %d%n", statsOptimizada.violacionesFactibilidad);
        System.out.println("==========================================");
    }
}
