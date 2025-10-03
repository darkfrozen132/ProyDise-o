package morapack.colonia.componentes;

import morapack.core.solucion.Solucion;
import morapack.core.solucion.SolucionMoraPack;
import morapack.core.problema.Problema;
import morapack.core.problema.ProblemaMoraPack;
import morapack.datos.modelos.*;
import java.time.LocalDate;
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
    private Set<String> pedidosAsignados; // Cambio: usar String para ID completo
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

    // Modo debug para logging
    private static final boolean DEBUG = System.getProperty("hormiga.debug", "false").equals("true");

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

        if (DEBUG) System.out.println("[Hormiga-" + id + "] Inicializando solucion...");
        inicializarSolucionMoraPack(problemaMP);

        if (DEBUG) System.out.println("[Hormiga-" + id + "] Total pedidos: " + pedidosPendientes.size());

        // Construir rutas para cada pedido
        int iteracion = 0;
        while (!solucionCompleta()) {
            iteracion++;
            if (DEBUG && iteracion % 10 == 0) {
                System.out.println("[Hormiga-" + id + "] Iteracion " + iteracion + ", asignados: " + pedidosAsignados.size() + "/" + pedidosPendientes.size());
            }

            long inicio = DEBUG ? System.currentTimeMillis() : 0;

            Pedido pedido = seleccionarSiguientePedido(problemaMP, feromona, heuristica);

            if (DEBUG) {
                long tiempoSeleccion = System.currentTimeMillis() - inicio;
                if (tiempoSeleccion > 100) {
                    System.out.println("[Hormiga-" + id + "] ADVERTENCIA: seleccionarSiguientePedido tardo " + tiempoSeleccion + " ms");
                }
            }

            if (pedido != null) {
                long inicioRutas = DEBUG ? System.currentTimeMillis() : 0;

                construirRutasParaPedido(pedido, problemaMP, feromona, heuristica);

                if (DEBUG) {
                    long tiempoRutas = System.currentTimeMillis() - inicioRutas;
                    if (tiempoRutas > 100) {
                        System.out.println("[Hormiga-" + id + "] ADVERTENCIA: construirRutasParaPedido tardo " + tiempoRutas + " ms");
                    }
                }

                pedidosAsignados.add(pedido.getIdPedido()); // Usar ID completo
            } else {
                if (DEBUG) System.out.println("[Hormiga-" + id + "] No hay mas pedidos disponibles");
                break;
            }
        }

        if (DEBUG) System.out.println("[Hormiga-" + id + "] Calculando fitness...");
        calcularFitness(problemaMP);

        if (DEBUG) System.out.println("[Hormiga-" + id + "] Solucion completa con fitness: " + fitness);
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
            String idPedido = pedido.getIdPedido(); // Usar ID completo
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

        int intentosSinExito = 0;
        while (cantidadRestante > 0 && numeroEntrega <= 3 && intentosSinExito < sedesDisponibles.size()) {
            // Seleccionar sede origen usando heurística
            String sedeOrigen = seleccionarSedeOrigen(sedesDisponibles, destino, problema, heuristica);

            // Intentar construir ruta con la cantidad restante
            List<SolucionMoraPack.SegmentoVuelo> segmentos = construirRuta(sedeOrigen, destino, cantidadRestante, red, pedido);

            if (!segmentos.isEmpty()) {
                // Determinar cuánto se transportó en esta ruta
                int cantidadTransportada = calcularCantidadTransportada(segmentos, cantidadRestante, red);

                if (cantidadTransportada > 0) {
                    // Registrar uso de capacidad en TODOS los segmentos de la ruta
                    for (SolucionMoraPack.SegmentoVuelo segmento : segmentos) {
                        solucionActual.registrarUsoCapacidad(segmento.getIdVuelo(), cantidadTransportada);
                    }

                    // Crear entrega
                    LocalDateTime tiempoSalida = calcularTiempoSalida(segmentos);
                    LocalDateTime tiempoLlegada = calcularTiempoLlegada(segmentos);
                    boolean cumplePlazo = pedido.estaDentroPlazoUTC(tiempoLlegada, red.getAeropuerto(destino));

                    int idNumerico = pedido.getIdPedido().hashCode() & 0x7FFFFFFF;

                    SolucionMoraPack.RutaProducto ruta = new SolucionMoraPack.RutaProducto(
                        idNumerico,
                        cantidadTransportada, cantidadTotal, numeroEntrega,
                        cantidadTransportada < cantidadTotal,
                        sedeOrigen, destino, segmentos, tiempoSalida, tiempoLlegada, cumplePlazo
                    );

                    solucionActual.agregarRutaProducto(idNumerico, ruta);
                    cantidadRestante -= cantidadTransportada;
                    numeroEntrega++;
                    intentosSinExito = 0; // Reset en caso de éxito
                } else {
                    // No hay capacidad disponible, probar con otra sede
                    intentosSinExito++;
                }
            } else {
                // No se encontró ruta, probar con otra sede
                intentosSinExito++;
            }
        }

        // Si queda cantidad sin asignar, crear ruta dummy para evitar errores
        // (será fuertemente penalizada en fitness)
        if (cantidadRestante > 0 && solucionActual.getRutasProducto(pedido.getIdPedido().hashCode() & 0x7FFFFFFF).isEmpty()) {
            // Crear una ruta vacía para que al menos el pedido exista en la solución
            int idNumerico = pedido.getIdPedido().hashCode() & 0x7FFFFFFF;
            String sedeOrigen = sedesDisponibles.get(0);

            SolucionMoraPack.RutaProducto rutaDummy = new SolucionMoraPack.RutaProducto(
                idNumerico,
                0, cantidadTotal, 1,
                true,
                sedeOrigen, destino, new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(), false
            );

            solucionActual.agregarRutaProducto(idNumerico, rutaDummy);
        }
    }

    /**
     * Calcula cuánta cantidad se puede transportar en una ruta dada
     * Considera la capacidad disponible de todos los vuelos en la ruta
     */
    private int calcularCantidadTransportada(List<SolucionMoraPack.SegmentoVuelo> segmentos,
                                             int cantidadDeseada, RedDistribucion red) {
        if (segmentos.isEmpty()) {
            return 0;
        }

        // En una ruta con escalas, la capacidad está limitada por el vuelo con menor capacidad
        int capacidadMinima = Integer.MAX_VALUE;

        for (SolucionMoraPack.SegmentoVuelo segmento : segmentos) {
            String idVuelo = segmento.getIdVuelo();
            morapack.datos.modelos.VueloInstancia instancia = red.getInstanciaVuelo(idVuelo);

            if (instancia != null) {
                int capacidadDisponible = solucionActual.getCapacidadDisponible(
                    idVuelo,
                    instancia.getCapacidadMaxima()
                );
                capacidadMinima = Math.min(capacidadMinima, capacidadDisponible);
            }
        }

        return capacidadMinima == Integer.MAX_VALUE ? 0 : Math.min(cantidadDeseada, capacidadMinima);
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
                // Preferir sedes del mismo continente (factor más balanceado)
                double factorContinente = aeropuertoSede.getContinente().equals(aeropuertoDestino.getContinente()) ? 1.5 : 1.0;

                // Considerar vuelos directos pero NO penalizar mucho las escalas
                List<Vuelo> vuelosDirectos = problema.getRed().buscarVuelosDirectos(sede, destino);
                double factorDirecto = vuelosDirectos.isEmpty() ? 0.8 : 1.2; // Cambio: 0.8 vs 1.2 (antes 0.5 vs 1.5)

                // Considerar capacidad disponible
                double capacidadPromedio = vuelosDirectos.isEmpty()
                    ? 200.0  // Asumir capacidad razonable si no hay directos
                    : vuelosDirectos.stream()
                        .mapToDouble(Vuelo::getCapacidadDisponible)
                        .average().orElse(100.0);
                double factorCapacidad = Math.min(1.5, capacidadPromedio / 150.0);

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
     * Determina cantidad para esta entrega basada en capacidades REALES disponibles
     * ACTUALIZADO: Considera capacidad ya usada en esta solución
     */
    private int determinarCantidadEntrega(int cantidadRestante, int numeroEntrega, RedDistribucion red,
                                          String origen, String destino, Pedido pedido) {
        // Calcular ventana temporal del pedido
        LocalDate fechaInicio = pedido.getTiempoPedido() != null
            ? pedido.getTiempoPedido().toLocalDate()
            : LocalDate.of(red.getAnioOperacion(), red.getMesOperacion(), pedido.getDia());

        LocalDate fechaLimite = pedido.getTiempoLimiteEntrega() != null
            ? pedido.getTiempoLimiteEntrega().toLocalDate()
            : fechaInicio.plusDays(2);

        // Buscar vuelos en la ventana temporal del pedido
        List<VueloInstancia> vuelosDisponibles = red.buscarVuelosDisponiblesEnRango(origen, destino, fechaInicio, fechaLimite);

        if (vuelosDisponibles.isEmpty()) {
            // Buscar ruta con escalas (simplificado)
            List<String> rutaMinima = red.buscarRutaMinima(origen, destino);
            if (rutaMinima != null && rutaMinima.size() <= 3) {
                return Math.min(cantidadRestante, 150);
            }
            return 0;
        }

        // Calcular capacidad disponible REAL considerando uso en esta solución
        int maxCapacidadReal = 0;
        for (VueloInstancia vuelo : vuelosDisponibles) {
            int capacidadDisponible = solucionActual.getCapacidadDisponible(
                vuelo.getIdInstancia(),
                vuelo.getCapacidadMaxima()
            );
            maxCapacidadReal = Math.max(maxCapacidadReal, capacidadDisponible);
        }

        // Si no hay capacidad disponible, retornar 0 (forzará búsqueda de alternativas)
        if (maxCapacidadReal == 0) {
            return 0;
        }

        // ESTRATEGIA MEJORADA: Usar el 100% de la capacidad disponible
        if (numeroEntrega == 1 && cantidadRestante <= maxCapacidadReal) {
            return cantidadRestante; // Entrega completa si cabe
        }

        // Maximizar uso de capacidad (100% en lugar de 80%)
        int cantidadEntrega = Math.min(cantidadRestante, maxCapacidadReal);

        // Evitar entregas muy pequeñas al final
        if (cantidadRestante - cantidadEntrega < 20 && cantidadRestante - cantidadEntrega > 0) {
            cantidadEntrega = cantidadRestante;
        }

        return Math.max(1, cantidadEntrega);
    }

    /**
     * Construye ruta entre dos aeropuertos (directo o con escalas)
     * ACTUALIZADO: Considera capacidad disponible y usa VueloInstancia con fechas reales
     *
     * @param origen Aeropuerto origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos a transportar
     * @param red Red de distribución
     * @param pedido Pedido para restricciones temporales
     * @return Lista de segmentos de vuelo
     */
    private List<SolucionMoraPack.SegmentoVuelo> construirRuta(String origen, String destino, int cantidad,
                                                                RedDistribucion red, Pedido pedido) {
        List<SolucionMoraPack.SegmentoVuelo> segmentos = new ArrayList<>();

        // Calcular ventana temporal válida para el pedido
        LocalDate fechaInicio = pedido.getTiempoPedido() != null
            ? pedido.getTiempoPedido().toLocalDate()
            : LocalDate.of(red.getAnioOperacion(), red.getMesOperacion(), pedido.getDia());

        LocalDate fechaLimite = pedido.getTiempoLimiteEntrega() != null
            ? pedido.getTiempoLimiteEntrega().toLocalDate()
            : fechaInicio.plusDays(2);

        // ESTRATEGIA 1: Buscar vuelo directo con capacidad suficiente
        List<VueloInstancia> vuelosDirectos = red.buscarVuelosDisponiblesEnRango(origen, destino, fechaInicio, fechaLimite);

        // Filtrar vuelos con capacidad suficiente CONSIDERANDO LO YA USADO EN ESTA SOLUCIÓN
        List<VueloInstancia> vuelosConCapacidad = vuelosDirectos.stream()
            .filter(v -> solucionActual.tieneCapacidadDisponible(v.getIdInstancia(), v.getCapacidadMaxima(), cantidad))
            .collect(java.util.stream.Collectors.toList());

        // Si hay vuelo directo con capacidad, SIEMPRE usarlo (es la mejor opción)
        if (!vuelosConCapacidad.isEmpty()) {
            VueloInstancia vueloSeleccionado = seleccionarVueloConPonderacion(vuelosConCapacidad, fechaInicio, fechaLimite);

            if (vueloSeleccionado != null) {
                // NO registrar capacidad aquí - se hará después en construirRutasParaPedido
                // basándose en la cantidad REAL transportada

                segmentos.add(new SolucionMoraPack.SegmentoVuelo(
                    vueloSeleccionado.getIdInstancia(), origen, destino,
                    vueloSeleccionado.getHorarioSalidaCompleto(),
                    vueloSeleccionado.getHorarioLlegadaCompleto()
                ));
                return segmentos;
            }
        }

        // ESTRATEGIA 2: Intentar ruta con escalas si:
        // - No hay vuelos directos con capacidad suficiente
        // - O decidimos explorar escalas (30% probabilidad)
        List<String> rutaMinima = red.buscarRutaMinima(origen, destino);
        if (rutaMinima != null && rutaMinima.size() > 1 && rutaMinima.size() <= 4) {
            LocalDate fechaInicioRuta = calcularFechaInicioOptima(rutaMinima, fechaInicio, fechaLimite, red);

            if (fechaInicioRuta != null) {
                // Intentar construir ruta con escalas verificando capacidad
                LocalDateTime tiempoActual = fechaInicioRuta.atStartOfDay();
                List<SolucionMoraPack.SegmentoVuelo> rutaEscalas = new ArrayList<>();
                boolean rutaFactible = true;

                for (int i = 0; i < rutaMinima.size() - 1; i++) {
                    String origenSeg = rutaMinima.get(i);
                    String destinoSeg = rutaMinima.get(i + 1);

                    // Buscar vuelos para este segmento
                    List<VueloInstancia> vuelosSegmento = red.buscarVuelosDisponiblesEnRango(
                        origenSeg, destinoSeg,
                        tiempoActual.toLocalDate(), fechaLimite
                    );

                    // Filtrar por capacidad CONSIDERANDO USO EN ESTA SOLUCIÓN
                    VueloInstancia vueloSegmento = vuelosSegmento.stream()
                        .filter(v -> solucionActual.tieneCapacidadDisponible(v.getIdInstancia(), v.getCapacidadMaxima(), cantidad))
                        .findFirst()
                        .orElse(null);

                    if (vueloSegmento != null) {
                        rutaEscalas.add(new SolucionMoraPack.SegmentoVuelo(
                            vueloSegmento.getIdInstancia(),
                            origenSeg, destinoSeg,
                            vueloSegmento.getHorarioSalidaCompleto(),
                            vueloSegmento.getHorarioLlegadaCompleto()
                        ));
                        tiempoActual = vueloSegmento.getHorarioLlegadaCompleto();
                    } else {
                        rutaFactible = false;
                        break;
                    }
                }

                // Si logramos construir la ruta completa, usarla
                if (rutaFactible && !rutaEscalas.isEmpty()) {
                    return rutaEscalas;
                }
            }
        }

        // ESTRATEGIA 3: Fallback - usar cualquier vuelo directo disponible
        // (aunque no tenga capacidad suficiente - será penalizado en fitness)
        if (!vuelosDirectos.isEmpty()) {
            VueloInstancia vueloSeleccionado = seleccionarVueloConPonderacion(vuelosDirectos, fechaInicio, fechaLimite);

            if (vueloSeleccionado != null) {
                segmentos.add(new SolucionMoraPack.SegmentoVuelo(
                    vueloSeleccionado.getIdInstancia(), origen, destino,
                    vueloSeleccionado.getHorarioSalidaCompleto(),
                    vueloSeleccionado.getHorarioLlegadaCompleto()
                ));
                return segmentos;
            }
        }

        // ESTRATEGIA 4: Último recurso - ruta con escalas sin validar capacidad
        // (esto será penalizado en fitness pero al menos completa el pedido)
        if (rutaMinima == null) {
            rutaMinima = red.buscarRutaMinima(origen, destino);
        }

        if (rutaMinima != null && rutaMinima.size() > 1) {
            LocalDate fechaInicioRuta = calcularFechaInicioOptima(rutaMinima, fechaInicio, fechaLimite, red);

            if (fechaInicioRuta != null) {
                List<VueloInstancia> rutaConEscalas = red.buscarRutasConConexiones(origen, destino, fechaInicioRuta, fechaLimite, 2)
                    .stream()
                    .findFirst()
                    .orElse(null);

                if (rutaConEscalas != null) {
                    for (VueloInstancia instancia : rutaConEscalas) {
                        segmentos.add(new SolucionMoraPack.SegmentoVuelo(
                            instancia.getIdInstancia(),
                            instancia.getAeropuertoOrigen(),
                            instancia.getAeropuertoDestino(),
                            instancia.getHorarioSalidaCompleto(),
                            instancia.getHorarioLlegadaCompleto()
                        ));
                    }
                }
            }
        }

        return segmentos;
    }

    /**
     * Selecciona un vuelo con ponderación hacia fechas más tempranas
     * pero considerando capacidad disponible
     */
    private VueloInstancia seleccionarVueloConPonderacion(List<VueloInstancia> vuelos, LocalDate fechaInicio, LocalDate fechaLimite) {
        if (vuelos.isEmpty()) return null;

        double[] pesos = new double[vuelos.size()];
        double sumaPesos = 0.0;

        for (int i = 0; i < vuelos.size(); i++) {
            VueloInstancia vuelo = vuelos.get(i);

            // Calcular días desde el inicio (fechas tempranas = mayor peso)
            long diasDesdeInicio = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, vuelo.getFecha());
            long diasTotales = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaLimite);

            // Ponderación: fechas tempranas tienen mayor peso
            double factorTemporal = diasTotales > 0 ? 1.0 - (double) diasDesdeInicio / diasTotales : 1.0;

            // Factor de capacidad: más capacidad = mayor peso
            double factorCapacidad = (double) vuelo.getCapacidadDisponible() / vuelo.getCapacidadMaxima();

            // Peso combinado
            pesos[i] = (factorTemporal * 0.7 + factorCapacidad * 0.3) * (1.0 + random.nextDouble() * 0.2); // Añadir algo de aleatoriedad
            sumaPesos += pesos[i];
        }

        // Normalizar pesos
        if (sumaPesos > 0) {
            for (int i = 0; i < pesos.length; i++) {
                pesos[i] /= sumaPesos;
            }
        }

        // Seleccionar por ruleta
        int indice = seleccionarPorRuleta(pesos);
        return vuelos.get(indice);
    }

    /**
     * Calcula la fecha óptima para iniciar una ruta con escalas
     */
    private LocalDate calcularFechaInicioOptima(List<String> rutaAeropuertos, LocalDate fechaInicio, LocalDate fechaLimite, RedDistribucion red) {
        // Estimar tiempo mínimo necesario para la ruta
        int numSegmentos = rutaAeropuertos.size() - 1;
        int diasEstimados = numSegmentos; // Aproximadamente 1 día por segmento

        // Fecha óptima: lo más pronto posible pero con margen
        LocalDate fechaOptima = fechaLimite.minusDays(diasEstimados + 1); // +1 día de margen

        // Asegurar que no sea antes del inicio del mes
        if (fechaOptima.isBefore(fechaInicio)) {
            fechaOptima = fechaInicio;
        }

        // Verificar que haya tiempo suficiente
        if (fechaOptima.isAfter(fechaLimite.minusDays(1))) {
            return null; // No hay tiempo suficiente
        }

        return fechaOptima;
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
        double fitnessCalculado = problema.evaluarSolucion(solucionActual);
        this.fitness = fitnessCalculado;
        solucionActual.setFitness(fitnessCalculado);  // ¡IMPORTANTE! Asignar fitness a la solución
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