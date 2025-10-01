package morapack.main;

import morapack.colonia.algoritmo.AlgoritmoColoniaHormigas;
import morapack.core.problema.ProblemaMoraPack;
import morapack.core.solucion.Solucion;
import morapack.core.solucion.SolucionMoraPack;
import morapack.datos.modelos.Pedido;
import morapack.datos.modelos.RedDistribucion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main de prueba con datos reducidos para debugging rapido
 */
public class MainTest {

    public static void main(String[] args) {
        System.out.println("==============================================================");
        System.out.println("          MORAPACK - TEST CON DATOS REDUCIDOS                ");
        System.out.println("==============================================================");
        System.out.println();

        try {
            // PASO 1: Cargar red
            System.out.println("=== CARGANDO RED ===");
            RedDistribucion red = new RedDistribucion();
            red.inicializar(1, 2025);
            LocalDateTime tiempoReferencia = LocalDateTime.of(2025, 1, 1, 0, 0);
            red.setTiempoReferencia(tiempoReferencia);

            System.out.println("Total pedidos en red: " + red.getPedidos().size());
            System.out.println();

            // PASO 2: Tomar solo los primeros 10 pedidos para prueba rapida
            System.out.println("=== CONFIGURANDO PROBLEMA CON 10 PEDIDOS ===");
            List<Pedido> todosPedidos = new ArrayList<>(red.getPedidos().values());
            List<Pedido> pedidosPrueba = todosPedidos.subList(0, Math.min(10, todosPedidos.size()));

            int totalProductos = pedidosPrueba.stream()
                                             .mapToInt(Pedido::getCantidadProductos)
                                             .sum();

            System.out.println("Pedidos seleccionados: " + pedidosPrueba.size());
            System.out.println("Total productos: " + totalProductos);
            System.out.println();

            // PASO 3: Crear problema con datos reducidos
            ProblemaMoraPack problema = new ProblemaMoraPack(red, pedidosPrueba, tiempoReferencia);

            // PASO 4: Ejecutar con 3 hormigas y 5 iteraciones
            System.out.println("=== EJECUTANDO ACO (3 hormigas, 5 iteraciones) ===");
            AlgoritmoColoniaHormigas algoritmo = new AlgoritmoColoniaHormigas(
                problema, 3, 5, 0.15
            );

            System.out.println("Iniciando ejecucion...");
            long inicio = System.currentTimeMillis();

            Solucion solucion = algoritmo.ejecutar();

            long duracion = System.currentTimeMillis() - inicio;

            System.out.println();
            System.out.println("=== RESULTADOS ===");
            System.out.println("Tiempo de ejecucion: " + duracion + " ms (" + (duracion/1000.0) + " segundos)");

            if (solucion != null) {
                SolucionMoraPack sol = (SolucionMoraPack) solucion;
                System.out.println("Fitness: " + String.format("%.2f", sol.getFitness()));
                System.out.println(sol.getEstadisticas());
                System.out.println(sol.getEstadisticasEntregasParciales());
            } else {
                System.out.println("No se encontro solucion");
            }

            System.out.println();
            System.out.println("==============================================================");
            System.out.println("                  TEST COMPLETADO                             ");
            System.out.println("==============================================================");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
