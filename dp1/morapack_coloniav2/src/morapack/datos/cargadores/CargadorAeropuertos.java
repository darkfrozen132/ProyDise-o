package morapack.datos.cargadores;

import morapack.datos.modelos.Aeropuerto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cargador específico para el archivo aeropuertos.csv.
 * Maneja la carga y indexación de aeropuertos del sistema.
 */
public class CargadorAeropuertos extends CargadorCSV<Aeropuerto> {

    private Map<String, Aeropuerto> indiceICAO;
    private Map<String, Aeropuerto> indiceCodigo;

    /**
     * Constructor del cargador de aeropuertos
     * @param rutaArchivo Ruta del archivo aeropuertos.csv
     */
    public CargadorAeropuertos(String rutaArchivo) {
        super(rutaArchivo, true); // El archivo tiene encabezado
        this.indiceICAO = new HashMap<>();
        this.indiceCodigo = new HashMap<>();
    }

    /**
     * Constructor con ruta por defecto
     */
    public CargadorAeropuertos() {
        this("datos/aeropuertos.csv");
    }

    @Override
    protected Aeropuerto procesarLinea(String linea, int numeroLinea) throws Exception {
        try {
            Aeropuerto aeropuerto = Aeropuerto.desdeCSV(linea);

            // Verificar duplicados por código ICAO
            if (indiceICAO.containsKey(aeropuerto.getCodigoICAO())) {
                throw new CargadorException(
                    String.format("Código ICAO duplicado: %s", aeropuerto.getCodigoICAO())
                );
            }

            return aeropuerto;

        } catch (IllegalArgumentException e) {
            throw new CargadorException(
                String.format("Error al crear aeropuerto desde línea: %s", e.getMessage()),
                e
            );
        }
    }

    /**
     * Carga todos los aeropuertos y construye índices
     * @return Lista de aeropuertos cargados
     * @throws Exception si hay error en la carga
     */
    public List<Aeropuerto> cargarConIndices() throws Exception {
        List<Aeropuerto> aeropuertos = cargarTodos();

        // Construir índices
        indiceICAO.clear();
        indiceCodigo.clear();

        for (Aeropuerto aeropuerto : aeropuertos) {
            indiceICAO.put(aeropuerto.getCodigoICAO(), aeropuerto);
            if (!aeropuerto.getCodigoCorto().isEmpty()) {
                indiceCodigo.put(aeropuerto.getCodigoCorto(), aeropuerto);
            }
        }

        return aeropuertos;
    }

    /**
     * Busca un aeropuerto por código ICAO
     * @param codigoICAO Código ICAO a buscar
     * @return Aeropuerto encontrado o null si no existe
     */
    public Aeropuerto buscarPorICAO(String codigoICAO) {
        if (codigoICAO == null) {
            return null;
        }
        return indiceICAO.get(codigoICAO.toUpperCase());
    }

    /**
     * Busca un aeropuerto por código corto
     * @param codigoCorto Código corto a buscar
     * @return Aeropuerto encontrado o null si no existe
     */
    public Aeropuerto buscarPorCodigoCorto(String codigoCorto) {
        if (codigoCorto == null) {
            return null;
        }
        return indiceCodigo.get(codigoCorto.toLowerCase());
    }

    /**
     * Verifica si un código ICAO existe
     * @param codigoICAO Código a verificar
     * @return true si existe
     */
    public boolean existeICAO(String codigoICAO) {
        return buscarPorICAO(codigoICAO) != null;
    }

    /**
     * Obtiene todos los códigos ICAO válidos
     * @return Array con todos los códigos ICAO
     */
    public String[] getCodigosICAOValidos() {
        return indiceICAO.keySet().toArray(new String[0]);
    }

    /**
     * Obtiene estadísticas de los aeropuertos cargados
     * @return String con estadísticas
     */
    public String getEstadisticas() {
        if (indiceICAO.isEmpty()) {
            return "No hay aeropuertos cargados";
        }

        Map<String, Integer> porContinente = new HashMap<>();
        int sedesPrincipales = 0;
        int capacidadTotal = 0;

        for (Aeropuerto aeropuerto : indiceICAO.values()) {
            // Contar por continente
            String continente = aeropuerto.getContinente().getCodigo();
            porContinente.put(continente, porContinente.getOrDefault(continente, 0) + 1);

            // Contar sedes principales
            if (aeropuerto.esSedePrincipal()) {
                sedesPrincipales++;
            }

            // Sumar capacidad
            capacidadTotal += aeropuerto.getCapacidadAlmacen();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTADÍSTICAS AEROPUERTOS ===\n");
        sb.append("Total aeropuertos: ").append(indiceICAO.size()).append("\n");
        sb.append("Sedes principales: ").append(sedesPrincipales).append("\n");
        sb.append("Capacidad total: ").append(capacidadTotal).append(" productos\n");
        sb.append("Distribución por continente:\n");

        for (Map.Entry<String, Integer> entry : porContinente.entrySet()) {
            sb.append("  - ").append(entry.getKey()).append(": ")
              .append(entry.getValue()).append(" aeropuertos\n");
        }

        return sb.toString();
    }

    /**
     * Valida la integridad de los datos cargados
     * @throws CargadorException si hay problemas de integridad
     */
    public void validarIntegridad() throws CargadorException {
        if (indiceICAO.isEmpty()) {
            throw new CargadorException("No hay aeropuertos cargados para validar");
        }

        // Verificar que existan las sedes principales
        String[] sedesEsperadas = {"SPIM", "EBCI", "UBBB"};
        for (String sede : sedesEsperadas) {
            if (!existeICAO(sede)) {
                throw new CargadorException("Sede principal faltante: " + sede);
            }
        }

        // Verificar que todas las sedes principales estén marcadas correctamente
        for (String sede : sedesEsperadas) {
            Aeropuerto aeropuerto = buscarPorICAO(sede);
            if (!aeropuerto.esSedePrincipal()) {
                throw new CargadorException("Sede principal no reconocida: " + sede);
            }
        }

        // Verificar que cada continente tenga al menos un aeropuerto
        Map<String, Integer> porContinente = new HashMap<>();
        for (Aeropuerto aeropuerto : indiceICAO.values()) {
            String continente = aeropuerto.getContinente().getCodigo();
            porContinente.put(continente, porContinente.getOrDefault(continente, 0) + 1);
        }

        String[] continentesEsperados = {"SAM", "EUR", "ASI"};
        for (String continente : continentesEsperados) {
            if (!porContinente.containsKey(continente) || porContinente.get(continente) == 0) {
                throw new CargadorException("Continente sin aeropuertos: " + continente);
            }
        }
    }

    // Getters para los índices
    public Map<String, Aeropuerto> getIndiceICAO() {
        return new HashMap<>(indiceICAO);
    }

    public Map<String, Aeropuerto> getIndiceCodigo() {
        return new HashMap<>(indiceCodigo);
    }
}