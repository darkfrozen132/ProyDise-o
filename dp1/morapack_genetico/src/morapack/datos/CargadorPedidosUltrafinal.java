package morapack.datos;

import morapack.modelo.Pedido;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cargador de pedidos desde pedidoUltrafinal.txt
 * Formato: DIA-HORA-MINUTO-DESTINO-CANTIDAD-ID
 */
public class CargadorPedidosUltrafinal {
    
    /**
     * Carga pedidos desde el archivo pedidoUltrafinal.txt
     */
    public static List<Pedido> cargarPedidos(String rutaArchivo) {
        return cargarPedidos(rutaArchivo, null);
    }
    
    /**
     * Carga pedidos desde el archivo pedidoUltrafinal.txt filtrando por destinos válidos
     */
    public static List<Pedido> cargarPedidos(String rutaArchivo, java.util.Set<String> destinosValidos) {
        List<Pedido> pedidos = new ArrayList<>();
        List<Pedido> pedidosDescartados = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int contador = 0;
            
            System.out.println("📋 Cargando pedidos desde: " + rutaArchivo);
            
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                
                try {
                    Pedido pedido = parsearPedido(linea);
                    if (pedido != null) {
                        // Filtrar por destinos válidos si se proporciona la lista
                        if (destinosValidos == null || destinosValidos.contains(pedido.getAeropuertoDestinoId())) {
                            pedidos.add(pedido);
                            contador++;
                            
                            // Mostrar progreso cada 50 pedidos
                            if (contador % 50 == 0) {
                                System.out.println("   ✅ Cargados " + contador + " pedidos válidos...");
                            }
                        } else {
                            pedidosDescartados.add(pedido);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("   ⚠️  Error procesando línea: " + linea + " - " + e.getMessage());
                }
            }
            
            System.out.println("✅ Total pedidos cargados: " + pedidos.size());
            
            if (destinosValidos != null && !pedidosDescartados.isEmpty()) {
                System.out.println("⚠️ Pedidos descartados (destino no válido): " + pedidosDescartados.size());
                System.out.println("   Ejemplos de destinos no válidos:");
                pedidosDescartados.stream()
                    .map(Pedido::getAeropuertoDestinoId)
                    .distinct()
                    .limit(10)
                    .forEach(destino -> System.out.println("     ❌ " + destino));
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error leyendo archivo: " + e.getMessage());
        }
        
        return pedidos;
    }
    
    /**
     * Parsea una línea del formato: DIA-HORA-MINUTO-DESTINO-CANTIDAD-ID
     */
    private static Pedido parsearPedido(String linea) {
        String[] partes = linea.split("-");
        
        if (partes.length != 6) {
            throw new IllegalArgumentException("Formato inválido. Esperado: DIA-HORA-MINUTO-DESTINO-CANTIDAD-ID");
        }
        
        try {
            int dia = Integer.parseInt(partes[0]);
            int hora = Integer.parseInt(partes[1]);
            int minuto = Integer.parseInt(partes[2]);
            String destino = partes[3];
            int cantidad = Integer.parseInt(partes[4]);
            String id = partes[5];
            
            // Validaciones básicas
            if (dia < 1 || dia > 31) {
                throw new IllegalArgumentException("Día inválido: " + dia);
            }
            if (hora < 0 || hora > 23) {
                throw new IllegalArgumentException("Hora inválida: " + hora);
            }
            if (minuto < 0 || minuto > 59) {
                throw new IllegalArgumentException("Minuto inválido: " + minuto);
            }
            if (cantidad <= 0) {
                throw new IllegalArgumentException("Cantidad inválida: " + cantidad);
            }
            
            // Crear ID completo en formato esperado por Pedido
            String idCompleto = String.format("%02d-%02d-%02d-%s-%03d-%s", 
                                            dia, hora, minuto, destino, cantidad, id);
            
            // Usar constructor que parsea automáticamente el ID
            Pedido pedido = new Pedido(idCompleto);
            
            return pedido;
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error parseando números: " + e.getMessage());
        }
    }
    
    /**
     * Muestra estadísticas de los pedidos cargados
     */
    public static void mostrarEstadisticas(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            System.out.println("❌ No hay pedidos para analizar");
            return;
        }
        
        System.out.println("\n📊 ============= ESTADÍSTICAS DE PEDIDOS =============");
        System.out.println("📦 Total pedidos: " + pedidos.size());
        
        // Contar destinos únicos
        long destinosUnicos = pedidos.stream()
            .map(Pedido::getAeropuertoDestinoId)
            .distinct()
            .count();
        System.out.println("🛫 Destinos únicos: " + destinosUnicos);
        
        // Cantidad total de productos
        int totalProductos = pedidos.stream()
            .mapToInt(Pedido::getCantidadProductos)
            .sum();
        System.out.println("📦 Total productos: " + totalProductos);
        
        // Promedio de productos por pedido
        double promedioProductos = (double) totalProductos / pedidos.size();
        System.out.printf("📊 Promedio productos/pedido: %.1f\n", promedioProductos);
        
        // Rango de días
        int diaMin = pedidos.stream().mapToInt(Pedido::getDia).min().orElse(0);
        int diaMax = pedidos.stream().mapToInt(Pedido::getDia).max().orElse(0);
        System.out.printf("📅 Rango de días: %d - %d\n", diaMin, diaMax);
        
        // Destinos más frecuentes (top 10)
        System.out.println("\n🎯 TOP 10 DESTINOS MÁS FRECUENTES:");
        pedidos.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Pedido::getAeropuertoDestinoId,
                java.util.stream.Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> 
                System.out.printf("   %s: %d pedidos\n", entry.getKey(), entry.getValue()));
        
        System.out.println("===================================================");
    }
    
    /**
     * Muestra estadísticas sin iconos para una mejor legibilidad
     */
    public static void mostrarEstadisticasSinIconos(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            System.out.println("No hay pedidos para mostrar estadisticas");
            return;
        }
        
        System.out.println("===================================================");
        System.out.println("             ESTADISTICAS DE PEDIDOS              ");
        System.out.println("===================================================");
        
        System.out.printf("Total de pedidos cargados: %d%n", pedidos.size());
        
        // Total de productos
        int totalProductos = pedidos.stream().mapToInt(Pedido::getCantidadProductos).sum();
        System.out.printf("Total productos: %d%n", totalProductos);
        
        // Promedio de productos por pedido
        double promedioProductos = (double) totalProductos / pedidos.size();
        System.out.printf("Promedio productos/pedido: %.1f%n", promedioProductos);
        
        // Rango de días
        int diaMin = pedidos.stream().mapToInt(Pedido::getDia).min().orElse(0);
        int diaMax = pedidos.stream().mapToInt(Pedido::getDia).max().orElse(0);
        System.out.printf("Rango de dias: %d - %d%n", diaMin, diaMax);
        
        // Destinos más frecuentes (top 5)
        System.out.println("\\nTOP 5 DESTINOS MAS FRECUENTES:");
        pedidos.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Pedido::getAeropuertoDestinoId,
                java.util.stream.Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> 
                System.out.printf("   %s: %d pedidos%n", entry.getKey(), entry.getValue()));
        
        System.out.println("===================================================");
    }
}
