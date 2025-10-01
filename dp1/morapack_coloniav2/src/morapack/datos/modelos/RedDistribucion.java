package morapack.datos.modelos;

import morapack.datos.cargadores.CargadorAeropuertos;
import morapack.datos.cargadores.CargadorVuelos;
import morapack.datos.cargadores.CargadorPedidos;
import morapack.datos.cargadores.CargadorException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Representa la red completa de distribución de MoraPack.
 * Integra aeropuertos, vuelos, pedidos y clientes en un sistema cohesivo
 * para su uso con el algoritmo ACO.
 */
public class RedDistribucion {

    // Cargadores de datos
    private CargadorAeropuertos cargadorAeropuertos;
    private CargadorVuelos cargadorVuelos;
    private CargadorPedidos cargadorPedidos;

    // Datos principales
    private Map<String, Aeropuerto> aeropuertos;
    private Map<String, Vuelo> vuelos;
    private Map<String, Pedido> pedidos;
    private Map<String, Cliente> clientes;

    // Índices de conectividad
    private Map<String, Set<String>> grafoConectividad;
    private Map<String, List<Vuelo>> vuelosPorRuta;

    // Cache de rutas minimas (optimizacion para ACO)
    private Map<String, List<String>> cacheRutasMinimas;
    private boolean cacheRutasConstruido = false;

    // Configuración
    private int tiempoMinimoConexion = 60; // minutos
    private LocalDateTime tiempoReferencia;

    /**
     * Constructor de la red de distribución
     */
    public RedDistribucion() {
        this.aeropuertos = new HashMap<>();
        this.vuelos = new HashMap<>();
        this.pedidos = new HashMap<>();
        this.clientes = new HashMap<>();
        this.grafoConectividad = new HashMap<>();
        this.vuelosPorRuta = new HashMap<>();
        this.cacheRutasMinimas = new HashMap<>();
        this.tiempoReferencia = LocalDateTime.now();
    }

    /**
     * Inicializa la red completa cargando todos los datos
     * @param rutaAeropuertos Ruta del archivo aeropuertos.csv
     * @param rutaVuelos Ruta del archivo planes_de_vuelo.csv
     * @param rutaPedidos Ruta del archivo pedidos_XX.csv
     * @param mes Mes de los pedidos (1-12)
     * @param anio Año de los pedidos
     * @throws Exception si hay error en la carga
     */
    public void inicializar(String rutaAeropuertos, String rutaVuelos, String rutaPedidos,
                           int mes, int anio) throws Exception {

        System.out.println("Inicializando Red de Distribución MoraPack...");

        // Cargar aeropuertos
        System.out.println("Cargando aeropuertos...");
        cargadorAeropuertos = new CargadorAeropuertos(rutaAeropuertos);
        List<Aeropuerto> listaAeropuertos = cargadorAeropuertos.cargarConIndices();

        for (Aeropuerto aeropuerto : listaAeropuertos) {
            aeropuertos.put(aeropuerto.getCodigoICAO(), aeropuerto);
        }

        // Cargar vuelos
        System.out.println("Cargando vuelos...");
        cargadorVuelos = new CargadorVuelos(rutaVuelos);
        List<Vuelo> listaVuelos = cargadorVuelos.cargarConIndices();

        for (Vuelo vuelo : listaVuelos) {
            vuelos.put(vuelo.getIdVuelo(), vuelo);
        }

        // Cargar pedidos
        System.out.println("Cargando pedidos...");
        cargadorPedidos = new CargadorPedidos(rutaPedidos, mes, anio);
        List<Pedido> listaPedidos = cargadorPedidos.cargarConIndices();

        for (Pedido pedido : listaPedidos) {
            pedidos.put(pedido.getIdPedido(), pedido);
        }

        // Validar integridad
        System.out.println("Validando integridad de datos...");
        validarIntegridad();

        // Construir índices de conectividad
        System.out.println("Construyendo índices de conectividad...");
        construirGrafoConectividad();
        construirIndicesVuelos();

        // Generar clientes a partir de pedidos
        System.out.println("Generando información de clientes...");
        generarClientes();

        // Calcular plazos de entrega
        System.out.println("Calculando plazos de entrega...");
        calcularPlazosEntrega();

        // Pre-calcular cache de rutas minimas (OPTIMIZACION CRITICA)
        System.out.println("Pre-calculando cache de rutas minimas...");
        construirCacheRutasMinimas();

        System.out.println("Red de distribución inicializada exitosamente.");
        mostrarEstadisticas();
    }

    /**
     * Inicializa con rutas por defecto
     * @param mes Mes de los pedidos
     * @param anio Año de los pedidos
     * @throws Exception si hay error en la carga
     */
    public void inicializar(int mes, int anio) throws Exception {
        inicializar("datos/aeropuertos.csv", "datos/planes_de_vuelo.csv",
                   String.format("datos/pedidos/pedidos_%02d.csv", mes), mes, anio);
    }

    /**
     * Valida la integridad cruzada de todos los datos
     * @throws CargadorException si hay problemas de integridad
     */
    private void validarIntegridad() throws CargadorException {
        cargadorAeropuertos.validarIntegridad();
        cargadorVuelos.validarIntegridad(cargadorAeropuertos);
        cargadorPedidos.validarIntegridad(cargadorAeropuertos);
    }

    /**
     * Construye el grafo de conectividad entre aeropuertos
     */
    private void construirGrafoConectividad() {
        grafoConectividad.clear();

        // Inicializar todos los aeropuertos
        for (String aeropuerto : aeropuertos.keySet()) {
            grafoConectividad.put(aeropuerto, new HashSet<>());
        }

        // Agregar conexiones basadas en vuelos
        for (Vuelo vuelo : vuelos.values()) {
            String origen = vuelo.getAeropuertoOrigen();
            String destino = vuelo.getAeropuertoDestino();

            grafoConectividad.get(origen).add(destino);
        }
    }

    /**
     * Construye índices de vuelos por ruta
     */
    private void construirIndicesVuelos() {
        vuelosPorRuta.clear();

        for (Vuelo vuelo : vuelos.values()) {
            String ruta = vuelo.getAeropuertoOrigen() + "-" + vuelo.getAeropuertoDestino();
            vuelosPorRuta.computeIfAbsent(ruta, k -> new ArrayList<>()).add(vuelo);
        }
    }

    /**
     * Genera clientes a partir de los pedidos cargados
     */
    private void generarClientes() {
        clientes.clear();

        for (Pedido pedido : pedidos.values()) {
            String idCliente = pedido.getIdCliente();

            // Crear cliente si no existe
            if (!clientes.containsKey(idCliente)) {
                clientes.put(idCliente, new Cliente(idCliente));
            }

            // Registrar pedido en el cliente
            clientes.get(idCliente).registrarPedido(pedido);
        }
    }

    /**
     * Calcula plazos de entrega para todos los pedidos
     */
    private void calcularPlazosEntrega() {
        for (Pedido pedido : pedidos.values()) {
            // Buscar aeropuerto destino
            Aeropuerto destino = aeropuertos.get(pedido.getCodigoDestino());
            if (destino != null) {
                // Usar sede principal más cercana como origen
                Aeropuerto origen = buscarSedeOptima(destino);
                if (origen != null) {
                    pedido.calcularPlazoEntrega(origen, destino);
                }
            }
        }
    }

    /**
     * Busca la sede principal óptima para un destino
     * @param destino Aeropuerto destino
     * @return Sede principal más cercana/conveniente
     */
    public Aeropuerto buscarSedeOptima(Aeropuerto destino) {
        List<Aeropuerto> sedes = aeropuertos.values().stream()
                                          .filter(Aeropuerto::esSedePrincipal)
                                          .collect(Collectors.toList());

        if (sedes.isEmpty()) {
            return null;
        }

        // Priorizar sede del mismo continente
        Optional<Aeropuerto> sedeMismoContinente = sedes.stream()
                .filter(sede -> sede.esMismoContinente(destino))
                .findFirst();

        if (sedeMismoContinente.isPresent()) {
            return sedeMismoContinente.get();
        }

        // Si no hay sede del mismo continente, usar la más cercana
        return sedes.stream()
                   .min(Comparator.comparingDouble(sede -> sede.calcularDistancia(destino)))
                   .orElse(sedes.get(0));
    }

    /**
     * Busca vuelos directos entre dos aeropuertos
     * @param origen Código ICAO origen
     * @param destino Código ICAO destino
     * @return Lista de vuelos directos disponibles
     */
    public List<Vuelo> buscarVuelosDirectos(String origen, String destino) {
        String ruta = origen + "-" + destino;
        return vuelosPorRuta.getOrDefault(ruta, new ArrayList<>())
                           .stream()
                           .filter(Vuelo::estaDisponible)
                           .collect(Collectors.toList());
    }

    /**
     * Busca todas las conexiones posibles desde un aeropuerto
     * @param codigoAeropuerto Código ICAO del aeropuerto
     * @return Lista de aeropuertos conectados directamente
     */
    public List<String> buscarConexionesDirectas(String codigoAeropuerto) {
        return new ArrayList<>(grafoConectividad.getOrDefault(codigoAeropuerto, new HashSet<>()));
    }

    /**
     * Verifica si dos aeropuertos están conectados directamente
     * @param origen Código ICAO origen
     * @param destino Código ICAO destino
     * @return true si hay conexión directa
     */
    public boolean tieneConexionDirecta(String origen, String destino) {
        return grafoConectividad.getOrDefault(origen, new HashSet<>()).contains(destino);
    }

    /**
     * Busca ruta más corta entre dos aeropuertos usando BFS con cache
     * OPTIMIZADO: Usa cache pre-calculado para evitar BFS repetidos
     * @param origen Código ICAO origen
     * @param destino Código ICAO destino
     * @return Lista con la ruta más corta (null si no hay ruta)
     */
    public List<String> buscarRutaMinima(String origen, String destino) {
        // Usar cache si está disponible
        if (cacheRutasConstruido) {
            String clave = origen + "->" + destino;
            return cacheRutasMinimas.get(clave);
        }

        // Fallback a BFS tradicional si no hay cache
        return buscarRutaMinimaBFS(origen, destino);
    }

    /**
     * Busca ruta minima usando BFS (version sin cache)
     */
    private List<String> buscarRutaMinimaBFS(String origen, String destino) {
        if (!aeropuertos.containsKey(origen) || !aeropuertos.containsKey(destino)) {
            return null;
        }

        if (origen.equals(destino)) {
            return Arrays.asList(origen);
        }

        Queue<String> cola = new LinkedList<>();
        Map<String, String> padres = new HashMap<>();
        Set<String> visitados = new HashSet<>();

        cola.offer(origen);
        visitados.add(origen);
        padres.put(origen, null);

        while (!cola.isEmpty()) {
            String actual = cola.poll();

            for (String vecino : grafoConectividad.getOrDefault(actual, new HashSet<>())) {
                if (!visitados.contains(vecino)) {
                    visitados.add(vecino);
                    padres.put(vecino, actual);
                    cola.offer(vecino);

                    if (vecino.equals(destino)) {
                        // Reconstruir ruta
                        List<String> ruta = new ArrayList<>();
                        String nodo = destino;
                        while (nodo != null) {
                            ruta.add(0, nodo);
                            nodo = padres.get(nodo);
                        }
                        return ruta;
                    }
                }
            }
        }

        return null; // No hay ruta
    }

    /**
     * Pre-calcula todas las rutas minimas entre aeropuertos importantes
     * OPTIMIZACION CRITICA: Reduce complejidad de O(n*BFS) a O(1) por consulta
     */
    private void construirCacheRutasMinimas() {
        cacheRutasMinimas.clear();

        // Solo pre-calcular rutas desde sedes principales a todos los destinos
        List<String> sedesPrincipales = Arrays.asList("SPIM", "EBCI", "UBBB");
        Set<String> destinosRelevantes = new HashSet<>(aeropuertos.keySet());

        int totalCalculos = sedesPrincipales.size() * destinosRelevantes.size();
        int calculosRealizados = 0;

        for (String sede : sedesPrincipales) {
            if (!aeropuertos.containsKey(sede)) continue;

            for (String destino : destinosRelevantes) {
                List<String> ruta = buscarRutaMinimaBFS(sede, destino);
                String clave = sede + "->" + destino;
                cacheRutasMinimas.put(clave, ruta);

                calculosRealizados++;
                if (calculosRealizados % 100 == 0) {
                    System.out.println("   Progreso cache: " + calculosRealizados + "/" + totalCalculos);
                }
            }
        }

        cacheRutasConstruido = true;
        System.out.println("   Cache construido: " + cacheRutasMinimas.size() + " rutas pre-calculadas");
    }

    /**
     * Obtiene pedidos prioritarios (clientes VIP, urgentes, etc.)
     * @return Lista de pedidos con alta prioridad
     */
    public List<Pedido> obtenerPedidosPrioritarios() {
        return pedidos.values().stream()
                     .filter(pedido -> {
                         Cliente cliente = clientes.get(pedido.getIdCliente());
                         return cliente != null && cliente.getPrioridad() <= 2;
                     })
                     .sorted(Comparator.comparing(pedido -> {
                         Cliente cliente = clientes.get(pedido.getIdCliente());
                         return cliente != null ? cliente.getPrioridad() : 5;
                     }))
                     .collect(Collectors.toList());
    }

    /**
     * Obtiene pedidos por destino específico
     * @param codigoDestino Código ICAO del destino
     * @return Lista de pedidos para ese destino
     */
    public List<Pedido> obtenerPedidosPorDestino(String codigoDestino) {
        return pedidos.values().stream()
                     .filter(pedido -> pedido.getCodigoDestino().equals(codigoDestino))
                     .collect(Collectors.toList());
    }

    /**
     * Reinicia las capacidades de todos los vuelos
     */
    public void reiniciarCapacidades() {
        for (Vuelo vuelo : vuelos.values()) {
            vuelo.reiniciarCapacidad();
        }
        for (Aeropuerto aeropuerto : aeropuertos.values()) {
            aeropuerto.reiniciarCapacidad();
        }
    }

    /**
     * Muestra estadísticas completas de la red
     */
    public void mostrarEstadisticas() {
        System.out.println("\n=== ESTADÍSTICAS RED DE DISTRIBUCIÓN ===");
        System.out.println("Aeropuertos: " + aeropuertos.size());
        System.out.println("Vuelos: " + vuelos.size());
        System.out.println("Pedidos: " + pedidos.size());
        System.out.println("Clientes: " + clientes.size());
        System.out.println("Rutas únicas: " + vuelosPorRuta.size());

        // Estadísticas de conectividad
        int conexionesTotales = grafoConectividad.values().stream()
                                                .mapToInt(Set::size)
                                                .sum();
        System.out.println("Conexiones totales: " + conexionesTotales);

        // Sedes principales
        long sedesPrincipales = aeropuertos.values().stream()
                                          .filter(Aeropuerto::esSedePrincipal)
                                          .count();
        System.out.println("Sedes principales: " + sedesPrincipales);

        // Clientes VIP
        long clientesVIP = clientes.values().stream()
                                  .filter(Cliente::esVIP)
                                  .count();
        System.out.println("Clientes VIP: " + clientesVIP);
    }

    /**
     * Obtiene resumen de la red para debugging
     * @return String con información resumida
     */
    public String getResumenRed() {
        return String.format("RedDistribucion[aeropuertos=%d, vuelos=%d, pedidos=%d, clientes=%d]",
            aeropuertos.size(), vuelos.size(), pedidos.size(), clientes.size());
    }

    // Getters principales
    public Map<String, Aeropuerto> getAeropuertos() { return new HashMap<>(aeropuertos); }
    public Map<String, Vuelo> getVuelos() { return new HashMap<>(vuelos); }
    public Map<String, Pedido> getPedidos() { return new HashMap<>(pedidos); }
    public Map<String, Cliente> getClientes() { return new HashMap<>(clientes); }
    public Map<String, Set<String>> getGrafoConectividad() { return new HashMap<>(grafoConectividad); }

    // Getters de componentes específicos
    public Aeropuerto getAeropuerto(String codigo) { return aeropuertos.get(codigo); }
    public Vuelo getVuelo(String id) { return vuelos.get(id); }
    public Pedido getPedido(String id) { return pedidos.get(id); }
    public Cliente getCliente(String id) { return clientes.get(id); }

    // Configuración
    public int getTiempoMinimoConexion() { return tiempoMinimoConexion; }
    public void setTiempoMinimoConexion(int tiempoMinimoConexion) {
        this.tiempoMinimoConexion = Math.max(0, tiempoMinimoConexion);
    }

    public LocalDateTime getTiempoReferencia() { return tiempoReferencia; }
    public void setTiempoReferencia(LocalDateTime tiempoReferencia) {
        this.tiempoReferencia = tiempoReferencia;
    }
}