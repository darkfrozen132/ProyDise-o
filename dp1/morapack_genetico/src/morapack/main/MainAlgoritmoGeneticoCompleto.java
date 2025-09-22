package morapack.main;

import morapack.datos.CargadorDatos;
import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.genetico.AlgoritmoGeneticoRutas;
import morapack.genetico.IndividuoRutasCompletas;
import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Demostración del Algoritmo Genético con Planificación Completa de Rutas
 */
public class MainAlgoritmoGeneticoCompleto {
    
    public static void main(String[] args) {
        try {
            System.out.println("🚀 ALGORITMO GENÉTICO CON PLANIFICACIÓN COMPLETA DE RUTAS");
            System.out.println("===============================================================");
            
            // 1. CARGAR DATOS
            System.out.println("📂 Cargando datos...");
            CargadorDatos cargador = new CargadorDatos();
            List<Vuelo> vuelos = cargador.cargarVuelos("../datos/vuelos_simple.csv");
            List<Pedido> pedidos = cargador.generarPedidosAleatorios(20);
            
            System.out.println("✅ Cargados: " + vuelos.size() + " vuelos, " + pedidos.size() + " pedidos");
            
            // 2. CONFIGURAR ALGORITMO GENÉTICO
            int tamanoPoblacion = 50;
            int generaciones = 100;
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
            
            // 5. ESTADÍSTICAS ADICIONALES
            mostrarEstadisticas(mejorSolucion);
            
            // 6. GUARDAR EN ARCHIVO
            guardarResultados(mejorSolucion, tiempoTotal);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Muestra estadísticas detalladas de la solución
     */
    private static void mostrarEstadisticas(IndividuoRutasCompletas solucion) {
        System.out.println("\n📊 ESTADÍSTICAS DE LA SOLUCIÓN:");
        System.out.println("----------------------------------------");
        
        int rutasDirectas = 0;
        int rutasUnaConexion = 0;
        int rutasDosConexiones = 0;
        int rutasSinSolucion = 0;
        double tiempoTotal = 0;
        
        List<morapack.planificacion.RutaCompleta> rutas = solucion.getRutas();
        for (int i = 0; i < rutas.size(); i++) {
            if (rutas.get(i) != null) {
                switch (rutas.get(i).getTipo()) {
                    case DIRECTO:
                        rutasDirectas++;
                        break;
                    case UNA_CONEXION:
                        rutasUnaConexion++;
                        break;
                    case DOS_CONEXIONES:
                        rutasDosConexiones++;
                        break;
                }
                tiempoTotal += rutas.get(i).calcularTiempoTotal();
            } else {
                rutasSinSolucion++;
            }
        }
        
        System.out.println("✈️ Rutas directas: " + rutasDirectas);
        System.out.println("🔄 Rutas con 1 conexión: " + rutasUnaConexion);
        System.out.println("🔄🔄 Rutas con 2 conexiones: " + rutasDosConexiones);
        System.out.println("❌ Sin solución: " + rutasSinSolucion);
        System.out.println("⏱️ Tiempo total de rutas: " + tiempoTotal + " minutos");
        System.out.println("📈 Tiempo promedio por ruta: " + (tiempoTotal / (solucion.getRutas().length - rutasSinSolucion)) + " minutos");
    }
    
    /**
     * Guarda los resultados en un archivo TXT
     */
    private static void guardarResultados(IndividuoRutasCompletas solucion, long tiempoEjecucion) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("resultados_algoritmo_genetico_completo.txt"))) {
            writer.println("RESULTADOS DEL ALGORITMO GENÉTICO CON RUTAS COMPLETAS");
            writer.println("=" * 60);
            writer.println("Fecha de ejecución: " + new java.util.Date());
            writer.println("Tiempo de ejecución: " + tiempoEjecucion + " ms");
            writer.println("Fitness: " + solucion.getFitness());
            writer.println();
            
            writer.println("DETALLE DE LA SOLUCIÓN:");
            writer.println(solucion.obtenerDescripcion());
            
            System.out.println("💾 Resultados guardados en: resultados_algoritmo_genetico_completo.txt");
            
        } catch (IOException e) {
            System.err.println("❌ Error al guardar archivo: " + e.getMessage());
        }
    }
}
