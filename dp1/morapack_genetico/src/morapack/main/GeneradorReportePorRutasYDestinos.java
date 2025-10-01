package morapack.main;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Generador de reportes organizados por rutas y destinos basado en REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt
 */
public class GeneradorReportePorRutasYDestinos {
    
    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("        GENERADOR DE REPORTES POR RUTAS Y DESTINOS");
        System.out.println("=================================================================");
        
        try {
            // Procesar el reporte completo existente
            procesarReporteCompleto();
            
            System.out.println("✅ Reportes generados exitosamente:");
            System.out.println("   - REPORTE_POR_DESTINOS.txt");
            System.out.println("   - REPORTE_POR_RUTAS.txt");
            System.out.println("   - REPORTE_FALLOS_DETALLADO.txt");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Procesa el reporte completo y genera reportes especializados
     */
    private static void procesarReporteCompleto() throws IOException {
        String archivoReporte = "REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt";
        
        // Estructuras para organizar los datos
        Map<String, List<PedidoInfo>> pedidosPorDestino = new HashMap<>();
        Map<String, List<PedidoInfo>> pedidosPorRuta = new HashMap<>();
        List<PedidoInfo> pedidosFallidos = new ArrayList<>();
        
        // Leer y procesar el archivo
        BufferedReader reader = new BufferedReader(new FileReader(archivoReporte));
        String linea;
        boolean enSeccionDatos = false;
        
        while ((linea = reader.readLine()) != null) {
            linea = linea.trim();
            
            // Detectar inicio de sección de datos
            if (linea.contains("DETALLE DE TODOS LOS PEDIDOS")) {
                enSeccionDatos = true;
                continue;
            }
            
            // Detectar fin de sección de datos
            if (linea.contains("FIN DEL REPORTE")) {
                break;
            }
            
            // Procesar líneas de datos
            if (enSeccionDatos && linea.length() > 50 && !linea.startsWith("=") && 
                !linea.startsWith("-") && !linea.startsWith("PEDIDO")) {
                
                PedidoInfo pedido = parsearLineaPedido(linea);
                if (pedido != null) {
                    
                    // Organizar por destino
                    pedidosPorDestino.computeIfAbsent(pedido.destino, k -> new ArrayList<>()).add(pedido);
                    
                    // Organizar por ruta (solo si no es fallo)
                    if (!pedido.esFallo()) {
                        String rutaKey = pedido.tipo + " - " + pedido.ruta;
                        pedidosPorRuta.computeIfAbsent(rutaKey, k -> new ArrayList<>()).add(pedido);
                    } else {
                        pedidosFallidos.add(pedido);
                    }
                }
            }
        }
        reader.close();
        
        // Generar reportes especializados
        generarReportePorDestinos(pedidosPorDestino, pedidosFallidos);
        generarReportePorRutas(pedidosPorRuta);
        generarReporteFallosDetallado(pedidosFallidos);
    }
    
    /**
     * Parsea una línea de pedido del reporte
     */
    private static PedidoInfo parsearLineaPedido(String linea) {
        try {
            // Formato esperado: ID DESTINO PRODUCTOS TIPO RUTA VUELO
            String[] partes = linea.split("\\s+");
            
            if (partes.length < 6) return null;
            
            PedidoInfo pedido = new PedidoInfo();
            pedido.id = partes[0];
            pedido.destino = partes[1];
            pedido.productos = Integer.parseInt(partes[2]);
            pedido.tipo = partes[3];
            
            // Ruta puede tener espacios
            StringBuilder rutaBuilder = new StringBuilder();
            StringBuilder vueloBuilder = new StringBuilder();
            
            boolean enVuelo = false;
            for (int i = 4; i < partes.length; i++) {
                if (partes[i].contains("-") && partes[i].length() > 5) {
                    enVuelo = true;
                }
                
                if (enVuelo) {
                    if (vueloBuilder.length() > 0) vueloBuilder.append(" ");
                    vueloBuilder.append(partes[i]);
                } else {
                    if (rutaBuilder.length() > 0) rutaBuilder.append(" ");
                    rutaBuilder.append(partes[i]);
                }
            }
            
            pedido.ruta = rutaBuilder.toString().trim();
            pedido.vuelo = vueloBuilder.toString().trim();
            
            return pedido;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Genera reporte organizado por destinos
     */
    private static void generarReportePorDestinos(Map<String, List<PedidoInfo>> pedidosPorDestino, 
                                                 List<PedidoInfo> pedidosFallidos) throws IOException {
        
        PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_POR_DESTINOS.txt"));
        
        writer.println("=========================================================================");
        writer.println("                    REPORTE ORGANIZADO POR DESTINOS");
        writer.println("=========================================================================");
        writer.println("Generado: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println("=========================================================================");
        
        // Ordenar destinos alfabéticamente
        List<String> destinosOrdenados = new ArrayList<>(pedidosPorDestino.keySet());
        Collections.sort(destinosOrdenados);
        
        for (String destino : destinosOrdenados) {
            List<PedidoInfo> pedidos = pedidosPorDestino.get(destino);
            
            writer.println();
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.printf("DESTINO: %s (%d pedidos)\\n", destino, pedidos.size());
            writer.println("═══════════════════════════════════════════════════════════════");
            
            // Estadísticas del destino
            int exitosos = 0, directos = 0, conEscalas = 0, fallidos = 0;
            int totalPaquetes = 0;
            
            for (PedidoInfo pedido : pedidos) {
                totalPaquetes += pedido.productos;
                if (pedido.esFallo()) {
                    fallidos++;
                } else {
                    exitosos++;
                    if (pedido.tipo.equals("DIRECTA")) {
                        directos++;
                    } else {
                        conEscalas++;
                    }
                }
            }
            
            writer.printf("📊 ESTADÍSTICAS DEL DESTINO:\\n");
            writer.printf("   Total pedidos: %d\\n", pedidos.size());
            writer.printf("   Exitosos: %d (%.1f%%)\\n", exitosos, (exitosos * 100.0 / pedidos.size()));
            writer.printf("   - Rutas directas: %d\\n", directos);
            writer.printf("   - Rutas con escalas: %d\\n", conEscalas);
            writer.printf("   Fallidos: %d (%.1f%%)\\n", fallidos, (fallidos * 100.0 / pedidos.size()));
            writer.printf("   Total paquetes: %d\\n", totalPaquetes);
            
            writer.println();
            writer.println("📋 DETALLE DE PEDIDOS:");
            writer.println("ID                        PRODUCTOS  TIPO         RUTA                     VUELO");
            writer.println("─".repeat(95));
            
            // Ordenar pedidos por productos (descendente)
            pedidos.sort((a, b) -> Integer.compare(b.productos, a.productos));
            
            for (PedidoInfo pedido : pedidos) {
                writer.printf("%-25s %-10d %-12s %-24s %s\\n",
                            pedido.id, pedido.productos, pedido.tipo,
                            truncar(pedido.ruta, 24), truncar(pedido.vuelo, 20));
            }
        }
        
        // Sección de pedidos fallidos
        if (!pedidosFallidos.isEmpty()) {
            writer.println();
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.printf("PEDIDOS FALLIDOS (%d pedidos)\\n", pedidosFallidos.size());
            writer.println("═══════════════════════════════════════════════════════════════");
            
            Map<String, Integer> fallosPorDestino = new HashMap<>();
            for (PedidoInfo pedido : pedidosFallidos) {
                fallosPorDestino.put(pedido.destino, 
                    fallosPorDestino.getOrDefault(pedido.destino, 0) + 1);
            }
            
            writer.println("📊 DISTRIBUCIÓN DE FALLOS POR DESTINO:");
            for (Map.Entry<String, Integer> entry : fallosPorDestino.entrySet()) {
                writer.printf("   %s: %d fallos\\n", entry.getKey(), entry.getValue());
            }
        }
        
        writer.println();
        writer.println("=========================================================================");
        writer.println("                          FIN DEL REPORTE POR DESTINOS");
        writer.println("=========================================================================");
        
        writer.close();
    }
    
    /**
     * Genera reporte organizado por rutas
     */
    private static void generarReportePorRutas(Map<String, List<PedidoInfo>> pedidosPorRuta) throws IOException {
        
        PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_POR_RUTAS.txt"));
        
        writer.println("=========================================================================");
        writer.println("                      REPORTE ORGANIZADO POR RUTAS");
        writer.println("=========================================================================");
        writer.println("Generado: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println("=========================================================================");
        
        // Separar rutas directas y con escalas
        Map<String, List<PedidoInfo>> rutasDirectas = new HashMap<>();
        Map<String, List<PedidoInfo>> rutasConEscalas = new HashMap<>();
        
        for (Map.Entry<String, List<PedidoInfo>> entry : pedidosPorRuta.entrySet()) {
            if (entry.getKey().startsWith("DIRECTA")) {
                rutasDirectas.put(entry.getKey(), entry.getValue());
            } else {
                rutasConEscalas.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Procesar rutas directas
        procesarSeccionRutas(writer, rutasDirectas, "RUTAS DIRECTAS");
        
        // Procesar rutas con escalas
        procesarSeccionRutas(writer, rutasConEscalas, "RUTAS CON ESCALAS");
        
        writer.println();
        writer.println("=========================================================================");
        writer.println("                          FIN DEL REPORTE POR RUTAS");
        writer.println("=========================================================================");
        
        writer.close();
    }
    
    /**
     * Procesa una sección de rutas (directas o con escalas)
     */
    private static void procesarSeccionRutas(PrintWriter writer, Map<String, List<PedidoInfo>> rutas, String titulo) {
        
        writer.println();
        writer.println("═══════════════════════════════════════════════════════════════");
        writer.printf("%s (%d rutas diferentes)\\n", titulo, rutas.size());
        writer.println("═══════════════════════════════════════════════════════════════");
        
        // Ordenar rutas por número de pedidos (descendente)
        List<Map.Entry<String, List<PedidoInfo>>> rutasOrdenadas = new ArrayList<>(rutas.entrySet());
        rutasOrdenadas.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
        
        for (Map.Entry<String, List<PedidoInfo>> entry : rutasOrdenadas) {
            String rutaKey = entry.getKey();
            List<PedidoInfo> pedidos = entry.getValue();
            
            writer.println();
            writer.println("─".repeat(70));
            writer.printf("RUTA: %s\\n", rutaKey.substring(rutaKey.indexOf("-") + 3)); // Quitar el prefijo DIRECTA/CON ESCALAS
            writer.printf("Pedidos: %d | Total paquetes: %d\\n", 
                         pedidos.size(), pedidos.stream().mapToInt(p -> p.productos).sum());
            writer.println("─".repeat(70));
            
            // Mostrar estadísticas de la ruta
            Map<String, Integer> destinosEnRuta = new HashMap<>();
            for (PedidoInfo pedido : pedidos) {
                destinosEnRuta.put(pedido.destino, 
                    destinosEnRuta.getOrDefault(pedido.destino, 0) + 1);
            }
            
            writer.printf("Destinos cubiertos: %d | ", destinosEnRuta.size());
            writer.printf("Promedio paquetes/pedido: %.1f\\n", 
                         pedidos.stream().mapToInt(p -> p.productos).average().orElse(0.0));
            
            // Mostrar algunos pedidos ejemplo (máximo 10)
            writer.println("\\nEjemplos de pedidos en esta ruta:");
            writer.println("ID                        DESTINO  PRODUCTOS  VUELO");
            writer.println("─".repeat(60));
            
            List<PedidoInfo> ejemplos = pedidos.subList(0, Math.min(10, pedidos.size()));
            for (PedidoInfo pedido : ejemplos) {
                writer.printf("%-25s %-8s %-10d %s\\n",
                            pedido.id, pedido.destino, pedido.productos, 
                            truncar(pedido.vuelo, 20));
            }
            
            if (pedidos.size() > 10) {
                writer.printf("... y %d pedidos más\\n", pedidos.size() - 10);
            }
        }
    }
    
    /**
     * Genera reporte detallado de fallos
     */
    private static void generarReporteFallosDetallado(List<PedidoInfo> pedidosFallidos) throws IOException {
        
        PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_FALLOS_DETALLADO.txt"));
        
        writer.println("=========================================================================");
        writer.println("                    ANÁLISIS DETALLADO DE FALLOS");
        writer.println("=========================================================================");
        writer.println("Generado: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println("=========================================================================");
        
        writer.printf("Total de pedidos fallidos: %d\\n\\n", pedidosFallidos.size());
        
        // Análisis por destino
        Map<String, List<PedidoInfo>> fallosPorDestino = new HashMap<>();
        for (PedidoInfo pedido : pedidosFallidos) {
            fallosPorDestino.computeIfAbsent(pedido.destino, k -> new ArrayList<>()).add(pedido);
        }
        
        writer.println("📊 ANÁLISIS POR DESTINO:");
        writer.println("─".repeat(50));
        
        for (Map.Entry<String, List<PedidoInfo>> entry : fallosPorDestino.entrySet()) {
            String destino = entry.getKey();
            List<PedidoInfo> fallos = entry.getValue();
            
            writer.printf("\\n🔴 DESTINO: %s (%d fallos)\\n", destino, fallos.size());
            
            int totalPaquetes = fallos.stream().mapToInt(p -> p.productos).sum();
            double promedioPaquetes = fallos.stream().mapToInt(p -> p.productos).average().orElse(0.0);
            
            writer.printf("   Total paquetes perdidos: %d\\n", totalPaquetes);
            writer.printf("   Promedio paquetes por fallo: %.1f\\n", promedioPaquetes);
            
            // Determinar causa probable
            String causaProbable = determinarCausaProbable(destino);
            writer.printf("   Causa probable: %s\\n", causaProbable);
            
            writer.println("   Pedidos afectados:");
            for (PedidoInfo pedido : fallos) {
                writer.printf("     - %s (%d paquetes)\\n", pedido.id, pedido.productos);
            }
        }
        
        // Recomendaciones
        writer.println("\\n\\n📋 RECOMENDACIONES PARA RESOLVER FALLOS:");
        writer.println("─".repeat(50));
        writer.println("\\n1. DESTINOS SIN CONECTIVIDAD:");
        writer.println("   - OOMS, OERK, SLLP: Implementar rutas con escalas");
        writer.println("   - Agregar vuelos directos desde sedes principales");
        writer.println("   - Considerar hubs regionales para estos destinos");
        
        writer.println("\\n2. FALLOS OPERATIVOS ALEATORIOS:");
        writer.println("   - Revisar el 5% de fallos por restricciones operativas");
        writer.println("   - Implementar vuelos de respaldo para destinos críticos");
        writer.println("   - Mejorar la planificación de capacidades");
        
        writer.println("\\n3. OPTIMIZACIONES SUGERIDAS:");
        writer.println("   - Expandir la red de vuelos a destinos problemáticos");
        writer.println("   - Implementar algoritmo de rutas alternativas");
        writer.println("   - Considerar consolidación de pedidos pequeños");
        
        writer.println();
        writer.println("=========================================================================");
        writer.println("                        FIN DEL ANÁLISIS DE FALLOS");
        writer.println("=========================================================================");
        
        writer.close();
    }
    
    /**
     * Determina la causa probable de fallo según el destino
     */
    private static String determinarCausaProbable(String destino) {
        // Basado en el análisis previo
        switch (destino) {
            case "OOMS":
            case "OERK":
            case "SLLP":
                return "Sin conectividad aérea directa";
            case "EHAM":
            case "OMDB":
            case "SVMI":
                return "Restricciones operativas aleatorias (5% de fallos)";
            default:
                return "Capacidad insuficiente o restricciones operativas";
        }
    }
    
    /**
     * Trunca una cadena a la longitud especificada
     */
    private static String truncar(String texto, int longitud) {
        if (texto == null) return "N/A";
        if (texto.length() <= longitud) return texto;
        return texto.substring(0, longitud - 3) + "...";
    }
    
    /**
     * Clase para almacenar información de pedidos
     */
    private static class PedidoInfo {
        String id;
        String destino;
        int productos;
        String tipo;
        String ruta;
        String vuelo;
        
        boolean esFallo() {
            return tipo.equals("FALLO");
        }
    }
}
