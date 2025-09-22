package morapack.main;

import morapack.datos.CargadorDatosCSV;
import morapack.datos.CargadorPedidosSimple;
import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.genetico.core.algoritmo.AlgoritmoGeneticoIntegrado;
import morapack.genetico.core.algoritmo.IndividuoIntegrado;
import java.util.List;

/**
 * Programa principal que usa pedidos con aeropuertos del CSV completo
 * Demuestra el funcionamiento con sedes y escalas
 */
public class MainAlgoritmoConAeropuertosCSV {
    
    public static void main(String[] args) {
        try {
            System.out.println("🌍 ALGORITMO GENÉTICO CON AEROPUERTOS DEL CSV COMPLETO");
            System.out.println("======================================================");
            System.out.println("🏢 Sedes MoraPack: SPIM (Lima), EBCI (Bruselas), UBBB (Baku)");
            System.out.println("✈️ Estrategia: Productos salen de sedes, pueden usar escalas");
            System.out.println();
            
            // 1. CARGAR DATOS
            System.out.println("📂 Cargando datos...");
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            
            // Cargar pedidos desde archivo generado usando cargador simple
            List<Pedido> pedidos = CargadorPedidosSimple.cargarPedidosDesdeArchivo("datos/pedidos/pedidos_13.csv");
            
            if (pedidos.isEmpty()) {
                System.out.println("⚠️ No se encontraron pedidos. Generando pedidos de ejemplo...");
                pedidos = generarPedidosConAeropuertosCSV();
            }
            
            System.out.println("✅ Datos cargados:");
            System.out.println("  • Vuelos: " + vuelos.size());
            System.out.println("  • Pedidos: " + pedidos.size());
            System.out.println();
            
            // Mostrar algunos pedidos de ejemplo
            mostrarEjemplosPedidos(pedidos);
            
            // 2. CONFIGURAR ALGORITMO
            int tamanoPoblacion = 30;
            int numeroGeneraciones = 50;
            
            AlgoritmoGeneticoIntegrado algoritmo = new AlgoritmoGeneticoIntegrado(
                pedidos, vuelos, tamanoPoblacion, numeroGeneraciones
            );
            
            // 3. EJECUTAR ALGORITMO
            System.out.println("🧬 Ejecutando algoritmo genético integrado...");
            System.out.println();
            
            long inicioTiempo = System.currentTimeMillis();
            IndividuoIntegrado mejorSolucion = algoritmo.ejecutar();
            long tiempoEjecucion = System.currentTimeMillis() - inicioTiempo;
            
            // 4. MOSTRAR RESULTADOS
            System.out.println();
            System.out.println("🏆 RESULTADOS FINALES");
            System.out.println("=====================");
            System.out.println("⏱️ Tiempo de ejecución: " + tiempoEjecucion + " ms");
            System.out.println("🎯 Fitness final: " + String.format("%.2f", mejorSolucion.getFitness()));
            System.out.println("📊 Rutas planificadas: " + mejorSolucion.contarRutasPlanificadas() + "/" + pedidos.size());
            
            // Calcular porcentaje de éxito
            double porcentajeExito = (double) mejorSolucion.contarRutasPlanificadas() / pedidos.size() * 100;
            System.out.println("📈 Porcentaje de éxito: " + String.format("%.1f%%", porcentajeExito));
            System.out.println();
            
            // 5. ANÁLISIS DETALLADO
            System.out.println("🔍 ANÁLISIS DE RESULTADOS:");
            System.out.println("==========================");
            analizarResultados(mejorSolucion);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Genera pedidos usando los aeropuertos del CSV
     */
    private static List<Pedido> generarPedidosConAeropuertosCSV() {
        // Aeropuertos destino del CSV (excluyendo sedes para más realismo)
        String[] destinos = {
            "SKBO", "SEQM", "SVMI", "SBBR", "SLLP", "SCEL", "SABE", "SGAS", "SUAA",  // América del Sur
            "LATI", "EDDI", "LOWW", "UMMS", "LBSF", "LKPR", "LDZA", "EKCH", "EHAM",  // Europa
            "VIDP", "OSDI", "OERK", "OMDB", "OAKB", "OOMS", "OYSN", "OPKC", "OJAI", "LTBA", "UUDD", "ZBAA"  // Asia/Medio Oriente
        };
        
        List<Pedido> pedidos = new java.util.ArrayList<>();
        
        for (int i = 0; i < Math.min(12, destinos.length); i++) {
            String clienteId = String.format("%07d", i + 1);
            String destino = destinos[i];
            int cantidad = 50 + (i * 10); // Cantidades variadas
            int prioridad = (i % 3) + 1;   // Prioridades 1, 2, 3
            
            Pedido pedido = new Pedido(clienteId, destino, cantidad, prioridad);
            pedidos.add(pedido);
        }
        
        return pedidos;
    }
    
    /**
     * Muestra algunos pedidos de ejemplo
     */
    private static void mostrarEjemplosPedidos(List<Pedido> pedidos) {
        System.out.println("📦 PEDIDOS A PROCESAR:");
        System.out.println("----------------------");
        
        int limite = Math.min(10, pedidos.size());
        for (int i = 0; i < limite; i++) {
            Pedido p = pedidos.get(i);
            System.out.printf("  [%02d] Cliente %s → %s (Cantidad: %d, Prioridad: %d)%n", 
                           i+1, p.getClienteId(), p.getAeropuertoDestinoId(), 
                           p.getCantidadProductos(), p.getPrioridad());
        }
        
        if (pedidos.size() > limite) {
            System.out.println("  ... y " + (pedidos.size() - limite) + " pedidos más");
        }
        System.out.println();
    }
    
    /**
     * Analiza los resultados del algoritmo
     */
    private static void analizarResultados(IndividuoIntegrado solucion) {
        List<morapack.planificacion.RutaCompleta> rutas = solucion.getRutasCompletas();
        
        int rutasDirectas = 0;
        int rutasConEscalas = 0;
        int rutasSinPlanificar = 0;
        
        System.out.println("📋 DETALLE POR RUTA:");
        System.out.println("-------------------");
        
        for (int i = 0; i < rutas.size(); i++) {
            morapack.planificacion.RutaCompleta ruta = rutas.get(i);
            
            if (ruta != null) {
                String tipoRuta = ruta.getTipoRuta();
                if ("DIRECTO".equals(tipoRuta)) {
                    rutasDirectas++;
                    System.out.printf("  [%02d] ✅ DIRECTO: %s%n", i+1, ruta.obtenerDescripcion());
                } else {
                    rutasConEscalas++;
                    System.out.printf("  [%02d] 🔄 ESCALAS: %s%n", i+1, ruta.obtenerDescripcion());
                }
            } else {
                rutasSinPlanificar++;
                System.out.printf("  [%02d] ❌ Sin ruta planificada%n", i+1);
            }
        }
        
        System.out.println();
        System.out.println("📊 RESUMEN ESTADÍSTICO:");
        System.out.println("----------------------");
        System.out.println("✈️ Rutas directas: " + rutasDirectas);
        System.out.println("🔄 Rutas con escalas: " + rutasConEscalas);
        System.out.println("❌ Sin planificar: " + rutasSinPlanificar);
        System.out.println("🎯 Total procesado: " + rutas.size() + " pedidos");
        
        System.out.println();
        System.out.println("🎯 INTERPRETACIÓN:");
        System.out.println("------------------");
        System.out.println("• Las sedes (SPIM, EBCI, UBBB) son puntos de origen");
        System.out.println("• Los productos salen de las sedes hacia destinos finales");
        System.out.println("• El algoritmo busca rutas viables (directas o con escalas)");
        System.out.println("• Si no hay ruta desde la sede asignada, queda sin planificar");
        System.out.println("• El sistema optimiza tiempo total y costo de transporte");
    }
}
