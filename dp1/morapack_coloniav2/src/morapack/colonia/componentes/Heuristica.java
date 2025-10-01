package morapack.colonia.componentes;

import morapack.core.problema.ProblemaMoraPack;
import morapack.datos.modelos.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Gestiona la información heurística del algoritmo ACO para MoraPack.
 * La información heurística guía a las hormigas hacia decisiones
 * localmente prometedoras, independiente de las feromonas.
 *
 * En MoraPack, la heurística se centra en:
 * - Urgencia de pedidos
 * - Eficiencia de rutas
 * - Capacidad de vuelos
 * - Proximidad geográfica
 */
public class Heuristica {

    private ProblemaMoraPack problema;
    private Map<String, Double> cacheHeuristicaRutas;
    private Map<Integer, Double> cacheHeuristicaPedidos;
    private TipoHeuristica tipo;

    /**
     * Tipos de heurística disponibles para MoraPack
     */
    public enum TipoHeuristica {
        URGENCIA_PEDIDOS,     // Basada en plazos de entrega
        EFICIENCIA_RUTAS,     // Basada en eficiencia de rutas
        CAPACIDAD_VUELOS,     // Basada en capacidad disponible
        PROXIMIDAD_GEOGRAFICA, // Basada en proximidad continente/región
        HIBRIDA              // Combinación de múltiples factores
    }

    /**
     * Constructor para heurística MoraPack
     * @param problema Problema MoraPack con toda la información necesaria
     * @param tipo Tipo de heurística a aplicar
     */
    public Heuristica(ProblemaMoraPack problema, TipoHeuristica tipo) {
        this.problema = problema;
        this.tipo = tipo;
        this.cacheHeuristicaRutas = new HashMap<>();
        this.cacheHeuristicaPedidos = new HashMap<>();

        inicializarCacheHeuristico();
    }

    /**
     * Constructor con heurística híbrida (recomendado)
     * @param problema Problema MoraPack
     */
    public Heuristica(ProblemaMoraPack problema) {
        this(problema, TipoHeuristica.HIBRIDA);
    }

    /**
     * Inicializa cache heurístico precalculando valores comunes
     */
    private void inicializarCacheHeuristico() {
        // Precalcular heurísticas de rutas comunes
        List<String> sedes = Arrays.asList("SPIM", "EBCI", "UBBB");
        RedDistribucion red = problema.getRed();

        for (String sede : sedes) {
            for (Map.Entry<String, Aeropuerto> entry : red.getAeropuertos().entrySet()) {
                String codigoDestino = entry.getKey();
                if (!sedes.contains(codigoDestino)) {
                    String claveRuta = sede + "-" + codigoDestino;
                    double valor = calcularHeuristicaRuta(sede, codigoDestino);
                    cacheHeuristicaRutas.put(claveRuta, valor);
                }
            }
        }

        // Precalcular heurísticas de pedidos
        for (Pedido pedido : problema.getPedidos()) {
            int idPedido = Integer.parseInt(pedido.getIdPedido().split("-")[0]);
            double valor = calcularHeuristicaPedido(pedido);
            cacheHeuristicaPedidos.put(idPedido, valor);
        }
    }

    /**
     * Calcula valor heurístico para un pedido específico
     * @param pedido Pedido a evaluar
     * @return Valor heurístico normalizado [0.1, 1.0]
     */
    private double calcularHeuristicaPedido(Pedido pedido) {
        switch (tipo) {
            case URGENCIA_PEDIDOS:
                return calcularHeuristicaUrgencia(pedido);
            case EFICIENCIA_RUTAS:
                return calcularHeuristicaEficienciaRutas(pedido);
            case CAPACIDAD_VUELOS:
                return calcularHeuristicaCapacidad(pedido);
            case PROXIMIDAD_GEOGRAFICA:
                return calcularHeuristicaProximidad(pedido);
            case HIBRIDA:
            default:
                return calcularHeuristicaHibrida(pedido);
        }
    }

    /**
     * Calcula heurística basada en urgencia del pedido
     */
    private double calcularHeuristicaUrgencia(Pedido pedido) {
        LocalDateTime tiempoActual = problema.getTiempoInicio();
        Aeropuerto destino = problema.getRed().getAeropuerto(pedido.getCodigoDestino());

        if (destino == null) return 0.1;

        long horasRestantes = pedido.horasRestantesUTC(tiempoActual, destino);

        // Más urgente = mayor heurística
        if (horasRestantes <= 0) return 0.1; // Pedido vencido
        if (horasRestantes >= 72) return 0.2; // No urgente

        // Normalizar urgencia: [0.2, 1.0]
        double urgencia = Math.max(0.2, (72.0 - horasRestantes) / 72.0);

        // Bonus por cantidad (pedidos grandes son prioritarios)
        double factorCantidad = 1.0 + Math.log(1 + pedido.getCantidadProductos() / 100.0) * 0.2;

        return Math.min(1.0, urgencia * factorCantidad);
    }

    /**
     * Calcula heurística basada en eficiencia de rutas disponibles
     */
    private double calcularHeuristicaEficienciaRutas(Pedido pedido) {
        String destino = pedido.getCodigoDestino();
        List<String> sedes = Arrays.asList("SPIM", "EBCI", "UBBB");

        double mejorEficiencia = 0.0;

        for (String sede : sedes) {
            List<Vuelo> vuelosDirectos = problema.getRed().buscarVuelosDirectos(sede, destino);

            if (!vuelosDirectos.isEmpty()) {
                // Ruta directa disponible - alta eficiencia
                double capacidadPromedio = vuelosDirectos.stream()
                    .mapToDouble(Vuelo::getCapacidadDisponible)
                    .average().orElse(100.0);

                double eficiencia = Math.min(1.0, capacidadPromedio / 300.0); // Normalizar
                mejorEficiencia = Math.max(mejorEficiencia, eficiencia);
            } else {
                // Ruta con escalas - eficiencia reducida
                List<String> ruta = problema.getRed().buscarRutaMinima(sede, destino);
                if (ruta.size() <= 3) { // Máximo 2 escalas
                    double eficiencia = 0.5 / ruta.size(); // Penalizar escalas
                    mejorEficiencia = Math.max(mejorEficiencia, eficiencia);
                }
            }
        }

        return Math.max(0.1, mejorEficiencia);
    }

    /**
     * Calcula heurística basada en capacidad de vuelos disponibles
     */
    private double calcularHeuristicaCapacidad(Pedido pedido) {
        String destino = pedido.getCodigoDestino();
        int cantidadPedido = pedido.getCantidadProductos();

        double mejorCapacidad = 0.0;

        for (String sede : Arrays.asList("SPIM", "EBCI", "UBBB")) {
            List<Vuelo> vuelos = problema.getRed().buscarVuelosDirectos(sede, destino);

            if (!vuelos.isEmpty()) {
                int capacidadTotal = vuelos.stream()
                    .mapToInt(Vuelo::getCapacidadDisponible)
                    .sum();

                // Evaluar si puede manejar el pedido
                double ratio = Math.min(1.0, (double) capacidadTotal / cantidadPedido);
                mejorCapacidad = Math.max(mejorCapacidad, ratio);
            }
        }

        return Math.max(0.1, mejorCapacidad);
    }

    /**
     * Calcula heurística basada en proximidad geográfica
     */
    private double calcularHeuristicaProximidad(Pedido pedido) {
        String destino = pedido.getCodigoDestino();
        Aeropuerto aeropuertoDestino = problema.getRed().getAeropuerto(destino);

        if (aeropuertoDestino == null) return 0.1;

        double mejorProximidad = 0.0;

        for (String sede : Arrays.asList("SPIM", "EBCI", "UBBB")) {
            Aeropuerto aeropuertoSede = problema.getRed().getAeropuerto(sede);

            if (aeropuertoSede != null) {
                // Mismo continente = alta proximidad
                if (aeropuertoSede.getContinente().equals(aeropuertoDestino.getContinente())) {
                    mejorProximidad = Math.max(mejorProximidad, 0.9);
                } else {
                    // Diferente continente = proximidad media
                    mejorProximidad = Math.max(mejorProximidad, 0.4);
                }
            }
        }

        return Math.max(0.1, mejorProximidad);
    }

    /**
     * Calcula heurística híbrida combinando múltiples factores
     */
    private double calcularHeuristicaHibrida(Pedido pedido) {
        double urgencia = calcularHeuristicaUrgencia(pedido);
        double eficiencia = calcularHeuristicaEficienciaRutas(pedido);
        double capacidad = calcularHeuristicaCapacidad(pedido);
        double proximidad = calcularHeuristicaProximidad(pedido);

        // Pesos para cada factor
        double pesoUrgencia = 0.4;    // Urgencia es factor principal
        double pesoEficiencia = 0.3;  // Eficiencia importante
        double pesoCapacidad = 0.2;   // Capacidad moderada
        double pesoProximidad = 0.1;  // Proximidad como tie-breaker

        double heuristicaTotal = urgencia * pesoUrgencia +
                                eficiencia * pesoEficiencia +
                                capacidad * pesoCapacidad +
                                proximidad * pesoProximidad;

        return Math.max(0.1, Math.min(1.0, heuristicaTotal));
    }

    /**
     * Obtiene el valor heurístico para un pedido
     * @param pedido Pedido a evaluar
     * @return Valor heurístico del pedido
     */
    public double getHeuristicaPedido(Pedido pedido) {
        int idPedido = Integer.parseInt(pedido.getIdPedido().split("-")[0]);
        return cacheHeuristicaPedidos.getOrDefault(idPedido, calcularHeuristicaPedido(pedido));
    }

    /**
     * Calcula valor heurístico para una ruta específica
     * @param origen Código del aeropuerto origen
     * @param destino Código del aeropuerto destino
     * @return Valor heurístico de la ruta
     */
    private double calcularHeuristicaRuta(String origen, String destino) {
        RedDistribucion red = problema.getRed();

        // Buscar vuelos directos
        List<Vuelo> vuelosDirectos = red.buscarVuelosDirectos(origen, destino);
        if (!vuelosDirectos.isEmpty()) {
            double capacidadPromedio = vuelosDirectos.stream()
                .mapToDouble(Vuelo::getCapacidadDisponible)
                .average().orElse(100.0);

            // Vuelo directo - alta heurística
            return Math.min(1.0, capacidadPromedio / 300.0 + 0.3);
        }

        // Buscar ruta con escalas
        List<String> ruta = red.buscarRutaMinima(origen, destino);
        if (ruta.size() <= 3) { // Máximo 2 escalas
            return 0.5 / ruta.size(); // Penalizar escalas
        }

        return 0.1; // Ruta muy compleja o inexistente
    }

    /**
     * Obtiene valor heurístico para selección de sede origen
     * @param sede Código de la sede origen
     * @param destino Código del destino
     * @param pedido Pedido para contexto adicional
     * @return Valor heurístico para esta combinación sede-destino
     */
    public double getHeuristicaSedeDestino(String sede, String destino, Pedido pedido) {
        String claveRuta = sede + "-" + destino;
        double heuristicaRuta = cacheHeuristicaRutas.getOrDefault(claveRuta,
            calcularHeuristicaRuta(sede, destino));

        // Considerar proximidad geográfica
        Aeropuerto aeropuertoSede = problema.getRed().getAeropuerto(sede);
        Aeropuerto aeropuertoDestino = problema.getRed().getAeropuerto(destino);

        double bonusProximidad = 0.0;
        if (aeropuertoSede != null && aeropuertoDestino != null) {
            if (aeropuertoSede.getContinente().equals(aeropuertoDestino.getContinente())) {
                bonusProximidad = 0.2; // Bonus por mismo continente
            }
        }

        return Math.min(1.0, heuristicaRuta + bonusProximidad);
    }

    /**
     * Obtiene pedidos ordenados por valor heurístico
     * @param pedidosDisponibles Lista de pedidos disponibles
     * @return Lista ordenada por valor heurístico descendente
     */
    public List<Pedido> getPedidosOrdenadosPorHeuristica(List<Pedido> pedidosDisponibles) {
        return pedidosDisponibles.stream()
            .sorted((p1, p2) -> Double.compare(
                getHeuristicaPedido(p2),
                getHeuristicaPedido(p1)))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Obtiene sedes ordenadas por valor heurístico para un destino
     * @param destino Código del destino
     * @param pedido Pedido para contexto
     * @return Lista de sedes ordenadas por valor heurístico descendente
     */
    public List<String> getSedesOrdenadasPorHeuristica(String destino, Pedido pedido) {
        List<String> sedes = Arrays.asList("SPIM", "EBCI", "UBBB");

        return sedes.stream()
            .sorted((s1, s2) -> Double.compare(
                getHeuristicaSedeDestino(s2, destino, pedido),
                getHeuristicaSedeDestino(s1, destino, pedido)))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Recalcula cache heurístico (útil si cambian condiciones del problema)
     */
    public void recalcularCache() {
        cacheHeuristicaRutas.clear();
        cacheHeuristicaPedidos.clear();
        inicializarCacheHeuristico();
    }

    /**
     * Obtiene estadísticas del cache heurístico
     * @return String con estadísticas
     */
    public String getEstadisticasCache() {
        return String.format("Cache Heurístico - Rutas: %d, Pedidos: %d, Tipo: %s",
            cacheHeuristicaRutas.size(), cacheHeuristicaPedidos.size(), tipo);
    }

    // Getters
    public TipoHeuristica getTipo() {
        return tipo;
    }

    public ProblemaMoraPack getProblema() {
        return problema;
    }

}