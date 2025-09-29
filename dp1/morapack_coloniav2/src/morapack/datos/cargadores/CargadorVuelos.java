package morapack.datos.cargadores;

import morapack.datos.modelos.Vuelo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cargador específico para el archivo planes_de_vuelo.csv.
 * Maneja la carga y organización de vuelos del sistema.
 */
public class CargadorVuelos extends CargadorCSV<Vuelo> {

    private Map<String, Vuelo> indicePorId;
    private Map<String, List<Vuelo>> indicePorOrigen;
    private Map<String, List<Vuelo>> indicePorDestino;
    private Map<String, List<Vuelo>> indicePorRuta;

    /**
     * Constructor del cargador de vuelos
     * @param rutaArchivo Ruta del archivo planes_de_vuelo.csv
     */
    public CargadorVuelos(String rutaArchivo) {
        super(rutaArchivo, true); // El archivo tiene encabezado
        this.indicePorId = new HashMap<>();
        this.indicePorOrigen = new HashMap<>();
        this.indicePorDestino = new HashMap<>();
        this.indicePorRuta = new HashMap<>();
    }

    /**
     * Constructor con ruta por defecto
     */
    public CargadorVuelos() {
        this("datos/planes_de_vuelo.csv");
    }

    @Override
    protected Vuelo procesarLinea(String linea, int numeroLinea) throws Exception {
        try {
            Vuelo vuelo = Vuelo.desdeCSV(linea);

            // Verificar duplicados por ID
            if (indicePorId.containsKey(vuelo.getIdVuelo())) {
                throw new CargadorException(
                    String.format("ID de vuelo duplicado: %s", vuelo.getIdVuelo())
                );
            }

            return vuelo;

        } catch (IllegalArgumentException e) {
            throw new CargadorException(
                String.format("Error al crear vuelo desde línea: %s", e.getMessage()),
                e
            );
        }
    }

    /**
     * Carga todos los vuelos y construye índices
     * @return Lista de vuelos cargados
     * @throws Exception si hay error en la carga
     */
    public List<Vuelo> cargarConIndices() throws Exception {
        List<Vuelo> vuelos = cargarTodos();

        // Construir índices
        indicePorId.clear();
        indicePorOrigen.clear();
        indicePorDestino.clear();
        indicePorRuta.clear();

        for (Vuelo vuelo : vuelos) {
            // Índice por ID
            indicePorId.put(vuelo.getIdVuelo(), vuelo);

            // Índice por aeropuerto origen
            indicePorOrigen.computeIfAbsent(vuelo.getAeropuertoOrigen(), k -> new ArrayList<>())
                          .add(vuelo);

            // Índice por aeropuerto destino
            indicePorDestino.computeIfAbsent(vuelo.getAeropuertoDestino(), k -> new ArrayList<>())
                           .add(vuelo);

            // Índice por ruta (origen-destino)
            String ruta = vuelo.getAeropuertoOrigen() + "-" + vuelo.getAeropuertoDestino();
            indicePorRuta.computeIfAbsent(ruta, k -> new ArrayList<>())
                        .add(vuelo);
        }

        return vuelos;
    }

    /**
     * Busca un vuelo por su ID
     * @param idVuelo ID del vuelo
     * @return Vuelo encontrado o null si no existe
     */
    public Vuelo buscarPorId(String idVuelo) {
        if (idVuelo == null) {
            return null;
        }
        return indicePorId.get(idVuelo);
    }

    /**
     * Busca todos los vuelos desde un aeropuerto origen
     * @param codigoOrigen Código ICAO del aeropuerto origen
     * @return Lista de vuelos desde el origen
     */
    public List<Vuelo> buscarPorOrigen(String codigoOrigen) {
        if (codigoOrigen == null) {
            return new ArrayList<>();
        }
        return indicePorOrigen.getOrDefault(codigoOrigen.toUpperCase(), new ArrayList<>());
    }

    /**
     * Busca todos los vuelos hacia un aeropuerto destino
     * @param codigoDestino Código ICAO del aeropuerto destino
     * @return Lista de vuelos hacia el destino
     */
    public List<Vuelo> buscarPorDestino(String codigoDestino) {
        if (codigoDestino == null) {
            return new ArrayList<>();
        }
        return indicePorDestino.getOrDefault(codigoDestino.toUpperCase(), new ArrayList<>());
    }

    /**
     * Busca vuelos directos entre dos aeropuertos
     * @param origen Código ICAO origen
     * @param destino Código ICAO destino
     * @return Lista de vuelos directos
     */
    public List<Vuelo> buscarVuelosDirectos(String origen, String destino) {
        if (origen == null || destino == null) {
            return new ArrayList<>();
        }

        String ruta = origen.toUpperCase() + "-" + destino.toUpperCase();
        return indicePorRuta.getOrDefault(ruta, new ArrayList<>());
    }

    /**
     * Busca vuelos disponibles (con capacidad) desde un origen
     * @param codigoOrigen Código ICAO del aeropuerto origen
     * @return Lista de vuelos disponibles
     */
    public List<Vuelo> buscarVuelosDisponibles(String codigoOrigen) {
        return buscarPorOrigen(codigoOrigen).stream()
                                           .filter(Vuelo::estaDisponible)
                                           .filter(v -> v.getCapacidadDisponible() > 0)
                                           .collect(Collectors.toList());
    }

    /**
     * Busca conexiones posibles desde un aeropuerto
     * @param aeropuertoIntermedio Aeropuerto de conexión
     * @param tiempoMinimoConexion Tiempo mínimo de conexión en minutos
     * @return Lista de pares de vuelos que pueden conectarse
     */
    public List<ConexionVuelos> buscarConexiones(String aeropuertoIntermedio, int tiempoMinimoConexion) {
        List<ConexionVuelos> conexiones = new ArrayList<>();

        List<Vuelo> vuelosLlegada = buscarPorDestino(aeropuertoIntermedio);
        List<Vuelo> vuelosSalida = buscarPorOrigen(aeropuertoIntermedio);

        for (Vuelo vueloLlegada : vuelosLlegada) {
            for (Vuelo vueloSalida : vuelosSalida) {
                if (vueloLlegada.puedeConectarCon(vueloSalida, tiempoMinimoConexion)) {
                    conexiones.add(new ConexionVuelos(vueloLlegada, vueloSalida, aeropuertoIntermedio));
                }
            }
        }

        return conexiones;
    }

    /**
     * Obtiene todos los aeropuertos únicos (origen y destino)
     * @return Array con códigos ICAO de aeropuertos
     */
    public String[] getAeropuertosUnicos() {
        var aeropuertos = new java.util.HashSet<String>();
        aeropuertos.addAll(indicePorOrigen.keySet());
        aeropuertos.addAll(indicePorDestino.keySet());
        return aeropuertos.toArray(new String[0]);
    }

    /**
     * Obtiene todas las rutas únicas
     * @return Array con rutas en formato ORIGEN-DESTINO
     */
    public String[] getRutasUnicas() {
        return indicePorRuta.keySet().toArray(new String[0]);
    }

    /**
     * Calcula estadísticas de los vuelos cargados
     * @return String con estadísticas detalladas
     */
    public String getEstadisticas() {
        if (indicePorId.isEmpty()) {
            return "No hay vuelos cargados";
        }

        int totalVuelos = indicePorId.size();
        int totalCapacidad = indicePorId.values().stream()
                                       .mapToInt(Vuelo::getCapacidadMaxima)
                                       .sum();

        int aeropuertosOrigen = indicePorOrigen.size();
        int aeropuertosDestino = indicePorDestino.size();
        int rutasUnicas = indicePorRuta.size();

        // Estadísticas por capacidad
        var estadisticasCapacidad = indicePorId.values().stream()
                                              .mapToInt(Vuelo::getCapacidadMaxima)
                                              .summaryStatistics();

        // Top rutas por frecuencia
        var topRutas = indicePorRuta.entrySet().stream()
                                   .sorted(Map.Entry.<String, List<Vuelo>>comparingByValue(
                                       (a, b) -> Integer.compare(b.size(), a.size())))
                                   .limit(5)
                                   .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTADÍSTICAS VUELOS ===\n");
        sb.append("Total vuelos: ").append(totalVuelos).append("\n");
        sb.append("Capacidad total: ").append(totalCapacidad).append(" productos\n");
        sb.append("Aeropuertos origen: ").append(aeropuertosOrigen).append("\n");
        sb.append("Aeropuertos destino: ").append(aeropuertosDestino).append("\n");
        sb.append("Rutas únicas: ").append(rutasUnicas).append("\n");

        sb.append("\nCapacidades:\n");
        sb.append(String.format("  Mínima: %d productos\n", estadisticasCapacidad.getMin()));
        sb.append(String.format("  Máxima: %d productos\n", estadisticasCapacidad.getMax()));
        sb.append(String.format("  Promedio: %.1f productos\n", estadisticasCapacidad.getAverage()));

        sb.append("\nTop 5 rutas por frecuencia:\n");
        for (var entry : topRutas) {
            sb.append(String.format("  %s: %d vuelos\n", entry.getKey(), entry.getValue().size()));
        }

        return sb.toString();
    }

    /**
     * Valida la integridad de los vuelos cargados
     * @param cargadorAeropuertos Cargador de aeropuertos para validar códigos
     * @throws CargadorException si hay problemas de integridad
     */
    public void validarIntegridad(CargadorAeropuertos cargadorAeropuertos) throws CargadorException {
        if (indicePorId.isEmpty()) {
            throw new CargadorException("No hay vuelos cargados para validar");
        }

        List<String> aeropuertosInvalidos = new ArrayList<>();
        List<String> vuelosProblematicos = new ArrayList<>();

        for (Vuelo vuelo : indicePorId.values()) {
            // Validar que origen y destino existen en aeropuertos
            if (!cargadorAeropuertos.existeICAO(vuelo.getAeropuertoOrigen())) {
                aeropuertosInvalidos.add(vuelo.getAeropuertoOrigen());
                vuelosProblematicos.add(vuelo.getIdVuelo() + " (origen inválido)");
            }

            if (!cargadorAeropuertos.existeICAO(vuelo.getAeropuertoDestino())) {
                aeropuertosInvalidos.add(vuelo.getAeropuertoDestino());
                vuelosProblematicos.add(vuelo.getIdVuelo() + " (destino inválido)");
            }

            // Validar duración razonable (no más de 24 horas)
            if (vuelo.calcularDuracion().toHours() > 24) {
                vuelosProblematicos.add(vuelo.getIdVuelo() + " (duración > 24h)");
            }
        }

        if (!aeropuertosInvalidos.isEmpty() || !vuelosProblematicos.isEmpty()) {
            StringBuilder error = new StringBuilder("Problemas de integridad encontrados:\n");

            if (!aeropuertosInvalidos.isEmpty()) {
                error.append("Aeropuertos inválidos: ").append(aeropuertosInvalidos).append("\n");
            }

            if (!vuelosProblematicos.isEmpty()) {
                error.append("Vuelos problemáticos: ").append(vuelosProblematicos).append("\n");
            }

            throw new CargadorException(error.toString());
        }
    }

    /**
     * Reinicia las capacidades de todos los vuelos
     */
    public void reiniciarCapacidades() {
        for (Vuelo vuelo : indicePorId.values()) {
            vuelo.reiniciarCapacidad();
        }
    }

    // Getters para los índices
    public Map<String, Vuelo> getIndicePorId() {
        return new HashMap<>(indicePorId);
    }

    public Map<String, List<Vuelo>> getIndicePorOrigen() {
        return new HashMap<>(indicePorOrigen);
    }

    public Map<String, List<Vuelo>> getIndicePorDestino() {
        return new HashMap<>(indicePorDestino);
    }

    public Map<String, List<Vuelo>> getIndicePorRuta() {
        return new HashMap<>(indicePorRuta);
    }

    /**
     * Clase interna para representar conexiones entre vuelos
     */
    public static class ConexionVuelos {
        private final Vuelo vueloLlegada;
        private final Vuelo vueloSalida;
        private final String aeropuertoConexion;

        public ConexionVuelos(Vuelo vueloLlegada, Vuelo vueloSalida, String aeropuertoConexion) {
            this.vueloLlegada = vueloLlegada;
            this.vueloSalida = vueloSalida;
            this.aeropuertoConexion = aeropuertoConexion;
        }

        public Vuelo getVueloLlegada() { return vueloLlegada; }
        public Vuelo getVueloSalida() { return vueloSalida; }
        public String getAeropuertoConexion() { return aeropuertoConexion; }

        @Override
        public String toString() {
            return String.format("Conexión en %s: %s → %s",
                aeropuertoConexion, vueloLlegada.getIdVuelo(), vueloSalida.getIdVuelo());
        }
    }
}