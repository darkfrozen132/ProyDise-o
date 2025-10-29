package com.proyecto.backend.algoritmo.service;

import com.proyecto.backend.algoritmo.core.*;
import com.proyecto.backend.algoritmo.dto.request.PlanificacionRequest;
import com.proyecto.backend.algoritmo.dto.response.*;
import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.model.PlanDeVuelo;
import com.proyecto.backend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Servicio del algoritmo genetico para planificacion de rutas
 * Version inicial simplificada - base para iteraciones futuras
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlgoritmoGeneticoService {

    private final WorldCacheService worldCacheService;
    private final PedidoRepository pedidoRepository;

    // Constantes de negocio
    private static final int PLAZO_MISMO_CONTINENTE_DIAS = 2;
    private static final int PLAZO_DIFERENTE_CONTINENTE_DIAS = 3;
    private static final int VENTANA_RECOJO_HORAS = 2;

    // Parametros del algoritmo genetico
    private static final int TAMANIO_POBLACION = 50;      // Numero de individuos
    private static final int MAX_GENERACIONES = 200;      // Generaciones maximas
    private static final int NO_MEJORA_LIMITE = 40;       // Parar si 40 gen sin mejora
    private static final int ELITE_K = 4;                 // Mejores preservados (elitismo)
    private static final double PROB_CRUCE = 0.8;         // Probabilidad de cruce
    private static final double PROB_MUTACION = 0.05;     // Probabilidad de mutacion
    private static final int TAMANIO_TORNEO = 3;          // Individuos en torneo

    /**
     * Ejecuta la planificacion de rutas para una fecha dada
     *
     * @param request Request con parametros de planificacion
     * @return Response con la planificacion completa
     */
    @Transactional(readOnly = true)
    public PlanificacionResponse planificar(PlanificacionRequest request) {
        long inicio = System.currentTimeMillis();

        log.info("Iniciando planificacion para fecha {} con K={}", request.getFecha(), request.getFactorK());

        // Obtener el World base (singleton inmutable)
        World world = worldCacheService.getWorld();

        // Cargar pedidos en el rango de tiempo
        List<Pedido> pedidos = cargarPedidosEnRango(request);
        log.info("Cargados {} pedidos para procesar", pedidos.size());

        if (pedidos.isEmpty()) {
            log.warn("No hay pedidos para procesar en el rango especificado");
            return crearResponseVacio(request, inicio);
        }

        // Calcular horizonte temporal (dias a expandir)
        int numeroDias = calcularHorizonteDias(request, pedidos);
        log.info("Horizonte temporal: {} dias", numeroDias);

        // Crear WorldTemporal para esta ejecucion
        WorldTemporal worldTemporal = new WorldTemporal(world, request.getFecha(), numeroDias);
        log.info("WorldTemporal creado: {}", worldTemporal.getEstadisticas());

        // Crear controlador de almacenes para rastrear ocupacion
        LocalDateTime fechaBaseUTC = LocalDateTime.of(request.getFecha(), LocalTime.MIDNIGHT);
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
     * Ejecuta el algoritmo genetico completo
     *
     * @param worldTemporal World temporal con vuelos expandidos
     * @param controladorAlmacenes Controlador de capacidad de almacenes
     * @param pedidos Lista de pedidos a planificar
     * @return Mejor solucion encontrada
     */
    private Solution ejecutarAlgoritmoGenetico(WorldTemporal worldTemporal,
                                               ControladorAlmacenes controladorAlmacenes,
                                               List<Pedido> pedidos) {
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

        return mejorIndividuo.solucion;
    }

    /**
     * Genera poblacion inicial de cromosomas aleatorios
     */
    private List<Individuo> generarPoblacionInicial(int numeroPedidos, Random random) {
        List<Individuo> poblacion = new ArrayList<>();
        for (int i = 0; i < TAMANIO_POBLACION; i++) {
            Chromosome cromosoma = new Chromosome(numeroPedidos);
            cromosoma.inicializarAleatorio(random);
            poblacion.add(new Individuo(cromosoma));
        }
        return poblacion;
    }

    /**
     * Evalua todos los individuos de la poblacion
     */
    private void evaluarPoblacion(List<Individuo> poblacion, DecodificadorGenetico decodificador,
                                   List<Pedido> pedidos, WorldTemporal worldTemporal,
                                   ControladorAlmacenes controladorAlmacenes) {
        for (Individuo individuo : poblacion) {
            if (individuo.solucion == null) {
                // Resetear el estado de vuelos y almacenes antes de evaluar
                worldTemporal.resetearCapacidades();
                controladorAlmacenes.limpiar();

                // Decodificar cromosoma con instancias compartidas (pero reseteadas)
                individuo.solucion = decodificador.decodificar(individuo.cromosoma, pedidos);
                individuo.fitness = individuo.solucion.getObjetivo();
            }
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
     * - Minimo: plazo maximo de entrega (3 dias) + 1 dia buffer = 4 dias
     * - Dinamico: dia maximo de pedidos + 3 dias
     *
     * @param request Request de planificacion
     * @param pedidos Lista de pedidos
     * @return Numero de dias del horizonte
     */
    private int calcularHorizonteDias(PlanificacionRequest request, List<Pedido> pedidos) {
        // Por defecto: 7 dias (una semana)
        int diasBase = 7;

        // Calcular dia maximo de los pedidos
        LocalDate fechaMaxPedido = request.getFecha();
        for (Pedido pedido : pedidos) {
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
    private List<Pedido> cargarPedidosEnRango(PlanificacionRequest request) {
        LocalDate fecha = request.getFecha();
        int rangoMinutos = request.calcularRangoConsumoMinutos();

        // Calcular fecha/hora de inicio (00:00 del dia especificado)
        LocalDateTime inicio = LocalDateTime.of(fecha, LocalTime.MIDNIGHT);

        // Calcular fecha/hora de fin (inicio + Sc minutos)
        LocalDateTime fin = inicio.plusMinutes(rangoMinutos);

        log.info("Cargando pedidos en rango: {} a {} ({} minutos)",
                inicio, fin, rangoMinutos);

        // Cargar todos los pedidos y filtrar por rango de tiempo
        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> "PENDIENTE".equals(p.getEstado()))
                .filter(p -> estaDentroDelRango(p, inicio, fin))
                .toList();

        log.info("Encontrados {} pedidos que cumplen los criterios en el rango de tiempo", pedidos.size());

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
    private boolean estaDentroDelRango(Pedido pedido, LocalDateTime inicio, LocalDateTime fin) {
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
    private Solution crearSolucionVacia(List<Pedido> pedidos) {
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
            List<Pedido> pedidos, long inicio) {

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
        for (Map.Entry<Pedido, List<SubRuta>> entry : solucion.getRutas().entrySet()) {
            Pedido pedido = entry.getKey();
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
        log.info("Convertidos {} vuelos unicos con pedidos agrupados", vuelos.size());

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

        for (Map.Entry<Pedido, List<SubRuta>> entry : solucion.getRutas().entrySet()) {
            Pedido pedido = entry.getKey();
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
    private List<PlanificacionResponse.PedidoResumenDTO> convertirPedidosAResumen(List<Pedido> pedidos) {
        List<PlanificacionResponse.PedidoResumenDTO> resumen = new ArrayList<>();

        for (Pedido pedido : pedidos) {
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
            dto.setEstado(pedido.getEstado());

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
}
