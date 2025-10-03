package morapack.ejemplos;

import morapack.datos.modelos.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Ejemplo que demuestra cómo el sistema considera que los planes de vuelo
 * se repiten diariamente durante todo el mes.
 *
 * Este ejemplo muestra:
 * 1. Un vuelo SKBO->SEQM con capacidad 300 productos
 * 2. El mismo vuelo disponible TODOS LOS DÍAS del mes (31 días)
 * 3. Capacidad total: 300 × 31 = 9,300 productos en el mes
 */
public class EjemploVuelosDiarios {

    public static void main(String[] args) {
        System.out.println("==============================================================");
        System.out.println("        EJEMPLO: VUELOS DIARIOS EN MORAPACK                   ");
        System.out.println("==============================================================");
        System.out.println();

        try {
            // Cargar la red de distribución
            System.out.println("=== PASO 1: CARGANDO RED DE DISTRIBUCIÓN ===");
            RedDistribucion red = new RedDistribucion();
            red.inicializar(1, 2025); // Enero 2025
            System.out.println("Red cargada exitosamente");
            System.out.println();

            // Ejemplo: Vuelo SKBO->SEQM
            String origen = "SKBO";
            String destino = "SEQM";

            System.out.println("=== PASO 2: ANALIZANDO VUELO " + origen + " -> " + destino + " ===");
            System.out.println();

            // Buscar plantilla del vuelo
            List<Vuelo> plantillas = red.buscarVuelosDirectos(origen, destino);
            if (plantillas.isEmpty()) {
                System.out.println("No se encontraron vuelos directos entre " + origen + " y " + destino);
                return;
            }

            Vuelo plantilla = plantillas.get(0);
            System.out.println("Plantilla de vuelo encontrada:");
            System.out.println("  Ruta: " + plantilla.getAeropuertoOrigen() + " -> " + plantilla.getAeropuertoDestino());
            System.out.println("  Horario: " + plantilla.getHoraSalida() + " - " + plantilla.getHoraLlegada());
            System.out.println("  Capacidad por vuelo: " + plantilla.getCapacidadMaxima() + " productos");
            System.out.println();

            // Buscar instancias diarias
            LocalDate fechaInicio = red.getFechaInicioMes();
            LocalDate fechaFin = red.getFechaFinMes().plusDays(1);

            System.out.println("=== PASO 3: BUSCANDO INSTANCIAS DIARIAS ===");
            System.out.println("Mes de operación: Enero 2025");
            System.out.println("Días en el mes: " + red.getDiasDelMes());
            System.out.println();

            List<VueloInstancia> instancias = red.buscarVuelosDisponiblesEnRango(origen, destino, fechaInicio, fechaFin);

            System.out.println("Instancias encontradas: " + instancias.size());
            System.out.println();

            // Mostrar algunas instancias de ejemplo
            System.out.println("=== EJEMPLOS DE INSTANCIAS (primeras 5 y últimas 5) ===");
            System.out.println();

            int count = 0;
            int capacidadTotal = 0;

            for (int i = 0; i < Math.min(5, instancias.size()); i++) {
                VueloInstancia inst = instancias.get(i);
                System.out.println("Día " + inst.getFecha().getDayOfMonth() + ":");
                System.out.println("  ID Instancia: " + inst.getIdInstancia());
                System.out.println("  Salida: " + inst.getHorarioSalidaCompleto());
                System.out.println("  Llegada: " + inst.getHorarioLlegadaCompleto());
                System.out.println("  Capacidad: " + inst.getCapacidadDisponible() + " productos");
                System.out.println();
                capacidadTotal += inst.getCapacidadDisponible();
                count++;
            }

            if (instancias.size() > 10) {
                System.out.println("... (días " + (count + 1) + " a " + (instancias.size() - 5) + " omitidos) ...");
                System.out.println();

                for (int i = instancias.size() - 5; i < instancias.size(); i++) {
                    VueloInstancia inst = instancias.get(i);
                    System.out.println("Día " + inst.getFecha().getDayOfMonth() + ":");
                    System.out.println("  ID Instancia: " + inst.getIdInstancia());
                    System.out.println("  Salida: " + inst.getHorarioSalidaCompleto());
                    System.out.println("  Llegada: " + inst.getHorarioLlegadaCompleto());
                    System.out.println("  Capacidad: " + inst.getCapacidadDisponible() + " productos");
                    System.out.println();
                    capacidadTotal += inst.getCapacidadDisponible();
                    count++;
                }

                // Calcular capacidad de días omitidos
                int diasOmitidos = instancias.size() - count;
                capacidadTotal += diasOmitidos * plantilla.getCapacidadMaxima();
            }

            // Resumen final
            System.out.println("=== PASO 4: RESUMEN DE CAPACIDAD ===");
            System.out.println();
            System.out.println("Capacidad por vuelo: " + plantilla.getCapacidadMaxima() + " productos");
            System.out.println("Número de instancias (días): " + instancias.size());
            System.out.println("Capacidad total del mes: " + capacidadTotal + " productos");
            System.out.println();
            System.out.println("Cálculo: " + plantilla.getCapacidadMaxima() + " × " + instancias.size() + " = " + capacidadTotal);
            System.out.println();

            // Demostración: reservar capacidad en días específicos
            System.out.println("=== PASO 5: DEMOSTRACIÓN DE RESERVAS ===");
            System.out.println();

            VueloInstancia dia1 = instancias.get(0);
            VueloInstancia dia15 = instancias.get(14);
            VueloInstancia dia30 = instancias.get(29);

            System.out.println("Reservando capacidad en días específicos:");
            System.out.println();

            // Reservar en día 1
            int cantidadDia1 = 250;
            if (dia1.reservarCapacidad(cantidadDia1)) {
                System.out.println("Día 1: Reservados " + cantidadDia1 + " productos");
                System.out.println("  Capacidad restante: " + dia1.getCapacidadDisponible());
            }

            // Reservar en día 15
            int cantidadDia15 = 200;
            if (dia15.reservarCapacidad(cantidadDia15)) {
                System.out.println("Día 15: Reservados " + cantidadDia15 + " productos");
                System.out.println("  Capacidad restante: " + dia15.getCapacidadDisponible());
            }

            // Reservar en día 30
            int cantidadDia30 = 180;
            if (dia30.reservarCapacidad(cantidadDia30)) {
                System.out.println("Día 30: Reservados " + cantidadDia30 + " productos");
                System.out.println("  Capacidad restante: " + dia30.getCapacidadDisponible());
            }

            System.out.println();
            System.out.println("Verificación:");
            System.out.println("  Día 1 - Ocupación: " + String.format("%.1f%%", dia1.getPorcentajeOcupacion()));
            System.out.println("  Día 15 - Ocupación: " + String.format("%.1f%%", dia15.getPorcentajeOcupacion()));
            System.out.println("  Día 30 - Ocupación: " + String.format("%.1f%%", dia30.getPorcentajeOcupacion()));
            System.out.println();

            // Mostrar estadísticas de instancias
            System.out.println("=== PASO 6: ESTADÍSTICAS DEL SISTEMA ===");
            System.out.println();
            System.out.println(red.getEstadisticasInstancias());
            System.out.println();

            System.out.println("==============================================================");
            System.out.println("                    CONCLUSIÓN                                ");
            System.out.println("==============================================================");
            System.out.println();
            System.out.println("El sistema AHORA considera correctamente que:");
            System.out.println("1. Cada plan de vuelo se repite DIARIAMENTE");
            System.out.println("2. Cada día tiene su propia capacidad independiente");
            System.out.println("3. Un vuelo del día 1 NO afecta la capacidad del día 2");
            System.out.println("4. Capacidad total = capacidad_vuelo × días_del_mes");
            System.out.println();
            System.out.println("Esto permite al algoritmo ACO:");
            System.out.println("- Distribuir pedidos a lo largo del mes");
            System.out.println("- Usar el mismo vuelo en diferentes días");
            System.out.println("- Maximizar el uso de la capacidad disponible");
            System.out.println("- Evitar falsas detecciones de colapso del sistema");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
