package morapack.main;

import morapack.datos.CargadorDatosCSV;
import morapack.datos.CargadorPedidos;
import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.planificacion.PlanificadorTemporalMejorado;
import morapack.planificacion.RutaCompleta;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Generador de archivo de resultados en formato específico
 * Procesa pedidos reales y genera output formateado
 */
public class GeneradorResultadosFormateados {
    
    public static void main(String[] args) {
        System.out.println("🚀 GENERANDO ARCHIVO DE RESULTADOS FORMATEADOS");
        System.out.println("==============================================");
        
        try {
            // PASO 1: Cargar datos
            System.out.println("\n📂 Cargando datos...");
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            System.out.println("   ✅ Vuelos cargados: " + vuelos.size());
            
            // PASO 2: Cargar pedidos reales
            String rutaPedidos = "../morapack_coloniav2/datos/pedidoUltrafinal.txt";
            List<Pedido> pedidos = CargadorPedidos.cargarPedidosDesdeArchivo(rutaPedidos);
            System.out.println("   ✅ Pedidos cargados: " + pedidos.size());
            
            // PASO 3: Inicializar planificador
            PlanificadorTemporalMejorado planificador = new PlanificadorTemporalMejorado(vuelos);
            
            // PASO 4: Procesar y generar archivo
            System.out.println("\n📋 Procesando pedidos y generando archivo...");
            generarArchivoResultados(planificador, pedidos);
            
            System.out.println("\n✅ ARCHIVO GENERADO: resultados_pedidos_reales_formateado.txt");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Genera el archivo de resultados en el formato solicitado
     */
    private static void generarArchivoResultados(PlanificadorTemporalMejorado planificador, List<Pedido> pedidos) {
        try (FileWriter writer = new FileWriter("resultados_pedidos_reales_formateado.txt")) {
            
            int exitosas = 0;
            int directas = 0;
            int conEscalas = 0;
            int fallidas = 0;
            double fitnessTotal = 0.0;
            
            // Encabezado
            writer.write("EJECUCIÓN 1 CON PEDIDOS REALES (Fitness: calculado, Temporal: ACTIVADO):\n");
            writer.write("----------------------------------------------------------------\n");
            
            // Procesar cada pedido
            for (Pedido pedido : pedidos) {
                try {
                    // Determinar sede óptima
                    String sedeOptima = determinarSedeOptima(pedido.getAeropuertoDestinoId());
                    pedido.setSedeAsignadaId(sedeOptima);
                    
                    // Planificar ruta
                    RutaCompleta ruta = planificador.planificarRutaTemporal(pedido, sedeOptima);
                    
                    String lineaResultado;
                    if (ruta != null && ruta.esViable()) {
                        exitosas++;
                        
                        if (ruta.getVuelos().size() == 1) {
                            directas++;
                            lineaResultado = String.format("  Pedido %s: %s → %s [DIRECTO]", 
                                pedido.getId(), sedeOptima, pedido.getAeropuertoDestinoId());
                        } else {
                            conEscalas++;
                            lineaResultado = String.format("  Pedido %s: %s → %s [%d ESCALAS]", 
                                pedido.getId(), sedeOptima, pedido.getAeropuertoDestinoId(), 
                                ruta.getVuelos().size() - 1);
                        }
                        
                        writer.write(lineaResultado + "\n");
                        
                        // Escribir detalles de escalas si las hay
                        if (ruta.getVuelos().size() > 1) {
                            for (int i = 0; i < ruta.getVuelos().size(); i++) {
                                Vuelo vuelo = ruta.getVuelos().get(i);
                                writer.write(String.format("    %d. %s → %s (%s-%s)\n", 
                                    i + 1, 
                                    vuelo.getOrigen(), 
                                    vuelo.getDestino(),
                                    vuelo.getHoraSalida(), 
                                    vuelo.getHoraLlegada()));
                            }
                        }
                        
                        // Calcular fitness simplificado (distancia + tiempo + costo)
                        fitnessTotal += calcularFitnessRuta(ruta, pedido.getCantidadProductos());
                        
                    } else {
                        fallidas++;
                        lineaResultado = String.format("  Pedido %s: SIN RUTA", pedido.getId());
                        writer.write(lineaResultado + "\n");
                        
                        // Penalización por ruta fallida
                        fitnessTotal += 10000; 
                    }
                    
                } catch (Exception e) {
                    fallidas++;
                    writer.write(String.format("  Pedido %s: ERROR - %s\n", pedido.getId(), e.getMessage()));
                }
            }
            
            // Resumen final
            writer.write(String.format("\nResumen: %d rutas exitosas (%d directas, %d con escalas)\n", 
                exitosas, directas, conEscalas));
            
            writer.write(String.format("Fitness Total: %.2f\n", fitnessTotal));
            writer.write(String.format("Tasa de éxito: %.1f%% (%d/%d)\n", 
                (exitosas * 100.0) / pedidos.size(), exitosas, pedidos.size()));
            
            writer.write(String.format("Rutas fallidas: %d\n", fallidas));
            
            // Estadísticas adicionales
            writer.write("\n=== ESTADÍSTICAS DETALLADAS ===\n");
            writer.write(String.format("Total pedidos procesados: %d\n", pedidos.size()));
            writer.write(String.format("Rutas directas: %d (%.1f%%)\n", 
                directas, exitosas > 0 ? (directas * 100.0) / exitosas : 0));
            writer.write(String.format("Rutas con escalas: %d (%.1f%%)\n", 
                conEscalas, exitosas > 0 ? (conEscalas * 100.0) / exitosas : 0));
            
            int totalPaquetes = pedidos.stream().mapToInt(Pedido::getCantidadProductos).sum();
            writer.write(String.format("Total paquetes: %d\n", totalPaquetes));
            writer.write(String.format("Promedio paquetes/pedido: %.1f\n", totalPaquetes / (double) pedidos.size()));
            
            System.out.printf("📊 Procesados: %d exitosos, %d fallidos (%.1f%% éxito)\n", 
                exitosas, fallidas, (exitosas * 100.0) / pedidos.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error escribiendo archivo: " + e.getMessage());
        }
    }
    
    /**
     * Calcula un fitness simplificado para la ruta
     */
    private static double calcularFitnessRuta(RutaCompleta ruta, int cantidadPaquetes) {
        if (!ruta.esViable()) return Double.MAX_VALUE;
        
        double fitness = 0.0;
        
        // Factor por número de escalas (penalizar escalas)
        int numeroEscalas = ruta.getVuelos().size() - 1;
        fitness += numeroEscalas * 500; // 500 puntos por cada escala
        
        // Factor por tiempo total de viaje
        if (!ruta.getVuelos().isEmpty()) {
            Vuelo primero = ruta.getVuelos().get(0);
            Vuelo ultimo = ruta.getVuelos().get(ruta.getVuelos().size() - 1);
            
            // Calcular tiempo total estimado (simplificado)
            String[] horaSalidaParts = primero.getHoraSalida().split(":");
            String[] horaLlegadaParts = ultimo.getHoraLlegada().split(":");
            
            try {
                int minutosInicio = Integer.parseInt(horaSalidaParts[0]) * 60 + Integer.parseInt(horaSalidaParts[1]);
                int minutosFin = Integer.parseInt(horaLlegadaParts[0]) * 60 + Integer.parseInt(horaLlegadaParts[1]);
                
                int tiempoViaje = minutosFin - minutosInicio;
                if (tiempoViaje < 0) tiempoViaje += 24 * 60; // Cruza medianoche
                
                fitness += tiempoViaje * 0.5; // 0.5 puntos por minuto
            } catch (Exception e) {
                fitness += 1000; // Penalización por error de cálculo
            }
        }
        
        // Factor por cantidad de paquetes (más paquetes = más complejo)
        fitness += cantidadPaquetes * 0.1;
        
        return fitness;
    }
    
    /**
     * Determina la sede óptima basada en el destino (lógica geográfica simplificada)
     */
    private static String determinarSedeOptima(String destino) {
        // Sede SPIM (Lima, Perú) para Sudamérica
        if (destino.startsWith("S") || destino.equals("SKBO") || destino.equals("SGAS") || 
            destino.equals("SBBR") || destino.equals("SEQM") || destino.equals("SUAA") || 
            destino.equals("SABE") || destino.equals("SVMI")) {
            return "SPIM";
        }
        
        // Sede EBCI (Bruselas, Bélgica) para Europa
        if (destino.startsWith("E") || destino.startsWith("L") || destino.equals("LOWW") || 
            destino.equals("EHAM") || destino.equals("LKPR") || destino.equals("EKCH") ||
            destino.equals("EDDI") || destino.equals("LDZA") || destino.equals("LATI")) {
            return "EBCI";
        }
        
        // Sede UBBB (Moscú, Rusia) para Asia, Medio Oriente y África
        if (destino.startsWith("O") || destino.startsWith("U") || destino.startsWith("V") || 
            destino.equals("OMDB") || destino.equals("OERK") || destino.equals("VIDP") || 
            destino.equals("OAKB") || destino.equals("OSDI") || destino.equals("OJAI") ||
            destino.equals("UMMS") || destino.equals("OOMS") || destino.equals("OPKC") ||
            destino.equals("OYSN")) {
            return "UBBB";
        }
        
        // Por defecto, asignar a SPIM
        return "SPIM";
    }
}
