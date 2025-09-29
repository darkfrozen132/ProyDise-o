package morapack.ejemplos;

import morapack.datos.modelos.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Ejemplo de uso del sistema de validación de colapso de MoraPack.
 * Demuestra cómo detectar y reportar condiciones críticas del sistema.
 */
public class EjemploValidacionColapso {

    public static void main(String[] args) {
        System.out.println("=== EJEMPLO: VALIDACIÓN DE COLAPSO SISTEMA MORAPACK ===\n");

        try {
            // Simular inicialización del sistema
            RedDistribucion red = new RedDistribucion();
            red.inicializar(1, 2025); // Enero 2025

            // Obtener pedidos del sistema
            List<Pedido> pedidos = new ArrayList<>(red.getPedidos().values());
            LocalDateTime tiempoActual = LocalDateTime.of(2025, 1, 15, 12, 0); // 15 enero 12:00

            System.out.println("🔍 ANÁLISIS DEL SISTEMA:");
            System.out.printf("Pedidos cargados: %d%n", pedidos.size());
            System.out.printf("Tiempo de análisis: %s%n%n", tiempoActual);

            // 1. Verificación completa del sistema
            System.out.println("📊 VERIFICACIÓN COMPLETA DEL SISTEMA:");
            List<ValidadorColapso.CondicionColapso> condiciones =
                ValidadorColapso.verificarSistemaCompleto(red, pedidos, tiempoActual);

            if (condiciones.isEmpty()) {
                System.out.println("✅ Sistema operativo - No se detectaron condiciones de colapso");
            } else {
                System.out.printf("⚠️  Se detectaron %d condiciones de colapso:%n", condiciones.size());
                for (ValidadorColapso.CondicionColapso condicion : condiciones) {
                    System.out.println("   " + condicion);
                }
            }

            // 2. Reporte de métricas completo
            System.out.println("\n" + MetricasSistema.generarReporteCompleto(red, pedidos, tiempoActual));

            // 3. Ejemplos específicos de validación
            System.out.println("🧪 EJEMPLOS ESPECÍFICOS DE VALIDACIÓN:\n");

            // Ejemplo 1: Verificar un pedido específico
            if (!pedidos.isEmpty()) {
                Pedido pedidoEjemplo = pedidos.get(0);
                Aeropuerto destino = red.getAeropuerto(pedidoEjemplo.getCodigoDestino());

                System.out.printf("📦 Verificando pedido: %s%n", pedidoEjemplo.getIdPedido());
                ValidadorColapso.CondicionColapso retraso =
                    ValidadorColapso.verificarRetrasopedido(pedidoEjemplo, tiempoActual, destino);

                if (retraso != null) {
                    System.out.println("❌ " + retraso.getDescripcion());
                } else {
                    System.out.println("✅ Pedido dentro del plazo");
                }
            }

            // Ejemplo 2: Verificar capacidad de vuelos
            List<Vuelo> vuelos = new ArrayList<>(red.getVuelos().values());
            if (!vuelos.isEmpty()) {
                Vuelo vueloEjemplo = vuelos.get(0);
                int cantidadTest = 500; // Cantidad que excede la capacidad normal

                System.out.printf("%n✈️  Verificando vuelo: %s (capacidad: %d)%n",
                    vueloEjemplo.getIdVuelo(), vueloEjemplo.getCapacidadMaxima());

                ValidadorColapso.CondicionColapso capacidadVuelo =
                    ValidadorColapso.verificarCapacidadVuelo(vueloEjemplo, cantidadTest);

                if (capacidadVuelo != null) {
                    System.out.println("❌ " + capacidadVuelo.getDescripcion());
                } else {
                    System.out.printf("✅ Vuelo puede transportar %d productos%n", cantidadTest);
                }
            }

            // Ejemplo 3: Métricas en tiempo real
            System.out.println("\n📈 MONITOREO EN TIEMPO REAL:");
            String reporteSimple = MetricasSistema.generarReporteSimple(red, pedidos, tiempoActual);
            System.out.println(reporteSimple);

            // 4. Simulación de sobrecarga
            System.out.println("\n🔥 SIMULACIÓN DE SOBRECARGA:");
            simularSobrecargaSistema(red, pedidos, tiempoActual);

        } catch (Exception e) {
            System.err.println("❌ Error durante la validación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Simula una sobrecarga del sistema para demostrar la detección de colapso
     */
    private static void simularSobrecargaSistema(RedDistribucion red, List<Pedido> pedidos,
                                               LocalDateTime tiempoActual) {
        System.out.println("Simulando sobrecarga del sistema...");

        // Simular vuelos llenos
        List<Vuelo> vuelos = new ArrayList<>(red.getVuelos().values());
        int vuelosModificados = 0;
        for (Vuelo vuelo : vuelos) {
            if (vuelosModificados < 10) { // Llenar solo algunos vuelos para la demo
                vuelo.reservarCapacidad(vuelo.getCapacidadDisponible());
                vuelosModificados++;
            }
        }

        // Verificar impacto
        List<ValidadorColapso.CondicionColapso> condicionesColapso =
            ValidadorColapso.verificarSistemaCompleto(red, pedidos, tiempoActual);

        System.out.printf("Después de la sobrecarga: %d condiciones críticas detectadas%n",
            condicionesColapso.size());

        // Mostrar métricas actualizadas
        double utilizacionVuelos = MetricasSistema.calcularUtilizacionVuelos(vuelos);
        System.out.printf("Utilización de vuelos: %.2f%%%n", utilizacionVuelos);

        boolean haColapsado = ValidadorColapso.sistemaHaColapsado(condicionesColapso, 0.3);
        System.out.printf("Estado del sistema: %s%n",
            haColapsado ? "🔴 EN COLAPSO" : "🟡 EN RIESGO");

        // Restaurar capacidades para no afectar otros ejemplos
        red.reiniciarCapacidades();
        System.out.println("✅ Capacidades restauradas");
    }
}