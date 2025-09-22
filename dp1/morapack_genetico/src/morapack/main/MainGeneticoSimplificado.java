package morapack.main;

import morapack.datos.CargadorDatosCSV;
import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.genetico.AlgoritmoGeneticoRutas;
import morapack.genetico.IndividuoRutasCompletas;
import java.util.List;

/**
 * Demostración del Algoritmo Genético con Planificación Completa de Rutas
 */
public class MainGeneticoSimplificado {
    
    public static void main(String[] args) {
        try {
            System.out.println("🚀 ALGORITMO GENÉTICO CON PLANIFICACIÓN COMPLETA DE RUTAS");
            System.out.println("===============================================================");
            
            // 1. CARGAR DATOS
            System.out.println("📂 Cargando datos...");
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            List<Pedido> pedidos = CargadorDatosCSV.cargarPedidosDesdeArchivo("aeropuertos_simple.csv");
            
            System.out.println("✅ Cargados: " + vuelos.size() + " vuelos, " + pedidos.size() + " pedidos");
            
            // 2. CONFIGURAR ALGORITMO GENÉTICO
            int tamanoPoblacion = 30;
            int generaciones = 50;
            double probabilidadCruce = 0.8;
            double probabilidadMutacion = 0.1;
            
            AlgoritmoGeneticoRutas algoritmo = new AlgoritmoGeneticoRutas(
                pedidos, vuelos, tamanoPoblacion, probabilidadCruce, probabilidadMutacion
            );
            
            // 3. EJECUTAR ALGORITMO
            System.out.println("\n🧬 Ejecutando algoritmo genético...");
            long inicioTiempo = System.currentTimeMillis();
            
            IndividuoRutasCompletas mejorSolucion = algoritmo.ejecutar(generaciones);
            
            long tiempoTotal = System.currentTimeMillis() - inicioTiempo;
            
            // 4. MOSTRAR RESULTADOS
            System.out.println("\n🎯 RESULTADOS FINALES");
            System.out.println("==================================================");
            System.out.println("⏱️ Tiempo de ejecución: " + tiempoTotal + " ms");
            System.out.println("🏆 Fitness de la mejor solución: " + mejorSolucion.getFitness());
            
            System.out.println("\n📋 DETALLE DE LA MEJOR SOLUCIÓN:");
            System.out.println(mejorSolucion.obtenerDescripcion());
            
            // 5. COMPARACIÓN CON ALGORITMO TRADICIONAL
            System.out.println("\n🔍 COMPARACIÓN:");
            System.out.println("✅ ESTE algoritmo planifica rutas COMPLETAS con conexiones");
            System.out.println("❌ El algoritmo tradicional solo asigna sedes sin planificar rutas");
            System.out.println("🎯 Resultado: Rutas optimizadas con tiempos de vuelo reales");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
