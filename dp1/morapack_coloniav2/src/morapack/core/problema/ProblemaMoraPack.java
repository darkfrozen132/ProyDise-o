package morapack.core.problema;

import morapack.core.solucion.Solucion;
import morapack.core.solucion.SolucionMoraPack;
import morapack.datos.modelos.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Implementación específica del problema de distribución logística MoraPack.
 *
 * Características del problema:
 * - Multi-depot: 3 sedes principales (Lima, Bruselas, Baku)
 * - Restricciones temporales: 2 días mismo continente, 3 días diferente continente
 * - Capacidades limitadas: Vuelos (300-360) y almacenes (400-480)
 * - Rutas con escalas permitidas (tiempo mínimo 1 hora)
 * - Productos divisibles entre múltiples rutas
 */
public class ProblemaMoraPack extends Problema {

    private final RedDistribucion red;
    private final List<Pedido> pedidos;
    private final LocalDateTime tiempoInicio;
    private final Map<String, Integer> indiceAeropuertos;
    private final double[][] matrizCostos;

    // Pesos para función heurística multi-criterio
    private final double pesoUrgencia;
    private final double pesoCapacidad;
    private final double pesoCosto;

    // Penalizaciones para función objetivo
    private final double penalizacionRetraso;
    private final double penalizacionCapacidad;
    private final double bonificacionEficiencia;

    /**
     * Constructor con parámetros por defecto
     * @param red Red de distribución con datos cargados
     * @param pedidos Lista de pedidos a procesar
     * @param tiempoInicio Tiempo de referencia para cálculos
     */
    public ProblemaMoraPack(RedDistribucion red, List<Pedido> pedidos, LocalDateTime tiempoInicio) {
        this(red, pedidos, tiempoInicio,
             1.0, 2.0, 100.0,              // Parámetros ACO (alfa, beta, Q)
             0.4, 0.3, 0.3,                // Pesos heurística (urgencia, capacidad, costo)
             200.0, 100.0, 500.0);         // OPTIMIZADO: (retraso, capacidad, bonificación)
    }

    /**
     * Constructor completo con todos los parámetros
     * @param red Red de distribución
     * @param pedidos Lista de pedidos
     * @param tiempoInicio Tiempo de referencia
     * @param alfa Importancia de feromona
     * @param beta Importancia de heurística
     * @param constanteQ Constante Q para feromonas
     * @param pesoUrgencia Peso de urgencia temporal en heurística
     * @param pesoCapacidad Peso de capacidad en heurística
     * @param pesoCosto Peso de costo operacional en heurística
     * @param penalizacionRetraso Penalización por retrasos
     * @param penalizacionCapacidad Penalización por violaciones de capacidad
     * @param bonificacionEficiencia Bonificación por entregas tempranas
     */
    public ProblemaMoraPack(RedDistribucion red, List<Pedido> pedidos, LocalDateTime tiempoInicio,
                           double alfa, double beta, double constanteQ,
                           double pesoUrgencia, double pesoCapacidad, double pesoCosto,
                           double penalizacionRetraso, double penalizacionCapacidad,
                           double bonificacionEficiencia) {

        super(red.getAeropuertos().size(), alfa, beta, constanteQ);

        this.red = red;
        this.pedidos = pedidos;
        this.tiempoInicio = tiempoInicio;
        this.pesoUrgencia = pesoUrgencia;
        this.pesoCapacidad = pesoCapacidad;
        this.pesoCosto = pesoCosto;
        this.penalizacionRetraso = penalizacionRetraso;
        this.penalizacionCapacidad = penalizacionCapacidad;
        this.bonificacionEficiencia = bonificacionEficiencia;

        // Construir índice de aeropuertos
        this.indiceAeropuertos = construirIndiceAeropuertos();

        // Calcular matriz de costos
        this.matrizCostos = calcularMatrizCostos();
    }

    /**
     * Construye un índice para mapear códigos ICAO a números
     */
    private Map<String, Integer> construirIndiceAeropuertos() {
        Map<String, Integer> indice = new HashMap<>();
        int contador = 0;

        for (String codigoICAO : red.getAeropuertos().keySet()) {
            indice.put(codigoICAO, contador++);
        }

        return indice;
    }

    /**
     * Calcula la matriz de costos entre aeropuertos
     */
    private double[][] calcularMatrizCostos() {
        int n = red.getAeropuertos().size();
        double[][] matriz = new double[n][n];

        // Inicializar con infinito (sin conexión)
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matriz[i][j] = (i == j) ? 0.0 : Double.POSITIVE_INFINITY;
            }
        }

        // Llenar con costos de vuelos disponibles
        for (Vuelo vuelo : red.getVuelos().values()) {
            Integer indiceOrigen = indiceAeropuertos.get(vuelo.getAeropuertoOrigen());
            Integer indiceDestino = indiceAeropuertos.get(vuelo.getAeropuertoDestino());

            if (indiceOrigen != null && indiceDestino != null) {
                // Costo base: distancia + factor de capacidad
                Aeropuerto origen = red.getAeropuerto(vuelo.getAeropuertoOrigen());
                Aeropuerto destino = red.getAeropuerto(vuelo.getAeropuertoDestino());

                double distancia = origen.calcularDistancia(destino);
                double factorCapacidad = 1.0 + (500.0 - vuelo.getCapacidadMaxima()) / 500.0;
                double costoVuelo = distancia * factorCapacidad;

                // Usar el mínimo costo si hay múltiples vuelos
                matriz[indiceOrigen][indiceDestino] = Math.min(
                    matriz[indiceOrigen][indiceDestino],
                    costoVuelo
                );
            }
        }

        return matriz;
    }

    @Override
    public double evaluarSolucion(Solucion solucion) {
        if (!(solucion instanceof SolucionMoraPack)) {
            throw new IllegalArgumentException("Solución debe ser de tipo SolucionMoraPack");
        }

        SolucionMoraPack solucionMP = (SolucionMoraPack) solucion;

        double costoOperacional = 0.0;
        double penalizacionTotal = 0.0;
        double bonificacionTotal = 0.0;
        int pedidosCompletos = 0;
        int pedidosATiempo = 0;
        int pedidosTempranos = 0;

        // Evaluar cada pedido y sus entregas parciales
        for (Map.Entry<Integer, List<SolucionMoraPack.RutaProducto>> entrada :
             solucionMP.getRutasPorPedido().entrySet()) {

            int idPedido = entrada.getKey();
            List<SolucionMoraPack.RutaProducto> rutasPedido = entrada.getValue();

            if (rutasPedido.isEmpty()) continue;

            pedidosCompletos++;

            // Variables para este pedido específico
            boolean pedidoCompleto = solucionMP.pedidoCompleto(idPedido);
            boolean pedidoCumplePlazo = solucionMP.pedidoCumplePlazo(idPedido);
            int totalTransportado = rutasPedido.stream().mapToInt(r -> r.getCantidadTransportada()).sum();
            int totalRequerido = rutasPedido.get(0).getCantidadTotalPedido();

            // 1. Evaluación por completitud del pedido
            if (pedidoCompleto) {
                bonificacionTotal += bonificacionEficiencia * 2.0; // Bonificación grande por completar pedido
                pedidosATiempo++;
            } else {
                // Bonificación parcial proporcional
                double porcentajeCompletado = (double) totalTransportado / totalRequerido;
                bonificacionTotal += bonificacionEficiencia * porcentajeCompletado;
            }

            // 2. Evaluación temporal
            if (pedidoCumplePlazo) {
                bonificacionTotal += bonificacionEficiencia;

                // Bonificación extra por entregas tempranas
                long entregasTempranas = rutasPedido.stream()
                    .filter(this::esEntregaTemprana)
                    .count();
                if (entregasTempranas > 0) {
                    pedidosTempranos++;
                    bonificacionTotal += bonificacionEficiencia * 0.3 * entregasTempranas;
                }
            } else {
                // Penalización por retrasos (menos severa si es entrega parcial)
                double factorPenalizacion = pedidoCompleto ? 1.0 : 0.5;
                penalizacionTotal += penalizacionRetraso * factorPenalizacion;
            }

            // 3. Costos operacionales de todas las rutas del pedido
            for (SolucionMoraPack.RutaProducto ruta : rutasPedido) {
                double costoRuta = calcularCostoOperacionalRuta(ruta);
                costoOperacional += Math.max(0, 1000.0 - costoRuta / 10.0);
            }

            // 4. Bonificación por eficiencia en entregas (menos rutas = mejor)
            if (rutasPedido.size() == 1) {
                bonificacionTotal += bonificacionEficiencia * 0.2; // Entrega directa
            } else if (rutasPedido.size() <= 3) {
                bonificacionTotal += bonificacionEficiencia * 0.1; // Pocas entregas
            }
        }

        // 4. Penalizaciones por violaciones de capacidad
        penalizacionTotal += calcularPenalizacionesCapacidad(solucionMP);

        // 5. Bonificación por completitud del sistema (considerando entregas parciales)
        int pedidosCompletosReales = (int) solucionMP.getRutasPorPedido().keySet().stream()
            .filter(solucionMP::pedidoCompleto)
            .count();

        double bonificacionCompletitud = (pedidosCompletos > 0) ?
            (double) pedidosCompletosReales / pedidosCompletos * 800.0 : 0.0;

        // 6. Bonificación por eficiencia en entregas parciales
        int totalEntregas = solucionMP.getRutasPorPedido().values().stream()
            .mapToInt(List::size)
            .sum();
        double eficienciaEntregas = (pedidosCompletos > 0) ?
            Math.max(0, 1.0 - (double) totalEntregas / pedidosCompletos / 3.0) * 300.0 : 0.0;

        // 7. Bonificación por entregas tempranas
        double bonificacionEficienciaGeneral = (pedidosCompletos > 0) ?
            (double) pedidosTempranos / pedidosCompletos * 200.0 : 0.0;

        // NUEVA FUNCIÓN OBJETIVO: MAYOR FITNESS = MEJOR SOLUCIÓN (CON ENTREGAS PARCIALES)
        double fitness = costoOperacional + bonificacionTotal + bonificacionCompletitud +
                        eficienciaEntregas + bonificacionEficienciaGeneral - penalizacionTotal;

        // Asegurar que el fitness sea siempre positivo
        return Math.max(1.0, fitness);
    }

    /**
     * Calcula el costo operacional de una ruta específica
     */
    private double calcularCostoOperacionalRuta(SolucionMoraPack.RutaProducto ruta) {
        double costo = 0.0;

        // Costo base por distancia
        for (SolucionMoraPack.SegmentoVuelo segmento : ruta.getSegmentos()) {
            Integer indiceOrigen = indiceAeropuertos.get(segmento.getAeropuertoOrigen());
            Integer indiceDestino = indiceAeropuertos.get(segmento.getAeropuertoDestino());

            if (indiceOrigen != null && indiceDestino != null) {
                costo += matrizCostos[indiceOrigen][indiceDestino];
            }
        }

        // Factor por cantidad de productos
        costo *= ruta.getCantidadTransportada();

        return costo;
    }

    /**
     * Verifica si una entrega es temprana (antes del 80% del plazo)
     */
    private boolean esEntregaTemprana(SolucionMoraPack.RutaProducto ruta) {
        // Implementación simplificada - en versión completa calcular tiempo real
        return ruta.getSegmentos().size() <= 2; // Entregas directas o con 1 escala
    }

    /**
     * Calcula penalizaciones por violaciones de capacidad (considerando entregas parciales)
     */
    private double calcularPenalizacionesCapacidad(SolucionMoraPack solucion) {
        double penalizacion = 0.0;

        // Verificar capacidades de vuelos (ACTUALIZADO: considerar instancias diarias)
        Map<String, Integer> usoVuelos = solucion.calcularUsoCapacidadVuelos();
        for (Map.Entry<String, Integer> uso : usoVuelos.entrySet()) {
            String idVueloOInstancia = uso.getKey();

            // Intentar primero buscar como instancia (incluye fecha)
            VueloInstancia instancia = red.getInstanciaVuelo(idVueloOInstancia);
            if (instancia != null) {
                // Es una instancia con fecha específica
                if (uso.getValue() > instancia.getCapacidadMaxima()) {
                    penalizacion += penalizacionCapacidad * (uso.getValue() - instancia.getCapacidadMaxima());
                }
            } else {
                // Fallback: buscar como plantilla (retrocompatibilidad)
                Vuelo vuelo = red.getVuelo(idVueloOInstancia);
                if (vuelo != null && uso.getValue() > vuelo.getCapacidadMaxima()) {
                    penalizacion += penalizacionCapacidad * (uso.getValue() - vuelo.getCapacidadMaxima());
                }
            }
        }

        // Verificar capacidades de aeropuertos
        Map<String, Integer> usoAeropuertos = solucion.calcularUsoCapacidadAeropuertos();
        for (Map.Entry<String, Integer> uso : usoAeropuertos.entrySet()) {
            Aeropuerto aeropuerto = red.getAeropuerto(uso.getKey());
            if (aeropuerto != null && !aeropuerto.tieneStockIlimitado() &&
                uso.getValue() > aeropuerto.getCapacidadAlmacen()) {
                penalizacion += penalizacionCapacidad * (uso.getValue() - aeropuerto.getCapacidadAlmacen());
            }
        }

        // Penalización adicional por fragmentación excesiva
        int totalEntregas = solucion.getRutasPorPedido().values().stream()
            .mapToInt(List::size)
            .sum();
        int totalPedidos = solucion.getRutasPorPedido().size();

        if (totalPedidos > 0) {
            double promedioEntregasPorPedido = (double) totalEntregas / totalPedidos;
            if (promedioEntregasPorPedido > 3.0) { // Más de 3 entregas promedio es ineficiente
                penalizacion += penalizacionCapacidad * 0.1 * (promedioEntregasPorPedido - 3.0);
            }
        }

        return penalizacion;
    }

    @Override
    public double[][] getMatrizDistancias() {
        return matrizCostos;
    }

    @Override
    public boolean esSolucionValida(Solucion solucion) {
        if (!(solucion instanceof SolucionMoraPack)) {
            return false;
        }

        SolucionMoraPack solucionMP = (SolucionMoraPack) solucion;

        // 1. Verificar que todos los pedidos tengan al menos una ruta
        if (solucionMP.getRutasPorPedido().size() != pedidos.size()) {
            return false;
        }

        // 2. Verificar que cada ruta sea válida y que las cantidades sean consistentes
        for (Map.Entry<Integer, List<SolucionMoraPack.RutaProducto>> entrada :
             solucionMP.getRutasPorPedido().entrySet()) {

            List<SolucionMoraPack.RutaProducto> rutas = entrada.getValue();
            if (rutas.isEmpty()) {
                return false; // Pedido sin rutas
            }

            // Verificar cada ruta individual
            for (SolucionMoraPack.RutaProducto ruta : rutas) {
                if (!esRutaValida(ruta)) {
                    return false;
                }
            }

            // Verificar consistencia de cantidades
            if (!cantidadesConsistentes(rutas)) {
                return false;
            }
        }

        // 3. Verificar restricciones de capacidad (flexibles con penalizaciones)
        return true; // Las violaciones se manejan con penalizaciones en la función objetivo
    }

    /**
     * Verifica que las cantidades en las rutas sean consistentes
     */
    private boolean cantidadesConsistentes(List<SolucionMoraPack.RutaProducto> rutas) {
        if (rutas.isEmpty()) return false;

        int cantidadTotal = rutas.get(0).getCantidadTotalPedido();
        int sumaTransportada = rutas.stream()
            .mapToInt(SolucionMoraPack.RutaProducto::getCantidadTransportada)
            .sum();

        // Permitir entregas parciales (suma <= total) pero no exceder
        return sumaTransportada <= cantidadTotal && sumaTransportada > 0;
    }

    /**
     * Verifica si una ruta individual es válida
     */
    private boolean esRutaValida(SolucionMoraPack.RutaProducto ruta) {
        if (ruta.getSegmentos().isEmpty()) {
            return false;
        }

        // Verificar conectividad entre segmentos
        for (int i = 0; i < ruta.getSegmentos().size() - 1; i++) {
            SolucionMoraPack.SegmentoVuelo actual = ruta.getSegmentos().get(i);
            SolucionMoraPack.SegmentoVuelo siguiente = ruta.getSegmentos().get(i + 1);

            if (!actual.getAeropuertoDestino().equals(siguiente.getAeropuertoOrigen())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescripcion() {
        return String.format("ProblemaMoraPack [%d aeropuertos, %d pedidos, tiempo: %s]",
                red.getAeropuertos().size(), pedidos.size(), tiempoInicio);
    }

    @Override
    public Problema clonarConParametros(double nuevaAlfa, double nuevaBeta) {
        return new ProblemaMoraPack(red, pedidos, tiempoInicio,
                nuevaAlfa, nuevaBeta, constanteQ,
                pesoUrgencia, pesoCapacidad, pesoCosto,
                penalizacionRetraso, penalizacionCapacidad, bonificacionEficiencia);
    }

    /**
     * Calcula la heurística multi-criterio para ACO
     * @param aeropuertoOrigen Código ICAO del aeropuerto origen
     * @param aeropuertoDestino Código ICAO del aeropuerto destino
     * @param pedido Pedido siendo procesado
     * @return Valor heurístico [0, 1]
     */
    public double calcularHeuristica(String aeropuertoOrigen, String aeropuertoDestino, Pedido pedido) {
        Aeropuerto origen = red.getAeropuerto(aeropuertoOrigen);
        Aeropuerto destino = red.getAeropuerto(aeropuertoDestino);

        if (origen == null || destino == null) {
            return 0.0;
        }

        // 1. Urgencia temporal (0 = no urgente, 1 = muy urgente)
        long horasRestantes = pedido.horasRestantesUTC(tiempoInicio, destino);
        double urgencia = Math.max(0.0, Math.min(1.0, (72.0 - horasRestantes) / 72.0));

        // 2. Eficiencia de capacidad (disponibilidad de vuelos)
        List<Vuelo> vuelosDisponibles = red.buscarVuelosDirectos(aeropuertoOrigen, aeropuertoDestino);
        double capacidadPromedio = vuelosDisponibles.stream()
                .mapToDouble(Vuelo::getCapacidadDisponible)
                .average()
                .orElse(0.0);
        double eficienciaCapacidad = Math.min(1.0, capacidadPromedio / pedido.getCantidadProductos());

        // 3. Eficiencia de costo (inverso de distancia normalizada)
        double distancia = origen.calcularDistancia(destino);
        double eficienciaCosto = 1.0 / (1.0 + distancia / 10.0); // Normalizar distancia

        // Combinación ponderada
        return pesoUrgencia * urgencia +
               pesoCapacidad * eficienciaCapacidad +
               pesoCosto * eficienciaCosto;
    }

    // Getters para acceso a componentes internos
    public RedDistribucion getRed() { return red; }
    public List<Pedido> getPedidos() { return pedidos; }
    public LocalDateTime getTiempoInicio() { return tiempoInicio; }
    public Map<String, Integer> getIndiceAeropuertos() { return indiceAeropuertos; }

    // Getters para parámetros del problema
    public double getPesoUrgencia() { return pesoUrgencia; }
    public double getPesoCapacidad() { return pesoCapacidad; }
    public double getPesoCosto() { return pesoCosto; }
    public double getPenalizacionRetraso() { return penalizacionRetraso; }
    public double getPenalizacionCapacidad() { return penalizacionCapacidad; }
    public double getBonificacionEficiencia() { return bonificacionEficiencia; }
}