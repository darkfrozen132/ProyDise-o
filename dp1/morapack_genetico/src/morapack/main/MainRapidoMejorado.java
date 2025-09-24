package morapack.main;

import morapack.modelo.*;
import morapack.datos.CargadorDatosCSV;
import morapack.datos.CargadorPedidos;
import morapack.genetico.core.algoritmo.AlgoritmoGeneticoIntegrado;
import morapack.genetico.core.algoritmo.IndividuoIntegrado;
import morapack.planificacion.RutaCompleta;
import java.util.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * MainRapido mejorado que usa el nuevo sistema de continentes CSV
 */
public class MainRapidoMejorado {
    
    public static void main(String[] args) {
        try {
            System.out.println("EJECUTANDO 20 VECES - GENERANDO REPORTE TXT");
            
            // 0. LIMPIAR ARCHIVO DE RUTAS DETALLADAS
            try (PrintWriter cleaner = new PrintWriter(new FileWriter("resultados_rutas_genetico_detalladas.txt"))) {
                // Archivo limpio para nueva ejecución
            } catch (Exception e) {
                System.err.println("Error limpiando archivo de rutas: " + e.getMessage());
            }
            
            // 1. CARGAR DATOS (una sola vez)
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            List<Pedido> pedidos = CargadorPedidos.cargarDesdeArchivo("datos/pedidos/pedidos_prueba_final.csv");
            
            System.out.println("Pedidos a procesar: " + pedidos.size());
            System.out.println("Ejecutando 20 veces...");
            
            // Crear archivo de resultados
            try (PrintWriter writer = new PrintWriter(new FileWriter("resultados_fitness.txt"))) {
                writer.println("RESULTADOS DE 20 EJECUCIONES - MORAPACK GENETICO");
                writer.println("=====================================================");
                writer.println("Archivo: datos/pedidos/pedidos_prueba_final.csv");
                writer.println("=====================================================");
                writer.println();
                
                List<Double> todosLosFitness = new ArrayList<>();
                
                // Ejecutar 20 veces
                for (int ejecucion = 1; ejecucion <= 20; ejecucion++) {
                    System.out.printf("Ejecución %d/20...%n", ejecucion);
                    
                    // Cada ejecución usa una semilla diferente
                    AlgoritmoGeneticoIntegrado algoritmo = new AlgoritmoGeneticoIntegrado(
                        pedidos, vuelos, 
                        25,  // Población
                        30,  // Generaciones
                        1111L + ejecucion // Semilla diferente para cada ejecución
                    );
                    
                    IndividuoIntegrado mejorSolucion = algoritmo.ejecutar();
                    
                    double fitness = mejorSolucion.getFitness();
                    todosLosFitness.add(fitness);
                    
                    // Guardar resultado en archivo
                    writer.printf("Ejecucion %02d: Fitness=%.2f, Semilla=%d%n", 
                                 ejecucion, fitness, 1111L + ejecucion);
                    writer.flush(); // Asegurar que se escriba inmediatamente
                    
                    // Guardar rutas detalladas para esta ejecución
                    guardarRutasDetalladas(mejorSolucion.getRutasCompletas(), pedidos, 
                                         mejorSolucion.getAsignacionSedes(), ejecucion, fitness, 1111L + ejecucion);
                }
                
                // Calcular estadísticas
                double fitnessPromedio = todosLosFitness.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double fitnessMejor = todosLosFitness.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double fitnessPeor = todosLosFitness.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                
                writer.println();
                writer.println("=== RESUMEN ===");
                writer.printf("Fitness Promedio: %.2f%n", fitnessPromedio);
                writer.printf("Mejor Fitness: %.2f%n", fitnessMejor);
                writer.printf("Peor Fitness: %.2f%n", fitnessPeor);
                
                System.out.println();
                System.out.println("=== RESUMEN ===");
                System.out.printf("Fitness Promedio: %.2f%n", fitnessPromedio);
                System.out.printf("Mejor Fitness: %.2f%n", fitnessMejor);
                System.out.printf("Peor Fitness: %.2f%n", fitnessPeor);
                System.out.println("Resultados guardados en: resultados_fitness.txt");
                System.out.println("Rutas detalladas guardadas en: resultados_rutas_genetico_detalladas.txt");
                
            } catch (IOException e) {
                System.err.println("Error escribiendo archivo: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Guarda las rutas detalladas para una ejecución específica
     */
    private static void guardarRutasDetalladas(List<RutaCompleta> rutas, List<Pedido> pedidos, 
                                             int[] asignacionSedes, int numEjecucion, double fitness, long semilla) {
        try (PrintWriter writerRutas = new PrintWriter(new FileWriter("resultados_rutas_genetico_detalladas.txt", true))) {
            
            // Solo escribir cabecera en la primera ejecución
            if (numEjecucion == 1) {
                writerRutas.println("ALGORITMO GENETICO - RUTAS DETALLADAS");
                writerRutas.println("=====================================");
                writerRutas.println("Archivo: datos/pedidos/pedidos_prueba_final.csv");
                writerRutas.println("Fecha: " + new java.util.Date());
                writerRutas.println("=====================================");
                writerRutas.println();
            }
            
            writerRutas.printf("EJECUCIÓN %d (Fitness: %.2f, Semilla: %d):%n", numEjecucion, fitness, semilla);
            writerRutas.println("-----------------------------------------------");

            
            // Estadísticas de la ejecución
            int rutasExitosas = 0;
            int rutasDirectas = 0;
            int rutasEscalas = 0;
            
            for (int i = 0; i < rutas.size(); i++) {
                RutaCompleta ruta = rutas.get(i);
                Pedido pedido = pedidos.get(i);
                
                if (ruta != null) {
                    rutasExitosas++;
                    
                    writerRutas.printf("  Pedido %s: %s → %s", 
                        pedido.getId(), 
                        obtenerSedeAsignada(asignacionSedes[i]), 
                        pedido.getAeropuertoDestinoId());
                    
                    if ("DIRECTO".equals(ruta.getTipoRuta())) {
                        rutasDirectas++;
                        writerRutas.print(" [DIRECTO]");
                        writerRutas.println();
                    } else {
                        rutasEscalas++;
                        int numEscalas = ruta.getVuelos().size() - 1;
                        writerRutas.printf(" [%d ESCALAS]%n", numEscalas);
                        
                        // Mostrar detalles de la ruta con escalas
                        for (int j = 0; j < ruta.getVuelos().size(); j++) {
                            Vuelo vuelo = ruta.getVuelos().get(j);
                            writerRutas.printf("    %d. %s → %s (%s-%s)%n", 
                                j + 1, 
                                vuelo.getOrigen(), 
                                vuelo.getDestino(), 
                                vuelo.getHoraSalida(),
                                vuelo.getHoraLlegada());
                        }
                    }
                } else {
                    writerRutas.printf("  Pedido %s: SIN RUTA%n", pedido.getId());
                }
            }
            
            writerRutas.printf("Resumen: %d rutas exitosas (%d directas, %d con escalas)%n", 
                rutasExitosas, rutasDirectas, rutasEscalas);
            writerRutas.println();
            
        } catch (Exception e) {
            System.err.println("Error al guardar rutas detalladas: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene la sede asignada basada en el índice
     */
    private static String obtenerSedeAsignada(int indiceSedeAsignada) {
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        return sedes[indiceSedeAsignada];
    }
}
