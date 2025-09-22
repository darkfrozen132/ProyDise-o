package morapack;

import morapack.dao.impl.*;
import morapack.datos.CargadorDatosCSV;
import morapack.optimizacion.FuncionObjetivoOptimizada;
import morapack.modelo.Vuelo;
import morapack.modelo.Pedido;
import java.util.List;
import java.util.ArrayList;

/**
 * Clase principal para evaluar fitness en múltiples archivos de pedidos
 */
public class MainFitnessComparacion {
    
    public static void main(String[] args) {
        System.out.println("=== SISTEMA MORAPACK - EVALUACIÓN MÚLTIPLES ARCHIVOS ===\n");
        
        // Inicializar DAOs
        AeropuertoDAOImpl aeropuertoDAO = new AeropuertoDAOImpl();
        SedeDAOImpl sedeDAO = new SedeDAOImpl();
        PedidoDAOImpl pedidoDAO = new PedidoDAOImpl();
        
        // Leer los CSV base
        System.out.println("📂 Cargando datos CSV base...");
        CargadorDatosCSV.cargarAeropuertos(aeropuertoDAO);
        List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
        CargadorDatosCSV.cargarClientes();
        
        System.out.println("✅ Datos base cargados: " + aeropuertoDAO.contarTotal() + " aeropuertos, " + 
                          vuelos.size() + " vuelos");
        
        // Evaluar fitness para cada uno de los 20 archivos de pedidos
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 EVALUACIÓN DE FITNESS PARA 20 ARCHIVOS DE PEDIDOS");
        System.out.println("=".repeat(80));
        
        List<String> resultadosComparacion = new ArrayList<>();
        
        for (int i = 1; i <= 20; i++) {
            String nombreArchivo = String.format("pedidos_%02d.csv", i);
            System.out.println("\n📁 Procesando archivo: " + nombreArchivo);
            
            // Cargar pedidos desde archivo específico
            List<Pedido> pedidosArchivo = CargadorDatosCSV.cargarPedidosDesdeArchivo(nombreArchivo);
            
            if (pedidosArchivo.isEmpty()) {
                System.out.println("⚠️  No se pudieron cargar pedidos desde " + nombreArchivo);
                continue;
            }
            
            System.out.println("📦 Pedidos cargados: " + pedidosArchivo.size());
            
            // Crear función objetivo para evaluar este conjunto de pedidos
            FuncionObjetivoOptimizada funcionObj = new FuncionObjetivoOptimizada(aeropuertoDAO, pedidosArchivo, vuelos);
            
            // Generar solución aleatoria y evaluar
            int[] solucion = funcionObj.generarSolucionAleatoria();
            double fitness = funcionObj.calcularFitness(solucion);
            int productosATiempo = funcionObj.obtenerProductosATiempo(solucion);
            int productosRetrasados = funcionObj.obtenerProductosRetrasados(solucion);
            
            String resultado = String.format(
                "Archivo %2d: Fitness=%8.0f | A tiempo: %3d | Retrasados: %3d", 
                i, fitness, productosATiempo, productosRetrasados
            );
            
            resultadosComparacion.add(resultado);
            System.out.println("✅ " + resultado);
        }
        
        // Mostrar resumen comparativo
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📈 RESUMEN COMPARATIVO DE FITNESS");
        System.out.println("=".repeat(80));
        
        for (String resultado : resultadosComparacion) {
            System.out.println(resultado);
        }
        
        System.out.println("\n💡 Nota: Fitness = (Productos a tiempo × 1) + (Productos retrasados × -1000)");
    }
}
