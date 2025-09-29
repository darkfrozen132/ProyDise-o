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

    private final Map<Integer, RutaProducto> rutasProductos;
    private final LocalDateTime tiempoCreacion;
    private boolean cumplePlazos;
    private boolean validacionRealizada;

    /**
     * Constructor por defecto
     */
    public SolucionMoraPack() {
        super();
        this.rutasProductos = new HashMap<>();
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
        this.rutasProductos = new HashMap<>();
        this.tiempoCreacion = LocalDateTime.now();
        this.cumplePlazos = otra.cumplePlazos;
        this.validacionRealizada = otra.validacionRealizada;

        // Copiar rutas profundamente
        for (Map.Entry<Integer, RutaProducto> entrada : otra.rutasProductos.entrySet()) {
            this.rutasProductos.put(entrada.getKey(), entrada.getValue().clonar());
        }
    }

    /**
     * Agrega una ruta para un pedido específico
     * @param idPedido ID del pedido
     * @param ruta Ruta asignada al pedido
     */
    public void agregarRutaProducto(int idPedido, RutaProducto ruta) {
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta no puede ser null");
        }
        rutasProductos.put(idPedido, ruta);
        validacionRealizada = false;
        invalidarFitness();
    }

    /**
     * Obtiene la ruta asignada a un pedido
     * @param idPedido ID del pedido
     * @return Ruta del pedido, o null si no existe
     */
    public RutaProducto getRutaProducto(int idPedido) {
        return rutasProductos.get(idPedido);
    }

    /**
     * Obtiene todas las rutas de productos
     * @return Mapa con todas las rutas
     */
    public Map<Integer, RutaProducto> getRutasProductos() {
        return Collections.unmodifiableMap(rutasProductos);
    }

    /**
     * Elimina la ruta de un pedido
     * @param idPedido ID del pedido
     * @return La ruta eliminada, o null si no existía
     */
    public RutaProducto eliminarRutaProducto(int idPedido) {
        RutaProducto rutaEliminada = rutasProductos.remove(idPedido);
        if (rutaEliminada != null) {
            validacionRealizada = false;
            invalidarFitness();
        }
        return rutaEliminada;
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
     * Valida la solución completa
     */
    private void validarSolucion() {
        cumplePlazos = rutasProductos.values().stream()
                .allMatch(RutaProducto::cumplePlazo);
        validacionRealizada = true;
    }

    /**
     * Calcula el uso de capacidad por vuelo
     * @return Mapa con el uso de capacidad por ID de vuelo
     */
    public Map<String, Integer> calcularUsoCapacidadVuelos() {
        Map<String, Integer> usoVuelos = new HashMap<>();

        for (RutaProducto ruta : rutasProductos.values()) {
            for (SegmentoVuelo segmento : ruta.getSegmentos()) {
                String idVuelo = segmento.getIdVuelo();
                usoVuelos.merge(idVuelo, ruta.getCantidadProductos(), Integer::sum);
            }
        }

        return usoVuelos;
    }

    /**
     * Calcula el uso de capacidad por aeropuerto
     * @return Mapa con el uso de capacidad por código ICAO
     */
    public Map<String, Integer> calcularUsoCapacidadAeropuertos() {
        Map<String, Integer> usoAeropuertos = new HashMap<>();

        for (RutaProducto ruta : rutasProductos.values()) {
            // Contar productos que pasan por cada aeropuerto (excepto destino final)
            for (int i = 0; i < ruta.getSegmentos().size() - 1; i++) {
                String aeropuerto = ruta.getSegmentos().get(i).getAeropuertoDestino();
                usoAeropuertos.merge(aeropuerto, ruta.getCantidadProductos(), Integer::sum);
            }
        }

        return usoAeropuertos;
    }

    /**
     * Obtiene estadísticas de la solución
     * @return String con estadísticas resumidas
     */
    public String getEstadisticas() {
        if (rutasProductos.isEmpty()) {
            return "Solución vacía";
        }

        int totalPedidos = rutasProductos.size();
        int pedidosConPlazo = (int) rutasProductos.values().stream()
                .filter(RutaProducto::cumplePlazo)
                .count();

        double promedioSegmentos = rutasProductos.values().stream()
                .mapToInt(ruta -> ruta.getSegmentos().size())
                .average()
                .orElse(0.0);

        return String.format(
            "Estadísticas: %d pedidos, %d con plazo (%.1f%%), promedio %.1f segmentos/ruta",
            totalPedidos, pedidosConPlazo,
            (double) pedidosConPlazo / totalPedidos * 100.0,
            promedioSegmentos
        );
    }

    /**
     * Invalida el fitness calculado (override del método padre)
     */
    private void invalidarFitness() {
        setFitness(Double.MAX_VALUE);
    }

    @Override
    public SolucionMoraPack clone() {
        return new SolucionMoraPack(this);
    }

    @Override
    public String toString() {
        if (rutasProductos.isEmpty()) {
            return "SolucionMoraPack vacía";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SolucionMoraPack[").append(rutasProductos.size()).append(" rutas]\n");

        // Mostrar algunas rutas de ejemplo
        int contador = 0;
        for (Map.Entry<Integer, RutaProducto> entrada : rutasProductos.entrySet()) {
            if (contador >= 3) { // Mostrar máximo 3 rutas en toString
                sb.append("  ... (").append(rutasProductos.size() - 3).append(" rutas más)\n");
                break;
            }
            sb.append("  Pedido ").append(entrada.getKey()).append(": ")
              .append(entrada.getValue().toString()).append("\n");
            contador++;
        }

        if (esFitnessValido()) {
            sb.append("Fitness: ").append(String.format("%.2f", getFitness()));
        }

        return sb.toString();
    }

    /**
     * Representa una ruta específica para un producto/pedido
     */
    public static class RutaProducto {
        private final int idPedido;
        private final int cantidadProductos;
        private final String aeropuertoOrigen;
        private final String aeropuertoDestino;
        private final List<SegmentoVuelo> segmentos;
        private final LocalDateTime tiempoSalida;
        private final LocalDateTime tiempoLlegada;
        private final boolean cumplePlazo;

        public RutaProducto(int idPedido, int cantidadProductos, String aeropuertoOrigen,
                           String aeropuertoDestino, List<SegmentoVuelo> segmentos,
                           LocalDateTime tiempoSalida, LocalDateTime tiempoLlegada,
                           boolean cumplePlazo) {
            this.idPedido = idPedido;
            this.cantidadProductos = cantidadProductos;
            this.aeropuertoOrigen = aeropuertoOrigen;
            this.aeropuertoDestino = aeropuertoDestino;
            this.segmentos = new ArrayList<>(segmentos);
            this.tiempoSalida = tiempoSalida;
            this.tiempoLlegada = tiempoLlegada;
            this.cumplePlazo = cumplePlazo;
        }

        public int getIdPedido() { return idPedido; }
        public int getCantidadProductos() { return cantidadProductos; }
        public String getAeropuertoOrigen() { return aeropuertoOrigen; }
        public String getAeropuertoDestino() { return aeropuertoDestino; }
        public List<SegmentoVuelo> getSegmentos() { return Collections.unmodifiableList(segmentos); }
        public LocalDateTime getTiempoSalida() { return tiempoSalida; }
        public LocalDateTime getTiempoLlegada() { return tiempoLlegada; }
        public boolean cumplePlazo() { return cumplePlazo; }

        /**
         * Crea una copia profunda de la ruta
         */
        public RutaProducto clonar() {
            List<SegmentoVuelo> segmentosCopia = new ArrayList<>();
            for (SegmentoVuelo segmento : segmentos) {
                segmentosCopia.add(segmento.clonar());
            }

            return new RutaProducto(idPedido, cantidadProductos, aeropuertoOrigen,
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
            sb.append(" (").append(cantidadProductos).append(" prod, ");
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