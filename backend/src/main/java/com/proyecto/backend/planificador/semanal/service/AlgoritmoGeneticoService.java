    package com.proyecto.backend.planificador.semanal.service;

    import com.proyecto.backend.planificador.semanal.model.*;
    import com.proyecto.backend.planificador.semanal.dto.request.PlanificacionRequest;
    import com.proyecto.backend.planificador.semanal.dto.response.*;
    import com.proyecto.backend.simulation.dto.ProgresoAGDTO;
    import com.proyecto.backend.simulation.state.SessionStateManager;
    import com.proyecto.backend.model.Aeropuerto;
    import com.proyecto.backend.model.PedidoDiario;
    import com.proyecto.backend.model.PedidoSemanal;
    import com.proyecto.backend.repository.PedidoSemanalRepository;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.LocalTime;
    import java.util.*;
    import java.util.concurrent.*;

    /**
     * Servicio del algoritmo genetico para planificacion de rutas
     * Version inicial simplificada - base para iteraciones futuras
     */
    @Service
    @Slf4j
    @RequiredArgsConstructor
    public class AlgoritmoGeneticoService {

        private final WorldCacheService worldCacheService;
        private final PedidoSemanalRepository pedidoSemanalRepository;

        // Constantes de negocio
        private static final int PLAZO_MISMO_CONTINENTE_DIAS = 2;
        private static final int PLAZO_DIFERENTE_CONTINENTE_DIAS = 3;
        private static final int VENTANA_RECOJO_HORAS = 2;

        // Parametros del algoritmo genetico (valores por defecto - MODO ULTRA RÁPIDO)
        private static final int TAMANIO_POBLACION_DEFAULT = 3;       // Mínimo para diversidad
        private static final int MAX_GENERACIONES_DEFAULT = 1;        // Solo 1 generación (el más rápido)
        private static final int NO_MEJORA_LIMITE_DEFAULT = 1;        // Parar inmediatamente si no mejora
        private static final int ELITE_K = 1;                         // Solo el mejor
        private static final double PROB_CRUCE = 0.8;                 // Probabilidad de cruce
        private static final double PROB_MUTACION = 0.05;             // Probabilidad de mutacion
        private static final int TAMANIO_TORNEO = 2;                  // Torneo mínimo
        
        // Parámetros configurables (pueden ser sobrescritos desde WebSocket)
        private int TAMANIO_POBLACION = TAMANIO_POBLACION_DEFAULT;
        private int MAX_GENERACIONES = MAX_GENERACIONES_DEFAULT;
        private int NO_MEJORA_LIMITE = NO_MEJORA_LIMITE_DEFAULT;

        /**
         * Ejecuta la planificacion de rutas para una fecha dada
         *
         * @param request Request con parametros de planificacion
         * @return Response con la planificacion completa
         */
        @Transactional(readOnly = true)
        public PlanificacionResponse planificar(PlanificacionRequest request) {
            long inicio = System.currentTimeMillis();

            log.debug("Iniciando planificacion para fecha {} con K={}", request.getFecha(), request.getFactorK());

            // Obtener el World base (singleton inmutable)
            World world = worldCacheService.getWorld();

            // Cargar pedidos en el rango de tiempo
            List<PedidoSemanal> pedidos = cargarPedidosEnRango(request);
            log.debug("Cargados {} pedidos para procesar", pedidos.size());

            if (pedidos.isEmpty()) {
            log.warn("No hay pedidos para procesar en el rango especificado");
            return crearResponseVacio(request, inicio);
        }

        // Calcular horizonte temporal (dias a expandir)
        int numeroDias = calcularHorizonteDias(request, pedidos);
        log.info("Horizonte temporal: {} dias", numeroDias);

        // Usar la hora de inicio del request (por defecto 00:00)
        LocalDateTime fechaBaseUTC = request.getStartDateTime();

        // Crear WorldTemporal para esta ejecucion (filtra vuelos anteriores a fechaBaseUTC)
        WorldTemporal worldTemporal = new WorldTemporal(world, fechaBaseUTC, numeroDias);
        log.info("WorldTemporal creado: {}", worldTemporal.getEstadisticas());

        // Crear controlador de almacenes para rastrear ocupacion
        ControladorAlmacenes controladorAlmacenes = new ControladorAlmacenes(numeroDias, fechaBaseUTC);

        // Registrar todos los aeropuertos con sus capacidades
        for (Aeropuerto aeropuerto : world.getAeropuertos().values()) {
            // Hubs tienen capacidad 0 (ilimitada), otros tienen su capacidad real
            int capacidad = aeropuerto.tieneStockIlimitado() ? 0 : aeropuerto.getCapacidadAlmacen();
            controladorAlmacenes.registrarAeropuerto(aeropuerto.getCodigoICAO(), capacidad);
        }
        log.info("ControladorAlmacenes: {}", controladorAlmacenes.obtenerEstadisticas());

        // Ejecutar algoritmo genetico completo
        Solution solucion = ejecutarAlgoritmoGenetico(worldTemporal, controladorAlmacenes, pedidos);

        // Log estadisticas finales
        log.info("Estadisticas finales: {}", worldTemporal.getEstadisticas());
        log.info("Solucion final: {}", solucion.getResumen());

        // Convertir solucion a response DTO (incluye los pedidos para verificacion)
        PlanificacionResponse response = convertirAResponse(solucion, worldTemporal, request, pedidos, inicio);

        long tiempoTotal = System.currentTimeMillis() - inicio;
        log.info("Planificacion completada en {} ms", tiempoTotal);

        return response;
    }

    /**
     * Ejecuta la planificacion de rutas y retorna formato simplificado
     * Solo incluye lista de vuelos con sus pedidos asignados
     *
     * @param request Request con parametros de planificacion
     * @return Response simplificado con vuelos y pedidos
     */
    @Transactional(readOnly = true)
    public PlanificacionResponseSimple planificarSimple(PlanificacionRequest request) {
        long inicio = System.currentTimeMillis();

        log.info("Iniciando planificacion simplificada para fecha {} con K={}", request.getFecha(), request.getFactorK());

        // Obtener el World base (singleton inmutable)
        World world = worldCacheService.getWorld();

        // Cargar pedidos en el rango de tiempo
        List<PedidoSemanal> pedidos = cargarPedidosEnRango(request);
        log.info("Cargados {} pedidos para procesar", pedidos.size());

        if (pedidos.isEmpty()) {
            log.warn("No hay pedidos para procesar en el rango especificado");
            return new PlanificacionResponseSimple();
        }

        // Calcular horizonte temporal (dias a expandir)
        int numeroDias = calcularHorizonteDias(request, pedidos);
        log.info("Horizonte temporal: {} dias", numeroDias);

        // Usar la hora de inicio del request (por defecto 00:00)
        LocalDateTime fechaBaseUTC = request.getStartDateTime();

        // Crear WorldTemporal para esta ejecucion (filtra vuelos anteriores a fechaBaseUTC)
        WorldTemporal worldTemporal = new WorldTemporal(world, fechaBaseUTC, numeroDias);
        log.info("WorldTemporal creado: {}", worldTemporal.getEstadisticas());

        // Crear controlador de almacenes para rastrear ocupacion
        ControladorAlmacenes controladorAlmacenes = new ControladorAlmacenes(numeroDias, fechaBaseUTC);

        // Registrar todos los aeropuertos con sus capacidades
        for (Aeropuerto aeropuerto : world.getAeropuertos().values()) {
            // Hubs tienen capacidad 0 (ilimitada), otros tienen su capacidad real
            int capacidad = aeropuerto.tieneStockIlimitado() ? 0 : aeropuerto.getCapacidadAlmacen();
            controladorAlmacenes.registrarAeropuerto(aeropuerto.getCodigoICAO(), capacidad);
        }
        log.info("ControladorAlmacenes: {}", controladorAlmacenes.obtenerEstadisticas());

        // Ejecutar algoritmo genetico completo
        Solution solucion = ejecutarAlgoritmoGenetico(worldTemporal, controladorAlmacenes, pedidos);

        // Log estadisticas finales
        log.info("Estadisticas finales: {}", worldTemporal.getEstadisticas());
        log.info("Solucion final: {}", solucion.getResumen());

        // Convertir solucion a response simplificado
        PlanificacionResponseSimple response = convertirAResponseSimple(solucion, worldTemporal);

        long tiempoTotal = System.currentTimeMillis() - inicio;
        log.info("Planificacion simplificada completada en {} ms - {} vuelos generados",
                tiempoTotal, response.getTotalVuelos());

        return response;
    }

    /**
     * Ejecuta un ciclo del algoritmo genetico para la simulacion en tiempo real
     *
     * Este metodo es llamado por el SimulacionOrchestrator cada Sa minutos
     * con un timeout de Ta segundos.
     *
     * @param state Estado global de la simulacion
     * @param tiempoHasta Tiempo hasta el cual planificar (ventana acumulativa Sc)
     */
    public void ejecutarCiclo(SimulationState state, LocalDateTime tiempoHasta) {
        long inicio = System.currentTimeMillis();

        log.info("Ejecutando ciclo AG: desde {} hasta {} (ventana: {} min)",
            state.getTiempoRealInicio(), tiempoHasta,
            java.time.temporal.ChronoUnit.MINUTES.between(state.getTiempoRealInicio(), tiempoHasta));

        // 1. Filtrar pedidos PENDIENTES del state
        List<PedidoSemanal> pedidosPendientes = filtrarPedidosPendientes(state, tiempoHasta);

        if (pedidosPendientes.isEmpty()) {
            log.info("No hay pedidos pendientes para planificar en esta ventana");
            return;
        }

        log.info("Pedidos pendientes a planificar: {}", pedidosPendientes.size());

        // 2. Calcular horizonte dinamico (fecha maxima de pedido + 3 dias)
        LocalDateTime fechaBaseUTC = state.getTiempoRealInicio();
        int diasNecesarios = calcularHorizonteDinamico(fechaBaseUTC.toLocalDate(), tiempoHasta);
        log.info("Horizonte dinamico: {} dias", diasNecesarios);

        // 3. Obtener World base
        World world = worldCacheService.getWorld();

        // 4. Crear WorldTemporal con horizonte dinamico (filtra vuelos anteriores a fechaBaseUTC)
        WorldTemporal worldTemporal = new WorldTemporal(world, fechaBaseUTC, diasNecesarios);

        // 5. Inicializar WorldTemporal desde state (cargar capacidades ya usadas)
        inicializarWorldDesdeState(worldTemporal, state);

        // 6. Crear ControladorAlmacenes y cargar desde state
        ControladorAlmacenes controladorAlmacenes = new ControladorAlmacenes(diasNecesarios, fechaBaseUTC);

        // Registrar aeropuertos
        for (Aeropuerto aeropuerto : world.getAeropuertos().values()) {
            int capacidad = aeropuerto.tieneStockIlimitado() ? 0 : aeropuerto.getCapacidadAlmacen();
            controladorAlmacenes.registrarAeropuerto(aeropuerto.getCodigoICAO(), capacidad);
        }

        // Cargar ocupacion desde state
        inicializarControladorDesdeState(controladorAlmacenes, state);

        // 7. Ejecutar algoritmo genetico con parametros del state
        Solution solucion = ejecutarAlgoritmoGeneticoConParametros(
            worldTemporal, controladorAlmacenes, pedidosPendientes, state.getParametrosAG());

        // 8. Actualizar state con la solucion
        actualizarStateConSolucion(state, solucion, worldTemporal);

        long duracion = System.currentTimeMillis() - inicio;
        log.info("Ciclo AG completado en {} ms - Planificados: {}/{}",
            duracion, solucion.getPedidosATiempo() + solucion.getPedidosTarde(), pedidosPendientes.size());
    }

    /**
     * Filtra los pedidos PENDIENTES dentro de la ventana de tiempo
     */
    private List<PedidoSemanal> filtrarPedidosPendientes(SimulationState state, LocalDateTime tiempoHasta) {
        List<PedidoSemanal> pedidos = new ArrayList<>();

        for (PedidoState pedidoState : state.getPedidos().values()) {
            if (pedidoState.getEstado() == EstadoPedido.PENDIENTE) {
                // Verificar si el pedido esta en la ventana
                if (pedidoState.getFechaCreacion().isBefore(tiempoHasta)) {
                    // Cargar el pedido desde el repositorio
                    PedidoSemanal pedido = pedidoSemanalRepository.findById(pedidoState.getId()).orElse(null);
                    if (pedido != null) {
                        pedidos.add(pedido);
                    }
                }
            }
        }

        return pedidos;
    }

    /**
     * Calcula el horizonte dinamico en dias
     * Formula: dias desde fechaBase hasta tiempoHasta + 3 dias plazo maximo
     */
    private int calcularHorizonteDinamico(LocalDate fechaBase, LocalDateTime tiempoHasta) {
        int diasHastaVentana = (int) java.time.temporal.ChronoUnit.DAYS.between(
            fechaBase, tiempoHasta.toLocalDate()) + 1;

        // Agregar 3 dias de plazo maximo de entrega
        int diasTotal = diasHastaVentana + 3;

        // Minimo 4 dias, maximo 14 dias (2 semanas)
        return Math.max(4, Math.min(14, diasTotal));
    }

    /**
     * Inicializa WorldTemporal con las capacidades ya usadas desde el state
     * ⚠️ CRÍTICO: Sincroniza ESTADOS para que el AG NO modifique vuelos EN_VUELO o ATERRIZADO
     */
    private void inicializarWorldDesdeState(WorldTemporal worldTemporal, SimulationState state) {
        int vuelosEnVuelo = 0;
        int vuelosAterrizado = 0;
        int vuelosProgramados = 0;

        for (VueloState vueloState : state.getVuelos().values()) {
            // Reservar la capacidad ya usada
            VueloInstancia instancia = worldTemporal.getVuelo(vueloState.getId());
            if (instancia != null) {
                // ⚠️ SINCRONIZAR ESTADO: SimulationState → WorldTemporal
                instancia.setEstado(vueloState.getEstado());

                // Contar por estado
                switch (vueloState.getEstado()) {
                    case EN_VUELO -> vuelosEnVuelo++;
                    case ATERRIZADO -> vuelosAterrizado++;
                    case PROGRAMADO -> vuelosProgramados++;
                    case CANCELADO -> { /* Ignorar cancelados */ }
                }

                // Asignar la capacidad que ya está siendo usada
                int capacidadUsada = vueloState.getCapacidadUsada();
                if (capacidadUsada > 0) {
                    instancia.asignarCapacidad(capacidadUsada);
                }
            }
        }

        log.info("🔄 WorldTemporal sincronizado: {} PROGRAMADOS (modificables), {} EN_VUELO (bloqueados), {} ATERRIZADOS", 
            vuelosProgramados, vuelosEnVuelo, vuelosAterrizado);
    }

    /**
     * Inicializa ControladorAlmacenes con la ocupacion actual desde el state
     */
    private void inicializarControladorDesdeState(ControladorAlmacenes controlador,
                                                    SimulationState state) {
        for (AlmacenState almacen : state.getAlmacenes().values()) {
            // Calcular cuando llego cada pedido y reservar espacio
            for (Long pedidoId : almacen.getPedidosAlmacenados()) {
                PedidoState pedido = state.getPedidos().get(pedidoId);
                if (pedido != null) {
                    // El pedido esta en el almacen, reservar espacio desde ahora
                    LocalDateTime tiempoInicio = state.getTiempoSimulacion();
                    
                    // Reservar hasta el final del horizonte (sera liberado cuando salga)
                    LocalDateTime tiempoFin = tiempoInicio.plusDays(7);

                    // Usar agregarIntervalo en lugar de reservar
                    controlador.agregarIntervalo(almacen.getCodigoICAO(), 
                        tiempoInicio, tiempoFin, pedido.getCantidad());
                }
            }
        }

        log.debug("ControladorAlmacenes inicializado con ocupacion de {} almacenes",
            state.getAlmacenes().size());
    }

    /**
     * Actualiza el state con la solucion del algoritmo genetico
     */
    private void actualizarStateConSolucion(SimulationState state, Solution solucion,
                                             WorldTemporal worldTemporal) {
        int pedidosPlanificados = 0;

        for (Map.Entry<PedidoSemanal, List<SubRuta>> entry : solucion.getRutas().entrySet()) {
            PedidoSemanal pedido = entry.getKey();
            List<SubRuta> subrutas = entry.getValue();

            PedidoState pedidoState = state.getPedidos().get(pedido.getId());
            if (pedidoState == null) continue;

            // Cambiar estado a PLANIFICADO
            pedidoState.setEstado(EstadoPedido.PLANIFICADO);

            // Construir ruta planificada
            List<VueloAsignado> rutaPlanificada = new ArrayList<>();
            LocalDateTime llegadaEstimada = null;

            for (SubRuta subruta : subrutas) {
                for (VueloUso vueloUso : subruta.getVuelos()) {
                    String vueloId = vueloUso.generarId();
                    VueloInstancia instancia = worldTemporal.getVuelo(vueloId);

                    if (instancia != null) {
                        // Crear VueloAsignado
                        VueloAsignado vueloAsignado = new VueloAsignado();
                        vueloAsignado.setVueloId(vueloId);
                        vueloAsignado.setOrigen(vueloUso.getOrigen());
                        vueloAsignado.setDestino(vueloUso.getDestino());
                        vueloAsignado.setSalida(instancia.getSalidaUTC());
                        vueloAsignado.setLlegada(instancia.getLlegadaUTC());
                        vueloAsignado.setCantidad(vueloUso.getCantidadAsignada());

                        rutaPlanificada.add(vueloAsignado);
                        llegadaEstimada = instancia.getLlegadaUTC();

                        // Actualizar VueloState en el state
                        VueloState vueloState = state.getVuelos().computeIfAbsent(vueloId, k -> {
                            VueloState v = new VueloState();
                            v.setId(vueloId);
                            v.setOrigen(vueloUso.getOrigen());
                            v.setDestino(vueloUso.getDestino());
                            v.setSalida(instancia.getSalidaUTC());
                            v.setLlegada(instancia.getLlegadaUTC());
                            v.setCapacidadMaxima(vueloUso.getCapacidadMaxima());
                            v.setCapacidadUsada(0);
                            v.setEstado(EstadoVuelo.PROGRAMADO);
                            v.setProgreso(0.0);
                            return v;
                        });

                        // Reservar capacidad
                        vueloState.setCapacidadUsada(
                            vueloState.getCapacidadUsada() + vueloUso.getCantidadAsignada());
                    }
                }
            }

            // Actualizar pedido con ruta planificada
            pedidoState.setRutaPlanificada(rutaPlanificada);
            pedidoState.setFechaEntregaEstimada(llegadaEstimada);
            pedidoState.setProgresoRuta(0.0);

            pedidosPlanificados++;
        }

        log.info("State actualizado: {} pedidos planificados, {} vuelos creados/actualizados",
            pedidosPlanificados, state.getVuelos().size());
    }

    /**
     * Ejecuta el algoritmo genetico completo
     *
     * @param worldTemporal World temporal con vuelos expandidos
     * @param controladorAlmacenes Controlador de capacidad de almacenes
     * @param pedidos Lista de pedidos a planificar
     * @return Mejor solucion encontrada
     */
    private Solution ejecutarAlgoritmoGenetico(WorldTemporal worldTemporal,
                                               ControladorAlmacenes controladorAlmacenes,
                                               List<PedidoSemanal> pedidos) {
        log.info("Iniciando algoritmo genetico: poblacion={}, generaciones={}, elite={}",
                TAMANIO_POBLACION, MAX_GENERACIONES, ELITE_K);

        Random random = new Random();
        int numeroPedidos = pedidos.size();

        // Crear decodificador genetico
        DecodificadorGenetico decodificador = new DecodificadorGenetico(worldTemporal, controladorAlmacenes);

        // 1. Generar poblacion inicial
        List<Individuo> poblacion = generarPoblacionInicial(numeroPedidos, random);
        log.info("Poblacion inicial generada: {} individuos", poblacion.size());

        // Evaluar poblacion inicial
        evaluarPoblacion(poblacion, decodificador, pedidos, worldTemporal, controladorAlmacenes);

        // Ordenar por fitness (mejor primero)
        poblacion.sort(Comparator.comparingDouble((Individuo i) -> i.fitness).reversed());

        double mejorFitnessGlobal = poblacion.get(0).fitness;
        int generacionesSinMejora = 0;

        log.info("Gen 0: Mejor fitness = {:.2f}, Promedio = {:.2f}",
                mejorFitnessGlobal, calcularFitnessPromedio(poblacion));

        // 2. Loop evolutivo
        for (int generacion = 1; generacion <= MAX_GENERACIONES; generacion++) {
            // Crear nueva generacion
            List<Individuo> nuevaPoblacion = new ArrayList<>();

            // Elitismo: copiar mejores K individuos
            for (int i = 0; i < ELITE_K && i < poblacion.size(); i++) {
                nuevaPoblacion.add(new Individuo(poblacion.get(i).cromosoma.copiar()));
            }

            // Generar resto de la poblacion
            while (nuevaPoblacion.size() < TAMANIO_POBLACION) {
                // Seleccion por torneo
                Chromosome padre1 = seleccionTorneo(poblacion, random).cromosoma;
                Chromosome padre2 = seleccionTorneo(poblacion, random).cromosoma;

                // Cruce
                Chromosome hijo;
                if (random.nextDouble() < PROB_CRUCE) {
                    hijo = padre1.cruzar(padre2, random);
                } else {
                    hijo = padre1.copiar();
                }

                // Mutacion
                hijo.mutar(PROB_MUTACION, random);

                nuevaPoblacion.add(new Individuo(hijo));
            }

            // Evaluar nueva poblacion
            evaluarPoblacion(nuevaPoblacion, decodificador, pedidos, worldTemporal, controladorAlmacenes);

            // Ordenar por fitness
            nuevaPoblacion.sort(Comparator.comparingDouble((Individuo i) -> i.fitness).reversed());

            // Actualizar poblacion
            poblacion = nuevaPoblacion;

            // Verificar mejora
            double mejorFitnessActual = poblacion.get(0).fitness;
            double fitnessPromedio = calcularFitnessPromedio(poblacion);

            if (mejorFitnessActual > mejorFitnessGlobal) {
                mejorFitnessGlobal = mejorFitnessActual;
                generacionesSinMejora = 0;
                log.info("Gen {}: MEJORA - Mejor fitness = {:.2f}, Promedio = {:.2f}",
                        generacion, mejorFitnessGlobal, fitnessPromedio);
            } else {
                generacionesSinMejora++;
                if (generacion % 20 == 0) {
                    log.info("Gen {}: Mejor fitness = {:.2f}, Promedio = {:.2f}, Sin mejora: {}",
                            generacion, mejorFitnessGlobal, fitnessPromedio, generacionesSinMejora);
                }
            }

            // Criterio de parada: sin mejora por N generaciones
            if (generacionesSinMejora >= NO_MEJORA_LIMITE) {
                log.info("Algoritmo detenido: {} generaciones sin mejora", generacionesSinMejora);
                break;
            }
        }

        // Retornar mejor solucion encontrada
        Individuo mejorIndividuo = poblacion.get(0);
        log.info("Algoritmo genetico completado: Fitness final = {:.2f}", mejorIndividuo.fitness);

        // ❌ DESACTIVADO: Ya no guardamos estados en BD, solo en RAM
        // El estado se mantiene en PedidoState (memoria) durante la simulación
        // actualizarEstadoPedidosPlanificados(mejorIndividuo.solucion);

        return mejorIndividuo.solucion;
    }

    /**
     * Ejecuta el algoritmo genetico con parametros configurables desde el state
     *
     * @param worldTemporal World temporal con vuelos expandidos
     * @param controladorAlmacenes Controlador de capacidad de almacenes
     * @param pedidos Lista de pedidos a planificar
     * @param parametros Parametros configurables del AG
     * @return Mejor solucion encontrada
     */
    private Solution ejecutarAlgoritmoGeneticoConParametros(WorldTemporal worldTemporal,
                                                             ControladorAlmacenes controladorAlmacenes,
                                                             List<PedidoSemanal> pedidos,
                                                             SimulationState.ParametrosAG parametros) {
        log.info("Iniciando algoritmo genetico: poblacion={}, generaciones={}, elite={}",
                parametros.getTamanioPoblacion(), parametros.getMaxGeneraciones(), parametros.getEliteK());

        Random random = new Random();
        int numeroPedidos = pedidos.size();

        // Crear decodificador genetico
        DecodificadorGenetico decodificador = new DecodificadorGenetico(worldTemporal, controladorAlmacenes);

        // 1. Generar poblacion inicial
        List<Individuo> poblacion = generarPoblacionInicial(numeroPedidos, parametros.getTamanioPoblacion(), random);
        log.info("Poblacion inicial generada: {} individuos", poblacion.size());

        // Evaluar poblacion inicial
        evaluarPoblacion(poblacion, decodificador, pedidos, worldTemporal, controladorAlmacenes);

        // Ordenar por fitness (mejor primero)
        poblacion.sort(Comparator.comparingDouble((Individuo i) -> i.fitness).reversed());

        double mejorFitnessGlobal = poblacion.get(0).fitness;
        int generacionesSinMejora = 0;

        log.info("Gen 0: Mejor fitness = {:.2f}, Promedio = {:.2f}",
                mejorFitnessGlobal, calcularFitnessPromedio(poblacion));

        // 2. Loop evolutivo
        for (int generacion = 1; generacion <= parametros.getMaxGeneraciones(); generacion++) {
            // Verificar si el thread fue interrumpido (para timeout)
            if (Thread.currentThread().isInterrupted()) {
                log.warn("AG interrumpido en generacion {} por timeout", generacion);
                break;
            }

            // Crear nueva generacion
            List<Individuo> nuevaPoblacion = new ArrayList<>();

            // Elitismo: copiar mejores K individuos
            for (int i = 0; i < parametros.getEliteK() && i < poblacion.size(); i++) {
                nuevaPoblacion.add(new Individuo(poblacion.get(i).cromosoma.copiar()));
            }

            // Generar resto de la poblacion
            while (nuevaPoblacion.size() < parametros.getTamanioPoblacion()) {
                // Seleccion por torneo
                Chromosome padre1 = seleccionTorneo(poblacion, random).cromosoma;
                Chromosome padre2 = seleccionTorneo(poblacion, random).cromosoma;

                // Cruce
                Chromosome hijo;
                if (random.nextDouble() < parametros.getProbabilidadCruce()) {
                    hijo = padre1.cruzar(padre2, random);
                } else {
                    hijo = padre1.copiar();
                }

                // Mutacion
                hijo.mutar(parametros.getProbabilidadMutacion(), random);

                nuevaPoblacion.add(new Individuo(hijo));
            }

            // Evaluar nueva poblacion
            evaluarPoblacion(nuevaPoblacion, decodificador, pedidos, worldTemporal, controladorAlmacenes);

            // Ordenar por fitness
            nuevaPoblacion.sort(Comparator.comparingDouble((Individuo i) -> i.fitness).reversed());

            // Actualizar poblacion
            poblacion = nuevaPoblacion;

            // Verificar mejora
            double mejorFitnessActual = poblacion.get(0).fitness;
            double fitnessPromedio = calcularFitnessPromedio(poblacion);

            if (mejorFitnessActual > mejorFitnessGlobal) {
                mejorFitnessGlobal = mejorFitnessActual;
                generacionesSinMejora = 0;
                log.info("Gen {}: MEJORA - Mejor fitness = {:.2f}, Promedio = {:.2f}",
                        generacion, mejorFitnessGlobal, fitnessPromedio);
            } else {
                generacionesSinMejora++;
                if (generacion % 20 == 0) {
                    log.info("Gen {}: Mejor fitness = {:.2f}, Promedio = {:.2f}, Sin mejora: {}",
                            generacion, mejorFitnessGlobal, fitnessPromedio, generacionesSinMejora);
                }
            }

            // Criterio de parada: sin mejora por N generaciones
            if (generacionesSinMejora >= parametros.getLimiteGeneracionesSinMejora()) {
                log.info("Algoritmo detenido: {} generaciones sin mejora", generacionesSinMejora);
                break;
            }
        }

        // Retornar mejor solucion encontrada
        Individuo mejorIndividuo = poblacion.get(0);
        log.info("Algoritmo genetico completado: Fitness final = {:.2f}", mejorIndividuo.fitness);

        // ❌ DESACTIVADO: Ya no guardamos estados en BD, solo en RAM
        // El estado se mantiene en PedidoState (memoria) durante la simulación
        // actualizarEstadoPedidosPlanificados(mejorIndividuo.solucion);

        return mejorIndividuo.solucion;
    }

    /**
     * Genera poblacion inicial de cromosomas aleatorios
     */
    private List<Individuo> generarPoblacionInicial(int numeroPedidos, Random random) {
        return generarPoblacionInicial(numeroPedidos, TAMANIO_POBLACION, random);
    }

    /**
     * Genera poblacion inicial de cromosomas aleatorios con tamanio configurable
     */
    private List<Individuo> generarPoblacionInicial(int numeroPedidos, int tamanioPoblacion, Random random) {
        List<Individuo> poblacion = new ArrayList<>();
        for (int i = 0; i < tamanioPoblacion; i++) {
            Chromosome cromosoma = new Chromosome(numeroPedidos);
            cromosoma.inicializarAleatorio(random);
            poblacion.add(new Individuo(cromosoma));
        }
        return poblacion;
    }

    /**
     * Evalúa población con PARALELIZACIÓN CONTROLADA (2-3 threads máximo)
     * 
     * ESTRATEGIA SEGURA:
     * - Divide población en 2-3 chunks grandes
     * - Cada chunk en thread separado con lock fino
     * - Speedup: ~2x sin saturar CPU
     */
    private void evaluarPoblacion(List<Individuo> poblacion, DecodificadorGenetico decodificador,
                                   List<PedidoSemanal> pedidos, WorldTemporal worldTemporal,
                                   ControladorAlmacenes controladorAlmacenes) {
        
        // Filtrar solo no evaluados
        List<Individuo> sinEvaluar = poblacion.stream()
            .filter(ind -> ind.solucion == null)
            .toList();
        
        if (sinEvaluar.isEmpty()) return;
        
        // CONFIGURACIÓN ÓPTIMA: 2-4 threads según tamaño de población
        // - Población pequeña (≤20): 2 threads
        // - Población mediana (21-50): 3 threads
        // - Población grande (>50): 4 threads (máximo seguro)
        int NUM_THREADS;
        if (sinEvaluar.size() <= 20) {
            NUM_THREADS = 2; // Población pequeña: menos overhead
        } else if (sinEvaluar.size() <= 50) {
            NUM_THREADS = 3;
        } else {
            NUM_THREADS = Math.min(4, Runtime.getRuntime().availableProcessors());
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        
        // Dividir en chunks
        int chunkSize = (int) Math.ceil((double) sinEvaluar.size() / NUM_THREADS);
        List<List<Individuo>> chunks = new ArrayList<>();
        for (int i = 0; i < sinEvaluar.size(); i += chunkSize) {
            chunks.add(sinEvaluar.subList(i, Math.min(i + chunkSize, sinEvaluar.size())));
        }
        
        Object lock = new Object();
        
        try {
            // ⚡ EJECUTAR CHUNKS EN PARALELO
            List<Future<?>> futures = new ArrayList<>();
            for (List<Individuo> chunk : chunks) {
                futures.add(executor.submit(() -> {
                    for (Individuo ind : chunk) {
                        synchronized (lock) {
                            worldTemporal.resetearCapacidades();
                            controladorAlmacenes.limpiar();
                            ind.solucion = decodificador.decodificar(ind.cromosoma, pedidos);
                            ind.fitness = ind.solucion.getObjetivo();
                        }
                    }
                }));
            }
            
            // Esperar a que terminen todos
            for (Future<?> f : futures) f.get();
            
        } catch (Exception e) {
            log.error("Error evaluación paralela: {}", e.getMessage());
            throw new RuntimeException("Falló evaluación", e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Seleccion por torneo
     */
    private Individuo seleccionTorneo(List<Individuo> poblacion, Random random) {
        Individuo mejor = null;
        for (int i = 0; i < TAMANIO_TORNEO; i++) {
            Individuo candidato = poblacion.get(random.nextInt(poblacion.size()));
            if (mejor == null || candidato.fitness > mejor.fitness) {
                mejor = candidato;
            }
        }
        return mejor;
    }

    /**
     * Calcula fitness promedio de la poblacion
     */
    private double calcularFitnessPromedio(List<Individuo> poblacion) {
        return poblacion.stream()
                .mapToDouble(i -> i.fitness)
                .average()
                .orElse(0.0);
    }

    /**
     * Clase interna para representar un individuo
     */
    private static class Individuo {
        Chromosome cromosoma;
        Solution solucion;
        double fitness;

        Individuo(Chromosome cromosoma) {
            this.cromosoma = cromosoma;
            this.solucion = null;
            this.fitness = Double.NEGATIVE_INFINITY;
        }
    }

    /**
     * Calcula el horizonte temporal en dias
     *
     * Reglas:
     * - Minimo: 10 dias (una semana + buffer de 3 dias para entregas)
     * - Dinamico: dia maximo de pedidos + 3 dias
     *
     * @param request Request de planificacion
     * @param pedidos Lista de pedidos
     * @return Numero de dias del horizonte
     */
    private int calcularHorizonteDias(PlanificacionRequest request, List<PedidoSemanal> pedidos) {
        // 🆕 CAMBIO: Minimo 10 dias para cubrir semana completa + buffer
        int diasBase = 10;

        // Calcular dia maximo de los pedidos
        LocalDate fechaMaxPedido = request.getFecha();
        for (PedidoSemanal pedido : pedidos) {
            LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
            if (fechaPedido.isAfter(fechaMaxPedido)) {
                fechaMaxPedido = fechaPedido;
            }
        }

        // Dias necesarios = (fechaMaxPedido - fechaBase) + 3 dias plazo + 1 buffer
        int diasNecesarios = (int) java.time.temporal.ChronoUnit.DAYS.between(
                request.getFecha(), fechaMaxPedido) + 4;

        // Usar el mayor entre diasBase y diasNecesarios
        return Math.max(diasBase, diasNecesarios);
    }

    /**
     * Carga los pedidos en el rango de tiempo especificado
     * Filtra por fecha Y por rango de horas/minutos segun el factor K
     *
     * @param request Request con parametros
     * @return Lista de pedidos a procesar
     */
    private List<PedidoSemanal> cargarPedidosEnRango(PlanificacionRequest request) {
        return cargarPedidosEnRango(request, null);
    }
    
    // 🆕 SEDES/HUBS que no deben ser destino (constante para evitar recrear)
    private static final List<String> SEDES_HUBS = List.of("SPIM", "EBCI", "UBBB");
    
    private List<PedidoSemanal> cargarPedidosEnRango(PlanificacionRequest request, LocalDateTime tiempoActualSimulacion) {
        LocalDate fecha = request.getFecha();
        int saltoConsumoMinutos = request.calcularRangoConsumoMinutos(); // Sc = K × Sa (ej: 70 min)

        LocalDateTime inicio;
        LocalDateTime fin;
        
        if (tiempoActualSimulacion != null) {
            // Iteraciones posteriores: ventana desde tiempo actual hasta tiempo + Sc
            inicio = tiempoActualSimulacion;
            fin = tiempoActualSimulacion.plusMinutes(saltoConsumoMinutos);
        } else {
            // Primera iteración: ventana desde medianoche hasta medianoche + Sc
            inicio = LocalDateTime.of(fecha, LocalTime.MIDNIGHT);
            fin = inicio.plusMinutes(saltoConsumoMinutos);
        }

        // ⏱️ TIMING: Medir query BD
        long tQuery = System.currentTimeMillis();
        
        List<PedidoSemanal> pedidos = pedidoSemanalRepository.findByRangoFechaExcluyendoDestinos(
                inicio.getYear(),
                inicio.getMonthValue(),
                inicio.getDayOfMonth(),
                inicio.getHour(),
                inicio.getMinute(),
                fin.getYear(),
                fin.getMonthValue(),
                fin.getDayOfMonth(),
                fin.getHour(),
                fin.getMinute(),
                SEDES_HUBS
        );
        
        long queryMs = System.currentTimeMillis() - tQuery;
        log.info("⏱️ [TIMING] Query BD: {} pedidos en {}ms | Ventana: [{} → {}]", 
                pedidos.size(), queryMs, inicio.toLocalTime(), fin.toLocalTime());

        return pedidos;
    }

    /**
     * Verifica si un pedido esta dentro del rango de tiempo especificado
     *
     * @param pedido Pedido a verificar
     * @param inicio Fecha/hora de inicio del rango
     * @param fin Fecha/hora de fin del rango
     * @return true si el pedido esta en el rango
     */
    private boolean estaDentroDelRango(PedidoSemanal pedido, LocalDateTime inicio, LocalDateTime fin) {
        // Construir la fecha/hora del pedido
        LocalDateTime fechaPedido = LocalDateTime.of(
                pedido.getAnio(),
                pedido.getMes(),
                pedido.getDia(),
                pedido.getHora(),
                pedido.getMinuto()
        );

        // Verificar si esta dentro del rango [inicio, fin)
        return !fechaPedido.isBefore(inicio) && fechaPedido.isBefore(fin);
    }

    /**
     * Crea una solucion vacia (placeholder)
     *
     * @param pedidos Pedidos a procesar
     * @return Solucion vacia
     */
    private Solution crearSolucionVacia(List<PedidoSemanal> pedidos) {
        Solution solucion = new Solution();

        // Marcar todos como no entregados por ahora
        solucion.setPedidosNoEntregados(pedidos.size());
        solucion.setObjetivo(0.0);

        return solucion;
    }

    /**
     * Crea un response vacio cuando no hay pedidos
     *
     * @param request Request original
     * @param inicio Tiempo de inicio
     * @return Response vacio
     */
    private PlanificacionResponse crearResponseVacio(PlanificacionRequest request, long inicio) {
        PlanificacionResponse response = new PlanificacionResponse();

        PlanificacionResponse.MetadataPlanificacion metadata = new PlanificacionResponse.MetadataPlanificacion();
        LocalDateTime fechaInicio = LocalDateTime.of(request.getFecha(), LocalTime.MIDNIGHT);
        LocalDateTime fechaFin = fechaInicio.plusMinutes(request.calcularRangoConsumoMinutos());

        metadata.setFechaInicio(fechaInicio);
        metadata.setFechaFin(fechaFin);
        metadata.setRangoDescripcion(String.format("Pedidos entre %s y %s (sin pedidos encontrados)",
                fechaInicio, fechaFin));
        metadata.setFactorK(request.getFactorK());
        metadata.setSaltoConsumoMinutos(request.calcularRangoConsumoMinutos());
        metadata.setSaltoAlgoritmoMinutos(request.getParametrosGenetico().getSaltoAlgoritmoMinutos());
        metadata.setPedidosProcesados(0);
        metadata.setTiempoEjecucionMs(System.currentTimeMillis() - inicio);

        response.setMetadata(metadata);
        response.setAeropuertos(new ArrayList<>());
        response.setVuelos(new ArrayList<>());
        response.setRutas(new ArrayList<>());

        return response;
    }

    /**
     * Convierte una Solution a PlanificacionResponse
     *
     * @param solucion Solucion del algoritmo
     * @param worldTemporal WorldTemporal con datos
     * @param request Request original
     * @param pedidos Lista de pedidos procesados
     * @param inicio Tiempo de inicio
     * @return Response DTO
     */
    private PlanificacionResponse convertirAResponse(
            Solution solucion, WorldTemporal worldTemporal, PlanificacionRequest request,
            List<PedidoSemanal> pedidos, long inicio) {

        PlanificacionResponse response = new PlanificacionResponse();

        // Crear metadata
        PlanificacionResponse.MetadataPlanificacion metadata = new PlanificacionResponse.MetadataPlanificacion();
        LocalDateTime fechaInicio = LocalDateTime.of(request.getFecha(), LocalTime.MIDNIGHT);
        LocalDateTime fechaFin = fechaInicio.plusMinutes(request.calcularRangoConsumoMinutos());

        metadata.setFechaInicio(fechaInicio);
        metadata.setFechaFin(fechaFin);
        metadata.setRangoDescripcion(String.format("Pedidos entre %s y %s",
                fechaInicio, fechaFin));
        metadata.setFactorK(request.getFactorK());
        metadata.setSaltoConsumoMinutos(request.calcularRangoConsumoMinutos());
        metadata.setSaltoAlgoritmoMinutos(request.getParametrosGenetico().getSaltoAlgoritmoMinutos());
        metadata.setPedidosProcesados(solucion.getTotalPedidos());
        metadata.setPedidosATiempo(solucion.getPedidosATiempo());
        metadata.setPedidosTarde(solucion.getPedidosTarde());
        metadata.setPedidosNoEntregados(solucion.getPedidosNoEntregados());
        metadata.setObjetivo(solucion.getObjetivo());
        metadata.setTiempoEjecucionMs(System.currentTimeMillis() - inicio);
        metadata.setGeneracionesEjecutadas(0); // TODO: actualizar cuando implementemos el GA

        response.setMetadata(metadata);

        // Convertir pedidos a resumen para verificacion
        response.setPedidosProcesados(convertirPedidosAResumen(pedidos));

        // Convertir aeropuertos
        response.setAeropuertos(convertirAeropuertos(worldTemporal));

        // Convertir vuelos y rutas desde la solucion
        response.setVuelos(convertirVuelos(solucion, worldTemporal));
        response.setRutas(convertirRutas(solucion, worldTemporal));

        return response;
    }

    /**
     * Convierte los vuelos de la solucion a DTOs
     * Agrupa los pedidos por vuelo
     *
     * @param solucion Solucion con rutas
     * @param worldTemporal WorldTemporal con datos de aeropuertos
     * @return Lista de DTOs de vuelos
     */
    private List<VueloEnRutaDTO> convertirVuelos(Solution solucion, WorldTemporal worldTemporal) {
        // Mapa: vueloId -> DTO del vuelo
        Map<String, VueloEnRutaDTO> vuelosMap = new HashMap<>();

        // Recorrer todas las rutas para extraer los vuelos y agrupar pedidos
        for (Map.Entry<PedidoSemanal, List<SubRuta>> entry : solucion.getRutas().entrySet()) {
            PedidoSemanal pedido = entry.getKey();
            String pedidoId = "Ped" + pedido.getId();  // ID del pedido: Ped123

            for (SubRuta subruta : entry.getValue()) {
                for (VueloUso vueloUso : subruta.getVuelos()) {
                    String vueloId = vueloUso.generarId();

                    // Si el vuelo ya existe, agregar el pedido a su lista
                    if (vuelosMap.containsKey(vueloId)) {
                        VueloEnRutaDTO vueloDTO = vuelosMap.get(vueloId);
                        vueloDTO.getOrders().add(new VueloEnRutaDTO.OrdenVuelo(
                                pedidoId,
                                vueloUso.getCantidadAsignada()
                        ));
                    } else {
                        // Crear nuevo DTO de vuelo
                        VueloEnRutaDTO dto = new VueloEnRutaDTO();
                        dto.setId(vueloId);
                        dto.setOriginCode(vueloUso.getOrigen());
                        dto.setDestinationCode(vueloUso.getDestino());

                        // Obtener fechas UTC reales desde VueloInstancia
                        VueloInstancia instancia = worldTemporal.getVuelo(vueloId);
                        if (instancia != null) {
                            dto.setSalida(instancia.getSalidaUTC());
                            dto.setLlegada(instancia.getLlegadaUTC());
                        } else {
                            // Fallback (no debería ocurrir)
                            log.warn("VueloInstancia no encontrada para ID: {}", vueloId);
                            dto.setSalida(LocalDateTime.now());
                            dto.setLlegada(LocalDateTime.now().plusHours(2));
                        }

                        dto.setCapacidad(vueloUso.getCapacidadMaxima());
                        dto.setAltitude(35000);
                        dto.setSpeed(500);

                        // Agregar primer pedido
                        dto.getOrders().add(new VueloEnRutaDTO.OrdenVuelo(
                                pedidoId,
                                vueloUso.getCantidadAsignada()
                        ));

                        // Obtener coordenadas de los aeropuertos
                        Aeropuerto origen = worldTemporal.getAeropuerto(vueloUso.getOrigen());
                        Aeropuerto destino = worldTemporal.getAeropuerto(vueloUso.getDestino());

                        if (origen != null && destino != null) {
                            dto.setRegionOrigin(origen.getContinente());
                            dto.setRegionDestination(destino.getContinente());

                            VueloEnRutaDTO.RutaGeografica ruta = new VueloEnRutaDTO.RutaGeografica();
                            VueloEnRutaDTO.RutaGeografica.Coordenadas coordOrigen =
                                    new VueloEnRutaDTO.RutaGeografica.Coordenadas(
                                            origen.getLatitud(), origen.getLongitud());
                            VueloEnRutaDTO.RutaGeografica.Coordenadas coordDestino =
                                    new VueloEnRutaDTO.RutaGeografica.Coordenadas(
                                            destino.getLatitud(), destino.getLongitud());

                            ruta.setOrigin(coordOrigen);
                            ruta.setDestination(coordDestino);
                            dto.setRuta(ruta);
                        }

                        vuelosMap.put(vueloId, dto);
                    }
                }
            }
        }

        List<VueloEnRutaDTO> vuelos = new ArrayList<>(vuelosMap.values());
        log.debug("Convertidos {} vuelos unicos con pedidos agrupados", vuelos.size());

        return vuelos;
    }

    /**
     * Convierte las rutas de la solucion a DTOs
     *
     * @param solucion Solucion con rutas
     * @param worldTemporal WorldTemporal con datos
     * @return Lista de DTOs de rutas
     */
    private List<RutaPlanificadaDTO> convertirRutas(Solution solucion, WorldTemporal worldTemporal) {
        List<RutaPlanificadaDTO> rutas = new ArrayList<>();

        for (Map.Entry<PedidoSemanal, List<SubRuta>> entry : solucion.getRutas().entrySet()) {
            PedidoSemanal pedido = entry.getKey();
            List<SubRuta> subrutas = entry.getValue();

            RutaPlanificadaDTO dto = new RutaPlanificadaDTO();
            dto.setPedidoId(pedido.getId());
            dto.setClienteId(pedido.getClienteId());
            dto.setDestino(pedido.getAeropuertoDestinoId());
            dto.setCantidad(pedido.getCantidadProductos());

            // Estado por defecto (TODO: calcular real)
            dto.setEstado(RutaPlanificadaDTO.EstadoPedido.EN_PROCESO);

            // Fechas reales del pedido
            LocalDateTime fechaPedido = LocalDateTime.of(
                pedido.getAnio(), pedido.getMes(), pedido.getDia(),
                pedido.getHora(), pedido.getMinuto()
            );
            dto.setFechaPedido(fechaPedido);
            
            // Calcular fecha límite según el plazo del pedido
            // Si hay continente origen/destino, usar plazo real, sino usar 2 días por defecto
            LocalDateTime fechaLimite = fechaPedido.plusDays(2);
            dto.setFechaLimite(fechaLimite);

            // Convertir subrutas
            List<RutaPlanificadaDTO.SubrutaDTO> subrutasDTO = new ArrayList<>();
            for (SubRuta subruta : subrutas) {
                RutaPlanificadaDTO.SubrutaDTO subrutaDTO = new RutaPlanificadaDTO.SubrutaDTO();
                subrutaDTO.setHub(subruta.getHubOrigen());
                subrutaDTO.setCantidad(subruta.getCantidad());

                // Calcular fecha de llegada real desde el último vuelo
                LocalDateTime llegadaReal = calcularLlegadaReal(subruta, worldTemporal);
                subrutaDTO.setLlegada(llegadaReal);

                // IDs de vuelos
                List<String> vuelosIds = new ArrayList<>();
                List<String> escalas = new ArrayList<>();

                for (int i = 0; i < subruta.getVuelos().size(); i++) {
                    VueloUso vueloUso = subruta.getVuelos().get(i);
                    vuelosIds.add(vueloUso.generarId());

                    // Las escalas son los destinos de los vuelos intermedios
                    if (i < subruta.getVuelos().size() - 1) {
                        escalas.add(vueloUso.getDestino());
                    }
                }

                subrutaDTO.setVuelos(vuelosIds);
                subrutaDTO.setEscalas(escalas);

                subrutasDTO.add(subrutaDTO);
            }

            dto.setSubrutas(subrutasDTO);
            rutas.add(dto);
        }

        log.info("Convertidas {} rutas", rutas.size());
        return rutas;
    }

    /**
     * Calcula la fecha/hora de llegada real del último vuelo de una subruta
     *
     * @param subruta Subruta con vuelos
     * @param worldTemporal WorldTemporal con vuelos expandidos
     * @return Fecha/hora de llegada real o fecha actual si no hay vuelos
     */
    private LocalDateTime calcularLlegadaReal(SubRuta subruta, WorldTemporal worldTemporal) {
        if (subruta.getVuelos().isEmpty()) {
            return LocalDateTime.now();
        }

        // Obtener el último vuelo de la subruta
        VueloUso ultimoVuelo = subruta.getVuelos().get(subruta.getVuelos().size() - 1);
        
        // Buscar la instancia del vuelo usando el ID completo
        String vueloId = ultimoVuelo.getIdCompleto();
        if (vueloId == null || vueloId.isEmpty()) {
            // Fallback: generar ID y buscar
            vueloId = ultimoVuelo.generarId();
        }
        
        VueloInstancia instancia = worldTemporal.getVuelo(vueloId);
        if (instancia != null) {
            return instancia.getLlegadaUTC();
        }

        // Si no se encuentra, retornar fecha actual (no debería pasar)
        log.warn("No se pudo encontrar vuelo con ID: {}", vueloId);
        return LocalDateTime.now();
    }

    /**
     * Convierte pedidos a DTOs de resumen
     *
     * @param pedidos Lista de pedidos
     * @return Lista de DTOs
     */
    private List<PlanificacionResponse.PedidoResumenDTO> convertirPedidosAResumen(List<PedidoSemanal> pedidos) {
        List<PlanificacionResponse.PedidoResumenDTO> resumen = new ArrayList<>();

        for (PedidoSemanal pedido : pedidos) {
            PlanificacionResponse.PedidoResumenDTO dto = new PlanificacionResponse.PedidoResumenDTO();
            dto.setId(pedido.getId());

            // Formatear fecha: yyyy-MM-dd HH:mm
            String fecha = String.format("%04d-%02d-%02d %02d:%02d",
                    pedido.getAnio(), pedido.getMes(), pedido.getDia(),
                    pedido.getHora(), pedido.getMinuto());
            dto.setFecha(fecha);

            dto.setDestino(pedido.getAeropuertoDestinoId());
            dto.setCantidad(pedido.getCantidadProductos());
            dto.setClienteId(pedido.getClienteId());
            // 🆕 ESTADO EN RAM: Ya no se usa el estado de BD
            dto.setEstado("PENDIENTE"); // Por defecto, el estado real está en SessionStateManager

            resumen.add(dto);
        }

        return resumen;
    }

    /**
     * Convierte aeropuertos a DTOs
     *
     * @param worldTemporal WorldTemporal con datos
     * @return Lista de DTOs de aeropuertos
     */
    private List<AeropuertoEstadoDTO> convertirAeropuertos(WorldTemporal worldTemporal) {
        List<AeropuertoEstadoDTO> aeropuertos = new ArrayList<>();

        for (Aeropuerto aeropuerto : worldTemporal.getWorldBase().getAeropuertos().values()) {
            AeropuertoEstadoDTO dto = new AeropuertoEstadoDTO();
            dto.setCode(aeropuerto.getCodigoICAO());
            dto.setLat(aeropuerto.getLatitud());
            dto.setLng(aeropuerto.getLongitud());
            dto.setName(aeropuerto.getCiudad() + " - " + aeropuerto.getPais());
            dto.setRegion(aeropuerto.getContinente());
            dto.setCountry(aeropuerto.getPais());
            dto.setSede(aeropuerto.esSedePrincipal());
            dto.setCapacity(aeropuerto.tieneStockIlimitado() ? "ILIMITADO" : String.valueOf(aeropuerto.getCapacidadAlmacen()));
            dto.setPackages(0); // TODO: calcular ocupacion real

            aeropuertos.add(dto);
        }

        return aeropuertos;
    }

    /**
     * Convierte la solucion a formato simplificado con solo vuelos y pedidos
     * Formato de fechas: yyyy-MM-dd HH:mm
     *
     * @param solucion Solucion con rutas planificadas
     * @param worldTemporal WorldTemporal con datos temporales
     * @return Response simplificado con lista de vuelos
     */
    public PlanificacionResponseSimple convertirAResponseSimple(Solution solucion, WorldTemporal worldTemporal) {
        // Mapa: vueloId -> DTO del vuelo simplificado
        Map<String, VueloSimplificadoDTO> vuelosMap = new HashMap<>();

        // Recorrer todas las rutas para extraer los vuelos y agrupar pedidos
        for (Map.Entry<PedidoSemanal, List<SubRuta>> entry : solucion.getRutas().entrySet()) {
            PedidoSemanal pedido = entry.getKey();
            Long pedidoId = pedido.getId();

            for (SubRuta subruta : entry.getValue()) {
                for (VueloUso vueloUso : subruta.getVuelos()) {
                    String vueloId = vueloUso.generarId();

                    // Si el vuelo ya existe, agregar el pedido a su lista
                    if (vuelosMap.containsKey(vueloId)) {
                        VueloSimplificadoDTO vueloDTO = vuelosMap.get(vueloId);
                        vueloDTO.agregarPedido(pedidoId, vueloUso.getCantidadAsignada());
                        // Actualizar cantidad total
                        Integer cantidadActual = vueloDTO.getQuantity() != null ? vueloDTO.getQuantity() : 0;
                        vueloDTO.setQuantity(cantidadActual + vueloUso.getCantidadAsignada());
                    } else {
                        // Crear nuevo DTO de vuelo simplificado
                        VueloSimplificadoDTO dto = new VueloSimplificadoDTO();

                        // Establecer codigos ICAO
                        dto.setOrigenCodigoICAO(vueloUso.getOrigen());
                        dto.setDestinoCodigoICAO(vueloUso.getDestino());

                        // Obtener fechas UTC reales desde VueloInstancia y formatearlas
                        VueloInstancia instancia = worldTemporal.getVuelo(vueloId);
                        if (instancia != null) {
                            LocalDateTime salidaUTC = instancia.getSalidaUTC();
                            LocalDateTime llegadaUTC = instancia.getLlegadaUTC();
                            
                            // Formato: yyyy-MM-dd HH:mm (para compatibilidad)
                            dto.setFechaInicial(formatearFecha(salidaUTC));
                            dto.setFechaFinal(formatearFecha(llegadaUTC));
                            
                            // 🆕 CAMPOS PARA FRONTEND:
                            // 1. VueloId: ID único completo (incluye fecha) - USAR ESTE PARA IDENTIFICAR VUELOS
                            dto.setVueloId(vueloId);
                            
                            // 2. FlightId: {ORIGEN}-{DESTINO}-{HORA} (versión corta, puede repetirse entre días)
                            String hora = String.format("%02d%02d", salidaUTC.getHour(), salidaUTC.getMinute());
                            dto.setFlightId(vueloUso.getOrigen() + "-" + vueloUso.getDestino() + "-" + hora);
                            
                            // 2. DepartureUtc y ArrivalUtc en formato ISO-8601 con 'Z'
                            dto.setDepartureUtc(formatearFechaUTC(salidaUTC));
                            dto.setArrivalUtc(formatearFechaUTC(llegadaUTC));
                            
                            // 3. Quantity (se actualizará con el total de paquetes)
                            dto.setQuantity(vueloUso.getCantidadAsignada());
                            
                            // 4. SlackMinutes (calcular holgura vs deadline del pedido)
                            // Por ahora lo dejamos en 0, se calculará después con todos los pedidos
                            dto.setSlackMinutes(0);
                            
                            // 5. Capacidad máxima del avión
                            dto.setCapacidadMaxima(vueloUso.getCapacidadMaxima());
                            
                        } else {
                            // Fallback (no debería ocurrir)
                            log.warn("VueloInstancia no encontrada para ID: {}", vueloId);
                            LocalDateTime ahora = LocalDateTime.now();
                            dto.setFechaInicial(formatearFecha(ahora));
                            dto.setFechaFinal(formatearFecha(ahora.plusHours(2)));
                            dto.setVueloId(vueloId); // Usar el ID que ya tenemos
                            dto.setDepartureUtc(formatearFechaUTC(ahora));
                            dto.setArrivalUtc(formatearFechaUTC(ahora.plusHours(2)));
                            dto.setFlightId(vueloUso.getOrigen() + "-" + vueloUso.getDestino() + "-0000");
                            dto.setQuantity(vueloUso.getCantidadAsignada());
                            dto.setSlackMinutes(0);
                            dto.setCapacidadMaxima(vueloUso.getCapacidadMaxima());
                        }

                        // Agregar primer pedido
                        dto.agregarPedido(pedidoId, vueloUso.getCantidadAsignada());

                        vuelosMap.put(vueloId, dto);
                    }
                }
            }
        }

        // 🆕 Calcular slackMinutes para cada vuelo (holgura vs deadline más ajustado)
        for (Map.Entry<PedidoSemanal, List<SubRuta>> entry : solucion.getRutas().entrySet()) {
            PedidoSemanal pedido = entry.getKey();
            
            // Calcular deadline del pedido
            LocalDateTime deadlinePedido = LocalDateTime.of(
                pedido.getAnio(), 
                pedido.getMes(), 
                pedido.getDia(), 
                pedido.getHora(), 
                pedido.getMinuto()
            );
            
            // Para cada subruta del pedido, encontrar el último vuelo (llegada final)
            for (SubRuta subruta : entry.getValue()) {
                if (subruta.getVuelos().isEmpty()) continue;
                
                // El último vuelo de la subruta determina cuándo llega el pedido
                VueloUso ultimoVuelo = subruta.getVuelos().get(subruta.getVuelos().size() - 1);
                String ultimoVueloId = ultimoVuelo.generarId();
                
                VueloInstancia instanciaUltimo = worldTemporal.getVuelo(ultimoVueloId);
                if (instanciaUltimo != null) {
                    LocalDateTime llegadaFinal = instanciaUltimo.getLlegadaUTC();
                    
                    // Calcular holgura: minutos entre llegada y deadline
                    long minutosHolgura = java.time.Duration.between(llegadaFinal, deadlinePedido).toMinutes();
                    
                    // Actualizar el slackMinutes del último vuelo con la holgura más crítica
                    VueloSimplificadoDTO vueloDTO = vuelosMap.get(ultimoVueloId);
                    if (vueloDTO != null) {
                        // Si ya tiene un slack calculado, tomar el menor (más crítico)
                        if (vueloDTO.getSlackMinutes() == null || vueloDTO.getSlackMinutes() == 0) {
                            vueloDTO.setSlackMinutes((int) minutosHolgura);
                        } else {
                            vueloDTO.setSlackMinutes(Math.min(vueloDTO.getSlackMinutes(), (int) minutosHolgura));
                        }
                    }
                }
            }
        }
        
        // Convertir el mapa a lista y crear el response
        List<VueloSimplificadoDTO> vuelos = new ArrayList<>(vuelosMap.values());
        log.debug("Convertidos {} vuelos unicos en formato simplificado con slackMinutes", vuelos.size());

        return PlanificacionResponseSimple.conVuelos(vuelos);
    }

    /**
     * Formatea una fecha a string en formato yyyy-MM-dd HH:mm
     *
     * @param fecha Fecha a formatear
     * @return String formateado
     */
    private String formatearFecha(LocalDateTime fecha) {
        return String.format("%04d-%02d-%02d %02d:%02d",
                fecha.getYear(),
                fecha.getMonthValue(),
                fecha.getDayOfMonth(),
                fecha.getHour(),
                fecha.getMinute());
    }
    
    /**
     * Formatea una fecha a string en formato ISO-8601 UTC
     * Formato: yyyy-MM-ddTHH:mm:ssZ
     * Ejemplo: "2025-01-15T13:00:00Z"
     *
     * @param fecha Fecha a formatear (asumida como UTC)
     * @return String formateado en ISO-8601 con 'Z'
     */
    private String formatearFechaUTC(LocalDateTime fecha) {
        return String.format("%04d-%02d-%02dT%02d:%02d:%02dZ",
                fecha.getYear(),
                fecha.getMonthValue(),
                fecha.getDayOfMonth(),
                fecha.getHour(),
                fecha.getMinute(),
                fecha.getSecond());
    }

    // ============================================================================
    // WEBSOCKET: Planificación con progreso en tiempo real
    // ============================================================================

    /**
     * Estado de ejecución por sesión (para pausar/cancelar)
     */
    private static class EstadoEjecucion {
        private volatile boolean pausado = false;
        private volatile boolean cancelado = false;
        private long inicioMs = System.currentTimeMillis();

        public boolean isPausado() { return pausado; }
        public boolean isCancelado() { return cancelado; }
        public long getInicioMs() { return inicioMs; }
        public void setPausado(boolean pausado) { this.pausado = pausado; }
        public void setCancelado(boolean cancelado) { this.cancelado = cancelado; }
    }

    // Control de sesiones WebSocket
    private final Map<String, EstadoEjecucion> sesionesActivas = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Cache de WorldTemporal por sesión (para evitar recrear en cada iteración)
    private final Map<String, WorldTemporal> worldTemporalCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Cache de ControladorAlmacenes por sesión
    private final Map<String, ControladorAlmacenes> controladorAlmacenesCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Planifica con progreso en tiempo real vía WebSocket
     *
     * @param sessionId ID de sesión WebSocket
     * @param tiempoActualSimulacion Tiempo actual de la simulación
     * @param factorK Factor de expansión temporal
     * @param callbackProgreso Callback para enviar progreso
     */
    public void planificarConProgresoWS(
            String sessionId,
            LocalDateTime tiempoActualSimulacion,
            int factorK,
            java.util.function.Consumer<ProgresoAGDTO> callbackProgreso) {
        planificarConProgresoWS(sessionId, tiempoActualSimulacion, factorK, null, null, null, callbackProgreso);
    }
    
    public void planificarConProgresoWS(
            String sessionId,
            LocalDateTime tiempoActualSimulacion,
            int factorK,
            Integer tamanioPoblacion,
            Integer maxGeneraciones,
            Integer limiteGeneracionesSinMejora,
            java.util.function.Consumer<ProgresoAGDTO> callbackProgreso) {

        EstadoEjecucion estado = new EstadoEjecucion();
        sesionesActivas.put(sessionId, estado);

        // Configurar parámetros del AG (usar valores por defecto si no se especifican)
        this.TAMANIO_POBLACION = (tamanioPoblacion != null) ? tamanioPoblacion : TAMANIO_POBLACION_DEFAULT;
        this.MAX_GENERACIONES = (maxGeneraciones != null) ? maxGeneraciones : MAX_GENERACIONES_DEFAULT;
        this.NO_MEJORA_LIMITE = (limiteGeneracionesSinMejora != null) ? limiteGeneracionesSinMejora : NO_MEJORA_LIMITE_DEFAULT;

        try {
            long inicioIteracion = System.currentTimeMillis();
            log.info("🚀 ═══════════════════════════════════════════════════════════════");
            log.info("🚀 [ITERACIÓN START] sessionId={}, tiempoSim={}", sessionId, tiempoActualSimulacion);
            log.info("⚙️  Parámetros AG: población={}, maxGen={}, límiteSinMejora={}", 
                     TAMANIO_POBLACION, MAX_GENERACIONES, NO_MEJORA_LIMITE);

            // 1. Cargar datos
            callbackProgreso.accept(ProgresoAGDTO.builder()
                    .tipo("PROGRESO_AG")
                    .generacion(0)
                    .maxGeneraciones(MAX_GENERACIONES)
                    .progreso(0.0)
                    .mejorFitness(0.0)
                    .fitnessPromedio(0.0)
                    .pedidosProcesados(0)
                    .pedidosTotales(0)
                    .timestamp(LocalDateTime.now())
                    .build());

            // ⏱️ TIMING: Obtener World
            long t1 = System.currentTimeMillis();
            World world = worldCacheService.getWorld();
            log.info("⏱️ [TIMING] Obtener World (cache): {}ms", System.currentTimeMillis() - t1);
            
            LocalDate fecha = tiempoActualSimulacion.toLocalDate();
            
            // Crear request con la hora de la simulación
            PlanificacionRequest tempRequest = new PlanificacionRequest();
            tempRequest.setFecha(fecha);
            tempRequest.setStartTime(tiempoActualSimulacion.toLocalTime());
            tempRequest.setFactorK(factorK);
            
            // ⏱️ TIMING: Cargar pedidos (incluye query BD)
            long t2 = System.currentTimeMillis();
            List<PedidoSemanal> pedidos = cargarPedidosEnRango(tempRequest, tiempoActualSimulacion);
            log.info("⏱️ [TIMING] Cargar Pedidos: {} pedidos en {}ms | Ventana: {} → {}", 
                    pedidos.size(), System.currentTimeMillis() - t2,
                    tiempoActualSimulacion.toLocalTime(),
                    tiempoActualSimulacion.plusMinutes(factorK * 5).toLocalTime());
            
            // Si no hay pedidos, saltar esta iteración
            if (pedidos.isEmpty()) {
                log.info("⏸️ Sin pedidos en esta ventana, saltando...");
                return;
            }
            
            int numeroDias = calcularHorizonteDias(tempRequest, pedidos);

            // ⏱️ TIMING: Obtener WorldTemporal y ControladorAlmacenes (con cache por sesión)
            long t3 = System.currentTimeMillis();
            LocalDateTime fechaBaseUTC = tiempoActualSimulacion;
            
            // 🚀 OPTIMIZACIÓN: Cachear WorldTemporal por sesión
            // Solo se crea en la primera iteración, luego se reutiliza
            WorldTemporal worldTemporal = worldTemporalCache.get(sessionId);
            ControladorAlmacenes controladorAlmacenes = controladorAlmacenesCache.get(sessionId);
            
            if (worldTemporal == null) {
                // Primera iteración: crear WorldTemporal
                worldTemporal = new WorldTemporal(world, fechaBaseUTC, numeroDias);
                worldTemporalCache.put(sessionId, worldTemporal);
                log.info("⏱️ [TIMING] Crear WorldTemporal (NUEVO): {}ms | {}", 
                        System.currentTimeMillis() - t3, worldTemporal.getEstadisticas());
            } else {
                log.info("⏱️ [TIMING] WorldTemporal (CACHE HIT): 0ms | {}", worldTemporal.getEstadisticas());
            }
            
            // ⏱️ TIMING: Crear ControladorAlmacenes
            long t4 = System.currentTimeMillis();
            if (controladorAlmacenes == null) {
                // Primera iteración: crear ControladorAlmacenes
                controladorAlmacenes = new ControladorAlmacenes(numeroDias, fechaBaseUTC);
                for (Aeropuerto aeropuerto : world.getAeropuertos().values()) {
                    int capacidad = aeropuerto.tieneStockIlimitado() ? 0 : aeropuerto.getCapacidadAlmacen();
                    controladorAlmacenes.registrarAeropuerto(aeropuerto.getCodigoICAO(), capacidad);
                }
                controladorAlmacenesCache.put(sessionId, controladorAlmacenes);
                log.info("⏱️ [TIMING] Crear ControladorAlmacenes (NUEVO): {}ms", System.currentTimeMillis() - t4);
            } else {
                log.info("⏱️ [TIMING] ControladorAlmacenes (CACHE HIT): 0ms");
            }

            // 2. Ejecutar AG con progreso
            Solution solucion = ejecutarAlgoritmoGeneticoConProgreso(worldTemporal, controladorAlmacenes, pedidos, estado, callbackProgreso);
            
            int pedidosAsignados = (solucion != null && solucion.getRutas() != null) 
                ? (int) solucion.getRutas().values().stream().filter(r -> !r.isEmpty()).count()
                : 0;
            int pedidosSinRuta = pedidos.size() - pedidosAsignados;

            long duracionTotal = System.currentTimeMillis() - inicioIteracion;
            log.info("✅ [ITERACIÓN END] Pedidos: {} cargados → {} con ruta ({} sin ruta) | {}ms", 
                    pedidos.size(), pedidosAsignados, pedidosSinRuta, duracionTotal);
            
            if (pedidosSinRuta > 0 && pedidos.size() > 0) {
                double porcentajeExito = (pedidosAsignados * 100.0) / pedidos.size();
                log.warn("⚠️ Tasa de éxito: {:.1f}% - {} pedidos sin ruta", porcentajeExito, pedidosSinRuta);
            }
            log.info("🚀 ═══════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("❌ Error en planificación WS", e);
            throw new RuntimeException("Error en planificación: " + e.getMessage(), e);
        }
        // Nota: NO limpiar sesionesActivas aquí - se hace en limpiarCacheSesion() al final de la simulación
    }
    
    /**
     * Limpia los caches de una sesión de simulación
     * DEBE llamarse desde SimulationService cuando la simulación termina (complete/cancel/error)
     *
     * @param sessionId ID de la sesión
     */
    public void limpiarCacheSesion(String sessionId) {
        sesionesActivas.remove(sessionId);
        WorldTemporal wt = worldTemporalCache.remove(sessionId);
        ControladorAlmacenes ca = controladorAlmacenesCache.remove(sessionId);
        log.info("🧹 Cache limpiado para sesión {} | WorldTemporal={}, ControladorAlmacenes={}", 
                sessionId, wt != null, ca != null);
    }

    /**
     * Convierte PedidoDiario a PedidoSemanal para reutilizar la lógica del AG.
     * Ambas entidades tienen la misma estructura, solo diferente nombre de tabla.
     */
    private PedidoSemanal convertirDiarioASemanal(PedidoDiario diario) {
        PedidoSemanal semanal = new PedidoSemanal(
            diario.getAnio(),
            diario.getMes(),
            diario.getDia(),
            diario.getHora(),
            diario.getMinuto(),
            diario.getAeropuertoDestinoId(),
            diario.getCantidadProductos(),
            diario.getClienteId()
        );
        semanal.setId(diario.getId()); // Mantener el ID original
        return semanal;
    }

    /**
     * Planifica pedidos diarios con progreso en tiempo real via WebSocket.
     * Esta versión recibe directamente la lista de pedidos diarios y los convierte internamente.
     *
     * @param sessionId ID de la sesión WebSocket
     * @param pedidosDiarios Lista de pedidos diarios a planificar
     * @param tiempoActualSimulacion Tiempo actual de la simulación
     * @param factorK Factor K para el algoritmo
     * @param tamanioPoblacion Tamaño de la población del AG (opcional)
     * @param maxGeneraciones Máximo de generaciones del AG (opcional)
     * @param limiteGeneracionesSinMejora Límite de generaciones sin mejora (opcional)
     * @param callbackProgreso Callback para enviar progreso
     */
    public void planificarDiarioConProgresoWS(
            String sessionId,
            List<PedidoDiario> pedidosDiarios,
            LocalDateTime tiempoActualSimulacion,
            int factorK,
            Integer tamanioPoblacion,
            Integer maxGeneraciones,
            Integer limiteGeneracionesSinMejora,
            java.util.function.Consumer<ProgresoAGDTO> callbackProgreso) {

        // Convertir PedidoDiario a PedidoSemanal para reutilizar la lógica existente
        List<PedidoSemanal> pedidos = pedidosDiarios.stream()
                .map(this::convertirDiarioASemanal)
                .toList();

        log.info("📦 [DIARIO] Convertidos {} pedidos diarios a formato semanal para procesamiento", pedidos.size());

        // Reutilizar la lógica existente pero con los pedidos ya cargados
        planificarConPedidosDirectosWS(sessionId, pedidos, tiempoActualSimulacion, factorK,
                tamanioPoblacion, maxGeneraciones, limiteGeneracionesSinMejora, callbackProgreso);
    }

    /**
     * Planifica con una lista de pedidos ya cargados (sin consultar BD).
     * Usado para operación diaria y otros casos donde los pedidos ya están en memoria.
     */
    private void planificarConPedidosDirectosWS(
            String sessionId,
            List<PedidoSemanal> pedidos,
            LocalDateTime tiempoActualSimulacion,
            int factorK,
            Integer tamanioPoblacion,
            Integer maxGeneraciones,
            Integer limiteGeneracionesSinMejora,
            java.util.function.Consumer<ProgresoAGDTO> callbackProgreso) {

        EstadoEjecucion estado = new EstadoEjecucion();
        sesionesActivas.put(sessionId, estado);

        // Configurar parámetros del AG
        this.TAMANIO_POBLACION = (tamanioPoblacion != null) ? tamanioPoblacion : TAMANIO_POBLACION_DEFAULT;
        this.MAX_GENERACIONES = (maxGeneraciones != null) ? maxGeneraciones : MAX_GENERACIONES_DEFAULT;
        this.NO_MEJORA_LIMITE = (limiteGeneracionesSinMejora != null) ? limiteGeneracionesSinMejora : NO_MEJORA_LIMITE_DEFAULT;

        try {
            long inicioIteracion = System.currentTimeMillis();
            log.info("🚀 ═══════════════════════════════════════════════════════════════");
            log.info("🚀 [DIARIO ITERACIÓN START] sessionId={}, tiempoSim={}", sessionId, tiempoActualSimulacion);
            log.info("⚙️  Parámetros AG: población={}, maxGen={}, límiteSinMejora={}", 
                     TAMANIO_POBLACION, MAX_GENERACIONES, NO_MEJORA_LIMITE);

            // 1. Enviar progreso inicial
            callbackProgreso.accept(ProgresoAGDTO.builder()
                    .tipo("PROGRESO_AG")
                    .generacion(0)
                    .maxGeneraciones(MAX_GENERACIONES)
                    .progreso(0.0)
                    .mejorFitness(0.0)
                    .fitnessPromedio(0.0)
                    .pedidosProcesados(0)
                    .pedidosTotales(pedidos.size())
                    .timestamp(LocalDateTime.now())
                    .build());

            // Obtener World desde cache
            long t1 = System.currentTimeMillis();
            World world = worldCacheService.getWorld();
            log.info("⏱️ [TIMING] Obtener World (cache): {}ms", System.currentTimeMillis() - t1);
            
            // Si no hay pedidos, terminar
            if (pedidos.isEmpty()) {
                log.info("⏸️ Sin pedidos para procesar");
                return;
            }

            // Calcular horizonte de días basado en los pedidos
            int numeroDias = calcularHorizonteDiasDesdePedidos(pedidos);
            log.info("📅 Horizonte de días calculado: {}", numeroDias);

            // Crear WorldTemporal y ControladorAlmacenes
            long t3 = System.currentTimeMillis();
            LocalDateTime fechaBaseUTC = tiempoActualSimulacion;
            
            WorldTemporal worldTemporal = worldTemporalCache.get(sessionId);
            ControladorAlmacenes controladorAlmacenes = controladorAlmacenesCache.get(sessionId);
            
            if (worldTemporal == null) {
                worldTemporal = new WorldTemporal(world, fechaBaseUTC, numeroDias);
                worldTemporalCache.put(sessionId, worldTemporal);
                log.info("⏱️ [TIMING] Crear WorldTemporal (NUEVO): {}ms | {}", 
                        System.currentTimeMillis() - t3, worldTemporal.getEstadisticas());
            } else {
                log.info("⏱️ [TIMING] WorldTemporal (CACHE HIT): 0ms | {}", worldTemporal.getEstadisticas());
            }
            
            long t4 = System.currentTimeMillis();
            if (controladorAlmacenes == null) {
                controladorAlmacenes = new ControladorAlmacenes(numeroDias, fechaBaseUTC);
                for (Aeropuerto aeropuerto : world.getAeropuertos().values()) {
                    int capacidad = aeropuerto.tieneStockIlimitado() ? 0 : aeropuerto.getCapacidadAlmacen();
                    controladorAlmacenes.registrarAeropuerto(aeropuerto.getCodigoICAO(), capacidad);
                }
                controladorAlmacenesCache.put(sessionId, controladorAlmacenes);
                log.info("⏱️ [TIMING] Crear ControladorAlmacenes (NUEVO): {}ms", System.currentTimeMillis() - t4);
            } else {
                log.info("⏱️ [TIMING] ControladorAlmacenes (CACHE HIT): 0ms");
            }

            // 2. Ejecutar AG con progreso
            Solution solucion = ejecutarAlgoritmoGeneticoConProgreso(worldTemporal, controladorAlmacenes, pedidos, estado, callbackProgreso);
            
            int pedidosAsignados = (solucion != null && solucion.getRutas() != null) 
                ? (int) solucion.getRutas().values().stream().filter(r -> !r.isEmpty()).count()
                : 0;
            int pedidosSinRuta = pedidos.size() - pedidosAsignados;

            long duracionTotal = System.currentTimeMillis() - inicioIteracion;
            log.info("✅ [DIARIO ITERACIÓN END] Pedidos: {} cargados → {} con ruta ({} sin ruta) | {}ms", 
                    pedidos.size(), pedidosAsignados, pedidosSinRuta, duracionTotal);
            
            if (pedidosSinRuta > 0 && pedidos.size() > 0) {
                double porcentajeExito = (pedidosAsignados * 100.0) / pedidos.size();
                log.warn("⚠️ Tasa de éxito: {:.1f}% - {} pedidos sin ruta", porcentajeExito, pedidosSinRuta);
            }
            log.info("🚀 ═══════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("❌ Error en planificación diaria WS", e);
            throw new RuntimeException("Error en planificación diaria: " + e.getMessage(), e);
        }
    }

    /**
     * Calcula el horizonte de días basado en los pedidos cargados
     */
    private int calcularHorizonteDiasDesdePedidos(List<PedidoSemanal> pedidos) {
        if (pedidos.isEmpty()) {
            return 1;
        }
        
        // Encontrar el rango de días en los pedidos
        int minDia = pedidos.stream().mapToInt(PedidoSemanal::getDia).min().orElse(1);
        int maxDia = pedidos.stream().mapToInt(PedidoSemanal::getDia).max().orElse(1);
        
        // Agregar margen para entregas (3 días adicionales para entregas intercontinentales)
        int horizonte = (maxDia - minDia) + 1 + 3;
        
        return Math.max(horizonte, 7); // Mínimo 7 días para permitir entregas
    }

    /**
     * DTO interno para el resultado de planificación diaria síncrona
     */
    public static class ResultadoPlanificacionDiaria {
        public final Solution solucion;
        public final int pedidosAsignados;
        public final int pedidosSinRuta;
        public final double mejorFitness;
        public final long tiempoProcesamiento;
        
        public ResultadoPlanificacionDiaria(Solution solucion, int pedidosAsignados, 
                int pedidosSinRuta, double mejorFitness, long tiempoProcesamiento) {
            this.solucion = solucion;
            this.pedidosAsignados = pedidosAsignados;
            this.pedidosSinRuta = pedidosSinRuta;
            this.mejorFitness = mejorFitness;
            this.tiempoProcesamiento = tiempoProcesamiento;
        }
    }

    /**
     * Planifica pedidos diarios de forma SÍNCRONA (sin WebSocket).
     * Procesa todos los pedidos y devuelve el resultado directamente.
     *
     * @param pedidosDiarios Lista de pedidos diarios a planificar
     * @param tiempoInicio Tiempo de inicio para la planificación
     * @param tamanioPoblacion Tamaño de la población del AG
     * @param maxGeneraciones Máximo de generaciones del AG
     * @param limiteGeneracionesSinMejora Límite de generaciones sin mejora
     * @return ResultadoPlanificacionDiaria con la solución y métricas
     */
    public ResultadoPlanificacionDiaria planificarDiarioSincrono(
            List<PedidoDiario> pedidosDiarios,
            LocalDateTime tiempoInicio,
            int tamanioPoblacion,
            int maxGeneraciones,
            int limiteGeneracionesSinMejora) {

        long inicioMs = System.currentTimeMillis();
        
        // Convertir PedidoDiario a PedidoSemanal
        List<PedidoSemanal> pedidos = pedidosDiarios.stream()
                .map(this::convertirDiarioASemanal)
                .toList();

        log.info("📦 [DIARIO-SYNC] Procesando {} pedidos diarios", pedidos.size());

        if (pedidos.isEmpty()) {
            log.warn("⚠️ [DIARIO-SYNC] No hay pedidos para procesar");
            return new ResultadoPlanificacionDiaria(null, 0, 0, 0.0, 0);
        }

        // Configurar parámetros del AG
        this.TAMANIO_POBLACION = tamanioPoblacion;
        this.MAX_GENERACIONES = maxGeneraciones;
        this.NO_MEJORA_LIMITE = limiteGeneracionesSinMejora;

        log.info("⚙️  [DIARIO-SYNC] Parámetros AG: población={}, maxGen={}, límiteSinMejora={}", 
                 TAMANIO_POBLACION, MAX_GENERACIONES, NO_MEJORA_LIMITE);

        try {
            // Obtener World
            World world = worldCacheService.getWorld();
            
            // Calcular horizonte de días
            int numeroDias = calcularHorizonteDiasDesdePedidos(pedidos);
            log.info("📅 [DIARIO-SYNC] Horizonte de días: {}", numeroDias);

            // Crear WorldTemporal y ControladorAlmacenes (sin cache, es operación única)
            WorldTemporal worldTemporal = new WorldTemporal(world, tiempoInicio, numeroDias);
            
            ControladorAlmacenes controladorAlmacenes = new ControladorAlmacenes(numeroDias, tiempoInicio);
            for (Aeropuerto aeropuerto : world.getAeropuertos().values()) {
                int capacidad = aeropuerto.tieneStockIlimitado() ? 0 : aeropuerto.getCapacidadAlmacen();
                controladorAlmacenes.registrarAeropuerto(aeropuerto.getCodigoICAO(), capacidad);
            }

            // Ejecutar AG (sin callbacks, síncrono)
            EstadoEjecucion estado = new EstadoEjecucion();
            Solution solucion = ejecutarAlgoritmoGeneticoSincrono(worldTemporal, controladorAlmacenes, pedidos, estado);
            
            // Calcular métricas
            int pedidosAsignados = 0;
            double mejorFitness = 0.0;
            
            if (solucion != null && solucion.getRutas() != null) {
                pedidosAsignados = (int) solucion.getRutas().values().stream()
                        .filter(r -> r != null && !r.isEmpty())
                        .count();
                mejorFitness = solucion.getObjetivo();
            }
            
            int pedidosSinRuta = pedidos.size() - pedidosAsignados;
            long tiempoProcesamiento = System.currentTimeMillis() - inicioMs;

            log.info("✅ [DIARIO-SYNC] Completado: {} asignados, {} sin ruta, fitness={:.2f}, tiempo={}ms", 
                    pedidosAsignados, pedidosSinRuta, mejorFitness, tiempoProcesamiento);

            return new ResultadoPlanificacionDiaria(solucion, pedidosAsignados, pedidosSinRuta, 
                    mejorFitness, tiempoProcesamiento);

        } catch (Exception e) {
            log.error("❌ [DIARIO-SYNC] Error: {}", e.getMessage(), e);
            throw new RuntimeException("Error en planificación diaria: " + e.getMessage(), e);
        }
    }

    /**
     * Ejecuta el AG de forma síncrona (sin callbacks)
     */
    private Solution ejecutarAlgoritmoGeneticoSincrono(
            WorldTemporal worldTemporal,
            ControladorAlmacenes controladorAlmacenes,
            List<PedidoSemanal> pedidos,
            EstadoEjecucion estado) {

        log.info("🧬 [AG-SYNC] Iniciando AG con {} pedidos", pedidos.size());

        Random random = new Random();
        int numeroPedidos = pedidos.size();

        DecodificadorGenetico decodificador = new DecodificadorGenetico(worldTemporal, controladorAlmacenes);

        // Generar población inicial
        List<Individuo> poblacion = generarPoblacionInicial(numeroPedidos, TAMANIO_POBLACION, random);
        
        // Evaluar población inicial
        evaluarPoblacion(poblacion, decodificador, pedidos, worldTemporal, controladorAlmacenes);
        poblacion.sort(Comparator.comparingDouble((Individuo i) -> i.fitness).reversed());

        double mejorFitnessGlobal = poblacion.get(0).fitness;
        Solution mejorSolucionGlobal = poblacion.get(0).solucion;

        log.info("🧬 [AG-SYNC] Gen 0: fitness={:.2f}", mejorFitnessGlobal);

        // Loop evolutivo
        for (int generacion = 1; generacion <= MAX_GENERACIONES; generacion++) {
            List<Individuo> nuevaPoblacion = new ArrayList<>();

            // Elitismo
            for (int i = 0; i < ELITE_K && i < poblacion.size(); i++) {
                nuevaPoblacion.add(new Individuo(poblacion.get(i).cromosoma.copiar()));
            }

            // Generar resto
            while (nuevaPoblacion.size() < TAMANIO_POBLACION) {
                Chromosome padre1 = seleccionTorneo(poblacion, random).cromosoma;
                Chromosome padre2 = seleccionTorneo(poblacion, random).cromosoma;

                Chromosome hijo;
                if (random.nextDouble() < PROB_CRUCE) {
                    hijo = padre1.cruzar(padre2, random);
                } else {
                    hijo = padre1.copiar();
                }

                hijo.mutar(PROB_MUTACION, random);
                nuevaPoblacion.add(new Individuo(hijo));
            }

            // Evaluar nueva población
            evaluarPoblacion(nuevaPoblacion, decodificador, pedidos, worldTemporal, controladorAlmacenes);
            nuevaPoblacion.sort(Comparator.comparingDouble((Individuo i) -> i.fitness).reversed());

            poblacion = nuevaPoblacion;

            // Actualizar mejor solución
            double mejorFitnessActual = poblacion.get(0).fitness;
            if (mejorFitnessActual > mejorFitnessGlobal) {
                mejorFitnessGlobal = mejorFitnessActual;
                mejorSolucionGlobal = poblacion.get(0).solucion;
            }

            log.debug("🧬 [AG-SYNC] Gen {}/{}: fitness={:.2f}", generacion, MAX_GENERACIONES, mejorFitnessGlobal);
        }

        log.info("🧬 [AG-SYNC] Finalizado: fitness final={:.2f}", mejorFitnessGlobal);
        return mejorSolucionGlobal;
    }

    /**
     * Ejecuta el AG con progreso en tiempo real
     * Ejecuta generación por generación y envía la mejor solución en cada iteración
     */
    private Solution ejecutarAlgoritmoGeneticoConProgreso(
            WorldTemporal worldTemporal,
            ControladorAlmacenes controladorAlmacenes,
            List<PedidoSemanal> pedidos,
            EstadoEjecucion estado,
            java.util.function.Consumer<ProgresoAGDTO> callbackProgreso) {

        long inicioTotal = System.currentTimeMillis();
        log.info("⏱️ ═══════════════════════════════════════════════════════════════");
        log.info("⏱️ [AG START] Pedidos={}, Población={}, MaxGen={}", 
                pedidos.size(), TAMANIO_POBLACION, MAX_GENERACIONES);

        Random random = new Random();
        int numeroPedidos = pedidos.size();

        // ⏱️ TIMING: Crear decodificador
        long t1 = System.currentTimeMillis();
        DecodificadorGenetico decodificador = new DecodificadorGenetico(worldTemporal, controladorAlmacenes);
        log.info("⏱️ [TIMING] Crear Decodificador: {}ms", System.currentTimeMillis() - t1);

        // ⏱️ TIMING: Generar población inicial
        long t2 = System.currentTimeMillis();
        List<Individuo> poblacion = generarPoblacionInicial(numeroPedidos, TAMANIO_POBLACION, random);
        log.info("⏱️ [TIMING] Generar Población Inicial: {}ms ({} individuos)", 
                System.currentTimeMillis() - t2, poblacion.size());

        // ⏱️ TIMING: Evaluar población inicial
        long t3 = System.currentTimeMillis();
        evaluarPoblacion(poblacion, decodificador, pedidos, worldTemporal, controladorAlmacenes);
        poblacion.sort(Comparator.comparingDouble((Individuo i) -> i.fitness).reversed());
        long evalInicialMs = System.currentTimeMillis() - t3;
        log.info("⏱️ [TIMING] Evaluar Población Inicial: {}ms", evalInicialMs);

        double mejorFitnessGlobal = poblacion.get(0).fitness;
        Solution mejorSolucionGlobal = poblacion.get(0).solucion;

        log.info("⏱️ [GEN 0] Fitness={} | Total acumulado: {}ms", 
                String.format("%.2f", mejorFitnessGlobal), System.currentTimeMillis() - inicioTotal);

        // Enviar progreso inicial (generación 0)
        enviarProgresoConSolucion(0, mejorFitnessGlobal, mejorSolucionGlobal, inicioTotal, 
                                  pedidos.size(), worldTemporal, callbackProgreso);

        // 2. Loop evolutivo
        for (int generacion = 1; generacion <= MAX_GENERACIONES; generacion++) {
            long tGen = System.currentTimeMillis();
            
            // ⚠️ Verificar estado (pausar/cancelar)
            verificarEstadoEjecucion(estado);

            // Crear nueva generación
            List<Individuo> nuevaPoblacion = new ArrayList<>();

            // Elitismo: copiar mejores K individuos
            for (int i = 0; i < ELITE_K && i < poblacion.size(); i++) {
                nuevaPoblacion.add(new Individuo(poblacion.get(i).cromosoma.copiar()));
            }

            // Generar resto de la población
            while (nuevaPoblacion.size() < TAMANIO_POBLACION) {
                Chromosome padre1 = seleccionTorneo(poblacion, random).cromosoma;
                Chromosome padre2 = seleccionTorneo(poblacion, random).cromosoma;

                Chromosome hijo;
                if (random.nextDouble() < PROB_CRUCE) {
                    hijo = padre1.cruzar(padre2, random);
                } else {
                    hijo = padre1.copiar();
                }

                hijo.mutar(PROB_MUTACION, random);
                nuevaPoblacion.add(new Individuo(hijo));
            }

            // ⏱️ TIMING: Evaluar nueva población
            long tEval = System.currentTimeMillis();
            evaluarPoblacion(nuevaPoblacion, decodificador, pedidos, worldTemporal, controladorAlmacenes);
            nuevaPoblacion.sort(Comparator.comparingDouble((Individuo i) -> i.fitness).reversed());
            long evalMs = System.currentTimeMillis() - tEval;

            poblacion = nuevaPoblacion;

            // Actualizar mejor solución
            double mejorFitnessActual = poblacion.get(0).fitness;
            if (mejorFitnessActual > mejorFitnessGlobal) {
                mejorFitnessGlobal = mejorFitnessActual;
                mejorSolucionGlobal = poblacion.get(0).solucion;
            }

            long genMs = System.currentTimeMillis() - tGen;
            log.info("⏱️ [GEN {}/{}] Fitness={} | Gen: {}ms (eval: {}ms) | Total: {}ms", 
                    generacion, MAX_GENERACIONES,
                    String.format("%.2f", mejorFitnessGlobal),
                    genMs, evalMs,
                    System.currentTimeMillis() - inicioTotal);

            // Enviar progreso con la mejor solución actual
            enviarProgresoConSolucion(generacion, mejorFitnessGlobal, mejorSolucionGlobal, inicioTotal,
                                      pedidos.size(), worldTemporal, callbackProgreso);
        }

        long totalMs = System.currentTimeMillis() - inicioTotal;
        log.info("⏱️ [AG END] Fitness Final={} | Duración Total: {}ms", 
                String.format("%.2f", mejorFitnessGlobal), totalMs);
        log.info("⏱️ ═══════════════════════════════════════════════════════════════");
        
        return mejorSolucionGlobal;
    }

    /**
     * Envía un mensaje de progreso con la solución actual
     */
    private void enviarProgresoConSolucion(int generacion, double fitness, Solution solucion,
                                           long inicioMs, int totalPedidos, WorldTemporal worldTemporal,
                                           java.util.function.Consumer<ProgresoAGDTO> callback) {
        double progreso = (generacion * 100.0) / MAX_GENERACIONES;

        // Convertir la solución a formato simplificado
        PlanificacionResponseSimple response = convertirAResponseSimple(solucion, worldTemporal);

        callback.accept(ProgresoAGDTO.builder()
                .tipo("PROGRESO_AG")
                .generacion(generacion)
                .maxGeneraciones(MAX_GENERACIONES)
                .progreso(progreso)
                .mejorFitness(fitness)
                .fitnessPromedio(fitness) // TODO: calcular promedio real
                .solucion(response)
                .pedidosProcesados(totalPedidos)
                .pedidosTotales(totalPedidos)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Verifica si debe pausar o cancelar
     */
    private void verificarEstadoEjecucion(EstadoEjecucion estado) {
        // Pausado: esperar hasta reanudar
        while (estado.isPausado() && !estado.isCancelado()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Cancelado: lanzar excepción
        if (estado.isCancelado()) {
            throw new RuntimeException("Planificación cancelada por el usuario");
        }
    }

    /**
     * Pausa la ejecución de una sesión
     */
    public void pausar(String sessionId) {
        EstadoEjecucion estado = sesionesActivas.get(sessionId);
        if (estado != null) {
            estado.setPausado(true);
            log.info("⏸️ Planificación pausada: sessionId={}", sessionId);
        }
    }

    /**
     * Reanuda la ejecución de una sesión
     */
    public void reanudar(String sessionId) {
        EstadoEjecucion estado = sesionesActivas.get(sessionId);
        if (estado != null) {
            estado.setPausado(false);
            log.info("▶️ Planificación reanudada: sessionId={}", sessionId);
        }
    }

    /**
     * Cancela la ejecución de una sesión
     */
    public void cancelar(String sessionId) {
        EstadoEjecucion estado = sesionesActivas.get(sessionId);
        if (estado != null) {
            estado.setCancelado(true);
            log.info("❌ Planificación cancelada: sessionId={}", sessionId);
        }
        sesionesActivas.remove(sessionId);
    }

    /**
     * ❌ DEPRECATED - Ya no se usa
     * 
     * Antes este método actualizaba el estado de los pedidos en la BD.
     * Ahora el estado se mantiene SOLO EN RAM mediante PedidoState y StateUpdater.
     * 
     * RAZÓN DEL CAMBIO:
     * - La simulación no debe modificar datos reales en la BD
     * - El estado en RAM es suficiente durante la simulación
     * - Los pedidos asignados se controlan via PedidoState.estado
     * - StateUpdater.java actualiza estados en RAM durante la simulación
     *
     * @deprecated Ya no se usa. Estados se manejan en RAM via PedidoState
     * @param solucion Solución generada por el AG (ignorada)
     */
    @Deprecated
    @Transactional
    private void actualizarEstadoPedidosPlanificados(Solution solucion) {
        // ⚠️ MÉTODO DESACTIVADO - NO HACE NADA
        // El código original está comentado abajo por referencia
        log.debug("⚠️ actualizarEstadoPedidosPlanificados() DESACTIVADO - Estados solo en RAM");
        
        /*
        // CÓDIGO ORIGINAL (ya no se ejecuta):
        if (solucion == null || solucion.getRutas().isEmpty()) {
            log.debug("No hay solución o rutas para actualizar estados");
            return;
        }

        int pedidosActualizados = 0;
        Set<Long> pedidosUnicos = new HashSet<>();

        for (Map.Entry<PedidoSemanal, List<SubRuta>> entry : solucion.getRutas().entrySet()) {
            pedidosUnicos.add(entry.getKey().getId());
        }

        for (Long pedidoId : pedidosUnicos) {
            try {
                PedidoSemanal pedido = pedidoSemanalRepository.findById(pedidoId).orElse(null);
                if (pedido != null && "PENDIENTE".equals(pedido.getEstado())) {
                    pedido.setEstado("ASIGNADO");
                    pedidoSemanalRepository.save(pedido);
                    pedidosActualizados++;
                }
            } catch (Exception e) {
                log.error("Error actualizando estado del pedido {}: {}", pedidoId, e.getMessage());
            }
        }

        log.debug("✅ Estados actualizados: {} pedidos cambiados a ASIGNADO", pedidosActualizados);
        */
    }
}
