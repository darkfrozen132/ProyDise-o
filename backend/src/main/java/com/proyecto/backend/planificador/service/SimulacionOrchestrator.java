package com.proyecto.backend.planificador.service;

import com.proyecto.backend.planificador.semanal.dto.sse.*;
import com.proyecto.backend.planificador.semanal.model.*;
import com.proyecto.backend.planificador.semanal.service.AlgoritmoGeneticoService;
import com.proyecto.backend.planificador.semanal.service.StateUpdater;
import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.repository.AeropuertoRepository;
import com.proyecto.backend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Orquestador de simulacion en tiempo real con estado persistente
 *
 * Arquitectura dual-thread:
 * - Thread 1: Ejecuta AlgoritmoGenetico cada Sa minutos (con timeout Ta=60s)
 * - Thread 2: Actualiza estado cada 1 segundo y hace broadcast al front
 *
 * El estado (SimulationState) es compartido entre ambos threads
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulacionOrchestrator {

    private final AlgoritmoGeneticoService algoritmoService;
    private final StateUpdater stateUpdater;
    private final PedidoRepository pedidoRepository;
    private final AeropuertoRepository aeropuertoRepository;

    // Clientes SSE conectados (thread-safe)
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // Estado global de la simulacion (compartido entre threads)
    private SimulationState state;

    // Control de ejecucion
    private volatile boolean activa = false;
    private Thread threadAG;
    private Thread threadState;
    private ExecutorService executorAG;

    // Configuracion temporal
    private static final int SA_MINUTOS = 5;      // Salto algoritmo (tiempo real)
    private static final int TA_SEGUNDOS = 60;    // Tiempo maximo de ejecucion del AG
    private static final long SSE_TIMEOUT = 30 * 60 * 1000; // 30 minutos

    /**
     * Inicia la simulacion
     *
     * @param fechaInicio Fecha de inicio de la planificacion
     * @param factorK Factor de ampliacion temporal
     */
    public void iniciarSimulacion(LocalDate fechaInicio, int factorK) {
        if (activa) {
            throw new IllegalStateException("Ya hay una simulacion activa");
        }

        log.info("🚀 Iniciando simulacion: fecha={}, K={}, Sa={}min, Ta={}s",
            fechaInicio, factorK, SA_MINUTOS, TA_SEGUNDOS);

        // Crear estado limpio
        this.state = new SimulationState();
        state.inicializar(LocalDateTime.of(fechaInicio, LocalTime.MIDNIGHT), factorK);

        // Cargar datos iniciales
        cargarPedidosIniciales(fechaInicio);
        cargarAeropuertos();

        // Pool de 1 thread para el AG
        this.executorAG = Executors.newSingleThreadExecutor();
        this.activa = true;

        // Thread 1: Algoritmo Genetico cada Sa minutos
        threadAG = new Thread(this::ejecutarLoopAlgoritmo, "AG-Scheduler");
        threadAG.start();

        // Thread 2: StateUpdater cada 1 segundo
        threadState = new Thread(this::ejecutarLoopEstado, "StateUpdater");
        threadState.start();

        log.info("✅ Simulacion iniciada - Pedidos cargados: {}", state.getPedidos().size());
    }

    /**
     * Loop del Algoritmo Genetico
     * Se ejecuta cada Sa minutos con timeout Ta
     */
    private void ejecutarLoopAlgoritmo() {
        int tickAG = 0;

        while (activa) {
            tickAG++;
            long inicioEjecucion = System.currentTimeMillis();

            try {
                // Calcular ventana acumulativa
                int Sc = state.getFactorK() * SA_MINUTOS * tickAG;
                LocalDateTime tiempoHasta = state.getTiempoRealInicio().plusMinutes(Sc);

                log.info("🧬 AG Tick {}: Iniciando planificacion hasta {} (ventana: {}min)",
                    tickAG, tiempoHasta, Sc);

                // Ejecutar AG con timeout
                Future<Boolean> futureAG = executorAG.submit(() -> {
                    try {
                        algoritmoService.ejecutarCiclo(state, tiempoHasta);
                        return true;
                    } catch (Exception e) {
                        log.error("Error en AG", e);
                        return false;
                    }
                });

                // Esperar maximo Ta segundos
                try {
                    Boolean resultado = futureAG.get(TA_SEGUNDOS, TimeUnit.SECONDS);

                    long duracion = System.currentTimeMillis() - inicioEjecucion;
                    log.info("✅ AG Tick {} completado en {}ms (limite: {}ms)",
                        tickAG, duracion, TA_SEGUNDOS * 1000);

                    // Advertencia si usa >80% del tiempo
                    if (duracion > TA_SEGUNDOS * 1000 * 0.8) {
                        log.warn("⚠️ AG usando {}% del tiempo limite",
                            (duracion * 100) / (TA_SEGUNDOS * 1000));
                    }

                } catch (TimeoutException e) {
                    log.error("❌ AG Tick {} TIMEOUT despues de {}s - Cancelando",
                        tickAG, TA_SEGUNDOS);

                    futureAG.cancel(true);
                    manejarTimeoutAG(tickAG);
                }

                // Esperar hasta completar Sa minutos
                long tiempoRestante = (SA_MINUTOS * 60 * 1000) -
                    (System.currentTimeMillis() - inicioEjecucion);

                if (tiempoRestante > 0) {
                    Thread.sleep(tiempoRestante);
                }

            } catch (InterruptedException e) {
                log.info("Loop AG interrumpido");
                break;
            } catch (Exception e) {
                log.error("Error critico en loop AG", e);
            }
        }
    }

    /**
     * Loop del StateUpdater
     * Se ejecuta cada 1 segundo (NUNCA se bloquea)
     */
    private void ejecutarLoopEstado() {
        while (activa) {
            try {
                // Avanzar tiempo de simulacion (K segundos en datos)
                stateUpdater.avanzarTiempo(state, state.getFactorK());

                // Broadcast snapshot cada segundo
                broadcastSnapshot();

                // Esperar 1 segundo
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                log.info("Loop StateUpdater interrumpido");
                break;
            } catch (Exception e) {
                log.error("Error en loop StateUpdater", e);
            }
        }
    }

    /**
     * Maneja el timeout del AG
     */
    private void manejarTimeoutAG(int tick) {
        log.warn("🔄 Aplicando estrategia de fallback por timeout en tick {}", tick);

        // Reducir parametros drasticamente
        int poblacionReducida = Math.max(5, state.getParametrosAG().getTamanioPoblacion() / 4);
        int generacionesReducidas = Math.max(10, state.getParametrosAG().getMaxGeneraciones() / 4);

        state.getParametrosAG().setTamanioPoblacion(poblacionReducida);
        state.getParametrosAG().setMaxGeneraciones(generacionesReducidas);

        log.warn("📉 Parametros reducidos: poblacion={}, generaciones={}",
            poblacionReducida, generacionesReducidas);

        state.setAlertaRendimiento(true);
    }

    /**
     * Carga todos los pedidos iniciales y los marca como PENDIENTE
     */
    private void cargarPedidosIniciales(LocalDate fechaInicio) {
        List<Pedido> pedidos = pedidoRepository.findAll();

        for (Pedido pedido : pedidos) {
            PedidoState pedidoState = new PedidoState();
            pedidoState.setId(pedido.getId());
            pedidoState.setEstado(EstadoPedido.PENDIENTE);
            pedidoState.setFechaCreacion(LocalDateTime.of(
                pedido.getAnio(), pedido.getMes(), pedido.getDia(),
                pedido.getHora(), pedido.getMinuto()
            ));
            pedidoState.setDestino(pedido.getAeropuertoDestinoId());
            pedidoState.setCantidad(pedido.getCantidadProductos());

            state.getPedidos().put(pedido.getId(), pedidoState);
        }

        log.info("Pedidos iniciales cargados: {}", pedidos.size());
    }

    /**
     * Carga los aeropuertos en el state
     */
    private void cargarAeropuertos() {
        List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();

        for (Aeropuerto aeropuerto : aeropuertos) {
            AlmacenState almacen = new AlmacenState();
            almacen.setCodigoICAO(aeropuerto.getCodigoICAO());
            almacen.setCapacidadMaxima(
                aeropuerto.tieneStockIlimitado() ? 0 : aeropuerto.getCapacidadAlmacen()
            );

            state.getAlmacenes().put(aeropuerto.getCodigoICAO(), almacen);
        }

        log.info("Aeropuertos cargados: {}", aeropuertos.size());
    }

    /**
     * Envia snapshot del estado a todos los clientes SSE
     */
    private void broadcastSnapshot() {
        SnapshotDTO snapshot = crearSnapshot();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("snapshot")
                    .data(snapshot));
            } catch (IOException e) {
                log.debug("Error enviando snapshot, cliente desconectado");
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Crea un snapshot del estado actual
     */
    private SnapshotDTO crearSnapshot() {
        // Convertir aeropuertos
        List<SnapshotDTO.AeropuertoSnapshotDTO> aeropuertosDTO = new ArrayList<>();
        for (AlmacenState almacen : state.getAlmacenes().values()) {
            Aeropuerto aeropuerto = aeropuertoRepository.findById(almacen.getCodigoICAO()).orElse(null);
            if (aeropuerto != null) {
                SnapshotDTO.AeropuertoSnapshotDTO dto = SnapshotDTO.AeropuertoSnapshotDTO.builder()
                    .codigo(almacen.getCodigoICAO())
                    .nombre(aeropuerto.getCiudad() + " - " + aeropuerto.getPais())
                    .latitud(aeropuerto.getLatitud())
                    .longitud(aeropuerto.getLongitud())
                    .capacidadAlmacen(almacen.getCapacidadMaxima())
                    .ocupacionActual(almacen.getOcupacionActual())
                    .pedidosAlmacenados(almacen.getPedidosAlmacenados().size())
                    .build();
                aeropuertosDTO.add(dto);
            }
        }

        // Convertir vuelos activos (programados o en vuelo, no aterrizados)
        List<SnapshotDTO.VueloSnapshotDTO> vuelosDTO = new ArrayList<>();
        for (VueloState vuelo : state.getVuelos().values()) {
            if (vuelo.getEstado() != EstadoVuelo.ATERRIZADO) {
                SnapshotDTO.VueloSnapshotDTO dto = SnapshotDTO.VueloSnapshotDTO.builder()
                    .id(vuelo.getId())
                    .origen(vuelo.getOrigen())
                    .destino(vuelo.getDestino())
                    .salida(vuelo.getSalida())
                    .llegada(vuelo.getLlegada())
                    .progreso(vuelo.getProgreso())
                    .estado(vuelo.getEstado())
                    .capacidadUsada(vuelo.getCapacidadUsada())
                    .capacidadMaxima(vuelo.getCapacidadMaxima())
                    .pedidosAbordo(new ArrayList<>(vuelo.getPedidosAbordo()))
                    .build();
                vuelosDTO.add(dto);
            }
        }

        // Convertir pedidos
        List<SnapshotDTO.PedidoSnapshotDTO> pedidosDTO = new ArrayList<>();
        for (PedidoState pedido : state.getPedidos().values()) {
            SnapshotDTO.PedidoSnapshotDTO dto = SnapshotDTO.PedidoSnapshotDTO.builder()
                .id(pedido.getId())
                .estado(pedido.getEstado())
                .ubicacionActual(pedido.getUbicacionActual())
                .destino(pedido.getDestino())
                .progresoRuta(pedido.getProgresoRuta())
                .cantidad(pedido.getCantidad())
                .fechaCreacion(pedido.getFechaCreacion())
                .fechaEntregaEstimada(pedido.getFechaEntregaEstimada())
                .build();
            pedidosDTO.add(dto);
        }

        // Calcular estadisticas
        SnapshotDTO.EstadisticasDTO estadisticas = SnapshotDTO.EstadisticasDTO.builder()
            .totalPedidos(state.getPedidos().size())
            .pedidosPendientes((int) state.contarPedidosPorEstado(EstadoPedido.PENDIENTE))
            .pedidosPlanificados((int) state.contarPedidosPorEstado(EstadoPedido.PLANIFICADO))
            .pedidosEnTransito((int) state.contarPedidosPorEstado(EstadoPedido.EN_TRANSITO))
            .pedidosEntregados((int) state.contarPedidosPorEstado(EstadoPedido.ENTREGADO))
            .vuelosActivos((int) state.getVuelos().values().stream()
                .filter(v -> v.getEstado() != EstadoVuelo.ATERRIZADO)
                .count())
            .vuelosProgramados((int) state.getVuelos().values().stream()
                .filter(v -> v.getEstado() == EstadoVuelo.PROGRAMADO)
                .count())
            .vuelosEnVuelo((int) state.getVuelos().values().stream()
                .filter(v -> v.getEstado() == EstadoVuelo.EN_VUELO)
                .count())
            .vuelosAterrizado((int) state.getVuelos().values().stream()
                .filter(v -> v.getEstado() == EstadoVuelo.ATERRIZADO)
                .count())
            .build();

        return SnapshotDTO.builder()
            .tiempoSimulacion(state.getTiempoSimulacion())
            .tick(state.getTickActual())
            .factorK(state.getFactorK())
            .aeropuertos(aeropuertosDTO)
            .vuelosActivos(vuelosDTO)
            .pedidos(pedidosDTO)
            .estadisticas(estadisticas)
            .build();
    }

    /**
     * Registra un cliente SSE
     */
    public SseEmitter registrarCliente() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("🔌 Cliente SSE desconectado - Quedan: {}", emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.warn("⏱️ Cliente SSE timeout - Quedan: {}", emitters.size());
        });

        emitter.onError((e) -> {
            emitters.remove(emitter);
            log.error("❌ Error en cliente SSE - Quedan: {}", emitters.size(), e);
        });

        emitters.add(emitter);
        log.info("🔌 Cliente SSE conectado - Total: {}", emitters.size());

        return emitter;
    }

    /**
     * Detiene la simulacion
     */
    public void detenerSimulacion() {
        if (!activa) {
            log.warn("No hay simulacion activa para detener");
            return;
        }

        activa = false;

        if (threadAG != null && threadAG.isAlive()) {
            threadAG.interrupt();
        }

        if (threadState != null && threadState.isAlive()) {
            threadState.interrupt();
        }

        if (executorAG != null) {
            executorAG.shutdownNow();
        }

        // Cerrar todos los clientes SSE
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("finalizado")
                    .data("Simulacion detenida"));
                emitter.complete();
            } catch (IOException e) {
                log.error("Error enviando evento de finalizacion", e);
            }
        }

        emitters.clear();

        log.info("🛑 Simulacion detenida");
    }

    /**
     * Verifica si hay una simulacion activa
     */
    public boolean estaActiva() {
        return activa;
    }

    /**
     * Obtiene el estado actual de la simulacion
     */
    public SimulationState getState() {
        return state;
    }

    /**
     * Obtiene el estado actual de la simulacion como DTO
     */
    public SimulacionEstadoDTO obtenerEstadoDTO() {
        if (state == null) {
            return SimulacionEstadoDTO.builder()
                .activa(false)
                .tickActual(0)
                .clientesConectados(emitters.size())
                .progreso(0.0)
                .build();
        }

        return SimulacionEstadoDTO.builder()
            .activa(activa)
            .fechaInicio(state.getTiempoRealInicio().toLocalDate())
            .inicioSimulacion(state.getTiempoRealInicio())
            .minutoActual((int) java.time.Duration.between(
                state.getTiempoRealInicio(), 
                state.getTiempoSimulacion()
            ).toMinutes())
            .saltoMinutos(state.getFactorK())
            .tickActual(0) // TODO: agregar contador de ticks en SimulationState
            .clientesConectados(emitters.size())
            .progreso(calcularProgreso())
            .limiteMinutos(1440) // 24 horas
            .build();
    }

    /**
     * Calcula el progreso de la simulacion (0.0 a 1.0)
     */
    private double calcularProgreso() {
        if (state == null) {
            return 0.0;
        }
        
        long minutosTranscurridos = java.time.Duration.between(
            state.getTiempoRealInicio(), 
            state.getTiempoSimulacion()
        ).toMinutes();
        
        return Math.min(1.0, minutosTranscurridos / 1440.0); // 1440 = 24 horas
    }
}
