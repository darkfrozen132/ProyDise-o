package morapack.datos;

import morapack.modelo.Pedido;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * Cargador de pedidos reales desde pedidoUltrafinal.txt con filtrado por destinos válidos
 * Formato: dd-hh-mm-DEST-###-IdClien
 */
public class CargadorPedidosUltrafinal {
    
    /**
     * Carga pedidos desde pedidoUltrafinal.txt filtrando por destinos válidos
     */
    public static List<Pedido> cargarPedidos(String rutaArchivo, Set<String> destinosValidos) throws IOException {
        List<Pedido> pedidos = new ArrayList<>();
        List<String> pedidosDescartados = new ArrayList<>();
        
        System.out.println("📋 Cargando pedidos desde: " + rutaArchivo);
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int contador = 0;
            int descartados = 0;
            
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                
                try {
                    // Extraer destino del ID para filtrar
                    String destino = extraerDestino(linea);
                    
                    if (destinosValidos.contains(destino)) {
                        Pedido pedido = new Pedido(linea);
                        pedidos.add(pedido);
                        contador++;
                        
                        // Progreso cada 50 pedidos válidos
                        if (contador % 50 == 0) {
                            System.out.println("   ✅ Cargados " + contador + " pedidos válidos...");
                        }
                    } else {
                        pedidosDescartados.add(linea + " (destino: " + destino + ")");
                        descartados++;
                    }
                    
                } catch (Exception e) {
                    System.err.println("⚠️ Error procesando pedido: " + linea + " - " + e.getMessage());
                    descartados++;
                }
            }
            
            System.out.println("✅ Total pedidos cargados: " + contador);
            if (descartados > 0) {
                System.out.println("⚠️ Total pedidos descartados: " + descartados);
                System.out.println("   (Destinos no válidos según aeropuertos_simple.csv)");
            }
        }
        
        return pedidos;
    }
    
    /**
     * Carga todos los pedidos sin filtrar (para compatibilidad)
     */
    public static List<Pedido> cargarPedidos(String rutaArchivo) throws IOException {
        List<Pedido> pedidos = new ArrayList<>();
        
        System.out.println("📋 Cargando TODOS los pedidos desde: " + rutaArchivo);
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int contador = 0;
            
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                
                try {
                    Pedido pedido = new Pedido(linea);
                    pedidos.add(pedido);
                    contador++;
                    
                    if (contador % 50 == 0) {
                        System.out.println("   ✅ Cargados " + contador + " pedidos...");
                    }
                    
                } catch (Exception e) {
                    System.err.println("⚠️ Error procesando pedido: " + linea + " - " + e.getMessage());
                }
            }
            
            System.out.println("✅ Total pedidos cargados: " + contador);
        }
        
        return pedidos;
    }
    
    /**
     * Extrae el código de destino del ID del pedido
     * Formato: dd-hh-mm-DEST-###-IdClien
     */
    private static String extraerDestino(String idPedido) {
        String[] partes = idPedido.split("-");
        if (partes.length >= 4) {
            return partes[3]; // El destino está en la 4ta posición (índice 3)
        }
        throw new IllegalArgumentException("Formato de ID inválido: " + idPedido);
    }
    
    /**
     * Muestra estadísticas detalladas de los pedidos cargados
     */
    public static void mostrarEstadisticas(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            System.out.println("❌ No hay pedidos para mostrar estadísticas");
            return;
        }
        
        System.out.println("\n📊 ============= ESTADÍSTICAS DE PEDIDOS =============");
        
        Set<String> destinosUnicos = new java.util.HashSet<>();
        int totalProductos = 0;
        int minDia = Integer.MAX_VALUE;
        int maxDia = Integer.MIN_VALUE;
        
        for (Pedido pedido : pedidos) {
            destinosUnicos.add(pedido.getAeropuertoDestinoId());
            totalProductos += pedido.getCantidadProductos();
            
            int dia = pedido.getDia();
            if (dia < minDia) minDia = dia;
            if (dia > maxDia) maxDia = dia;
        }
        
        System.out.println("📦 Total pedidos: " + pedidos.size());
        System.out.println("🛫 Destinos únicos: " + destinosUnicos.size());
        System.out.println("📦 Total productos: " + totalProductos);
        System.out.println("📊 Promedio productos/pedido: " + (totalProductos / (double) pedidos.size()));
        System.out.println("📅 Rango de días: " + minDia + " - " + maxDia);
        
        // Top destinos más frecuentes
        java.util.Map<String, Integer> frecuenciaDestinos = new java.util.HashMap<>();
        for (Pedido pedido : pedidos) {
            String destino = pedido.getAeropuertoDestinoId();
            frecuenciaDestinos.put(destino, frecuenciaDestinos.getOrDefault(destino, 0) + 1);
        }
        
        System.out.println("\n🎯 TOP 10 DESTINOS MÁS FRECUENTES:");
        frecuenciaDestinos.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(10)
            .forEach(entry -> System.out.println("   " + entry.getKey() + ": " + entry.getValue() + " pedidos"));
        
        System.out.println("===================================================");
    }
}
