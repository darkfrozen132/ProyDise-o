package morapack.main;

import morapack.datos.CargadorDatosCSV;
import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.genetico.core.algoritmo.AlgoritmoGeneticoIntegrado;
import morapack.genetico.core.algoritmo.IndividuoIntegrado;
import java.util.List;
import java.io.*;

/**
 * Programa principal que demuestra el Algoritmo Genético Integrado
 * Combina asignación simple con planificación completa avanzada
 */
public class MainAlgoritmoIntegrado {
    
    public static void main(String[] args) {
        try {
            System.out.println("🚀 ALGORITMO GENÉTICO INTEGRADO - MORAPACK");
            System.out.println("===========================================");
            System.out.println("🎯 Combinando asignación simple + planificación completa");
            System.out.println();
            
            // 1. CARGAR DATOS
            System.out.println("📂 Cargando datos...");
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            List<Pedido> pedidos = generarPedidosEjemplo();
            
            System.out.println("✅ Datos cargados:");
            System.out.println("  • Vuelos: " + vuelos.size());
            System.out.println("  • Pedidos: " + pedidos.size());
            System.out.println();
            
            // 2. CONFIGURAR ALGORITMO
            int tamanoPoblacion = 20;
            int numeroGeneraciones = 30;
            
            AlgoritmoGeneticoIntegrado algoritmo = new AlgoritmoGeneticoIntegrado(
                pedidos, vuelos, tamanoPoblacion, numeroGeneraciones
            );
            
            // 3. EJECUTAR ALGORITMO
            System.out.println("🧬 Ejecutando algoritmo genético integrado...");
            System.out.println();
            
            long inicioTiempo = System.currentTimeMillis();
            IndividuoIntegrado mejorSolucion = algoritmo.ejecutar();
            long tiempoEjecucion = System.currentTimeMillis() - inicioTiempo;
            
            // 4. MOSTRAR RESULTADOS
            System.out.println();
            System.out.println("🏆 RESULTADOS FINALES");
            System.out.println("=====================");
            System.out.println("⏱️ Tiempo de ejecución: " + tiempoEjecucion + " ms");
            System.out.println("🎯 Fitness final: " + String.format("%.2f", mejorSolucion.getFitness()));
            System.out.println("📊 Rutas planificadas: " + mejorSolucion.contarRutasPlanificadas() + "/" + pedidos.size());
            System.out.println();
            
            // 5. DETALLE DE LA SOLUCIÓN
            System.out.println("📋 DETALLE DE LA MEJOR SOLUCIÓN:");
            System.out.println(mejorSolucion.obtenerDescripcionDetallada());
            
            // 6. ESTADÍSTICAS DE EVOLUCIÓN
            mostrarEstadisticasEvolucion(algoritmo);
            
            // 7. GUARDAR RESULTADOS
            guardarResultados(mejorSolucion, algoritmo, tiempoEjecucion);
            
            System.out.println("✅ ¡Algoritmo genético integrado completado exitosamente!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Genera pedidos de ejemplo para la demostración
     */
    private static List<Pedido> generarPedidosEjemplo() {
        List<Pedido> pedidos = List.of(
            new Pedido("0000001", "KJFK", 25, 1),
            new Pedido("0000002", "EGLL", 35, 2),
            new Pedido("0000003", "LFPG", 15, 1),
            new Pedido("0000004", "EDDF", 45, 3),
            new Pedido("0000005", "LEMD", 20, 2),
            new Pedido("0000006", "LIRF", 30, 1),
            new Pedido("0000007", "LTFM", 40, 2),
            new Pedido("0000008", "SVMI", 25, 3)
        );
        
        System.out.println("📦 Pedidos generados:");
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido p = pedidos.get(i);
            System.out.printf("  [%d] Cliente %s → %s (Prioridad: %d)%n", 
                           i+1, p.getClienteId(), p.getAeropuertoDestinoId(), p.getPrioridad());
        }
        System.out.println();
        
        return pedidos;
    }
    
    /**
     * Muestra estadísticas de la evolución
     */
    private static void mostrarEstadisticasEvolucion(AlgoritmoGeneticoIntegrado algoritmo) {
        System.out.println("📈 ESTADÍSTICAS DE EVOLUCIÓN:");
        System.out.println("=============================");
        
        List<Double> fitnessMax = algoritmo.getFitnessMaximoPorGeneracion();
        List<Double> fitnessPromedio = algoritmo.getFitnessPromedioPorGeneracion();
        
        if (!fitnessMax.isEmpty()) {
            System.out.printf("🎯 Fitness inicial: %.2f%n", fitnessMax.get(0));
            System.out.printf("🏆 Fitness final: %.2f%n", fitnessMax.get(fitnessMax.size() - 1));
            System.out.printf("📊 Mejora total: %.2f%n", 
                           fitnessMax.get(fitnessMax.size() - 1) - fitnessMax.get(0));
            
            // Mostrar evolución por décadas
            System.out.println("\n📈 Evolución por décadas:");
            for (int i = 0; i < fitnessMax.size(); i += 10) {
                System.out.printf("  Gen %2d: Mejor=%.2f | Promedio=%.2f%n",
                               i + 1, fitnessMax.get(i), fitnessPromedio.get(i));
            }
        }
        System.out.println();
    }
    
    /**
     * Guarda los resultados en archivos
     */
    private static void guardarResultados(IndividuoIntegrado solucion, 
                                        AlgoritmoGeneticoIntegrado algoritmo, 
                                        long tiempoEjecucion) {
        try {
            // Archivo de resultados detallados
            try (PrintWriter writer = new PrintWriter(new FileWriter("resultados_algoritmo_integrado.txt"))) {
                writer.println("RESULTADOS DEL ALGORITMO GENÉTICO INTEGRADO");
                writer.println("============================================");
                writer.println("Fecha: " + new java.util.Date());
                writer.println("Tiempo de ejecución: " + tiempoEjecucion + " ms");
                writer.println();
                
                writer.println(solucion.obtenerDescripcionDetallada());
                
                writer.println("\nESTADÍSTICAS DE EVOLUCIÓN:");
                writer.println("==========================");
                List<Double> fitnessMax = algoritmo.getFitnessMaximoPorGeneracion();
                List<Double> fitnessPromedio = algoritmo.getFitnessPromedioPorGeneracion();
                
                for (int i = 0; i < fitnessMax.size(); i++) {
                    writer.printf("Gen %02d: Mejor=%.2f | Promedio=%.2f%n",
                                i + 1, fitnessMax.get(i), fitnessPromedio.get(i));
                }
            }
            
            System.out.println("💾 Resultados guardados en: resultados_algoritmo_integrado.txt");
            
        } catch (IOException e) {
            System.err.println("❌ Error al guardar resultados: " + e.getMessage());
        }
    }
}
