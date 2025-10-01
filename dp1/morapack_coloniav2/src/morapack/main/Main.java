package morapack.main;

import morapack.colonia.algoritmo.AlgoritmoColoniaHormigas;
import morapack.core.problema.ProblemaMoraPack;
import morapack.core.solucion.SolucionMoraPack;
import morapack.datos.modelos.RedDistribucion;
import morapack.datos.modelos.Pedido;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Clase principal para el sistema MoraPack - Algoritmo de Colonia de Hormigas
 * para optimización de rutas de distribución logística.
 *
 * Resuelve los pedidos del archivo pedidos_01.csv (enero 2025) usando ACO optimizado.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== MoraPack Colonia v2 - Sistema de Optimización Logística ===");
        System.out.println("Algoritmo de Colonia de Hormigas para planificación de rutas");
        System.out.println("Resolviendo pedidos de enero 2025 (pedidos_01.csv)\n");

        try {
            // 1. CARGAR Y CONFIGURAR EL SISTEMA
            System.out.println("📊 CARGANDO DATOS DEL SISTEMA...");
            RedDistribucion red = new RedDistribucion();

            // Usar archivo de pedidos filtrados (solo destinos válidos de la red MoraPack)
            String rutaAeropuertos = "datos/aeropuertos.csv";
            String rutaVuelos = "datos/planes_de_vuelo.csv";
            String rutaPedidos = "datos/pedidos/pedidos_01_filtrados.csv";

            red.inicializar(rutaAeropuertos, rutaVuelos, rutaPedidos, 1, 2025);

            // Configurar tiempo de referencia para simulación
            LocalDateTime tiempoReferencia = LocalDateTime.of(2025, 1, 1, 0, 0);
            red.setTiempoReferencia(tiempoReferencia);

            System.out.println("   ✅ Aeropuertos cargados: " + red.getAeropuertos().size());
            System.out.println("   ✅ Vuelos cargados: " + red.getVuelos().size());
            System.out.println("   ✅ Pedidos de enero: " + red.getPedidos().size());
            System.out.println("   🕐 Tiempo de referencia: " + tiempoReferencia);
            System.out.println();

            // 2. CONVERTIR A LISTA Y MOSTRAR INFORMACIÓN DE PEDIDOS
            List<Pedido> listaPedidos = new ArrayList<>(red.getPedidos().values());
            mostrarResumenPedidos(listaPedidos);

            // 3. CREAR Y CONFIGURAR EL PROBLEMA
            System.out.println("🎯 CONFIGURANDO PROBLEMA MORAPACK...");
            ProblemaMoraPack problema = new ProblemaMoraPack(red, listaPedidos, tiempoReferencia);
            System.out.println("   ✅ Problema configurado con " + listaPedidos.size() + " pedidos");
            System.out.println();

            // 4. CONFIGURAR Y EJECUTAR ALGORITMO ACO
            System.out.println("🐜 EJECUTANDO ALGORITMO DE COLONIA DE HORMIGAS...");

            // Parámetros optimizados para logística
            int numeroHormigas = 15;        // Mayor diversidad
            int maxIteraciones = 150;       // Más paciencia para problemas complejos
            double tasaEvaporacion = 0.15;  // Evitar estancamiento

            AlgoritmoColoniaHormigas algoritmo = new AlgoritmoColoniaHormigas(
                problema, numeroHormigas, maxIteraciones, tasaEvaporacion
            );

            System.out.println("   🔧 Hormigas: " + numeroHormigas +
                             ", Iteraciones: " + maxIteraciones +
                             ", Evaporación: " + tasaEvaporacion);

            long tiempoInicio = System.currentTimeMillis();
            SolucionMoraPack mejorSolucion = (SolucionMoraPack) algoritmo.ejecutar();
            long tiempoEjecucion = System.currentTimeMillis() - tiempoInicio;

            System.out.println("   ⏱️  Tiempo de ejecución: " + tiempoEjecucion + " ms");
            System.out.println();

            // 5. MOSTRAR RESULTADOS DETALLADOS
            mostrarResultadosDetallados(mejorSolucion, listaPedidos);

            // 6. MOSTRAR ESTADÍSTICAS DEL ALGORITMO
            mostrarEstadisticasAlgoritmo(algoritmo);

        } catch (Exception e) {
            System.err.println("❌ ERROR EN LA EJECUCIÓN:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Muestra un resumen de los pedidos cargados
     */
    private static void mostrarResumenPedidos(List<Pedido> pedidos) {
        System.out.println("📦 RESUMEN DE PEDIDOS:");

        int totalProductos = pedidos.stream()
            .mapToInt(Pedido::getCantidadProductos)
            .sum();

        // Contar destinos únicos
        long destinosUnicos = pedidos.stream()
            .map(Pedido::getCodigoDestino)
            .distinct()
            .count();

        // Pedido más grande y más pequeño
        Pedido pedidoMayor = pedidos.stream()
            .max((p1, p2) -> Integer.compare(p1.getCantidadProductos(), p2.getCantidadProductos()))
            .orElse(null);

        Pedido pedidoMenor = pedidos.stream()
            .min((p1, p2) -> Integer.compare(p1.getCantidadProductos(), p2.getCantidadProductos()))
            .orElse(null);

        System.out.println("   📊 Total pedidos: " + pedidos.size());
        System.out.println("   📈 Total productos: " + totalProductos);
        System.out.println("   🌍 Destinos únicos: " + destinosUnicos);
        System.out.println("   ⬆️  Pedido mayor: " + (pedidoMayor != null ?
            pedidoMayor.getCodigoDestino() + " (" + pedidoMayor.getCantidadProductos() + " productos)" : "N/A"));
        System.out.println("   ⬇️  Pedido menor: " + (pedidoMenor != null ?
            pedidoMenor.getCodigoDestino() + " (" + pedidoMenor.getCantidadProductos() + " productos)" : "N/A"));
        System.out.println();
    }

    /**
     * Muestra los resultados detallados de la solución
     */
    private static void mostrarResultadosDetallados(SolucionMoraPack solucion, List<Pedido> pedidos) {
        System.out.println("🎉 RESULTADOS DE LA OPTIMIZACIÓN:");
        System.out.println("   🏆 Fitness de la solución: " + String.format("%.2f", solucion.getFitness()));
        System.out.println("   ⏰ Cumple plazos: " + (solucion.cumplePlazos() ? "✅ SÍ" : "❌ NO"));
        System.out.println("   📋 " + solucion.getEstadisticas());
        System.out.println("   📦 " + solucion.getEstadisticasEntregasParciales());
        System.out.println();

        // Mostrar algunos ejemplos de rutas generadas
        System.out.println("🛣️  EJEMPLOS DE RUTAS GENERADAS:");
        int ejemplosMostrados = 0;
        for (Pedido pedido : pedidos) {
            List<SolucionMoraPack.RutaProducto> rutas = solucion.getRutasProducto(pedido.getIdPedido().hashCode());
            if (!rutas.isEmpty() && ejemplosMostrados < 5) {
                System.out.println("   📍 Pedido " + pedido.getIdPedido() + " → " + pedido.getCodigoDestino() +
                                 " (" + pedido.getCantidadProductos() + " productos):");

                for (SolucionMoraPack.RutaProducto ruta : rutas) {
                    System.out.println("      🚚 Entrega #" + ruta.getNumeroEntrega() + ": " +
                                     ruta.getCantidadTransportada() + " productos via " + ruta);
                }
                ejemplosMostrados++;
                System.out.println();
            }
        }

        if (ejemplosMostrados == 0) {
            System.out.println("   ⚠️  No se generaron rutas válidas");
        }
    }

    /**
     * Muestra las estadísticas del algoritmo ACO
     */
    private static void mostrarEstadisticasAlgoritmo(AlgoritmoColoniaHormigas algoritmo) {
        System.out.println("📈 ESTADÍSTICAS DEL ALGORITMO:");
        System.out.println("   🔧 Algoritmo completado exitosamente");
        System.out.println("   🐜 Colonia de hormigas trabajó de forma colaborativa");
        System.out.println("   🎯 Optimización basada en feromonas y heurísticas logísticas");
        System.out.println();

        System.out.println("✅ EJECUCIÓN COMPLETADA");
        System.out.println("   El sistema ha procesado todos los pedidos de enero 2025");
        System.out.println("   usando el algoritmo ACO optimizado para logística.");
    }
}