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
 * Test minimo con 1 solo pedido para debugging
 */
public class MainTestMinimo {

    public static void main(String[] args) {
        System.out.println("=== TEST MINIMO: 1 PEDIDO ===");
        System.out.println();

        try {
            // Cargar red
            System.out.println("[1/4] Cargando red...");
            RedDistribucion red = new RedDistribucion();
            red.inicializar(1, 2025);
            LocalDateTime tiempoReferencia = LocalDateTime.of(2025, 1, 1, 0, 0);
            red.setTiempoReferencia(tiempoReferencia);
            System.out.println("      Red cargada: " + red.getPedidos().size() + " pedidos totales");
            System.out.println();

            // Tomar solo 1 pedido
            System.out.println("[2/4] Seleccionando 1 pedido...");
            List<Pedido> todosPedidos = new ArrayList<>(red.getPedidos().values());
            List<Pedido> pedidoPrueba = new ArrayList<>();
            pedidoPrueba.add(todosPedidos.get(0));

            Pedido p = pedidoPrueba.get(0);
            System.out.println("      Pedido: " + p.getIdPedido());
            System.out.println("      Destino: " + p.getCodigoDestino());
            System.out.println("      Productos: " + p.getCantidadProductos());
            System.out.println();

            // Crear problema
            System.out.println("[3/4] Creando problema...");
            ProblemaMoraPack problema = new ProblemaMoraPack(red, pedidoPrueba, tiempoReferencia);
            System.out.println("      Problema creado");
            System.out.println();

            // Ejecutar con 1 hormiga y 1 iteracion
            System.out.println("[4/4] Ejecutando ACO (1 hormiga, 1 iteracion)...");
            AlgoritmoColoniaHormigas algoritmo = new AlgoritmoColoniaHormigas(
                problema, 1, 1, 0.15
            );

            System.out.println("      Llamando a algoritmo.ejecutar()...");
            long inicio = System.currentTimeMillis();

            Solucion solucion = algoritmo.ejecutar();

            long duracion = System.currentTimeMillis() - inicio;
            System.out.println("      Ejecucion completada en " + duracion + " ms");
            System.out.println();

            System.out.println("=== RESULTADO ===");
            if (solucion != null) {
                SolucionMoraPack sol = (SolucionMoraPack) solucion;
                System.out.println("Fitness: " + String.format("%.2f", sol.getFitness()));
                System.out.println(sol.getEstadisticas());
            } else {
                System.out.println("No se encontro solucion");
            }

            System.out.println();
            System.out.println("=== TEST COMPLETADO ===");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
