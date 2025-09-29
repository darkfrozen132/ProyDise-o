package morapack.datos.cargadores;

import morapack.datos.modelos.Pedido;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cargador específico para archivos de pedidos mensuales.
 * Maneja la carga de pedidos desde archivos CSV con formato pedidos_XX.csv
 */
public class CargadorPedidos extends CargadorCSV<Pedido> {

    private final int mes;
    private final int anio;
    private Map<String, Pedido> indicePorId;
    private Map<String, List<Pedido>> indicePorCliente;
    private Map<String, List<Pedido>> indicePorDestino;

    /**
     * Constructor del cargador de pedidos
     * @param rutaArchivo Ruta del archivo de pedidos CSV
     * @param mes Mes correspondiente al archivo (1-12)
     * @param anio Año de los pedidos
     */
    public CargadorPedidos(String rutaArchivo, int mes, int anio) {
        super(rutaArchivo, true); // El archivo tiene encabezado "IdPedido"

        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mes debe estar entre 1 y 12: " + mes);
        }
        if (anio < 2000) {
            throw new IllegalArgumentException("Año debe ser mayor a 2000: " + anio);
        }

        this.mes = mes;
        this.anio = anio;
        this.indicePorId = new HashMap<>();
        this.indicePorCliente = new HashMap<>();
        this.indicePorDestino = new HashMap<>();
    }

    /**
     * Constructor con ruta automática basada en mes
     * @param mes Mes del archivo (1-12)
     * @param anio Año de los pedidos
     */
    public CargadorPedidos(int mes, int anio) {
        this(String.format("datos/pedidos/pedidos_%02d.csv", mes), mes, anio);
    }

    @Override
    protected Pedido procesarLinea(String linea, int numeroLinea) throws Exception {
        try {
            Pedido pedido = Pedido.desdeCSV(linea, mes, anio);

            // Verificar duplicados por ID
            if (indicePorId.containsKey(pedido.getIdPedido())) {
                throw new CargadorException(
                    String.format("ID de pedido duplicado: %s", pedido.getIdPedido())
                );
            }

            // Solo procesar pedidos que no van a sedes principales
            if (!pedido.esProcesable()) {
                // Logging opcional: pedido a sede principal ignorado
                return null; // No agregar a la lista
            }

            return pedido;

        } catch (IllegalArgumentException e) {
            throw new CargadorException(
                String.format("Error al crear pedido desde línea: %s", e.getMessage()),
                e
            );
        }
    }

    /**
     * Carga todos los pedidos válidos y construye índices
     * @return Lista de pedidos procesables cargados
     * @throws Exception si hay error en la carga
     */
    public List<Pedido> cargarConIndices() throws Exception {
        List<Pedido> pedidos = cargarTodos();

        // Construir índices
        indicePorId.clear();
        indicePorCliente.clear();
        indicePorDestino.clear();

        for (Pedido pedido : pedidos) {
            // Índice por ID
            indicePorId.put(pedido.getIdPedido(), pedido);

            // Índice por cliente
            indicePorCliente.computeIfAbsent(pedido.getIdCliente(), k -> new ArrayList<>())
                           .add(pedido);

            // Índice por destino
            indicePorDestino.computeIfAbsent(pedido.getCodigoDestino(), k -> new ArrayList<>())
                           .add(pedido);
        }

        return pedidos;
    }

    /**
     * Carga solo pedidos para un destino específico
     * @param codigoDestino Código ICAO del destino
     * @return Lista de pedidos para el destino
     * @throws Exception si hay error en la carga
     */
    public List<Pedido> cargarPorDestino(String codigoDestino) throws Exception {
        List<Pedido> todosPedidos = cargarConIndices();

        return todosPedidos.stream()
                          .filter(p -> p.getCodigoDestino().equals(codigoDestino))
                          .collect(Collectors.toList());
    }

    /**
     * Busca un pedido por su ID
     * @param idPedido ID del pedido
     * @return Pedido encontrado o null si no existe
     */
    public Pedido buscarPorId(String idPedido) {
        if (idPedido == null) {
            return null;
        }
        return indicePorId.get(idPedido);
    }

    /**
     * Busca todos los pedidos de un cliente
     * @param idCliente ID del cliente
     * @return Lista de pedidos del cliente
     */
    public List<Pedido> buscarPorCliente(String idCliente) {
        if (idCliente == null) {
            return new ArrayList<>();
        }
        return indicePorCliente.getOrDefault(idCliente, new ArrayList<>());
    }

    /**
     * Busca todos los pedidos para un destino
     * @param codigoDestino Código ICAO del destino
     * @return Lista de pedidos para el destino
     */
    public List<Pedido> buscarPorDestino(String codigoDestino) {
        if (codigoDestino == null) {
            return new ArrayList<>();
        }
        return indicePorDestino.getOrDefault(codigoDestino, new ArrayList<>());
    }

    /**
     * Obtiene todos los destinos únicos en los pedidos
     * @return Array con códigos ICAO de destinos
     */
    public String[] getDestinosUnicos() {
        return indicePorDestino.keySet().toArray(new String[0]);
    }

    /**
     * Obtiene todos los clientes únicos
     * @return Array con IDs de clientes
     */
    public String[] getClientesUnicos() {
        return indicePorCliente.keySet().toArray(new String[0]);
    }

    /**
     * Calcula estadísticas de los pedidos cargados
     * @return String con estadísticas detalladas
     */
    public String getEstadisticas() {
        if (indicePorId.isEmpty()) {
            return "No hay pedidos cargados";
        }

        int totalPedidos = indicePorId.size();
        int totalProductos = indicePorId.values().stream()
                                       .mapToInt(Pedido::getCantidadProductos)
                                       .sum();

        // Estadísticas por destino
        Map<String, Integer> pedidosPorDestino = new HashMap<>();
        Map<String, Integer> productosPorDestino = new HashMap<>();

        for (Pedido pedido : indicePorId.values()) {
            String destino = pedido.getCodigoDestino();
            pedidosPorDestino.put(destino, pedidosPorDestino.getOrDefault(destino, 0) + 1);
            productosPorDestino.put(destino,
                productosPorDestino.getOrDefault(destino, 0) + pedido.getCantidadProductos());
        }

        // Estadísticas por cliente
        int clientesUnicos = indicePorCliente.size();
        double promedioPedidosPorCliente = (double) totalPedidos / clientesUnicos;

        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTADÍSTICAS PEDIDOS ===\n");
        sb.append(String.format("Mes/Año: %02d/%d\n", mes, anio));
        sb.append("Total pedidos: ").append(totalPedidos).append("\n");
        sb.append("Total productos: ").append(totalProductos).append("\n");
        sb.append("Clientes únicos: ").append(clientesUnicos).append("\n");
        sb.append(String.format("Promedio pedidos/cliente: %.2f\n", promedioPedidosPorCliente));
        sb.append("Destinos únicos: ").append(indicePorDestino.size()).append("\n");

        sb.append("\nTop 5 destinos por pedidos:\n");
        pedidosPorDestino.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(5)
                        .forEach(entry -> sb.append(String.format("  %s: %d pedidos (%d productos)\n",
                            entry.getKey(), entry.getValue(),
                            productosPorDestino.get(entry.getKey()))));

        return sb.toString();
    }

    /**
     * Valida la integridad de los pedidos cargados
     * @param cargadorAeropuertos Cargador de aeropuertos para validar destinos
     * @throws CargadorException si hay problemas de integridad
     */
    public void validarIntegridad(CargadorAeropuertos cargadorAeropuertos) throws CargadorException {
        if (indicePorId.isEmpty()) {
            throw new CargadorException("No hay pedidos cargados para validar");
        }

        List<String> destinosInvalidos = new ArrayList<>();
        List<String> pedidosProblematicos = new ArrayList<>();

        for (Pedido pedido : indicePorId.values()) {
            // Validar que el destino existe en aeropuertos
            if (!cargadorAeropuertos.existeICAO(pedido.getCodigoDestino())) {
                destinosInvalidos.add(pedido.getCodigoDestino());
                pedidosProblematicos.add(pedido.getIdPedido());
            }

            // Validar que no sea destino a sede principal (ya debería estar filtrado)
            if (!pedido.esProcesable()) {
                pedidosProblematicos.add(pedido.getIdPedido() + " (destino a sede principal)");
            }
        }

        if (!destinosInvalidos.isEmpty() || !pedidosProblematicos.isEmpty()) {
            StringBuilder error = new StringBuilder("Problemas de integridad encontrados:\n");

            if (!destinosInvalidos.isEmpty()) {
                error.append("Destinos inválidos: ").append(destinosInvalidos).append("\n");
            }

            if (!pedidosProblematicos.isEmpty()) {
                error.append("Pedidos problemáticos: ").append(pedidosProblematicos).append("\n");
            }

            throw new CargadorException(error.toString());
        }
    }

    /**
     * Filtra pedidos por rango de productos
     * @param minProductos Mínimo de productos
     * @param maxProductos Máximo de productos
     * @return Lista de pedidos en el rango
     */
    public List<Pedido> filtrarPorRangoProductos(int minProductos, int maxProductos) {
        return indicePorId.values().stream()
                         .filter(p -> p.getCantidadProductos() >= minProductos &&
                                     p.getCantidadProductos() <= maxProductos)
                         .collect(Collectors.toList());
    }

    // Getters
    public int getMes() { return mes; }
    public int getAnio() { return anio; }

    public Map<String, Pedido> getIndicePorId() {
        return new HashMap<>(indicePorId);
    }

    public Map<String, List<Pedido>> getIndicePorCliente() {
        return new HashMap<>(indicePorCliente);
    }

    public Map<String, List<Pedido>> getIndicePorDestino() {
        return new HashMap<>(indicePorDestino);
    }
}