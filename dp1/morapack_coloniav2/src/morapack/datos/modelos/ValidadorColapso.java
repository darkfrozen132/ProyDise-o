package morapack.datos.modelos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Validador de condiciones de colapso del sistema MoraPack.
 * Detecta cuando el sistema no puede cumplir con los requerimientos establecidos.
 */
public class ValidadorColapso {

    /**
     * Enumera los tipos de colapso posibles en el sistema
     */
    public enum TipoColapso {
        VIOLACION_PLAZO("Violación de plazos de entrega"),
        CAPACIDAD_VUELO_EXCEDIDA("Capacidad de vuelos excedida"),
        CAPACIDAD_AEROPUERTO_EXCEDIDA("Capacidad de aeropuertos excedida"),
        RUTA_INVALIDA("Ruta inválida o inexistente"),
        DESCONEXION_RED("Desconexión de la red de distribución");

        private final String descripcion;

        TipoColapso(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    /**
     * Representa una condición de colapso detectada
     */
    public static class CondicionColapso {
        private final TipoColapso tipo;
        private final String descripcion;
        private final String entidadAfectada;
        private final LocalDateTime momentoDeteccion;

        public CondicionColapso(TipoColapso tipo, String descripcion, String entidadAfectada) {
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.entidadAfectada = entidadAfectada;
            this.momentoDeteccion = LocalDateTime.now();
        }

        public TipoColapso getTipo() { return tipo; }
        public String getDescripcion() { return descripcion; }
        public String getEntidadAfectada() { return entidadAfectada; }
        public LocalDateTime getMomentoDeteccion() { return momentoDeteccion; }

        @Override
        public String toString() {
            return String.format("[%s] %s - %s (Detectado: %s)",
                tipo.name(), entidadAfectada, descripcion, momentoDeteccion);
        }
    }

    /**
     * Verifica si un pedido está retrasado
     * @param pedido Pedido a verificar
     * @param tiempoActualUTC Tiempo actual en UTC
     * @param aeropuertoDestino Aeropuerto destino para conversión de husos
     * @return CondicionColapso si está retrasado, null si está a tiempo
     */
    public static CondicionColapso verificarRetrasopedido(Pedido pedido, LocalDateTime tiempoActualUTC,
                                                          Aeropuerto aeropuertoDestino) {
        if (!pedido.estaDentroPlazoUTC(tiempoActualUTC, aeropuertoDestino)) {
            long horasRetraso = -pedido.horasRestantesUTC(tiempoActualUTC, aeropuertoDestino);
            return new CondicionColapso(
                TipoColapso.VIOLACION_PLAZO,
                String.format("Pedido retrasado por %d horas", horasRetraso),
                pedido.getIdPedido()
            );
        }
        return null;
    }

    /**
     * Verifica si un vuelo tiene capacidad insuficiente
     * @param vuelo Vuelo a verificar
     * @param cantidadRequerida Cantidad de productos necesaria
     * @return CondicionColapso si hay insuficiente capacidad, null si hay capacidad
     */
    public static CondicionColapso verificarCapacidadVuelo(Vuelo vuelo, int cantidadRequerida) {
        if (!vuelo.puedeTransportar(cantidadRequerida)) {
            return new CondicionColapso(
                TipoColapso.CAPACIDAD_VUELO_EXCEDIDA,
                String.format("Capacidad insuficiente: %d requerido, %d disponible",
                             cantidadRequerida, vuelo.getCapacidadDisponible()),
                vuelo.getIdVuelo()
            );
        }
        return null;
    }

    /**
     * Verifica si un aeropuerto tiene capacidad insuficiente
     * @param aeropuerto Aeropuerto a verificar
     * @param cantidadRequerida Cantidad de productos necesaria
     * @return CondicionColapso si hay insuficiente capacidad, null si hay capacidad
     */
    public static CondicionColapso verificarCapacidadAeropuerto(Aeropuerto aeropuerto, int cantidadRequerida) {
        if (!aeropuerto.puedeAlmacenar(cantidadRequerida)) {
            return new CondicionColapso(
                TipoColapso.CAPACIDAD_AEROPUERTO_EXCEDIDA,
                String.format("Capacidad insuficiente: %d requerido, %d disponible",
                             cantidadRequerida, aeropuerto.getCapacidadDisponible()),
                aeropuerto.getCodigoICAO()
            );
        }
        return null;
    }

    /**
     * Verifica si existe una ruta válida entre dos aeropuertos
     * @param red Red de distribución
     * @param origen Código ICAO del aeropuerto origen
     * @param destino Código ICAO del aeropuerto destino
     * @return CondicionColapso si no hay ruta válida, null si hay ruta
     */
    public static CondicionColapso verificarConectividadRuta(RedDistribucion red, String origen, String destino) {
        List<String> ruta = red.buscarRutaMinima(origen, destino);
        if (ruta == null || ruta.isEmpty()) {
            return new CondicionColapso(
                TipoColapso.RUTA_INVALIDA,
                String.format("No existe ruta entre %s y %s", origen, destino),
                origen + " → " + destino
            );
        }
        return null;
    }

    /**
     * Verifica si una ruta puede cumplir con el plazo temporal de un pedido
     * @param ruta Lista de vuelos que componen la ruta
     * @param pedido Pedido con restricción temporal
     * @param tiempoInicioRuta Tiempo de inicio de la ruta
     * @param aeropuertoDestino Aeropuerto destino para conversión de husos
     * @return CondicionColapso si no puede cumplir el plazo, null si puede cumplir
     */
    public static CondicionColapso verificarFactibilidadTemporal(List<Vuelo> ruta, Pedido pedido,
                                                                LocalDateTime tiempoInicioRuta,
                                                                Aeropuerto aeropuertoDestino) {
        if (ruta == null || ruta.isEmpty()) {
            return new CondicionColapso(
                TipoColapso.RUTA_INVALIDA,
                "Ruta vacía o nula",
                pedido.getIdPedido()
            );
        }

        // Calcular tiempo total de la ruta (simplificado)
        // En una implementación completa, esto calcularía el tiempo real considerando horarios
        LocalDateTime tiempoLlegadaEstimado = tiempoInicioRuta.plusHours(ruta.size() * 12); // Estimación simple

        LocalDateTime limiteEntrega = pedido.getTiempoLimiteEntregaUTC(aeropuertoDestino);
        if (limiteEntrega != null && tiempoLlegadaEstimado.isAfter(limiteEntrega)) {
            return new CondicionColapso(
                TipoColapso.VIOLACION_PLAZO,
                String.format("Ruta no puede cumplir plazo: llegada estimada %s, límite %s",
                             tiempoLlegadaEstimado, limiteEntrega),
                pedido.getIdPedido()
            );
        }

        return null;
    }

    /**
     * Realiza una verificación completa del sistema para detectar condiciones de colapso
     * @param red Red de distribución
     * @param pedidos Lista de pedidos a verificar
     * @param tiempoActualUTC Tiempo actual en UTC
     * @return Lista de condiciones de colapso detectadas
     */
    public static List<CondicionColapso> verificarSistemaCompleto(RedDistribucion red,
                                                                List<Pedido> pedidos,
                                                                LocalDateTime tiempoActualUTC) {
        List<CondicionColapso> colapsosDetectados = new ArrayList<>();

        for (Pedido pedido : pedidos) {
            Aeropuerto destino = red.getAeropuerto(pedido.getCodigoDestino());
            if (destino == null) {
                colapsosDetectados.add(new CondicionColapso(
                    TipoColapso.RUTA_INVALIDA,
                    "Aeropuerto destino no existe",
                    pedido.getIdPedido()
                ));
                continue;
            }

            // Verificar retraso del pedido
            CondicionColapso retraso = verificarRetrasopedido(pedido, tiempoActualUTC, destino);
            if (retraso != null) {
                colapsosDetectados.add(retraso);
            }

            // Verificar conectividad desde sedes principales
            Aeropuerto sedeOptima = red.buscarSedeOptima(destino);
            if (sedeOptima != null) {
                CondicionColapso conectividad = verificarConectividadRuta(red,
                    sedeOptima.getCodigoICAO(), pedido.getCodigoDestino());
                if (conectividad != null) {
                    colapsosDetectados.add(conectividad);
                }
            }
        }

        return colapsosDetectados;
    }

    /**
     * Calcula métricas de colapso del sistema
     * @param condiciones Lista de condiciones de colapso detectadas
     * @param totalPedidos Total de pedidos en el sistema
     * @return String con métricas calculadas
     */
    public static String calcularMetricasColapso(List<CondicionColapso> condiciones, int totalPedidos) {
        if (totalPedidos == 0) {
            return "No hay pedidos para evaluar";
        }

        long retrasados = condiciones.stream()
            .filter(c -> c.getTipo() == TipoColapso.VIOLACION_PLAZO)
            .count();

        long problemasCapacidad = condiciones.stream()
            .filter(c -> c.getTipo() == TipoColapso.CAPACIDAD_VUELO_EXCEDIDA ||
                         c.getTipo() == TipoColapso.CAPACIDAD_AEROPUERTO_EXCEDIDA)
            .count();

        long problemasConectividad = condiciones.stream()
            .filter(c -> c.getTipo() == TipoColapso.RUTA_INVALIDA ||
                         c.getTipo() == TipoColapso.DESCONEXION_RED)
            .count();

        double tasaRetraso = (double) retrasados / totalPedidos * 100;

        return String.format("""
            === MÉTRICAS DE COLAPSO DEL SISTEMA ===
            Total pedidos: %d
            Condiciones de colapso detectadas: %d

            Tasa de retraso: %.2f%% (%d pedidos)
            Problemas de capacidad: %d
            Problemas de conectividad: %d

            Estado del sistema: %s
            """,
            totalPedidos, condiciones.size(), tasaRetraso, retrasados,
            problemasCapacidad, problemasConectividad,
            condiciones.isEmpty() ? "OPERATIVO" : "EN COLAPSO"
        );
    }

    /**
     * Verifica si el sistema ha colapsado completamente
     * @param condiciones Lista de condiciones de colapso
     * @param umbralColapso Porcentaje de condiciones críticas para considerar colapso total
     * @return true si el sistema ha colapsado
     */
    public static boolean sistemaHaColapsado(List<CondicionColapso> condiciones, double umbralColapso) {
        if (condiciones.isEmpty()) {
            return false;
        }

        // Contar condiciones críticas (retrasos y falta de conectividad)
        long condicionesCriticas = condiciones.stream()
            .filter(c -> c.getTipo() == TipoColapso.VIOLACION_PLAZO ||
                         c.getTipo() == TipoColapso.RUTA_INVALIDA)
            .count();

        return condicionesCriticas > 0 &&
               (double) condicionesCriticas / condiciones.size() >= umbralColapso;
    }
}