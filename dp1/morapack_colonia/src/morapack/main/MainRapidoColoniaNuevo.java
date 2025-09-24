package morapack.main;

import morapack.datos.*;
import morapack.modelo.*;
import morapack.planificacion.PlanificadorAvanzadoEscalas;
import morapack.planificacion.RutaCompleta;
import morapack.colonia.core.algoritmo.IndividuoIntegrado;
import java.util.*;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Main para mostrar rutas detalladas del Algoritmo de Colonia de Hormigas (ACO)
 * Versión modificada para visualizar rutas generadas
 */
public class MainRapidoColoniaNuevo {
    
    public static void main(String[] args) {
        try {
            System.out.println("EJECUTANDO 20 VECES - ALGORITMO COLONIA DE HORMIGAS");
            
            // 1. CARGAR DATOS (una sola vez)
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            List<Pedido> pedidosOriginales = CargadorPedidosSimple.cargarPedidosDesdeArchivo("datos/pedidos/pedidos_prueba_final.csv");
            
            // Filtrar pedidos hacia sedes
            List<Pedido> pedidos = new ArrayList<>();
            for (Pedido pedido : pedidosOriginales) {
                String destino = pedido.getAeropuertoDestinoId();
                if (!destino.equals("SPIM") && !destino.equals("EBCI") && !destino.equals("UBBB")) {
                    pedidos.add(pedido);
                }
            }
            
                        // 2. CREAR PLANIFICADOR (una sola vez)
            PlanificadorAvanzadoEscalas planificador = new PlanificadorAvanzadoEscalas(vuelos);
            
            // 3. LIMPIAR ARCHIVO DE RUTAS DETALLADAS
            try (PrintWriter cleaner = new PrintWriter(new FileWriter("resultados_rutas_colonia_detalladas.txt"))) {
                // Archivo limpio para nueva ejecución
            } catch (Exception e) {
                System.err.println("Error limpiando archivo de rutas: " + e.getMessage());
            }
            
            System.out.printf("📊 Vuelos disponibles en CSV: %d%n", vuelos.size());
            System.out.printf("🔍 Intentando cargar desde: %s%n", "datos/pedidos/pedidos_prueba_final.csv");
            System.out.printf("✅ Pedidos cargados exitosamente: %d%n", pedidosOriginales.size());
            System.out.printf("Pedidos a procesar: %d%n", pedidos.size());
            System.out.println("Ejecutando 20 veces...");
            
            // Preparar archivo de resultados
            try (PrintWriter writer = new PrintWriter(new FileWriter("resultados_fitness_colonia.txt"))) {
                writer.println("RESULTADOS DE 20 EJECUCIONES - ALGORITMO COLONIA DE HORMIGAS");
                writer.println("============================================================");
                writer.println("Archivo: datos/pedidos/pedidos_prueba_final.csv");
                writer.println("============================================================");
                writer.println();
                
                List<Double> todosLosFitness = new ArrayList<>();
                
                // Ejecutar 20 veces
                for (int ejecucion = 1; ejecucion <= 20; ejecucion++) {
                    System.out.printf("Ejecución %d/20...%n", ejecucion);
                    
                    // Usar diferentes semillas para cada ejecución
                    long semilla = 1111L + ejecucion;
                    
                    // Ejecutar algoritmo de colonia de hormigas con rutas reales
                    IndividuoIntegrado individuo = new IndividuoIntegrado(pedidos, planificador);
                    individuo.inicializarConPlanificacion();
                    List<RutaCompleta> rutas = individuo.getRutasCompletas();
                    
                    double fitness = individuo.getFitness();
                    todosLosFitness.add(fitness);
                    
                    // Guardar resultado en archivo y mostrar en consola
                    String resultado = String.format("Ejecucion %02d: Fitness=%.2f, Semilla=%d", 
                                                   ejecucion, fitness, semilla);
                    writer.println(resultado);
                    System.out.println(resultado);
                    writer.flush(); // Asegurar que se escriba inmediatamente
                    
                    // Guardar rutas detalladas para esta ejecución
                    guardarRutasDetalladas(rutas, pedidos, individuo.getAsignacionSedes(), ejecucion, fitness, semilla);
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
                System.out.println("Fitness guardados en: resultados_fitness_colonia.txt");
                System.out.println("Rutas detalladas guardadas en: resultados_rutas_colonia_detalladas.txt");
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
        try (PrintWriter writerRutas = new PrintWriter(new FileWriter("resultados_rutas_colonia_detalladas.txt", true))) {
            
            // Solo escribir cabecera en la primera ejecución
            if (numEjecucion == 1) {
                writerRutas.println("ALGORITMO COLONIA DE HORMIGAS - RUTAS DETALLADAS");
                writerRutas.println("=================================================");
                writerRutas.println("Archivo: datos/pedidos/pedidos_prueba_final.csv");
                writerRutas.println("Fecha: " + new java.util.Date());
                writerRutas.println("=================================================");
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
     * Obtiene la sede asignada basada en el índice (simulado)
     */
    private static String obtenerSedeAsignada(int indiceSedeAsignada) {
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        return sedes[indiceSedeAsignada];
    }
}
