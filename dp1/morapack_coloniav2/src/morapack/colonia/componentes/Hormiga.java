package morapack.colonia.componentes;

import morapack.core.solucion.Solucion;
import morapack.core.solucion.SolucionMoraPack;
import morapack.core.problema.Problema;
import morapack.core.problema.ProblemaMoraPack;
import morapack.datos.modelos.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Representa una hormiga en el algoritmo de colonia de hormigas.
 * Cada hormiga construye una solución siguiendo las reglas de probabilidad
 * basadas en feromonas y información heurística.
 */
public class Hormiga {

    private int id;
    private SolucionMoraPack solucionActual;
    private double fitness;
    private List<Pedido> pedidosPendientes;
    private Set<Integer> pedidosAsignados;
    private Random random;

    /**
     * Constructor de la hormiga
     * @param id Identificador único de la hormiga
     */
    public Hormiga(int id) {
        this.id = id;
        this.fitness = Double.MIN_VALUE; // Cambio: MAYOR fitness = mejor
        this.pedidosAsignados = new HashSet<>();
        this.random = new Random();
    }

    /**
     * Construye una solución completa para el problema MoraPack
     * @param problema El problema a resolver
     * @param feromona Matriz de feromonas
     * @param heuristica Información heurística
     * @return La solución construida
     */
    public Solucion construirSolucion(Problema problema, Feromona feromona, Heuristica heuristica) {
        if (!(problema instanceof ProblemaMoraPack)) {
            throw new IllegalArgumentException("Hormiga requiere ProblemaMoraPack");
        }

        ProblemaMoraPack problemaMP = (ProblemaMoraPack) problema;
        inicializarSolucionMoraPack(problemaMP);

        // Construir rutas para cada pedido
        while (!solucionCompleta()) {
            Pedido pedido = seleccionarSiguientePedido(problemaMP, feromona, heuristica);
            if (pedido != null) {
                construirRutasParaPedido(pedido, problemaMP, feromona, heuristica);
                pedidosAsignados.add(Integer.parseInt(pedido.getIdPedido().split("-")[0])); // Simplificado
            }
        }

        calcularFitness(problemaMP);
        return solucionActual;
    }

    /**
     * Inicializa la solución MoraPack y estructuras auxiliares
     */
    private void inicializarSolucionMoraPack(ProblemaMoraPack problema) {
        this.solucionActual = new SolucionMoraPack();
        this.pedidosPendientes = new ArrayList<>(problema.getPedidos());
        this.pedidosAsignados.clear();

        // Mezclar pedidos para diversidad
        Collections.shuffle(pedidosPendientes, random);
    }

    /**
     * Verifica si la solución está completa (todos los pedidos asignados)
     */
    private boolean solucionCompleta() {
        return pedidosAsignados.size() >= pedidosPendientes.size();
    }

    /**
     * Selecciona el siguiente pedido a procesar usando regla de probabilidad ACO
     */
    private Pedido seleccionarSiguientePedido(ProblemaMoraPack problema, Feromona feromona, Heuristica heuristica) {
        List<Pedido> pedidosDisponibles = new ArrayList<>();
        for (Pedido pedido : pedidosPendientes) {
            int idPedido = Integer.parseInt(pedido.getIdPedido().split("-")[0]); // Simplificado
            if (!pedidosAsignados.contains(idPedido)) {
                pedidosDisponibles.add(pedido);
            }
        }

        if (pedidosDisponibles.isEmpty()) {
            return null;
        }

        // Seleccionar pedido basado en urgencia y heurística
        double[] probabilidades = calcularProbabilidadesPedidos(pedidosDisponibles, problema, heuristica);
        int indicePedido = seleccionarPorRuleta(probabilidades);

        return pedidosDisponibles.get(indicePedido);
    }

    /**
     * Construye rutas para un pedido específico (puede crear entregas parciales)
     */
    private void construirRutasParaPedido(Pedido pedido, ProblemaMoraPack problema, Feromona feromona, Heuristica heuristica) {
        RedDistribucion red = problema.getRed();
        String destino = pedido.getCodigoDestino();
        int cantidadTotal = pedido.getCantidadProductos();
        int cantidadRestante = cantidadTotal;
        int numeroEntrega = 1;

        List<String> sedesDisponibles = Arrays.asList("SPIM", "EBCI", "UBBB");

        while (cantidadRestante > 0 && numeroEntrega <= 3) { // Máximo 3 entregas
            // Seleccionar sede origen usando heurística
            String sedeOrigen = seleccionarSedeOrigen(sedesDisponibles, destino, problema, heuristica);

            // Determinar cantidad para esta entrega
            int cantidadEntrega = determinarCantidadEntrega(cantidadRestante, numeroEntrega, red, sedeOrigen, destino);

            if (cantidadEntrega > 0) {
                // Buscar ruta desde sede a destino
                List<SolucionMoraPack.SegmentoVuelo> segmentos = construirRuta(sedeOrigen, destino, red);

                if (!segmentos.isEmpty()) {
                    // Crear entrega
                    LocalDateTime tiempoSalida = calcularTiempoSalida(segmentos);
                    LocalDateTime tiempoLlegada = calcularTiempoLlegada(segmentos);
                    boolean cumplePlazo = pedido.estaDentroPlazoUTC(tiempoLlegada, red.getAeropuerto(destino));

                    SolucionMoraPack.RutaProducto ruta = new SolucionMoraPack.RutaProducto(
                        Integer.parseInt(pedido.getIdPedido().split("-")[0]), // ID simplificado
                        cantidadEntrega, cantidadTotal, numeroEntrega,
                        cantidadEntrega < cantidadTotal, // es parcial
                        sedeOrigen, destino, segmentos, tiempoSalida, tiempoLlegada, cumplePlazo
                    );

                    solucionActual.agregarRutaProducto(Integer.parseInt(pedido.getIdPedido().split("-")[0]), ruta);
                    cantidadRestante -= cantidadEntrega;
                }
            }
            numeroEntrega++;
        }
    }

    /**
     * Calcula probabilidades para seleccionar pedidos basado en urgencia
     */
    private double[] calcularProbabilidadesPedidos(List<Pedido> pedidos, ProblemaMoraPack problema, Heuristica heuristica) {
        double[] probabilidades = new double[pedidos.size()];
        double suma = 0.0;
        LocalDateTime tiempoActual = problema.getTiempoInicio();

        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            Aeropuerto destino = problema.getRed().getAeropuerto(pedido.getCodigoDestino());

            // Calcular urgencia (más urgente = mayor probabilidad)
            long horasRestantes = pedido.horasRestantesUTC(tiempoActual, destino);
            double urgencia = Math.max(0.1, (72.0 - horasRestantes) / 72.0); // Normalizado [0.1, 1.0]

            // Factor por cantidad (pedidos grandes = mayor probabilidad)
            double factorCantidad = Math.log(1 + pedido.getCantidadProductos() / 50.0);

            // Heurística combinada
            double heuristicaPedido = problema.calcularHeuristica("SPIM", pedido.getCodigoDestino(), pedido);

            probabilidades[i] = urgencia * factorCantidad * (1.0 + heuristicaPedido);
            suma += probabilidades[i];
        }

        // Normalizar
        if (suma > 0) {
            for (int i = 0; i < probabilidades.length; i++) {
                probabilidades[i] /= suma;
            }
        }

        return probabilidades;
    }

    /**
     * Selección por ruleta genérica
     */
    private int seleccionarPorRuleta(double[] probabilidades) {
        double r = random.nextDouble();
        double acumulado = 0.0;

        for (int i = 0; i < probabilidades.length; i++) {
            acumulado += probabilidades[i];
            if (r <= acumulado) {
                return i;
            }
        }

        // Fallback: seleccionar al azar
        return random.nextInt(probabilidades.length);
    }

    /**
     * Selecciona sede origen basada en heurística
     */
    private String seleccionarSedeOrigen(List<String> sedes, String destino, ProblemaMoraPack problema, Heuristica heuristica) {
        double[] probabilidades = new double[sedes.size()];
        double suma = 0.0;

        for (int i = 0; i < sedes.size(); i++) {
            String sede = sedes.get(i);
            Aeropuerto aeropuertoSede = problema.getRed().getAeropuerto(sede);
            Aeropuerto aeropuertoDestino = problema.getRed().getAeropuerto(destino);

            if (aeropuertoSede != null && aeropuertoDestino != null) {
                // Preferir sedes del mismo continente
                double factorContinente = aeropuertoSede.getContinente().equals(aeropuertoDestino.getContinente()) ? 2.0 : 1.0;

                // Preferir sedes con vuelos directos
                List<Vuelo> vuelosDirectos = problema.getRed().buscarVuelosDirectos(sede, destino);
                double factorDirecto = vuelosDirectos.isEmpty() ? 0.5 : 1.5;

                // Considerar capacidad disponible
                double capacidadPromedio = vuelosDirectos.stream()
                    .mapToDouble(Vuelo::getCapacidadDisponible)
                    .average().orElse(100.0);
                double factorCapacidad = Math.min(2.0, capacidadPromedio / 100.0);

                probabilidades[i] = factorContinente * factorDirecto * factorCapacidad;
                suma += probabilidades[i];
            }
        }

        // Normalizar
        if (suma > 0) {
            for (int i = 0; i < probabilidades.length; i++) {
                probabilidades[i] /= suma;
            }
        } else {
            // Si no hay información, distribución uniforme
            Arrays.fill(probabilidades, 1.0 / probabilidades.length);
        }

        int indiceSeleccionado = seleccionarPorRuleta(probabilidades);
        return sedes.get(indiceSeleccionado);
    }

    /**
     * Determina cantidad para esta entrega basada en capacidades
     */
    private int determinarCantidadEntrega(int cantidadRestante, int numeroEntrega, RedDistribucion red, String origen, String destino) {
        List<Vuelo> vuelosDisponibles = red.buscarVuelosDirectos(origen, destino);

        if (vuelosDisponibles.isEmpty()) {
            // Buscar ruta con escalas (simplificado)
            List<String> rutaMinima = red.buscarRutaMinima(origen, destino);
            if (rutaMinima.size() <= 3) { // Máximo 2 escalas
                return Math.min(cantidadRestante, 150); // Capacidad conservadora con escalas
            }
            return 0;
        }

        // Usar el vuelo con mayor capacidad disponible
        int maxCapacidad = vuelosDisponibles.stream()
            .mapToInt(Vuelo::getCapacidadDisponible)
            .max().orElse(100);

        // Estrategia de división inteligente
        if (numeroEntrega == 1 && cantidadRestante <= maxCapacidad) {
            return cantidadRestante; // Entrega completa si es posible
        }

        // División balanceada
        int cantidadEntrega = Math.min(cantidadRestante, maxCapacidad * 80 / 100); // 80% de capacidad

        // Evitar entregas muy pequeñas al final
        if (cantidadRestante - cantidadEntrega < 20 && cantidadRestante - cantidadEntrega > 0) {
            cantidadEntrega = cantidadRestante; // Entregar todo
        }

        return Math.max(1, cantidadEntrega);
    }

    /**
     * Construye ruta entre dos aeropuertos (directo o con escalas)
     */
    private List<SolucionMoraPack.SegmentoVuelo> construirRuta(String origen, String destino, RedDistribucion red) {
        List<SolucionMoraPack.SegmentoVuelo> segmentos = new ArrayList<>();

        // Intentar ruta directa primero
        List<Vuelo> vuelosDirectos = red.buscarVuelosDirectos(origen, destino);
        if (!vuelosDirectos.isEmpty()) {
            Vuelo vueloSeleccionado = vuelosDirectos.get(random.nextInt(vuelosDirectos.size()));
            LocalDateTime horaSalida = LocalDateTime.of(2025, 1, 1, vueloSeleccionado.getHoraSalida().getHour(), vueloSeleccionado.getHoraSalida().getMinute());
            LocalDateTime horaLlegada = LocalDateTime.of(2025, 1, 1, vueloSeleccionado.getHoraLlegada().getHour(), vueloSeleccionado.getHoraLlegada().getMinute());

            segmentos.add(new SolucionMoraPack.SegmentoVuelo(
                vueloSeleccionado.getIdVuelo(), origen, destino, horaSalida, horaLlegada
            ));
            return segmentos;
        }

        // Buscar ruta con escalas
        List<String> rutaMinima = red.buscarRutaMinima(origen, destino);
        if (rutaMinima.size() > 1) {
            LocalDateTime tiempoActual = LocalDateTime.of(2025, 1, 1, 8, 0); // Tiempo base

            for (int i = 0; i < rutaMinima.size() - 1; i++) {
                String origenSegmento = rutaMinima.get(i);
                String destinoSegmento = rutaMinima.get(i + 1);

                List<Vuelo> vuelosSegmento = red.buscarVuelosDirectos(origenSegmento, destinoSegmento);
                if (!vuelosSegmento.isEmpty()) {
                    Vuelo vuelo = vuelosSegmento.get(0);
                    LocalDateTime horaSalida = tiempoActual.plusHours(1); // 1 hora de conexión
                    LocalDateTime horaLlegada = horaSalida.plusHours(2); // 2 horas de vuelo aproximado

                    segmentos.add(new SolucionMoraPack.SegmentoVuelo(
                        vuelo.getIdVuelo(), origenSegmento, destinoSegmento, horaSalida, horaLlegada
                    ));

                    tiempoActual = horaLlegada;
                }
            }
        }

        return segmentos;
    }

    /**
     * Calcula tiempo de salida de una ruta
     */
    private LocalDateTime calcularTiempoSalida(List<SolucionMoraPack.SegmentoVuelo> segmentos) {
        return segmentos.isEmpty() ? LocalDateTime.now() : segmentos.get(0).getHoraSalida();
    }

    /**
     * Calcula tiempo de llegada de una ruta
     */
    private LocalDateTime calcularTiempoLlegada(List<SolucionMoraPack.SegmentoVuelo> segmentos) {
        return segmentos.isEmpty() ? LocalDateTime.now() : segmentos.get(segmentos.size() - 1).getHoraLlegada();
    }

    /**
     * Calcula el fitness de la solución actual
     */
    private void calcularFitness(ProblemaMoraPack problema) {
        this.fitness = problema.evaluarSolucion(solucionActual);
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public Solucion getSolucion() {
        return solucionActual;
    }

    public double getFitness() {
        return fitness;
    }

    public void reiniciar() {
        this.solucionActual = null;
        this.pedidosPendientes = null;
        this.pedidosAsignados.clear();
        this.fitness = Double.MIN_VALUE; // MAYOR fitness = mejor
    }
}