package morapack.core.solucion;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Representa una solución específica para el problema de distribución MoraPack.
 *
 * Una solución contiene las rutas asignadas para todos los productos/pedidos,
 * donde cada ruta puede incluir múltiples segmentos de vuelo (escalas).
 */
public class SolucionMoraPack extends Solucion {

    // MODELO HÍBRIDO: Un pedido puede tener múltiples entregas parciales
    private final Map<Integer, List<RutaProducto>> rutasPorPedido;
    private final LocalDateTime tiempoCreacion;
    private boolean cumplePlazos;
    private boolean validacionRealizada;

    /**
     * Constructor por defecto
     */
    public SolucionMoraPack() {
        super();
        this.rutasPorPedido = new HashMap<>();
        this.tiempoCreacion = LocalDateTime.now();
        this.cumplePlazos = true;
        this.validacionRealizada = false;
    }

    /**
     * Constructor copia
     * @param otra Solución a copiar
     */
    public SolucionMoraPack(SolucionMoraPack otra) {
        super(otra);
        this.rutasPorPedido = new HashMap<>();
        this.tiempoCreacion = LocalDateTime.now();
        this.cumplePlazos = otra.cumplePlazos;
        this.validacionRealizada = otra.validacionRealizada;

        // Copiar rutas profundamente
        for (Map.Entry<Integer, List<RutaProducto>> entrada : otra.rutasPorPedido.entrySet()) {
            List<RutaProducto> rutasCopia = new ArrayList<>();
            for (RutaProducto ruta : entrada.getValue()) {
                rutasCopia.add(ruta.clonar());
            }
            this.rutasPorPedido.put(entrada.getKey(), rutasCopia);
        }
    }

    /**
     * Agrega una ruta para un pedido específico (permite múltiples rutas por pedido)
     * @param idPedido ID del pedido
     * @param ruta Ruta asignada al pedido
     */
    public void agregarRutaProducto(int idPedido, RutaProducto ruta) {
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta no puede ser null");
        }
        rutasPorPedido.computeIfAbsent(idPedido, k -> new ArrayList<>()).add(ruta);
        validacionRealizada = false;
        invalidarFitness();
    }

    /**
     * Agrega múltiples rutas para un pedido (entrega parcial)
     * @param idPedido ID del pedido
     * @param rutas Lista de rutas para el pedido
     */
    public void agregarRutasProducto(int idPedido, List<RutaProducto> rutas) {
        if (rutas == null || rutas.isEmpty()) {
            throw new IllegalArgumentException("La lista de rutas no puede estar vacía");
        }
        for (RutaProducto ruta : rutas) {
            agregarRutaProducto(idPedido, ruta);
        }
    }

    /**
     * Obtiene todas las rutas asignadas a un pedido
     * @param idPedido ID del pedido
     * @return Lista de rutas del pedido, o lista vacía si no existe
     */
    public List<RutaProducto> getRutasProducto(int idPedido) {
        return rutasPorPedido.getOrDefault(idPedido, new ArrayList<>());
    }


    /**
     * Obtiene todas las rutas organizadas por pedido
     * @return Mapa con todas las rutas por pedido
     */
    public Map<Integer, List<RutaProducto>> getRutasPorPedido() {
        return Collections.unmodifiableMap(rutasPorPedido);
    }


    /**
     * Elimina todas las rutas de un pedido
     * @param idPedido ID del pedido
     * @return Lista de rutas eliminadas
     */
    public List<RutaProducto> eliminarRutasProducto(int idPedido) {
        List<RutaProducto> rutasEliminadas = rutasPorPedido.remove(idPedido);
        if (rutasEliminadas != null && !rutasEliminadas.isEmpty()) {
            validacionRealizada = false;
            invalidarFitness();
            return new ArrayList<>(rutasEliminadas);
        }
        return new ArrayList<>();
    }

    /**
     * Elimina una ruta específica de un pedido
     * @param idPedido ID del pedido
     * @param numeroEntrega Número de entrega a eliminar
     * @return La ruta eliminada, o null si no existía
     */
    public RutaProducto eliminarRutaProducto(int idPedido, int numeroEntrega) {
        List<RutaProducto> rutas = rutasPorPedido.get(idPedido);
        if (rutas != null) {
            for (int i = 0; i < rutas.size(); i++) {
                if (rutas.get(i).getNumeroEntrega() == numeroEntrega) {
                    validacionRealizada = false;
                    invalidarFitness();
                    return rutas.remove(i);
                }
            }
        }
        return null;
    }


    /**
     * Verifica si todos los pedidos cumplen sus plazos
     * @return true si todos los pedidos cumplen plazos
     */
    public boolean cumplePlazos() {
        if (!validacionRealizada) {
            validarSolucion();
        }
        return cumplePlazos;
    }

    /**
     * Valida la solución completa considerando entregas parciales
     */
    private void validarSolucion() {
        cumplePlazos = rutasPorPedido.values().stream()
                .allMatch(rutas -> rutas.stream().allMatch(RutaProducto::cumplePlazo));
        validacionRealizada = true;
    }

    /**
     * Verifica si un pedido específico está completo
     * @param idPedido ID del pedido
     * @return true si todas las entregas del pedido suman la cantidad total
     */
    public boolean pedidoCompleto(int idPedido) {
        List<RutaProducto> rutas = rutasPorPedido.get(idPedido);
        if (rutas == null || rutas.isEmpty()) {
            return false;
        }

        int totalTransportado = rutas.stream()
                .mapToInt(RutaProducto::getCantidadTransportada)
                .sum();
        int totalRequerido = rutas.get(0).getCantidadTotalPedido();

        return totalTransportado >= totalRequerido;
    }

    /**
     * Verifica si todas las entregas de un pedido cumplen plazo
     * @param idPedido ID del pedido
     * @return true si todas las entregas llegan a tiempo
     */
    public boolean pedidoCumplePlazo(int idPedido) {
        List<RutaProducto> rutas = rutasPorPedido.get(idPedido);
        return rutas != null && rutas.stream().allMatch(RutaProducto::cumplePlazo);
    }

    /**
     * Verifica si todos los pedidos están completos
     * @return true si todos los pedidos tienen entregas completas
     */
    public boolean todosLosPedidosCompletos() {
        return rutasPorPedido.keySet().stream().allMatch(this::pedidoCompleto);
    }

    /**
     * Calcula el uso de capacidad por vuelo (considerando entregas parciales)
     * @return Mapa con el uso de capacidad por ID de vuelo
     */
    public Map<String, Integer> calcularUsoCapacidadVuelos() {
        Map<String, Integer> usoVuelos = new HashMap<>();

        for (List<RutaProducto> rutas : rutasPorPedido.values()) {
            for (RutaProducto ruta : rutas) {
                for (SegmentoVuelo segmento : ruta.getSegmentos()) {
                    String idVuelo = segmento.getIdVuelo();
                    usoVuelos.merge(idVuelo, ruta.getCantidadTransportada(), Integer::sum);
                }
            }
        }

        return usoVuelos;
    }

    /**
     * Calcula el uso de capacidad por aeropuerto (considerando entregas parciales)
     * @return Mapa con el uso de capacidad por código ICAO
     */
    public Map<String, Integer> calcularUsoCapacidadAeropuertos() {
        Map<String, Integer> usoAeropuertos = new HashMap<>();

        for (List<RutaProducto> rutas : rutasPorPedido.values()) {
            for (RutaProducto ruta : rutas) {
                // Contar productos que pasan por cada aeropuerto (excepto destino final)
                for (int i = 0; i < ruta.getSegmentos().size() - 1; i++) {
                    String aeropuerto = ruta.getSegmentos().get(i).getAeropuertoDestino();
                    usoAeropuertos.merge(aeropuerto, ruta.getCantidadTransportada(), Integer::sum);
                }
            }
        }

        return usoAeropuertos;
    }

    /**
     * Obtiene estadísticas detalladas de la solución (con entregas parciales)
     * @return String con estadísticas resumidas
     */
    public String getEstadisticas() {
        if (rutasPorPedido.isEmpty()) {
            return "Solución vacía";
        }

        int totalPedidos = rutasPorPedido.size();
        int pedidosCompletos = (int) rutasPorPedido.keySet().stream()
                .filter(this::pedidoCompleto)
                .count();
        int pedidosConPlazo = (int) rutasPorPedido.keySet().stream()
                .filter(this::pedidoCumplePlazo)
                .count();

        int totalEntregas = rutasPorPedido.values().stream()
                .mapToInt(List::size)
                .sum();

        int entregasParciales = (int) rutasPorPedido.values().stream()
                .flatMap(List::stream)
                .filter(RutaProducto::esEntregaParcial)
                .count();

        double promedioSegmentos = rutasPorPedido.values().stream()
                .flatMap(List::stream)
                .mapToInt(ruta -> ruta.getSegmentos().size())
                .average()
                .orElse(0.0);

        double promedioEntregasPorPedido = totalPedidos > 0 ? (double) totalEntregas / totalPedidos : 0.0;

        return String.format(
            "Estadísticas: %d pedidos, %d completos (%.1f%%), %d con plazo (%.1f%%), " +
            "%.1f entregas/pedido, %d parciales, promedio %.1f segmentos/entrega",
            totalPedidos, pedidosCompletos, (double) pedidosCompletos / totalPedidos * 100.0,
            pedidosConPlazo, (double) pedidosConPlazo / totalPedidos * 100.0,
            promedioEntregasPorPedido, entregasParciales, promedioSegmentos
        );
    }

    /**
     * Obtiene estadísticas de entregas parciales
     * @return String con detalles de entregas parciales
     */
    public String getEstadisticasEntregasParciales() {
        Map<Integer, Integer> entregasPorPedido = new HashMap<>();
        int totalProductosTransportados = 0;
        int totalProductosRequeridos = 0;

        for (Map.Entry<Integer, List<RutaProducto>> entrada : rutasPorPedido.entrySet()) {
            int idPedido = entrada.getKey();
            List<RutaProducto> rutas = entrada.getValue();

            entregasPorPedido.put(idPedido, rutas.size());

            for (RutaProducto ruta : rutas) {
                totalProductosTransportados += ruta.getCantidadTransportada();
                totalProductosRequeridos = Math.max(totalProductosRequeridos,
                    ruta.getCantidadTotalPedido());
            }
        }

        double eficienciaEntrega = totalProductosRequeridos > 0 ?
            (double) totalProductosTransportados / totalProductosRequeridos * 100.0 : 0.0;

        return String.format(
            "Entregas Parciales: %d productos transportados, %.1f%% eficiencia, " +
            "máx %d entregas/pedido",
            totalProductosTransportados, eficienciaEntrega,
            entregasPorPedido.values().stream().mapToInt(Integer::intValue).max().orElse(0)
        );
    }

    /**
     * Invalida el fitness calculado (override del método padre)
     * Con nueva convención: MAYOR fitness = MEJOR solución
     */
    private void invalidarFitness() {
        setFitness(Double.MIN_VALUE); // Indica fitness no calculado (peor valor posible)
    }

    @Override
    public SolucionMoraPack clone() {
        return new SolucionMoraPack(this);
    }

    @Override
    public String toString() {
        if (rutasPorPedido.isEmpty()) {
            return "SolucionMoraPack vacía";
        }

        int totalEntregas = rutasPorPedido.values().stream().mapToInt(List::size).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("SolucionMoraPack[").append(rutasPorPedido.size()).append(" pedidos, ")
          .append(totalEntregas).append(" entregas]\n");

        // Mostrar algunos pedidos de ejemplo
        int contador = 0;
        for (Map.Entry<Integer, List<RutaProducto>> entrada : rutasPorPedido.entrySet()) {
            if (contador >= 3) { // Mostrar máximo 3 pedidos en toString
                sb.append("  ... (").append(rutasPorPedido.size() - 3).append(" pedidos más)\n");
                break;
            }

            int idPedido = entrada.getKey();
            List<RutaProducto> rutas = entrada.getValue();

            sb.append("  Pedido ").append(idPedido).append(" (").append(rutas.size())
              .append(" entrega").append(rutas.size() > 1 ? "s" : "").append("):" );

            if (rutas.size() == 1) {
                sb.append(" ").append(rutas.get(0).toString());
            } else {
                sb.append("\n");
                for (RutaProducto ruta : rutas) {
                    sb.append("    → ").append(ruta.toString()).append("\n");
                }
            }
            sb.append("\n");
            contador++;
        }

        if (esFitnessValido()) {
            sb.append("Fitness: ").append(String.format("%.2f", getFitness())).append("\n");
        }

        sb.append(getEstadisticas());

        return sb.toString();
    }

    /**
     * Representa una ruta específica para un producto/pedido (soporta entregas parciales)
     */
    public static class RutaProducto {
        private final int idPedido;
        private final int cantidadTransportada;      // Cantidad en esta ruta específica
        private final int cantidadTotalPedido;       // Cantidad total del pedido original
        private final int numeroEntrega;             // 1, 2, 3... para múltiples entregas
        private final boolean esEntregaParcial;      // true si cantidad < total
        private final String aeropuertoOrigen;
        private final String aeropuertoDestino;
        private final List<SegmentoVuelo> segmentos;
        private final LocalDateTime tiempoSalida;
        private final LocalDateTime tiempoLlegada;
        private final boolean cumplePlazo;

        /**
         * Constructor para entrega completa (compatibilidad hacia atrás)
         */
        public RutaProducto(int idPedido, int cantidadProductos, String aeropuertoOrigen,
                           String aeropuertoDestino, List<SegmentoVuelo> segmentos,
                           LocalDateTime tiempoSalida, LocalDateTime tiempoLlegada,
                           boolean cumplePlazo) {
            this(idPedido, cantidadProductos, cantidadProductos, 1, false,
                 aeropuertoOrigen, aeropuertoDestino, segmentos, tiempoSalida, tiempoLlegada, cumplePlazo);
        }

        /**
         * Constructor completo para entregas parciales
         */
        public RutaProducto(int idPedido, int cantidadTransportada, int cantidadTotalPedido,
                           int numeroEntrega, boolean esEntregaParcial,
                           String aeropuertoOrigen, String aeropuertoDestino,
                           List<SegmentoVuelo> segmentos,
                           LocalDateTime tiempoSalida, LocalDateTime tiempoLlegada,
                           boolean cumplePlazo) {
            this.idPedido = idPedido;
            this.cantidadTransportada = cantidadTransportada;
            this.cantidadTotalPedido = cantidadTotalPedido;
            this.numeroEntrega = numeroEntrega;
            this.esEntregaParcial = esEntregaParcial || (cantidadTransportada < cantidadTotalPedido);
            this.aeropuertoOrigen = aeropuertoOrigen;
            this.aeropuertoDestino = aeropuertoDestino;
            this.segmentos = new ArrayList<>(segmentos);
            this.tiempoSalida = tiempoSalida;
            this.tiempoLlegada = tiempoLlegada;
            this.cumplePlazo = cumplePlazo;
        }

        // Getters básicos
        public int getIdPedido() { return idPedido; }
        public int getCantidadTransportada() { return cantidadTransportada; }
        public int getCantidadTotalPedido() { return cantidadTotalPedido; }
        public int getNumeroEntrega() { return numeroEntrega; }
        public boolean esEntregaParcial() { return esEntregaParcial; }
        public String getAeropuertoOrigen() { return aeropuertoOrigen; }
        public String getAeropuertoDestino() { return aeropuertoDestino; }
        public List<SegmentoVuelo> getSegmentos() { return Collections.unmodifiableList(segmentos); }
        public LocalDateTime getTiempoSalida() { return tiempoSalida; }
        public LocalDateTime getTiempoLlegada() { return tiempoLlegada; }
        public boolean cumplePlazo() { return cumplePlazo; }


        // Métodos auxiliares para entregas parciales
        public double porcentajeCompletado() {
            return cantidadTotalPedido > 0 ? (double) cantidadTransportada / cantidadTotalPedido : 0.0;
        }

        public int cantidadRestante() {
            return Math.max(0, cantidadTotalPedido - cantidadTransportada);
        }

        public boolean esEntregaCompleta() {
            return cantidadTransportada >= cantidadTotalPedido;
        }

        /**
         * Crea una copia profunda de la ruta
         */
        public RutaProducto clonar() {
            List<SegmentoVuelo> segmentosCopia = new ArrayList<>();
            for (SegmentoVuelo segmento : segmentos) {
                segmentosCopia.add(segmento.clonar());
            }

            return new RutaProducto(idPedido, cantidadTransportada, cantidadTotalPedido,
                                   numeroEntrega, esEntregaParcial, aeropuertoOrigen,
                                   aeropuertoDestino, segmentosCopia, tiempoSalida,
                                   tiempoLlegada, cumplePlazo);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(aeropuertoOrigen);
            for (SegmentoVuelo segmento : segmentos) {
                sb.append(" -> ").append(segmento.getAeropuertoDestino());
            }
            sb.append(" (").append(cantidadTransportada);
            if (esEntregaParcial) {
                sb.append("/").append(cantidadTotalPedido).append(" - Entrega #").append(numeroEntrega);
            }
            sb.append(" prod, ");
            sb.append(cumplePlazo ? "a tiempo" : "retrasado").append(")");
            return sb.toString();
        }
    }

    /**
     * Representa un segmento de vuelo dentro de una ruta
     */
    public static class SegmentoVuelo {
        private final String idVuelo;
        private final String aeropuertoOrigen;
        private final String aeropuertoDestino;
        private final LocalDateTime horaSalida;
        private final LocalDateTime horaLlegada;

        public SegmentoVuelo(String idVuelo, String aeropuertoOrigen, String aeropuertoDestino,
                            LocalDateTime horaSalida, LocalDateTime horaLlegada) {
            this.idVuelo = idVuelo;
            this.aeropuertoOrigen = aeropuertoOrigen;
            this.aeropuertoDestino = aeropuertoDestino;
            this.horaSalida = horaSalida;
            this.horaLlegada = horaLlegada;
        }

        public String getIdVuelo() { return idVuelo; }
        public String getAeropuertoOrigen() { return aeropuertoOrigen; }
        public String getAeropuertoDestino() { return aeropuertoDestino; }
        public LocalDateTime getHoraSalida() { return horaSalida; }
        public LocalDateTime getHoraLlegada() { return horaLlegada; }

        /**
         * Crea una copia del segmento
         */
        public SegmentoVuelo clonar() {
            return new SegmentoVuelo(idVuelo, aeropuertoOrigen, aeropuertoDestino,
                                    horaSalida, horaLlegada);
        }

        @Override
        public String toString() {
            return String.format("%s (%s->%s)", idVuelo, aeropuertoOrigen, aeropuertoDestino);
        }
    }
}