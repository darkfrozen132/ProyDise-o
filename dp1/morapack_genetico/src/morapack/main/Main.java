package morapack.main;

import morapack.modelo.*;
import morapack.datos.CargadorDatosCSV;
import morapack.datos.CargadorPedidos;
import morapack.genetico.core.algoritmo.AlgoritmoGeneticoIntegrado;
import morapack.genetico.core.algoritmo.IndividuoIntegrado;
import java.util.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * MORAPACK GENETICO - Sistema de Optimización de Rutas
 */
public class Main {
    
    public static void main(String[] args) {
        try {
            System.out.println("EJECUTANDO 20 VECES - GENERANDO REPORTE TXT");
            
            long inicioTiempoTotal = System.currentTimeMillis();
            
            // 1. CARGAR DATOS (una sola vez)
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            List<Pedido> pedidos = CargadorPedidos.cargarDesdeArchivo("datos/pedidos/pedidos_pares_01.csv");
            
            System.out.println("Pedidos a procesar: " + pedidos.size());
            System.out.println("Ejecutando 20 veces...");
            
            // Crear archivo de resultados
            try (PrintWriter writer = new PrintWriter(new FileWriter("resultados_fitness.txt"))) {
                writer.println("RESULTADOS DE 20 EJECUCIONES - MORAPACK GENETICO");
                writer.println("=====================================================");
                writer.println("Archivo: datos/pedidos/pedidos_pares_01.csv");
                writer.println("Pedidos: " + pedidos.size());
                writer.println("Poblacion: 25, Generaciones: 30");
                writer.println("=====================================================");
                writer.println();
                
                List<Double> todosLosFitness = new ArrayList<>();
                
                // Ejecutar 20 veces
                for (int ejecucion = 1; ejecucion <= 20; ejecucion++) {
                    System.out.printf("Ejecución %d/20...%n", ejecucion);
                    
                    long inicioEjecucion = System.currentTimeMillis();
                    
                    // Cada ejecución usa una semilla diferente
                    AlgoritmoGeneticoIntegrado algoritmo = new AlgoritmoGeneticoIntegrado(
                        pedidos, vuelos, 
                        25,  // Población
                        30,  // Generaciones
                        1111L + ejecucion // Semilla diferente para cada ejecución
                    );
                    
                    IndividuoIntegrado mejorSolucion = algoritmo.ejecutar();
                    
                    long tiempoEjecucion = System.currentTimeMillis() - inicioEjecucion;
                    double fitness = mejorSolucion.getFitness();
                    todosLosFitness.add(fitness);
                    
                    // Guardar resultado en archivo
                    writer.printf("Ejecucion %02d: Fitness=%.2f, Tiempo=%d ms, Semilla=%d%n", 
                                 ejecucion, fitness, tiempoEjecucion, 1111L + ejecucion);
                    writer.flush(); // Asegurar que se escriba inmediatamente
                }
                
                // Calcular estadísticas
                double fitnessPromedio = todosLosFitness.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double fitnessMejor = todosLosFitness.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double fitnessPeor = todosLosFitness.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                
                long tiempoTotal = System.currentTimeMillis() - inicioTiempoTotal;
                
                System.out.println();
                System.out.println("=== RESUMEN ===");
                System.out.printf("Fitness Promedio: %.2f%n", fitnessPromedio);
                System.out.printf("Mejor Fitness: %.2f%n", fitnessMejor);
                System.out.printf("Peor Fitness: %.2f%n", fitnessPeor);
                System.out.printf("Tiempo total: %d ms%n", tiempoTotal);
                System.out.println("Resultados guardados en: resultados_fitness.txt");
                
            } catch (IOException e) {
                System.err.println("Error escribiendo archivo: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
