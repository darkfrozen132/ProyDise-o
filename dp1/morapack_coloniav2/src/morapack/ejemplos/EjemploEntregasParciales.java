package morapack.ejemplos;

import morapack.core.solucion.SolucionMoraPack;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ejemplo que demuestra el nuevo modelo híbrido con entregas parciales.
 *
 * Escenario: Un pedido de 145 productos se entrega en 3 partes:
 * - 60 productos via ruta directa (SPIM→SEQM)
 * - 45 productos via ruta con escala (SPIM→SBBR→SEQM)
 * - 40 productos via sede europea (EBCI→SEQM)
 */
public class EjemploEntregasParciales {

    public static void main(String[] args) {
        System.out.println("=== DEMOSTRACIÓN: MODELO HÍBRIDO CON ENTREGAS PARCIALES ===\n");

        // Crear solución con modelo híbrido
        SolucionMoraPack solucion = new SolucionMoraPack();

        // Ejemplo: Pedido 12345 solicita 145 productos a SEQM
        int idPedido = 12345;
        int cantidadTotal = 145;

        System.out.println("📦 PEDIDO ORIGINAL:");
        System.out.println("   ID: " + idPedido);
        System.out.println("   Destino: SEQM (Quito, Ecuador)");
        System.out.println("   Cantidad: " + cantidadTotal + " productos MPE");
        System.out.println("   Plazo: 2 días (mismo continente)\n");

        // ENTREGA 1: 60 productos vía ruta directa
        List<SolucionMoraPack.SegmentoVuelo> segmentos1 = Arrays.asList(
            new SolucionMoraPack.SegmentoVuelo("SPIM-SEQM-0334", "SPIM", "SEQM",
                LocalDateTime.of(2025, 1, 1, 3, 34),
                LocalDateTime.of(2025, 1, 1, 5, 21))
        );

        SolucionMoraPack.RutaProducto entrega1 = new SolucionMoraPack.RutaProducto(
            idPedido, 60, cantidadTotal, 1, true, // 60/145, entrega #1, es parcial
            "SPIM", "SEQM", segmentos1,
            LocalDateTime.of(2025, 1, 1, 3, 34),
            LocalDateTime.of(2025, 1, 1, 5, 21),
            true
        );

        // ENTREGA 2: 45 productos vía ruta con escala
        List<SolucionMoraPack.SegmentoVuelo> segmentos2 = Arrays.asList(
            new SolucionMoraPack.SegmentoVuelo("SPIM-SBBR-0800", "SPIM", "SBBR",
                LocalDateTime.of(2025, 1, 1, 8, 0),
                LocalDateTime.of(2025, 1, 1, 12, 30)),
            new SolucionMoraPack.SegmentoVuelo("SBBR-SEQM-1430", "SBBR", "SEQM",
                LocalDateTime.of(2025, 1, 1, 14, 30),
                LocalDateTime.of(2025, 1, 1, 18, 15))
        );

        SolucionMoraPack.RutaProducto entrega2 = new SolucionMoraPack.RutaProducto(
            idPedido, 45, cantidadTotal, 2, true, // 45/145, entrega #2, es parcial
            "SPIM", "SEQM", segmentos2,
            LocalDateTime.of(2025, 1, 1, 8, 0),
            LocalDateTime.of(2025, 1, 1, 18, 15),
            true
        );

        // ENTREGA 3: 40 productos vía sede europea
        List<SolucionMoraPack.SegmentoVuelo> segmentos3 = Arrays.asList(
            new SolucionMoraPack.SegmentoVuelo("EBCI-SEQM-2200", "EBCI", "SEQM",
                LocalDateTime.of(2025, 1, 2, 22, 0),
                LocalDateTime.of(2025, 1, 3, 6, 45))
        );

        SolucionMoraPack.RutaProducto entrega3 = new SolucionMoraPack.RutaProducto(
            idPedido, 40, cantidadTotal, 3, true, // 40/145, entrega #3, es parcial
            "EBCI", "SEQM", segmentos3,
            LocalDateTime.of(2025, 1, 2, 22, 0),
            LocalDateTime.of(2025, 1, 3, 6, 45),
            true
        );

        // Agregar todas las entregas a la solución
        solucion.agregarRutaProducto(idPedido, entrega1);
        solucion.agregarRutaProducto(idPedido, entrega2);
        solucion.agregarRutaProducto(idPedido, entrega3);

        // Mostrar resultados
        System.out.println("🚚 PLAN DE ENTREGAS PARCIALES:");
        System.out.println("   Total de entregas: " + solucion.getRutasProducto(idPedido).size());
        System.out.println("   Pedido completo: " + solucion.pedidoCompleto(idPedido));
        System.out.println("   Cumple plazos: " + solucion.pedidoCumplePlazo(idPedido));
        System.out.println();

        // Detalles de cada entrega
        List<SolucionMoraPack.RutaProducto> entregas = solucion.getRutasProducto(idPedido);
        for (SolucionMoraPack.RutaProducto entrega : entregas) {
            System.out.println("   📦 Entrega #" + entrega.getNumeroEntrega() + ":");
            System.out.println("      Cantidad: " + entrega.getCantidadTransportada() + "/" +
                             entrega.getCantidadTotalPedido() + " productos");
            System.out.println("      Porcentaje: " + String.format("%.1f%%", entrega.porcentajeCompletado() * 100));
            System.out.println("      Ruta: " + entrega);
            System.out.println("      Estado: " + (entrega.cumplePlazo() ? "✅ A tiempo" : "❌ Retrasado"));
            System.out.println();
        }

        // Estadísticas generales
        System.out.println("📊 ESTADÍSTICAS DE LA SOLUCIÓN:");
        System.out.println("   " + solucion.getEstadisticas());
        System.out.println("   " + solucion.getEstadisticasEntregasParciales());
        System.out.println();

        // Demostrar uso de métodos nuevos
        System.out.println("🔄 ACCESO A RUTAS INDIVIDUALES:");
        List<SolucionMoraPack.RutaProducto> todasLasRutas = solucion.getRutasProducto(idPedido);
        if (!todasLasRutas.isEmpty()) {
            SolucionMoraPack.RutaProducto primeraRuta = todasLasRutas.get(0);
            System.out.println("   Primera ruta: " + primeraRuta);
            System.out.println("   Cantidad transportada: " + primeraRuta.getCantidadTransportada());
        }
        System.out.println();

        System.out.println("✅ DEMOSTRACIÓN COMPLETADA");
        System.out.println("   El sistema ahora soporta entregas parciales múltiples");
        System.out.println("   usando la nueva API sin métodos deprecated.");
    }
}