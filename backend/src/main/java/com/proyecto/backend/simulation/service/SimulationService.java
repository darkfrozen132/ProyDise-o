package com.proyecto.backend.simulation.service;

import com.proyecto.backend.model.PedidoSemanal;
import com.proyecto.backend.model.PlanDeVuelo;
import com.proyecto.backend.planificador.semanal.service.AlgoritmoGeneticoService;
import com.proyecto.backend.repository.PedidoSemanalRepository;
import com.proyecto.backend.repository.PlanDeVueloRepository;
import com.proyecto.backend.simulation.dto.ProgresoAGDTO;
import com.proyecto.backend.simulation.dto.SimulationRequest;
import com.proyecto.backend.simulation.dto.SimulationSnapshot;
import com.proyecto.backend.simulation.session.SimulationSession;
import com.proyecto.backend.simulation.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio principal de gestión de simulaciones logísticas
 * 
 * Características:
 * - Gestión de múltiples simulaciones simultáneas con Virtual Threads
 * - Sin bloqueo de BD (sin @Transactional en simulación)
 * - Estado de pedidos COMPLETAMENTE EN RAM (SessionStateManager)
 * - Comunicación en tiempo real vía WebSocket/STOMP
 * - Throttling inteligente (500ms) para no saturar el frontend
 * - Limpieza automática de sesiones antiguas
 * 
 * Arquitectura:
 * - Cada simulación corre en su propio Virtual Thread
 * - Estado thread-safe con AtomicBoolean/AtomicReference
 * - ConcurrentHashMap para gestión de sesiones
 * - Cada sesión tiene su propio estado de pedidos AISLADO
 * 
 * @author Sistema Package Planner
 * @version 2.0 - Estado en RAM
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    // ============ DEPENDENCIAS ============
    private final PedidoSemanalRepository pedidoSemanalRepository;
    private final PlanDeVueloRepository planDeVueloRepository;
    private final WebSocketService webSocketService;
    private final AlgoritmoGeneticoService algoritmoGeneticoService;
    private final SessionStateManager sessionStateManager;

    // ============ GESTIÓN DE SESIONES ============
    private final ConcurrentHashMap<UUID, SimulationSession> activeSessions = new ConcurrentHashMap<>();

    // ============ EXECUTOR CON VIRTUAL THREADS (Java 21) ============
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ============ CONSTANTES ============
    private static final int SALTO_ALGORITMO_MINUTOS = 5; // Sa = 5 minutos (fijo)
    private static final String TOPIC_PREFIX = "/topic/simulations/";

    // ============ MÉTODOS PRINCIPALES ============

    /**
     * Inicia una nueva simulación
     * 
     * @param request Configuración de la simulación
     * @return UUID de la sesión creada
     */
    public UUID startSimulation(SimulationRequest request) {
        log.info("🚀 Iniciando nueva simulación con fecha={}, hora={}, factorK={}", 
                request.getStartDate(), 
                request.getStartTime() != null ? request.getStartTime() : "00:00",
                request.getFactorK());

        // 1. Crear sesión
        SimulationSession session = SimulationSession.create(request);
        UUID sessionId = session.getSessionId();
        activeSessions.put(sessionId, session);

        // 2. Cargar datos iniciales (snapshot de BD en memoria)
        WorldSnapshot world = loadWorldSnapshot(request);
        
        // 3. Enviar confirmación inicial
        sendInitialSnapshot(session, world.totalOrders());

        // 4. Lanzar simulación en Virtual Thread
        session.start();
        virtualThreadExecutor.submit(() -> runSimulation(session, world));

        log.info("✅ Simulación {} iniciada con {} pedidos", 
                session.getSessionName(), world.totalOrders());
        
        return sessionId;
    }

    /**
     * Cancela una simulación en ejecución
     * 
     * @param sessionId ID de la sesión
     * @return true si se canceló exitosamente
     */
    public boolean cancelSimulation(UUID sessionId) {
        SimulationSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            log.warn("⚠️ Sesión {} no encontrada", sessionId);
            return false;
        }

        session.cancel();
        
        // Enviar notificación de cancelación
        sendMessage(sessionId, SimulationSnapshot.builder()
                .simulationId(sessionId)
                .status(SimulationSnapshot.SimulationStatus.CANCELLED)
                .timestamp(LocalDateTime.now())
                .build());

        log.info("🛑 Simulación {} cancelada", session.getSessionName());
        return true;
    }

    /**
     * Pausa una simulación en ejecución
     * 
     * @param sessionId ID de la sesión
     * @return true si se pausó exitosamente
     */
    public boolean pauseSimulation(UUID sessionId) {
        SimulationSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            log.warn("⚠️ Sesión {} no encontrada", sessionId);
            return false;
        }

        session.pause();
        log.info("⏸️ Simulación {} pausada", session.getSessionName());
        return true;
    }

    /**
     * Reanuda una simulación pausada
     * 
     * @param sessionId ID de la sesión
     * @return true si se reanudó exitosamente
     */
    public boolean resumeSimulation(UUID sessionId) {
        SimulationSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            log.warn("⚠️ Sesión {} no encontrada", sessionId);
            return false;
        }

        session.resume();
        log.info("▶️ Simulación {} reanudada", session.getSessionName());
        return true;
    }

    /**
     * Obtiene el estado actual de una simulación
     * 
     * @param sessionId ID de la sesión
     * @return Snapshot actual o null si no existe
     */
    public SimulationSnapshot getSimulationStatus(UUID sessionId) {
        SimulationSession session = activeSessions.get(sessionId);
        return session != null ? session.getLatestSnapshot() : null;
    }

    /**
     * Obtiene todas las sesiones activas
     */
    public Map<UUID, SimulationSession> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }

    // ============ SIMULACIÓN PRINCIPAL (Thread Separado) ============

    /**
     * Ejecuta la simulación en un Virtual Thread
     * NO tiene @Transactional para no bloquear la BD
     * ESTADO DE PEDIDOS EN RAM: Cada sesión tiene su propio estado aislado
     */
    private void runSimulation(SimulationSession session, WorldSnapshot world) {
        session.setExecutionThread(Thread.currentThread());
        String sessionId = session.getSessionId().toString();
        log.info("🎯 Iniciando ejecución de {}", session.getSessionName());

        try {
            int iteration = 0;
            // 🆕 Usar getStartDateTime() que combina fecha + hora de inicio
            LocalDateTime currentTime = session.getConfiguration().getStartDateTime();
            int saltoConsumo = calculateSaltoConsumo(session.getConfiguration().getFactorK());
            
            log.info("⏰ Tiempo inicial de simulación: {}", currentTime);

            // 🆕 ESTADO EN RAM: Inicializar estado de pedidos para esta sesión
            List<PedidoSemanal> todosPedidos = world.orders();
            sessionStateManager.inicializarSesion(sessionId, todosPedidos);
            
            // Filtrar pedidos PENDIENTES usando SessionStateManager (RAM, no BD)
            List<PedidoSemanal> remainingOrders = new ArrayList<>(
                sessionStateManager.filtrarPedidosPendientes(sessionId, todosPedidos)
            );
            
            log.info("📊 Sesión {} iniciada con {} pedidos PENDIENTES en RAM", sessionId, remainingOrders.size());

            while (session.isRunning() && !remainingOrders.isEmpty()) {
                // Verificar pausa
                while (session.isPaused() && session.isRunning()) {
                    Thread.sleep(100); // Esperar mientras esté pausado
                }

                if (!session.isRunning()) {
                    break; // Cancelado
                }

                long startTime = System.currentTimeMillis();
                iteration++;

                log.debug("🔄 {} - Iteración {} iniciada en tiempo simulado {}", 
                        session.getSessionName(), iteration, currentTime);

                // ============ LÓGICA DE SIMULACIÓN ============
                
                // 1. Buscar pedidos en ventana de tiempo
                LocalDateTime windowEnd = currentTime.plusMinutes(saltoConsumo);
                List<PedidoSemanal> pendingOrders = findOrdersInWindow(remainingOrders, currentTime, windowEnd);

                if (pendingOrders.isEmpty()) {
                    // No hay pedidos en esta ventana, avanzar al siguiente grupo de pedidos
                    log.debug("📭 {} - No hay pedidos en ventana actual. Avanzando tiempo...", 
                            session.getSessionName());
                    currentTime = currentTime.plusMinutes(saltoConsumo);
                    continue; // Continuar con siguiente ventana
                }

                // 2. Ejecutar Algoritmo Genético REAL con progreso vía WebSocket
                SimulationResult result = runGeneticAlgorithm(
                        pendingOrders, 
                        world.flights(),
                        session.getConfiguration(),
                        session,
                        currentTime
                );

                // 3. 🆕 Marcar pedidos como PLANIFICADOS en RAM (SessionStateManager)
                Set<Long> pedidosAsignados = pendingOrders.stream()
                        .map(PedidoSemanal::getId)
                        .collect(java.util.stream.Collectors.toSet());
                sessionStateManager.marcarComoAsignados(sessionId, pedidosAsignados);
                
                // 4. Actualizar lista de pedidos restantes desde RAM
                remainingOrders = sessionStateManager.filtrarPedidosPendientes(sessionId, todosPedidos);
                
                log.debug("✅ Procesados {} pedidos. Restantes: {}", 
                        pendingOrders.size(), remainingOrders.size());

                // 5. Actualizar métricas
                int totalProcessed = world.totalOrders() - remainingOrders.size();
                session.updateMetrics(
                        iteration,
                        totalProcessed,
                        world.totalOrders(),
                        result.fitness()
                );
                session.updateSimulationTime(currentTime);

                // 6. Enviar actualización (con throttling)
                if (session.shouldSendUpdate()) {
                    long duration = System.currentTimeMillis() - startTime;
                    sendProgressUpdate(session, currentTime, windowEnd, duration, result);
                }

                // 7. Avanzar tiempo simulado
                currentTime = currentTime.plusMinutes(saltoConsumo);

                // Pequeña pausa para no saturar (ajustable)
                Thread.sleep(100);
            }
            
            log.info("🎉 {} - TODOS los pedidos procesados ({}/{})", 
                    session.getSessionName(), 
                    world.totalOrders(), 
                    world.totalOrders());

            // Simulación completada
            session.complete();
            sendCompletionUpdate(session);

        } catch (InterruptedException e) {
            log.warn("⚠️ {} interrumpido", session.getSessionName());
            session.cancel();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ Error en {}: {}", session.getSessionName(), e.getMessage(), e);
            session.error();
            sendErrorUpdate(session, e.getMessage());
        } finally {
            // 🆕 Limpiar estado de sesión de RAM
            sessionStateManager.limpiarSesion(sessionId);
            log.info("🏁 {} finalizado. Estado de sesión limpiado de RAM.", session.getSessionName());
        }
    }

    // ============ ALGORITMO GENÉTICO REAL ============

    /**
     * Ejecuta el algoritmo genético REAL con callback de progreso
     */
    private SimulationResult runGeneticAlgorithm(
            List<PedidoSemanal> orders,
            List<PlanDeVuelo> flights,
            SimulationRequest config,
            SimulationSession session,
            LocalDateTime tiempoActual) {
        
        log.info("🧬 Ejecutando Algoritmo Genético REAL: {} pedidos en tiempo {}", 
                orders.size(), tiempoActual);

        // Crear callback para recibir progreso del AG y enviarlo vía WebSocket
        java.util.function.Consumer<com.proyecto.backend.simulation.dto.ProgresoAGDTO> callback = 
            (progreso) -> {
                // Añadir sessionId si no viene
                if (progreso.getSessionId() == null) {
                    progreso.setSessionId(session.getSessionId().toString());
                }
                
                // Enviar progreso vía WebSocket
                webSocketService.sendToSimulation(session.getSessionId().toString(), progreso);
                
                log.debug("📤 Progreso AG enviado: Gen {}/{}, Fitness: {}", 
                         progreso.getGeneracion(), 
                         progreso.getMaxGeneraciones(),
                         progreso.getMejorFitness());
            };

        try {
            // Ejecutar algoritmo genético con progreso en tiempo real
            algoritmoGeneticoService.planificarConProgresoWS(
                session.getSessionId().toString(),
                tiempoActual,
                config.getFactorK(),
                callback
            );
            
            // El AG ya procesó todo y envió progreso, retornar resultado simulado
            // (la solución real ya fue enviada vía callback)
            return new SimulationResult(
                    orders.size(),
                    0.0, // El fitness real ya se envió en ProgresoAGDTO
                    Collections.emptyList() // Las rutas reales ya se enviaron en ProgresoAGDTO
            );
            
        } catch (Exception e) {
            log.error("❌ Error ejecutando AG: {}", e.getMessage(), e);
            return new SimulationResult(0, 0.0, Collections.emptyList());
        }
    }

    // ============ CARGA DE DATOS ============

    /**
     * Carga un snapshot inmutable de los datos de BD
     * Se ejecuta UNA SOLA VEZ al inicio (sin bloqueo transaccional)
     * 
     * 🆕 ESTADO EN RAM: Los pedidos se cargan sin filtrar por estado
     * El estado se maneja completamente en SessionStateManager
     */
    private WorldSnapshot loadWorldSnapshot(SimulationRequest request) {
        log.info("📦 Cargando snapshot del mundo...");

        // 🆕 Cargar TODOS los pedidos (sin filtro por estado - estado en RAM)
        List<PedidoSemanal> allOrders = pedidoSemanalRepository.findAll();
        
        // Cargar TODOS los planes de vuelo disponibles
        List<PlanDeVuelo> allFlights = planDeVueloRepository.findAll();

        log.info("✅ Snapshot cargado: {} pedidos totales, {} vuelos", 
                allOrders.size(), allFlights.size());

        return new WorldSnapshot(allOrders, allFlights);
    }

    /**
     * Busca pedidos en la ventana de tiempo [start, end)
     */
    private List<PedidoSemanal> findOrdersInWindow(
            List<PedidoSemanal> orders,
            LocalDateTime start,
            LocalDateTime end) {
        
        return orders.stream()
                .filter(order -> {
                    LocalDateTime orderTime = LocalDateTime.of(
                            order.getAnio(),
                            order.getMes(),
                            order.getDia(),
                            order.getHora(),
                            order.getMinuto()
                    );
                    return !orderTime.isBefore(start) && orderTime.isBefore(end);
                })
                .toList();
    }

    // ============ COMUNICACIÓN WEBSOCKET ============

    /**
     * Envía snapshot inicial al cliente
     */
    private void sendInitialSnapshot(SimulationSession session, int totalOrders) {
        SimulationSnapshot snapshot = SimulationSnapshot.initializing(
                session.getSessionId(),
                totalOrders
        );
        session.updateSnapshot(snapshot);
        sendMessage(session.getSessionId(), snapshot);
    }

    /**
     * Envía actualización de progreso
     */
    private void sendProgressUpdate(
            SimulationSession session,
            LocalDateTime currentTime,
            LocalDateTime nextTime,
            long durationMs,
            SimulationResult result) {
        
        SimulationSnapshot snapshot = SimulationSnapshot.builder()
                .simulationId(session.getSessionId())
                .status(SimulationSnapshot.SimulationStatus.RUNNING)
                .currentSimulationTime(currentTime)
                .nextSimulationTime(nextTime)
                .advanceMinutes(calculateSaltoConsumo(session.getConfiguration().getFactorK()))
                .iterationNumber(session.getCurrentIteration().get())
                .durationMs(durationMs)
                .processedOrders(session.getProcessedOrders().get())
                .totalOrders(session.getTotalOrders().get())
                .currentFitness(result.fitness())
                .bestFitness(session.getBestFitness().get())
                .solution(buildSolutionData(result))
                .timestamp(LocalDateTime.now())
                .build();

        session.updateSnapshot(snapshot);
        sendMessage(session.getSessionId(), snapshot);
    }

    /**
     * Envía actualización de completado
     */
    private void sendCompletionUpdate(SimulationSession session) {
        SimulationSnapshot snapshot = SimulationSnapshot.completed(
                session.getSessionId(),
                session.getTotalOrders().get(),
                session.getBestFitness().get()
        );
        session.updateSnapshot(snapshot);
        sendMessage(session.getSessionId(), snapshot);
    }

    /**
     * Envía actualización de error
     */
    private void sendErrorUpdate(SimulationSession session, String errorMessage) {
        SimulationSnapshot snapshot = SimulationSnapshot.error(
                session.getSessionId(),
                errorMessage
        );
        session.updateSnapshot(snapshot);
        sendMessage(session.getSessionId(), snapshot);
    }

    /**
     * Envía mensaje por WebSocket/STOMP
     */
    private void sendMessage(UUID sessionId, SimulationSnapshot snapshot) {
        webSocketService.sendToSimulation(sessionId.toString(), snapshot);
        log.trace("📤 Mensaje enviado a /topic/simulations/{}", sessionId);
    }

    /**
     * Construye los datos de solución para el snapshot
     */
    private SimulationSnapshot.SolutionData buildSolutionData(SimulationResult result) {
        // TODO: Convertir rutas reales a SimulationRoute
        return SimulationSnapshot.SolutionData.builder()
                .routes(Collections.emptyList())
                .metadata(SimulationSnapshot.Metadata.builder()
                        .totalFlights(0)
                        .totalOrders(result.processedOrders())
                        .fitness(result.fitness())
                        .build())
                .build();
    }

    // ============ UTILIDADES ============

    /**
     * Calcula el Salto de Consumo: Sc = K × Sa
     */
    private int calculateSaltoConsumo(int factorK) {
        return factorK * SALTO_ALGORITMO_MINUTOS;
    }

    /**
     * Limpieza automática de sesiones antiguas (cada hora)
     * Evita fugas de memoria (Memory Leaks)
     */
    @Scheduled(cron = "0 0 * * * *") // Cada hora en punto
    public void cleanupStaleSessions() {
        log.info("🧹 Iniciando limpieza de sesiones antiguas...");

        int removed = 0;
        Iterator<Map.Entry<UUID, SimulationSession>> iterator = activeSessions.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, SimulationSession> entry = iterator.next();
            SimulationSession session = entry.getValue();

            if (session.isStale()) {
                iterator.remove();
                removed++;
                log.info("🗑️ Sesión antigua eliminada: {}", session.getSessionName());
            }
        }

        log.info("✅ Limpieza completada: {} sesiones eliminadas, {} activas", 
                removed, activeSessions.size());
    }

    // ============ RECORDS INTERNOS ============

    /**
     * Snapshot inmutable del mundo (pedidos + vuelos)
     */
    private record WorldSnapshot(
            List<PedidoSemanal> orders,
            List<PlanDeVuelo> flights
    ) {
        public int totalOrders() {
            return orders.size();
        }
    }

    /**
     * Resultado de una ejecución del AG
     */
    private record SimulationResult(
            int processedOrders,
            double fitness,
            List<Object> routes // TODO: Tipo real de ruta
    ) {}
}
