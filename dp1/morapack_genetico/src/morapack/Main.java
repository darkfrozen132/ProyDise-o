package morapack;

import morapack.dao.impl.*;
import morapack.datos.CargadorDatosCSV;
import morapack.optimizacion.FuncionObjetivoOptimizada;
import morapack.modelo.Vuelo;
import morapack.planificacion.PlanificadorRutas;
import morapack.planificacion.PlanRutas;
import morapack.modelo.Pedido;
import java.util.List;

/**
 * Clase principal para probar el sistema MoraPack
 */
public class Main {
    
    public static void main(String[] args) {
        package morapack;

import morapack.dao.impl.*;
import morapack.datos.CargadorDatosCSV;
import morapack.optimizacion.FuncionObjetivoOptimizada;
import morapack.modelo.Vuelo;
import morapack.planificacion.PlanificadorRutas;
import morapack.planificacion.PlanRutas;
import morapack.modelo.Pedido;
import java.util.List;
import java.util.ArrayList;

/**
 * Clase principal para evaluar fitness en múltiples archivos de pedidos
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== SISTEMA MORAPACK - EVALUACIÓN MÚLTIPLES ARCHIVOS ===
");
        
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
        System.out.println("
" + "=".repeat(80));
        System.out.println("📊 EVALUACIÓN DE FITNESS PARA 20 ARCHIVOS DE PEDIDOS");
        System.out.println("=".repeat(80));
        
        List<String> resultadosComparacion = new ArrayList<>();
        
        for (int i = 1; i <= 20; i++) {
            String nombreArchivo = String.format("pedidos_%02d.csv", i);
            System.out.println("
📁 Procesando archivo: " + nombreArchivo);
            
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
        System.out.println("
" + "=".repeat(80));
        System.out.println("📈 RESUMEN COMPARATIVO DE FITNESS");
        System.out.println("=".repeat(80));
        
        for (String resultado : resultadosComparacion) {
            System.out.println(resultado);
        }
        
        System.out.println("
💡 Nota: Fitness = (Productos a tiempo × 1) + (Productos retrasados × -1000)");
    }
}
        
        // Inicializar DAOs
        AeropuertoDAOImpl aeropuertoDAO = new AeropuertoDAOImpl();
        SedeDAOImpl sedeDAO = new SedeDAOImpl();
        PedidoDAOImpl pedidoDAO = new PedidoDAOImpl();
        
        // Leer los 4 CSV
        System.out.println("📂 Cargando datos CSV...");
        CargadorDatosCSV.cargarAeropuertos(aeropuertoDAO);
        List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos(); // Cargar vuelos para calcular Cdia
        CargadorDatosCSV.cargarClientes();
        CargadorDatosCSV.cargarPedidosDesdeCSV(pedidoDAO);
        
        System.out.println("✅ Datos cargados: " + aeropuertoDAO.contarTotal() + " aeropuertos, " + 
                          pedidoDAO.contarTotal() + " pedidos");
        
        // Obtener pedidos para optimizar
        List<Pedido> pedidos = pedidoDAO.obtenerTodos();
        
        // Crear función objetivo optimizada con restricción Cdia
        System.out.println("\n🎯 Inicializando función objetivo (Restricción: Cdia = Σ Capacidades vuelos)...");
        FuncionObjetivoOptimizada funcionObjetivo = new FuncionObjetivoOptimizada(aeropuertoDAO, pedidos, vuelos);
        
        // Generar y evaluar solución aleatoria
        System.out.println("\n🎲 Generando solución aleatoria...");
        int[] solucionAleatoria = funcionObjetivo.generarSolucionAleatoria();
        
        // Evaluar la solución con la nueva función objetivo
        double fitness1 = funcionObjetivo.calcularFitness(solucionAleatoria);
        System.out.println("📊 EVALUACIÓN DE SOLUCIÓN:");
        System.out.println("Fitness total: $" + String.format("%.2f", fitness1));
        System.out.println();
        funcionObjetivo.mostrarAnalisisSolucion(solucionAleatoria);
        
        // Generar otra solución para comparar
        System.out.println("\n🎲 Generando segunda solución para comparar...");
        int[] solucion2 = funcionObjetivo.generarSolucionAleatoria();
        double fitness2 = funcionObjetivo.calcularFitness(solucion2);
        
        System.out.println("Fitness solución 2: $" + String.format("%.2f", fitness2));
        
        System.out.println("🎯 Función objetivo optimizada para RETRASAR COLAPSO LOGÍSTICO!");
        
        // ===== PLANIFICACIÓN DE RUTAS CON ALGORITMO GENÉTICO =====
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🧬 PLANIFICACIÓN DE RUTAS CON ALGORITMO GENÉTICO");
        System.out.println("=".repeat(60));
        
        // Crear planificador de rutas
        PlanificadorRutas planificador = new PlanificadorRutas(funcionObjetivo, aeropuertoDAO, pedidos, vuelos);
        
        // Configurar parámetros del algoritmo genético
        planificador.setTamañoPoblacion(100);       // 100 individuos
        planificador.setNumeroGeneraciones(20);     // 20 generaciones para mostrar cada iteración
        planificador.setProbabilidadCruce(0.6);    // 85% probabilidad de cruce
        planificador.setProbabilidadMutacion(0.4); // 12% probabilidad de mutación
        
        // Ejecutar planificación
        OptimizadorMoraPack optimizador = new OptimizadorMoraPack(aeropuertos, vuelos, 946000);
        
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
            
            // Ejecutar optimización genética (1 generación para obtener fitness)
            PlanRutas mejorPlan = optimizador.optimizar(pedidosArchivo, 1, 0.6, 0.4);
            
            if (mejorPlan != null) {
                FuncionObjetivoOptimizada funcionObj = new FuncionObjetivoOptimizada(aeropuertos, vuelos, 946000);
                double fitness = funcionObj.calcularFitness(mejorPlan.getRutas());
                int productosATiempo = funcionObj.obtenerProductosATiempo();
                int productosRetrasados = funcionObj.obtenerProductosRetrasados();
                
                String resultado = String.format(
                    "Archivo %2d: Fitness=%8.0f | A tiempo: %3d | Retrasados: %3d", 
                    i, fitness, productosATiempo, productosRetrasados
                );
                
                resultadosComparacion.add(resultado);
                System.out.println("✅ " + resultado);
            } else {
                System.out.println("❌ Error en optimización del archivo " + nombreArchivo);
            }
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
