package morapack.datos.modelos;

import morapack.datos.cargadores.CargadorAeropuertos;
import morapack.datos.cargadores.CargadorVuelos;
import morapack.datos.cargadores.CargadorPedidos;
import morapack.datos.cargadores.CargadorException;

import java.io.IOException;
import java.time.LocalDate;
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

    // NUEVO: Gestión de instancias de vuelos diarios
    private Map<String, VueloInstancia> instanciasVuelos; // Key: idInstancia (vuelo-fecha)
    private int mesOperacion;
    private int anioOperacion;

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
        this.instanciasVuelos = new HashMap<>();
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

        // Guardar mes y año de operación
        this.mesOperacion = mes;
        this.anioOperacion = anio;

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

    // ========================================================================
    // MÉTODOS NUEVOS: Gestión de vuelos diarios con VueloInstancia
    // ========================================================================

    /**
     * Busca instancias de vuelos disponibles en un rango de fechas.
     * Considera que cada plan de vuelo se repite diariamente.
     *
     * @param origen Código ICAO del aeropuerto origen
     * @param destino Código ICAO del aeropuerto destino
     * @param fechaInicio Fecha de inicio de búsqueda (inclusive)
     * @param fechaFin Fecha de fin de búsqueda (exclusive)
     * @return Lista de instancias de vuelos disponibles
     */
    public List<VueloInstancia> buscarVuelosDisponiblesEnRango(String origen, String destino,
                                                                LocalDate fechaInicio, LocalDate fechaFin) {
        List<VueloInstancia> instancias = new ArrayList<>();

        // Obtener plantillas de vuelos para esta ruta
        List<Vuelo> plantillas = buscarVuelosDirectos(origen, destino);

        // Expandir cada plantilla para todos los días en el rango
        for (Vuelo plantilla : plantillas) {
            for (LocalDate fecha = fechaInicio; fecha.isBefore(fechaFin); fecha = fecha.plusDays(1)) {
                VueloInstancia instancia = obtenerOCrearInstancia(plantilla, fecha);
                if (instancia.getCapacidadDisponible() > 0) {
                    instancias.add(instancia);
                }
            }
        }

        return instancias;
    }

    /**
     * Busca instancias de vuelos disponibles para una fecha específica.
     *
     * @param origen Código ICAO del aeropuerto origen
     * @param destino Código ICAO del aeropuerto destino
     * @param fecha Fecha específica
     * @return Lista de instancias de vuelos disponibles ese día
     */
    public List<VueloInstancia> buscarVuelosDisponiblesEnFecha(String origen, String destino, LocalDate fecha) {
        return buscarVuelosDisponiblesEnRango(origen, destino, fecha, fecha.plusDays(1));
    }

    /**
     * Obtiene o crea una instancia de vuelo para una fecha específica.
     * Implementa patrón lazy loading con caché.
     *
     * @param plantilla Vuelo plantilla del plan de vuelo
     * @param fecha Fecha específica
     * @return Instancia de vuelo para esa fecha
     */
    public VueloInstancia obtenerOCrearInstancia(Vuelo plantilla, LocalDate fecha) {
        if (plantilla == null || fecha == null) {
            throw new IllegalArgumentException("Plantilla y fecha no pueden ser null");
        }

        // Generar ID de instancia
        String idInstancia = String.format("%s-%04d%02d%02d",
            plantilla.getIdVuelo(),
            fecha.getYear(),
            fecha.getMonthValue(),
            fecha.getDayOfMonth()
        );

        // Buscar en caché o crear nueva instancia
        return instanciasVuelos.computeIfAbsent(idInstancia,
            k -> new VueloInstancia(plantilla, fecha)
        );
    }

    /**
     * Obtiene una instancia de vuelo específica si existe.
     *
     * @param idInstancia ID de la instancia (formato: VUELO-YYYYMMDD)
     * @return Instancia de vuelo o null si no existe
     */
    public VueloInstancia getInstanciaVuelo(String idInstancia) {
        return instanciasVuelos.get(idInstancia);
    }

    /**
     * Reserva capacidad en una instancia específica de vuelo.
     *
     * @param instancia Instancia de vuelo
     * @param cantidad Cantidad a reservar
     * @return true si se pudo reservar
     */
    public boolean reservarCapacidadEnInstancia(VueloInstancia instancia, int cantidad) {
        if (instancia == null) {
            return false;
        }

        return instancia.reservarCapacidad(cantidad);
    }

    /**
     * Libera capacidad en una instancia específica de vuelo.
     *
     * @param instancia Instancia de vuelo
     * @param cantidad Cantidad a liberar
     */
    public void liberarCapacidadEnInstancia(VueloInstancia instancia, int cantidad) {
        if (instancia != null) {
            instancia.liberarCapacidad(cantidad);
        }
    }

    /**
     * Reinicia las capacidades de todas las instancias de vuelos creadas.
     * También reinicia capacidades de vuelos plantilla y aeropuertos.
     */
    public void reiniciarTodasLasCapacidades() {
        // Reiniciar instancias
        for (VueloInstancia instancia : instanciasVuelos.values()) {
            instancia.reiniciarCapacidad();
        }

        // Reiniciar plantillas
        for (Vuelo vuelo : vuelos.values()) {
            vuelo.reiniciarCapacidad();
        }

        // Reiniciar aeropuertos
        for (Aeropuerto aeropuerto : aeropuertos.values()) {
            aeropuerto.reiniciarCapacidad();
        }
    }

    /**
     * Busca rutas con conexiones considerando ventanas temporales.
     * Permite encontrar rutas que requieren múltiples días.
     *
     * @param origen Código ICAO del aeropuerto origen
     * @param destino Código ICAO del aeropuerto destino
     * @param fechaInicio Fecha de inicio de búsqueda
     * @param fechaLimite Fecha límite de entrega
     * @param maxEscalas Número máximo de escalas permitidas
     * @return Lista de rutas posibles con instancias de vuelos
     */
    public List<List<VueloInstancia>> buscarRutasConConexiones(String origen, String destino,
                                                                LocalDate fechaInicio, LocalDate fechaLimite,
                                                                int maxEscalas) {
        List<List<VueloInstancia>> rutasEncontradas = new ArrayList<>();

        // Primero intentar vuelos directos en todo el rango
        List<VueloInstancia> vuelosDirectos = buscarVuelosDisponiblesEnRango(origen, destino, fechaInicio, fechaLimite);
        for (VueloInstancia vuelo : vuelosDirectos) {
            List<VueloInstancia> ruta = new ArrayList<>();
            ruta.add(vuelo);
            rutasEncontradas.add(ruta);
        }

        // Si no hay vuelos directos o se permiten escalas, buscar rutas con conexiones
        if (maxEscalas > 0) {
            List<String> rutaMinima = buscarRutaMinima(origen, destino);
            if (rutaMinima != null && rutaMinima.size() > 1 && rutaMinima.size() <= maxEscalas + 2) {
                // Construir rutas con escalas para diferentes fechas
                for (LocalDate fecha = fechaInicio; fecha.isBefore(fechaLimite.minusDays(1)); fecha = fecha.plusDays(1)) {
                    List<VueloInstancia> rutaConEscalas = construirRutaConEscalas(rutaMinima, fecha, fechaLimite);
                    if (rutaConEscalas != null && !rutaConEscalas.isEmpty()) {
                        rutasEncontradas.add(rutaConEscalas);
                    }
                }
            }
        }

        return rutasEncontradas;
    }

    /**
     * Construye una ruta con escalas para una fecha de inicio específica.
     * Verifica tiempos de conexión y llegada dentro del límite.
     *
     * @param rutaAeropuertos Lista de aeropuertos en la ruta
     * @param fechaInicio Fecha de inicio del primer vuelo
     * @param fechaLimite Fecha límite de llegada
     * @return Lista de instancias de vuelos o null si no es factible
     */
    private List<VueloInstancia> construirRutaConEscalas(List<String> rutaAeropuertos,
                                                         LocalDate fechaInicio, LocalDate fechaLimite) {
        List<VueloInstancia> segmentos = new ArrayList<>();
        LocalDateTime tiempoActual = fechaInicio.atStartOfDay();

        for (int i = 0; i < rutaAeropuertos.size() - 1; i++) {
            String origenSegmento = rutaAeropuertos.get(i);
            String destinoSegmento = rutaAeropuertos.get(i + 1);

            // Buscar vuelos disponibles desde el tiempo actual
            LocalDate fechaBusqueda = tiempoActual.toLocalDate();
            List<VueloInstancia> vuelosDisponibles = buscarVuelosDisponiblesEnFecha(
                origenSegmento, destinoSegmento, fechaBusqueda
            );

            // Filtrar vuelos que salen después del tiempo actual
            VueloInstancia vueloSeleccionado = null;
            for (VueloInstancia vuelo : vuelosDisponibles) {
                if (vuelo.getHorarioSalidaCompleto().isAfter(tiempoActual.plusMinutes(tiempoMinimoConexion))) {
                    vueloSeleccionado = vuelo;
                    break;
                }
            }

            if (vueloSeleccionado == null) {
                // Intentar al día siguiente
                fechaBusqueda = fechaBusqueda.plusDays(1);
                if (fechaBusqueda.isAfter(fechaLimite)) {
                    return null; // No hay tiempo suficiente
                }

                vuelosDisponibles = buscarVuelosDisponiblesEnFecha(origenSegmento, destinoSegmento, fechaBusqueda);
                if (!vuelosDisponibles.isEmpty()) {
                    vueloSeleccionado = vuelosDisponibles.get(0);
                } else {
                    return null; // No hay vuelos disponibles
                }
            }

            segmentos.add(vueloSeleccionado);
            tiempoActual = vueloSeleccionado.getHorarioLlegadaCompleto();

            // Verificar que no exceda el límite
            if (tiempoActual.toLocalDate().isAfter(fechaLimite)) {
                return null;
            }
        }

        return segmentos;
    }

    /**
     * Obtiene estadísticas de uso de instancias de vuelos.
     *
     * @return String con estadísticas de instancias creadas y utilizadas
     */
    public String getEstadisticasInstancias() {
        int totalInstancias = instanciasVuelos.size();
        long instanciasUsadas = instanciasVuelos.values().stream()
            .filter(inst -> inst.getCapacidadDisponible() < inst.getCapacidadMaxima())
            .count();

        int capacidadTotalDisponible = instanciasVuelos.values().stream()
            .mapToInt(VueloInstancia::getCapacidadDisponible)
            .sum();

        int capacidadTotalMaxima = instanciasVuelos.values().stream()
            .mapToInt(VueloInstancia::getCapacidadMaxima)
            .sum();

        double porcentajeUso = capacidadTotalMaxima > 0
            ? ((double)(capacidadTotalMaxima - capacidadTotalDisponible) / capacidadTotalMaxima) * 100.0
            : 0.0;

        return String.format("Instancias de Vuelos: %d creadas | %d usadas | %.1f%% ocupación",
            totalInstancias, instanciasUsadas, porcentajeUso);
    }

    /**
     * Calcula el número de días del mes de operación.
     *
     * @return Número de días en el mes
     */
    public int getDiasDelMes() {
        LocalDate primerDia = LocalDate.of(anioOperacion, mesOperacion, 1);
        return primerDia.lengthOfMonth();
    }

    /**
     * Obtiene la fecha de inicio del mes de operación.
     *
     * @return Fecha del primer día del mes
     */
    public LocalDate getFechaInicioMes() {
        return LocalDate.of(anioOperacion, mesOperacion, 1);
    }

    /**
     * Obtiene la fecha de fin del mes de operación.
     *
     * @return Fecha del último día del mes
     */
    public LocalDate getFechaFinMes() {
        return LocalDate.of(anioOperacion, mesOperacion, getDiasDelMes());
    }

    // Getters adicionales
    public int getMesOperacion() { return mesOperacion; }
    public int getAnioOperacion() { return anioOperacion; }
    public Map<String, VueloInstancia> getInstanciasVuelos() { return new HashMap<>(instanciasVuelos); }
}