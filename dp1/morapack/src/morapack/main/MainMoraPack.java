package morapack.main;

import morapack.modelo.*;
import morapack.datos.CargadorDatosMoraPack;
import morapack.optimizacion.OptimizadorMoraPack;
import java.util.List;

/**
 * Programa principal para ejecutar la optimización MoraPack
 */
public class MainMoraPack {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== SISTEMA DE OPTIMIZACIÓN MORAPACK ===");
            
            // Rutas de archivos CSV
            String rutaAeropuertos = "/home/leoncio/Documentos/dp1/datos/aeropuertos_simple.csv";
            String rutaVuelos = "/home/leoncio/Documentos/dp1/datos/vuelos_simple.csv";
            
            // Cargar datos
            System.out.println("Cargando datos...");
            List<Aeropuerto> aeropuertos = CargadorDatosMoraPack.cargarAeropuertos(rutaAeropuertos);
            List<Vuelo> vuelos = CargadorDatosMoraPack.cargarVuelos(rutaVuelos);
            
            // Obtener sedes MoraPack
            List<Aeropuerto> sedes = CargadorDatosMoraPack.obtenerSedes(aeropuertos);
            
            // Generar pedidos de ejemplo
            int numeroPedidos = 50;
            List<Pedido> pedidos = CargadorDatosMoraPack.generarPedidosEjemplo(aeropuertos, numeroPedidos);
            
            // Mostrar estadísticas
            CargadorDatosMoraPack.mostrarEstadisticas(aeropuertos, vuelos, pedidos);
            
            // Ejecutar optimización
            System.out.println("Iniciando optimización genética...");
            OptimizadorMoraPack optimizador = new OptimizadorMoraPack(sedes, pedidos);
            
            // Configurar parámetros del algoritmo genético
            optimizador.configurar(
                100,    // tamaño población
                1000,   // generaciones
                0.8,    // probabilidad cruce
                0.1     // probabilidad mutación
            );
            
            // Ejecutar optimización
            IndividuoMoraPack mejorSolucion = optimizador.optimizar();
            
            // Comparar con solución aleatoria
            optimizador.compararConSolucionAleatoria(mejorSolucion);
            
            // Mostrar resultados detallados
            mostrarResultados(mejorSolucion, sedes);
            
        } catch (Exception e) {
            System.err.println("Error en la ejecución: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void mostrarResultados(IndividuoMoraPack solucion, List<Aeropuerto> sedes) {
        System.out.println("\n=== RESULTADOS DE OPTIMIZACIÓN ===");
        
        IndividuoMoraPack.EstadisticasSolucion stats = solucion.obtenerEstadisticas();
        
        System.out.printf("Fitness final: %.6f%n", stats.fitness);
        System.out.printf("Costo total: $%.2f%n", stats.costoTotal);
        System.out.printf("Violaciones de factibilidad: %d%n", stats.violacionesFactibilidad);
        
        // Distribución de pedidos por sede
        System.out.println("\nDistribución de pedidos por sede:");
        for (int i = 0; i < sedes.size(); i++) {
            Aeropuerto sede = sedes.get(i);
            int pedidosAsignados = stats.pedidosPorSede[i];
            System.out.printf("  %s (%s): %d pedidos%n", 
                            sede.getCiudad(), sede.getCodigoICAO(), pedidosAsignados);
        }
        
        // Detalles de asignaciones
        System.out.println("\nPrimeras 10 asignaciones detalladas:");
        int[] asignaciones = solucion.getAsignaciones();
        List<Pedido> pedidos = solucion.getPedidos();
        
        for (int i = 0; i < Math.min(10, pedidos.size()); i++) {
            Pedido pedido = pedidos.get(i);
            Aeropuerto sede = sedes.get(asignaciones[i]);
            double distancia = sede.calcularDistancia(pedido.getDestino());
            double costo = pedido.calcularCosto(sede, distancia);
            
            System.out.printf("  %s → %s desde %s (%.0f km, $%.2f)%n",
                            pedido.getId(),
                            pedido.getDestino().getCiudad(),
                            sede.getCiudad(),
                            distancia,
                            costo);
        }
        
        System.out.println("=====================================");
    }
}
