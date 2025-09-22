package morapack.demo;

import morapack.datos.CargadorDatosCSV;
import morapack.modelo.Vuelo;
import morapack.planificacion.PlanificadorConexiones;
import morapack.planificacion.RutaCompleta;
import java.util.List;

/**
 * Demostración conceptual: ¿Por qué el algoritmo genético DEBE hacer planificación completa?
 */
public class DemoAlgoritmoGeneticoConcepto {
    
    public static void main(String[] args) {
        System.out.println("🤔 ¿POR QUÉ EL ALGORITMO GENÉTICO DEBE HACER PLANIFICACIÓN COMPLETA?");
        System.out.println("==================================================================");
        
        // Cargar vuelos reales
        List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
        System.out.println("📊 Vuelos cargados: " + vuelos.size());
        
        // Crear planificador
        PlanificadorConexiones planificador = new PlanificadorConexiones(vuelos);
        
        System.out.println("\n🔍 DIFERENCIAS ENTRE ALGORITMOS:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        System.out.println("\n❌ ALGORITMO GENÉTICO TRADICIONAL (SIMPLE):");
        System.out.println("   • Cromosoma: int[] sedeAsignada = {0, 1, 2, 0, 1, ...}");
        System.out.println("   • Solo asigna pedidos a sedes (SPIM=0, EBCI=1, UBBB=2)");
        System.out.println("   • NO planifica rutas reales");
        System.out.println("   • NO considera conexiones ni horarios");
        System.out.println("   • Fitness basado en distancia euclidiana aproximada");
        
        System.out.println("\n✅ ALGORITMO GENÉTICO AVANZADO (RUTAS COMPLETAS):");
        System.out.println("   • Cromosoma: RutaCompleta[] rutas = {ruta1, ruta2, ruta3, ...}");
        System.out.println("   • Planifica rutas COMPLETAS para cada pedido");
        System.out.println("   • Usa vuelos reales del CSV con horarios");
        System.out.println("   • Maneja conexiones (directo, 1 escala, 2 escalas)");
        System.out.println("   • Fitness basado en tiempo total REAL de entrega");
        
        // Ejemplo práctico
        System.out.println("\n🎯 EJEMPLO PRÁCTICO:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━");
        
        System.out.println("📦 Pedido: Cliente necesita envío de SPIM → KJFK");
        
        System.out.println("\n❌ Algoritmo Simple:");
        System.out.println("   Resultado: gen[0] = 0 (asignar a sede SPIM)");
        System.out.println("   ¿Cómo llega realmente? ¡NO LO SABE!");
        
        System.out.println("\n✅ Algoritmo Completo:");
        RutaCompleta ruta = planificador.buscarMejorRuta("SPIM", "KJFK", "08:00");
        if (ruta != null) {
            System.out.println("   Resultado: " + ruta.obtenerDescripcion());
            System.out.println("   Tiempo real: " + ruta.calcularTiempoTotal() + " minutos");
        } else {
            System.out.println("   Resultado: Sin ruta directa disponible");
            System.out.println("   Necesita planificar conexiones avanzadas");
        }
        
        System.out.println("\n🎯 CONCLUSIÓN:");
        System.out.println("━━━━━━━━━━━━━");
        System.out.println("✅ SÍ, el algoritmo genético DEBE hacer planificación completa");
        System.out.println("✅ Solo así puede optimizar rutas reales con horarios");
        System.out.println("✅ Solo así puede manejar conexiones y escalas");
        System.out.println("✅ Solo así puede dar tiempos de entrega precisos");
        
        System.out.println("\n🔧 IMPLEMENTACIÓN:");
        System.out.println("━━━━━━━━━━━━━━━━━");
        System.out.println("• Cromosoma: RutaCompleta[numPedidos]");
        System.out.println("• Inicialización: Buscar ruta factible para cada pedido");
        System.out.println("• Cruce: Intercambiar rutas completas entre individuos");  
        System.out.println("• Mutación: Re-planificar rutas de pedidos aleatorios");
        System.out.println("• Fitness: Suma de tiempos reales + penalizaciones");
    }
}
