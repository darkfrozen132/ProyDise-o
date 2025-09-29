package morapack.datos.modelos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculadora de métricas del sistema MoraPack para monitoreo y detección de colapso.
 */
public class MetricasSistema {

    /**
     * Calcula la utilización promedio de capacidad de vuelos
     * @param vuelos Lista de vuelos del sistema
     * @return Porcentaje de utilización (0-100)
     */
    public static double calcularUtilizacionVuelos(List<Vuelo> vuelos) {
        if (vuelos.isEmpty()) {
            return 0.0;
        }

        double utilizacionTotal = vuelos.stream()
            .mapToDouble(Vuelo::getPorcentajeOcupacion)
            .sum();

        return utilizacionTotal / vuelos.size();
    }

    /**
     * Calcula la utilización promedio de capacidad de aeropuertos
     * @param aeropuertos Lista de aeropuertos del sistema
     * @return Porcentaje de utilización (0-100)
     */
    public static double calcularUtilizacionAeropuertos(List<Aeropuerto> aeropuertos) {
        if (aeropuertos.isEmpty()) {
            return 0.0;
        }

        double utilizacionTotal = aeropuertos.stream()
            .mapToDouble(Aeropuerto::getPorcentajeOcupacion)
            .sum();

        return utilizacionTotal / aeropuertos.size();
    }

    /**
     * Calcula la tasa de pedidos retrasados
     * @param pedidos Lista de pedidos a evaluar
     * @param tiempoActualUTC Tiempo actual en UTC
     * @param red Red de distribución para obtener aeropuertos
     * @return Porcentaje de pedidos retrasados (0-100)
     */
    public static double calcularTasaRetrasos(List<Pedido> pedidos, LocalDateTime tiempoActualUTC,
                                            RedDistribucion red) {
        if (pedidos.isEmpty()) {
            return 0.0;
        }

        long pedidosRetrasados = pedidos.stream()
            .filter(pedido -> {
                Aeropuerto destino = red.getAeropuerto(pedido.getCodigoDestino());
                return destino != null && !pedido.estaDentroPlazoUTC(tiempoActualUTC, destino);
            })
            .count();

        return (double) pedidosRetrasados / pedidos.size() * 100.0;
    }

    /**
     * Calcula el porcentaje de conectividad de la red
     * @param red Red de distribución
     * @return Porcentaje de rutas factibles (0-100)
     */
    public static double calcularConectividadRed(RedDistribucion red) {
        Map<String, Aeropuerto> aeropuertos = red.getAeropuertos();
        List<String> sedes = aeropuertos.values().stream()
            .filter(Aeropuerto::esSedePrincipal)
            .map(Aeropuerto::getCodigoICAO)
            .collect(Collectors.toList());

        List<String> destinos = aeropuertos.values().stream()
            .filter(aeropuerto -> !aeropuerto.esSedePrincipal())
            .map(Aeropuerto::getCodigoICAO)
            .collect(Collectors.toList());

        if (sedes.isEmpty() || destinos.isEmpty()) {
            return 0.0;
        }

        int rutasPosibles = sedes.size() * destinos.size();
        int rutasFactibles = 0;

        for (String sede : sedes) {
            for (String destino : destinos) {
                if (red.buscarRutaMinima(sede, destino) != null) {
                    rutasFactibles++;
                }
            }
        }

        return (double) rutasFactibles / rutasPosibles * 100.0;
    }

    /**
     * Estima el tiempo hasta colapso basado en tendencias actuales
     * @param utilizacionVuelos Utilización actual de vuelos (%)
     * @param utilizacionAeropuertos Utilización actual de aeropuertos (%)
     * @param tasaRetrasos Tasa actual de retrasos (%)
     * @return Horas estimadas hasta colapso, -1 si no hay riesgo inmediato
     */
    public static long estimarTiempoHastaColapso(double utilizacionVuelos,
                                               double utilizacionAeropuertos,
                                               double tasaRetrasos) {
        // Algoritmo simple de estimación
        double riesgoTotal = (utilizacionVuelos + utilizacionAeropuertos + tasaRetrasos * 2) / 4.0;

        if (riesgoTotal < 70.0) {
            return -1; // No hay riesgo inmediato
        }

        if (riesgoTotal >= 95.0) {
            return 0; // Colapso inminente
        }

        // Estimación lineal: a mayor riesgo, menos tiempo hasta colapso
        // Rango: 70% riesgo = 48 horas, 95% riesgo = 0 horas
        double factorTiempo = (95.0 - riesgoTotal) / (95.0 - 70.0);
        return (long) (factorTiempo * 48.0);
    }

    /**
     * Genera un reporte completo de métricas del sistema
     * @param red Red de distribución
     * @param pedidos Lista de pedidos activos
     * @param tiempoActualUTC Tiempo actual en UTC
     * @return String con el reporte completo
     */
    public static String generarReporteCompleto(RedDistribucion red, List<Pedido> pedidos,
                                              LocalDateTime tiempoActualUTC) {
        // Calcular métricas principales
        List<Vuelo> vuelos = red.getVuelos().values().stream().collect(Collectors.toList());
        List<Aeropuerto> aeropuertos = red.getAeropuertos().values().stream().collect(Collectors.toList());

        double utilizacionVuelos = calcularUtilizacionVuelos(vuelos);
        double utilizacionAeropuertos = calcularUtilizacionAeropuertos(aeropuertos);
        double tasaRetrasos = calcularTasaRetrasos(pedidos, tiempoActualUTC, red);
        double conectividad = calcularConectividadRed(red);
        long tiempoHastaColapso = estimarTiempoHastaColapso(utilizacionVuelos, utilizacionAeropuertos, tasaRetrasos);

        // Detectar condiciones de colapso
        List<ValidadorColapso.CondicionColapso> condiciones =
            ValidadorColapso.verificarSistemaCompleto(red, pedidos, tiempoActualUTC);

        // Determinar estado del sistema
        String estadoSistema;
        if (condiciones.isEmpty()) {
            estadoSistema = "🟢 OPERATIVO";
        } else if (ValidadorColapso.sistemaHaColapsado(condiciones, 0.3)) {
            estadoSistema = "🔴 EN COLAPSO";
        } else {
            estadoSistema = "🟡 EN RIESGO";
        }

        return String.format("""
            ╔════════════════════════════════════════════════════════════════╗
            ║                    REPORTE MÉTRICAS SISTEMA MORAPACK                    ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ Estado: %-50s ║
            ║ Fecha/Hora: %-45s ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ MÉTRICAS PRINCIPALES:                                          ║
            ║ • Utilización vuelos:      %6.2f%%                             ║
            ║ • Utilización aeropuertos: %6.2f%%                             ║
            ║ • Tasa de retrasos:        %6.2f%%                             ║
            ║ • Conectividad de red:     %6.2f%%                             ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ ANÁLISIS DE RIESGO:                                            ║
            ║ • Condiciones de colapso:  %6d                                 ║
            ║ • Tiempo hasta colapso:    %-30s ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ RECURSOS:                                                      ║
            ║ • Total aeropuertos:       %6d                                 ║
            ║ • Total vuelos:            %6d                                 ║
            ║ • Total pedidos:           %6d                                 ║
            ╚════════════════════════════════════════════════════════════════╝

            %s
            """,
            estadoSistema,
            tiempoActualUTC.toString(),
            utilizacionVuelos,
            utilizacionAeropuertos,
            tasaRetrasos,
            conectividad,
            condiciones.size(),
            tiempoHastaColapso == -1 ? "Sin riesgo inmediato" :
                tiempoHastaColapso == 0 ? "INMEDIATO" :
                tiempoHastaColapso + " horas",
            aeropuertos.size(),
            vuelos.size(),
            pedidos.size(),
            ValidadorColapso.calcularMetricasColapso(condiciones, pedidos.size())
        );
    }

    /**
     * Genera un reporte simplificado para monitoreo en tiempo real
     * @param red Red de distribución
     * @param pedidos Lista de pedidos activos
     * @param tiempoActualUTC Tiempo actual en UTC
     * @return String con el reporte simplificado
     */
    public static String generarReporteSimple(RedDistribucion red, List<Pedido> pedidos,
                                            LocalDateTime tiempoActualUTC) {
        double tasaRetrasos = calcularTasaRetrasos(pedidos, tiempoActualUTC, red);
        List<ValidadorColapso.CondicionColapso> condiciones =
            ValidadorColapso.verificarSistemaCompleto(red, pedidos, tiempoActualUTC);

        String estado = condiciones.isEmpty() ? "OPERATIVO" :
                       ValidadorColapso.sistemaHaColapsado(condiciones, 0.3) ? "COLAPSO" : "RIESGO";

        return String.format("[%s] Estado: %s | Retrasos: %.1f%% | Problemas: %d",
            tiempoActualUTC.toString().substring(0, 19), estado, tasaRetrasos, condiciones.size());
    }
}