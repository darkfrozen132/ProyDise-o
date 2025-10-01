package morapack.main;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generador de Reportes Específicos por Rutas y Destinos
 * Basado en REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt
 */
public class GeneradorReportesEspecificos {
    
    private static class PedidoRuta {
        String pedidoId;
        String destino;
        int productos;
        String tipoRuta;
        String rutaCompleta;
        String vuelo;
        
        PedidoRuta(String linea) {
            // Parsear línea del reporte: PEDIDO DESTINO PRODUCTOS TIPO RUTA VUELO
            String[] partes = linea.trim().split("\\s+");
            if (partes.length >= 6) {
                this.pedidoId = partes[0];
                this.destino = partes[1];
                this.productos = Integer.parseInt(partes[2]);
                this.tipoRuta = partes[3];
                // Reconstruir ruta completa (puede tener espacios)
                StringBuilder rutaBuilder = new StringBuilder();
                StringBuilder vueloBuilder = new StringBuilder();
                boolean enRuta = true;
                for (int i = 4; i < partes.length; i++) {
                    if (partes[i].contains("-") && !partes[i].contains("→") && enRuta) {
                        enRuta = false;
                        vueloBuilder.append(partes[i]);
                    } else if (enRuta) {
                        if (rutaBuilder.length() > 0) rutaBuilder.append(" ");
                        rutaBuilder.append(partes[i]);
                    } else {
                        if (vueloBuilder.length() > 0) vueloBuilder.append(" ");
                        vueloBuilder.append(partes[i]);
                    }
                }
                this.rutaCompleta = rutaBuilder.toString();
                this.vuelo = vueloBuilder.toString();
            }
        }
    }
    
    public static void main(String[] args) {
        
        System.out.println("========== GENERADOR DE REPORTES ESPECÍFICOS ==========");
        System.out.println("Procesando REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt");
        System.out.println("=======================================================");
        
        try {
            // Cargar datos del reporte completo
            List<PedidoRuta> pedidos = cargarDatosDelReporte();
            
            if (pedidos.isEmpty()) {
                System.out.println("❌ No se encontraron datos en REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt");
                return;
            }
            
            System.out.printf("✅ Cargados %d pedidos del reporte\\n\\n", pedidos.size());
            
            // Generar reportes específicos
            generarReportePorDestinos(pedidos);
            generarReportePorRutas(pedidos);
            generarReportePorTipoRuta(pedidos);
            generarReporteEstadisticasSedes(pedidos);
            
            System.out.println("\\n✅ Todos los reportes específicos generados:");
            System.out.println("   📊 REPORTE_POR_DESTINOS.txt");
            System.out.println("   🛫 REPORTE_POR_RUTAS.txt");
            System.out.println("   📈 REPORTE_POR_TIPO_RUTA.txt");
            System.out.println("   🏢 REPORTE_ESTADISTICAS_SEDES.txt");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static List<PedidoRuta> cargarDatosDelReporte() throws IOException {
        List<PedidoRuta> pedidos = new ArrayList<>();
        
        File archivo = new File("REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt");
        if (!archivo.exists()) {
            throw new FileNotFoundException("Archivo REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt no encontrado");
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean enSeccionDatos = false;
            
            while ((linea = reader.readLine()) != null) {
                if (linea.contains("PEDIDO") && linea.contains("DESTINO") && linea.contains("PRODUCTOS")) {
                    enSeccionDatos = true;
                    continue;
                }
                
                if (enSeccionDatos && linea.trim().length() > 0 && 
                    !linea.contains("----") && !linea.contains("====")) {
                    
                    try {
                        PedidoRuta pedido = new PedidoRuta(linea);
                        if (pedido.pedidoId != null && !pedido.pedidoId.isEmpty()) {
                            pedidos.add(pedido);
                        }
                    } catch (Exception e) {
                        // Ignorar líneas que no se pueden parsear
                    }
                }
            }
        }
        
        return pedidos;
    }
    
    private static void generarReportePorDestinos(List<PedidoRuta> pedidos) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_POR_DESTINOS.txt"))) {
            
            writer.println("===============================================================");
            writer.println("              REPORTE DETALLADO POR DESTINOS");
            writer.println("===============================================================");
            writer.println("Fecha: " + new Date());
            writer.println("===============================================================");
            writer.println();
            
            // Agrupar por destino y ordenar por cantidad de pedidos
            Map<String, List<PedidoRuta>> porDestino = pedidos.stream()
                .collect(Collectors.groupingBy(p -> p.destino));
            
            List<Map.Entry<String, List<PedidoRuta>>> destinosOrdenados = 
                porDestino.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .collect(Collectors.toList());
            
            writer.println("RESUMEN POR DESTINOS:");
            writer.println("---------------------");
            
            for (Map.Entry<String, List<PedidoRuta>> entry : destinosOrdenados) {
                String destino = entry.getKey();
                List<PedidoRuta> pedidosDestino = entry.getValue();
                
                int totalProductos = pedidosDestino.stream().mapToInt(p -> p.productos).sum();
                long directas = pedidosDestino.stream().filter(p -> "DIRECTA".equals(p.tipoRuta)).count();
                long escalas = pedidosDestino.stream().filter(p -> "CON ESCALAS".equals(p.tipoRuta)).count();
                
                writer.printf("%-8s: %3d pedidos | %,6d productos | %2d directas | %2d escalas%n",
                    destino, pedidosDestino.size(), totalProductos, directas, escalas);
            }
            
            writer.println();
            writer.println("===============================================================");
            writer.println("                    DETALLE POR DESTINO");
            writer.println("===============================================================");
            writer.println();
            
            for (Map.Entry<String, List<PedidoRuta>> entry : destinosOrdenados) {
                String destino = entry.getKey();
                List<PedidoRuta> pedidosDestino = entry.getValue();
                
                writer.printf("DESTINO: %s (%d pedidos)%n", destino, pedidosDestino.size());
                writer.println("=" + "=".repeat(destino.length() + 15));
                
                for (PedidoRuta pedido : pedidosDestino) {
                    writer.printf("%-25s | %-12s | %,6d prod | %-35s%n",
                        pedido.pedidoId, pedido.tipoRuta, pedido.productos, pedido.rutaCompleta);
                }
                writer.println();
            }
        }
    }
    
    private static void generarReportePorRutas(List<PedidoRuta> pedidos) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_POR_RUTAS.txt"))) {
            
            writer.println("===============================================================");
            writer.println("              REPORTE DETALLADO POR RUTAS");
            writer.println("===============================================================");
            writer.println("Fecha: " + new Date());
            writer.println("===============================================================");
            writer.println();
            
            // Agrupar por ruta completa
            Map<String, List<PedidoRuta>> porRuta = pedidos.stream()
                .collect(Collectors.groupingBy(p -> p.rutaCompleta));
            
            List<Map.Entry<String, List<PedidoRuta>>> rutasOrdenadas = 
                porRuta.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .collect(Collectors.toList());
            
            writer.println("RESUMEN POR RUTAS:");
            writer.println("------------------");
            
            for (Map.Entry<String, List<PedidoRuta>> entry : rutasOrdenadas) {
                String ruta = entry.getKey();
                List<PedidoRuta> pedidosRuta = entry.getValue();
                
                int totalProductos = pedidosRuta.stream().mapToInt(p -> p.productos).sum();
                
                writer.printf("%-40s: %3d pedidos | %,6d productos%n",
                    ruta, pedidosRuta.size(), totalProductos);
            }
            
            writer.println();
            writer.println("===============================================================");
            writer.println("                    DETALLE POR RUTA");
            writer.println("===============================================================");
            writer.println();
            
            for (Map.Entry<String, List<PedidoRuta>> entry : rutasOrdenadas) {
                String ruta = entry.getKey();
                List<PedidoRuta> pedidosRuta = entry.getValue();
                
                writer.printf("RUTA: %s (%d pedidos)%n", ruta, pedidosRuta.size());
                writer.println("=" + "=".repeat(Math.min(ruta.length() + 15, 60)));
                
                for (PedidoRuta pedido : pedidosRuta) {
                    writer.printf("%-25s | %-8s | %,6d productos%n",
                        pedido.pedidoId, pedido.destino, pedido.productos);
                }
                writer.println();
            }
        }
    }
    
    private static void generarReportePorTipoRuta(List<PedidoRuta> pedidos) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_POR_TIPO_RUTA.txt"))) {
            
            writer.println("===============================================================");
            writer.println("            REPORTE POR TIPO DE RUTA (DIRECTA/ESCALAS)");
            writer.println("===============================================================");
            writer.println("Fecha: " + new Date());
            writer.println("===============================================================");
            writer.println();
            
            Map<String, List<PedidoRuta>> porTipo = pedidos.stream()
                .collect(Collectors.groupingBy(p -> p.tipoRuta));
            
            for (Map.Entry<String, List<PedidoRuta>> entry : porTipo.entrySet()) {
                String tipo = entry.getKey();
                List<PedidoRuta> pedidosTipo = entry.getValue();
                
                int totalProductos = pedidosTipo.stream().mapToInt(p -> p.productos).sum();
                
                writer.printf("%s:%n", tipo);
                writer.println("=" + "=".repeat(tipo.length() + 1));
                writer.printf("Total pedidos: %d%n", pedidosTipo.size());
                writer.printf("Total productos: %,d%n", totalProductos);
                writer.printf("Porcentaje: %.1f%%%n", (pedidosTipo.size() * 100.0 / pedidos.size()));
                writer.println();
                
                // Mostrar algunos ejemplos
                writer.println("Ejemplos:");
                pedidosTipo.stream().limit(10).forEach(p -> {
                    writer.printf("  %-25s | %-8s | %,6d prod | %s%n",
                        p.pedidoId, p.destino, p.productos, p.rutaCompleta);
                });
                
                if (pedidosTipo.size() > 10) {
                    writer.printf("  ... y %d más%n", pedidosTipo.size() - 10);
                }
                writer.println();
            }
        }
    }
    
    private static void generarReporteEstadisticasSedes(List<PedidoRuta> pedidos) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_ESTADISTICAS_SEDES.txt"))) {
            
            writer.println("===============================================================");
            writer.println("              ESTADÍSTICAS POR SEDE DE ORIGEN");
            writer.println("===============================================================");
            writer.println("Fecha: " + new Date());
            writer.println("===============================================================");
            writer.println();
            
            // Extraer sede de origen de la ruta
            Map<String, List<PedidoRuta>> porSede = new HashMap<>();
            
            for (PedidoRuta pedido : pedidos) {
                String sede = extraerSedeOrigen(pedido.rutaCompleta);
                porSede.computeIfAbsent(sede, k -> new ArrayList<>()).add(pedido);
            }
            
            for (Map.Entry<String, List<PedidoRuta>> entry : porSede.entrySet()) {
                String sede = entry.getKey();
                List<PedidoRuta> pedidosSede = entry.getValue();
                
                writer.printf("SEDE: %s%n", sede);
                writer.println("=" + "=".repeat(sede.length() + 6));
                
                int totalProductos = pedidosSede.stream().mapToInt(p -> p.productos).sum();
                writer.printf("Total pedidos: %d%n", pedidosSede.size());
                writer.printf("Total productos: %,d%n", totalProductos);
                
                // Destinos únicos desde esta sede
                Set<String> destinosUnicos = pedidosSede.stream()
                    .map(p -> p.destino)
                    .collect(Collectors.toSet());
                writer.printf("Destinos únicos: %d%n", destinosUnicos.size());
                writer.printf("Destinos: %s%n", String.join(", ", destinosUnicos));
                
                writer.println();
            }
        }
    }
    
    private static String extraerSedeOrigen(String rutaCompleta) {
        if (rutaCompleta.contains("→")) {
            return rutaCompleta.split("→")[0].trim();
        }
        return "DESCONOCIDO";
    }
}
