package morapack.datos;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import morapack.modelo.Pedido;

/**
 * Cargador de pedidos desde archivo de texto
 * Lee pedidos en formato: dd-hh-mm-dest-###-IdClien
 */
public class CargadorPedidos {
    
    /**
     * Carga pedidos desde archivo de texto
     */
    public static List<Pedido> cargarPedidosDesdeArchivo(String rutaArchivo) {
        List<Pedido> pedidos = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int numeroLinea = 0;
            
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                linea = linea.trim();
                
                // Saltar líneas vacías o comentarios
                if (linea.isEmpty() || linea.startsWith("#") || linea.startsWith("//")) {
                    continue;
                }
                
                try {
                    // Crear pedido usando el constructor que acepta ID
                    Pedido pedido = new Pedido(linea);
                    pedidos.add(pedido);
                    
                    if (pedidos.size() % 50 == 0) {
                        System.out.println("   📦 Pedidos cargados: " + pedidos.size());
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ Error procesando línea " + numeroLinea + 
                                     " '" + linea + "': " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error leyendo archivo: " + e.getMessage());
        }
        
        return pedidos;
    }
    
    /**
     * Muestra estadísticas de los pedidos cargados
     */
    public static void mostrarEstadisticasPedidos(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            System.out.println("📊 No hay pedidos para mostrar estadísticas");
            return;
        }
        
        System.out.println("\n📊 ESTADÍSTICAS DE PEDIDOS CARGADOS:");
        System.out.println("   Total pedidos: " + pedidos.size());
        
        // Contar pedidos por destino
        System.out.println("   📍 Destinos más frecuentes:");
        pedidos.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Pedido::getAeropuertoDestinoId,
                java.util.stream.Collectors.counting()
            ))
            .entrySet()
            .stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
            .limit(5)
            .forEach(entry -> 
                System.out.println("     " + entry.getKey() + ": " + entry.getValue() + " pedidos"));
        
        // Rango de horas
        int minHora = pedidos.stream().mapToInt(Pedido::getHora).min().orElse(0);
        int maxHora = pedidos.stream().mapToInt(Pedido::getHora).max().orElse(0);
        System.out.println("   ⏰ Rango horario: " + 
                          String.format("%02d:00", minHora) + " - " + 
                          String.format("%02d:00", maxHora));
        
        // Rango de cantidades
        int minCant = pedidos.stream().mapToInt(Pedido::getCantidadProductos).min().orElse(0);
        int maxCant = pedidos.stream().mapToInt(Pedido::getCantidadProductos).max().orElse(0);
        double promCant = pedidos.stream().mapToInt(Pedido::getCantidadProductos).average().orElse(0);
        System.out.println("   📦 Cantidades: Min=" + minCant + 
                          ", Max=" + maxCant + 
                          ", Promedio=" + String.format("%.1f", promCant));
    }
}
