package morapack.planificacion;

import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Plan completo de rutas optimizadas
 */
public class PlanRutas {
    private List<RutaOptimizada> rutas;
    private double fitnessTotal;
    private int capacidadTotalDiaria;
    
    // Estadísticas
    private int totalProductos;
    private int rutasAltaPrioridad;
    private double porcentajeUtilizacion;
    private Map<String, Integer> distribucionPorOrigen;
    private Map<Integer, Integer> distribucionPorHora;
    
    // Nuevos campos para productos a tiempo/retrasados
    private int productosATiempo;
    private int productosRetrasados;
    private List<String> historialIteraciones;
    
    public PlanRutas() {
        this.rutas = new ArrayList<>();
        this.distribucionPorOrigen = new HashMap<>();
        this.distribucionPorHora = new HashMap<>();
    }
    
    public void agregarRuta(RutaOptimizada ruta) {
        rutas.add(ruta);
    }
    
    public void calcularEstadisticas() {
        totalProductos = 0;
        rutasAltaPrioridad = 0;
        distribucionPorOrigen.clear();
        distribucionPorHora.clear();
        
        for (RutaOptimizada ruta : rutas) {
            totalProductos += ruta.getCantidadProductos();
            
            if (ruta.getPrioridad() == 1) {
                rutasAltaPrioridad++;
            }
            
            String origen = ruta.getOrigen().getCodigoICAO();
            distribucionPorOrigen.merge(origen, ruta.getCantidadProductos(), Integer::sum);
            
            int hora = ruta.getHoraEstimada();
            distribucionPorHora.merge(hora, ruta.getCantidadProductos(), Integer::sum);
        }
        
        porcentajeUtilizacion = (double) totalProductos / capacidadTotalDiaria * 100.0;
    }
    
    public void mostrarResumen() {
        System.out.println("📋 RESUMEN DEL PLAN DE RUTAS OPTIMIZADO");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.printf("🎯 Fitness total: $%.2f%n", fitnessTotal);
        System.out.printf("📦 Total productos: %,d%n", totalProductos);
        System.out.printf("✅ Productos a tiempo: %,d%n", productosATiempo);
        System.out.printf("⏰ Productos retrasados: %,d%n", productosRetrasados);
        System.out.printf("📈 Capacidad diaria: %,d productos%n", capacidadTotalDiaria);
        System.out.printf("📊 Utilización: %.2f%%%n", porcentajeUtilizacion);
        System.out.printf("🛣️  Total rutas: %d%n", rutas.size());
        
        // Estado del sistema
        if (porcentajeUtilizacion > 95) {
            System.out.println("❌ ESTADO: ZONA CRÍTICA - Riesgo de colapso");
        } else if (porcentajeUtilizacion > 80) {
            System.out.println("⚠️  ESTADO: ZONA DE ADVERTENCIA");
        } else {
            System.out.println("✅ ESTADO: ZONA SEGURA");
        }
        
        System.out.println("\n🏭 DISTRIBUCIÓN POR AEROPUERTO ORIGEN:");
        distribucionPorOrigen.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> 
                    System.out.printf("  %s: %,d productos%n", entry.getKey(), entry.getValue()));
        
        System.out.println("\n⏰ DISTRIBUCIÓN TEMPORAL (Top 10 horas):");
        distribucionPorHora.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> 
                    System.out.printf("  %02d:00 → %,d productos%n", entry.getKey(), entry.getValue()));
    }
    
    public void mostrarRutasDetalladas(int limite) {
        System.out.printf("\n🛣️  RUTAS DETALLADAS (mostrando %d de %d):%n", 
                         Math.min(limite, rutas.size()), rutas.size());
        
        rutas.stream()
             .sorted(Comparator.comparingInt(RutaOptimizada::getPrioridad)
                              .thenComparing(RutaOptimizada::getCantidadProductos, Comparator.reverseOrder()))
             .limit(limite)
             .forEach(ruta -> System.out.println("  " + ruta));
        
        if (rutas.size() > limite) {
            System.out.printf("  ... y %d rutas más%n", rutas.size() - limite);
        }
    }
    
    /**
     * Exporta todas las rutas detalladas a un archivo TXT
     */
    public void exportarRutasATXT(String nombreArchivo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(nombreArchivo))) {
            // Encabezado del archivo
            writer.println("=".repeat(80));
            writer.println("📋 PLAN DE RUTAS OPTIMIZADO - MORAPACK");
            writer.println("🧬 Generado por Algoritmo Genético");
            writer.println("📅 Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("=".repeat(80));
            writer.println();
            
            // Resumen ejecutivo
            writer.println("📊 RESUMEN EJECUTIVO:");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.printf("🎯 Fitness total optimizado: $%.2f%n", fitnessTotal);
            writer.printf("📦 Total productos a transportar: %,d%n", totalProductos);
            writer.printf("✅ Productos entregados a tiempo: %,d%n", productosATiempo);
            writer.printf("⏰ Productos entregados con demora: %,d%n", productosRetrasados);
            writer.printf("📈 Capacidad total diaria (Cdia): %,d productos%n", capacidadTotalDiaria);
            writer.printf("📊 Utilización del sistema: %.2f%%%n", porcentajeUtilizacion);
            writer.printf("🛣️  Total rutas planificadas: %d%n", rutas.size());
            
            // Estado del sistema
            writer.println();
            writer.print("🚦 Estado del sistema: ");
            if (porcentajeUtilizacion > 95) {
                writer.println("❌ ZONA CRÍTICA - Riesgo de colapso");
            } else if (porcentajeUtilizacion > 80) {
                writer.println("⚠️  ZONA DE ADVERTENCIA");
            } else {
                writer.println("✅ ZONA SEGURA");
            }
            writer.println();
            
            // Distribución por aeropuerto
            writer.println("🏭 DISTRIBUCIÓN POR AEROPUERTO ORIGEN:");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            distribucionPorOrigen.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> 
                        writer.printf("  %-6s: %,8d productos%n", entry.getKey(), entry.getValue()));
            writer.println();
            
            // Distribución temporal
            writer.println("⏰ DISTRIBUCIÓN TEMPORAL POR HORA:");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            for (int hora = 0; hora < 24; hora++) {
                int productos = distribucionPorHora.getOrDefault(hora, 0);
                if (productos > 0) {
                    writer.printf("  %02d:00 → %,8d productos%n", hora, productos);
                }
            }
            writer.println();
            
            // HISTORIAL DE LAS 20 ITERACIONES
            writer.println("🧬 HISTORIAL DE ITERACIONES DEL ALGORITMO GENÉTICO:");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            if (historialIteraciones != null && !historialIteraciones.isEmpty()) {
                for (String iteracion : historialIteraciones) {
                    writer.println("  " + iteracion);
                }
            } else {
                writer.println("  Sin historial de iteraciones disponible");
            }
            writer.println();
            
            // TODAS LAS RUTAS DETALLADAS
            writer.println("🛣️  TODAS LAS RUTAS DETALLADAS:");
            writer.println("=".repeat(80));
            writer.printf("Total: %d rutas optimizadas%n", rutas.size());
            writer.println("Formato: [ID_Pedido: Origen→Destino, Productos, Hora, Distancia_GMT]");
            writer.println("=".repeat(80));
            writer.println();
            
            // Ordenar rutas por cantidad de productos (mayor a menor), luego por hora
            List<RutaOptimizada> rutasOrdenadas = new ArrayList<>(rutas);
            rutasOrdenadas.sort(Comparator.comparingInt(RutaOptimizada::getCantidadProductos).reversed()
                                         .thenComparing(RutaOptimizada::getHoraEstimada));
            
            // Escribir cada ruta con detalles
            for (int i = 0; i < rutasOrdenadas.size(); i++) {
                RutaOptimizada ruta = rutasOrdenadas.get(i);
                writer.printf("%3d. %s%n", (i + 1), ruta.toString());
                
                // Agregar información adicional cada 10 rutas para legibilidad
                if ((i + 1) % 10 == 0 && i < rutasOrdenadas.size() - 1) {
                    writer.println("     " + "-".repeat(70));
                }
            }
            
            writer.println();
            writer.println("=".repeat(80));
            writer.println("📊 ESTADÍSTICAS ADICIONALES:");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // Estadísticas de entregas
            writer.println("⏰ Análisis de entregas:");
            writer.printf("   ✅ Productos a tiempo: %,d (%.1f%%)%n", 
                        productosATiempo, 
                        totalProductos > 0 ? (double) productosATiempo / totalProductos * 100 : 0);
            writer.printf("   ⏰ Productos con demora: %,d (%.1f%%)%n", 
                        productosRetrasados, 
                        totalProductos > 0 ? (double) productosRetrasados / totalProductos * 100 : 0);
            
            // Distribución por aeropuerto origen
            writer.println();
            writer.println("🏭 Productos por aeropuerto origen:");
            distribucionPorOrigen.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> 
                        writer.printf("   %-6s: %,8d productos%n", entry.getKey(), entry.getValue()));
            
            writer.println();
            writer.println("🎯 Días estimados hasta saturación: " + 
                          String.format("%.0f días", (capacidadTotalDiaria - totalProductos) / (double) totalProductos));
            writer.println("📈 Margen de crecimiento disponible: " + 
                          String.format("%,d productos (%.1fx el volumen actual)", 
                                      capacidadTotalDiaria - totalProductos,
                                      (double) capacidadTotalDiaria / totalProductos));
            
            writer.println();
            writer.println("=".repeat(80));
            writer.println("✅ PLAN DE RUTAS EXPORTADO EXITOSAMENTE");
            writer.println("🧬 Optimizado con Algoritmo Genético - MoraPack System");
            writer.println("=".repeat(80));
            
            System.out.printf("📄 Rutas exportadas a: %s%n", nombreArchivo);
            System.out.printf("📊 Total rutas en archivo: %d%n", rutas.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error al exportar rutas: " + e.getMessage());
        }
    }
    
    // Getters y setters
    public List<RutaOptimizada> getRutas() { return rutas; }
    public void setRutas(List<RutaOptimizada> rutas) { this.rutas = rutas; }
    
    public double getFitnessTotal() { return fitnessTotal; }
    public void setFitnessTotal(double fitnessTotal) { this.fitnessTotal = fitnessTotal; }
    
    public int getCapacidadTotalDiaria() { return capacidadTotalDiaria; }
    public void setCapacidadTotalDiaria(int capacidadTotalDiaria) { this.capacidadTotalDiaria = capacidadTotalDiaria; }
    
    public int getTotalProductos() { return totalProductos; }
    public double getPorcentajeUtilizacion() { return porcentajeUtilizacion; }
    public int getRutasAltaPrioridad() { return rutasAltaPrioridad; }
    
    // Setters para nuevos campos
    public void setProductosATiempo(int productosATiempo) { this.productosATiempo = productosATiempo; }
    public void setProductosRetrasados(int productosRetrasados) { this.productosRetrasados = productosRetrasados; }
    public void setHistorialIteraciones(List<String> historialIteraciones) { this.historialIteraciones = historialIteraciones; }
    
    // Getters para nuevos campos
    public int getProductosATiempo() { return productosATiempo; }
    public int getProductosRetrasados() { return productosRetrasados; }
}
