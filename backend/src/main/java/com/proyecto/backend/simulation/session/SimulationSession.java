package com.proyecto.backend.simulation.session;

import com.proyecto.backend.simulation.dto.SimulationRequest;
import com.proyecto.backend.simulation.dto.SimulationSnapshot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Representa una sesión de simulación individual con gestión de estado thread-safe
 * Cada sesión se ejecuta en su propio Virtual Thread
 * 
 * Características:
 * - Estado atómico para concurrencia segura
 * - Snapshot inmutable para lectura sin locks
 * - Control de ciclo de vida (start/pause/cancel)
 * - Throttling de actualizaciones (evita saturar el frontend)
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
@Slf4j
@Getter
public class SimulationSession {

    // ============ IDENTIFICACIÓN ============
    private final UUID sessionId;
    private final String sessionName;
    private final LocalDateTime createdAt;

    // ============ CONFIGURACIÓN ============
    private final SimulationRequest configuration;

    // ============ ESTADO ATÓMICO (Thread-Safe) ============
    private final AtomicBoolean running;
    private final AtomicBoolean paused;
    private final AtomicBoolean completed;
    private final AtomicReference<SimulationSnapshot.SimulationStatus> status;

    // ============ MÉTRICAS ATÓMICAS ============
    private final AtomicInteger currentIteration;
    private final AtomicInteger processedOrders;
    private final AtomicInteger totalOrders;
    private final AtomicReference<Double> currentFitness;
    private final AtomicReference<Double> bestFitness;

    // ============ CONTROL DE TIEMPO ============
    private final AtomicLong lastUpdateTimestamp;
    private final AtomicReference<LocalDateTime> currentSimulationTime;
    private volatile Thread executionThread;

    // ============ SNAPSHOT INMUTABLE ============
    private final AtomicReference<SimulationSnapshot> latestSnapshot;

    // ============ THROTTLING CONFIG ============
    private static final long THROTTLE_MS = 500; // Enviar actualización cada 500ms

    /**
     * Constructor privado - usar factory method
     */
    private SimulationSession(UUID sessionId, SimulationRequest configuration) {
        this.sessionId = sessionId;
        this.sessionName = "SIM-" + sessionId.toString().substring(0, 8);
        this.createdAt = LocalDateTime.now();
        this.configuration = configuration;

        // Inicialización de estado
        this.running = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.completed = new AtomicBoolean(false);
        this.status = new AtomicReference<>(SimulationSnapshot.SimulationStatus.INITIALIZING);

        // Inicialización de métricas
        this.currentIteration = new AtomicInteger(0);
        this.processedOrders = new AtomicInteger(0);
        this.totalOrders = new AtomicInteger(0);
        this.currentFitness = new AtomicReference<>(0.0);
        this.bestFitness = new AtomicReference<>(0.0);

        // Inicialización de tiempo
        this.lastUpdateTimestamp = new AtomicLong(System.currentTimeMillis());
        this.currentSimulationTime = new AtomicReference<>(configuration.getStartDate().atStartOfDay());
        this.latestSnapshot = new AtomicReference<>(null);

        log.info("✅ Sesión creada: {} con factorK={}", sessionName, configuration.getFactorK());
    }

    /**
     * Factory method para crear una nueva sesión
     */
    public static SimulationSession create(SimulationRequest request) {
        UUID sessionId = UUID.randomUUID();
        return new SimulationSession(sessionId, request);
    }

    // ============ CONTROL DE CICLO DE VIDA ============

    /**
     * Inicia la simulación
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            status.set(SimulationSnapshot.SimulationStatus.RUNNING);
            log.info("▶️ Sesión {} iniciada", sessionName);
        }
    }

    /**
     * Pausa la simulación
     */
    public void pause() {
        if (running.get() && paused.compareAndSet(false, true)) {
            status.set(SimulationSnapshot.SimulationStatus.PAUSED);
            log.info("⏸️ Sesión {} pausada", sessionName);
        }
    }

    /**
     * Reanuda la simulación
     */
    public void resume() {
        if (running.get() && paused.compareAndSet(true, false)) {
            status.set(SimulationSnapshot.SimulationStatus.RUNNING);
            log.info("▶️ Sesión {} reanudada", sessionName);
        }
    }

    /**
     * Cancela la simulación (detiene el hilo limpiamente)
     */
    public void cancel() {
        if (running.compareAndSet(true, false)) {
            status.set(SimulationSnapshot.SimulationStatus.CANCELLED);
            paused.set(false);
            log.warn("🛑 Sesión {} cancelada", sessionName);
        }
    }

    /**
     * Marca la simulación como completada
     */
    public void complete() {
        running.set(false);
        paused.set(false);
        completed.set(true);
        status.set(SimulationSnapshot.SimulationStatus.COMPLETED);
        log.info("✅ Sesión {} completada", sessionName);
    }

    /**
     * Marca la simulación con error
     */
    public void error() {
        running.set(false);
        paused.set(false);
        status.set(SimulationSnapshot.SimulationStatus.ERROR);
        log.error("❌ Sesión {} con error", sessionName);
    }

    // ============ ACTUALIZACIÓN DE ESTADO ============

    /**
     * Actualiza las métricas de la simulación
     */
    public void updateMetrics(int iteration, int processed, int total, double fitness) {
        currentIteration.set(iteration);
        processedOrders.set(processed);
        totalOrders.set(total);
        currentFitness.set(fitness);

        // Actualizar mejor fitness si es mejor
        bestFitness.updateAndGet(current -> Math.max(current, fitness));
    }

    /**
     * Actualiza el tiempo de simulación
     */
    public void updateSimulationTime(LocalDateTime newTime) {
        currentSimulationTime.set(newTime);
    }

    /**
     * Actualiza el snapshot si ha pasado el tiempo de throttling
     * 
     * @return true si se debe enviar actualización, false si está throttled
     */
    public boolean shouldSendUpdate() {
        long now = System.currentTimeMillis();
        long lastUpdate = lastUpdateTimestamp.get();

        if (now - lastUpdate >= THROTTLE_MS) {
            lastUpdateTimestamp.set(now);
            return true;
        }
        return false;
    }

    /**
     * Actualiza el snapshot más reciente
     */
    public void updateSnapshot(SimulationSnapshot snapshot) {
        latestSnapshot.set(snapshot);
    }

    /**
     * Registra el thread de ejecución para control
     */
    public void setExecutionThread(Thread thread) {
        this.executionThread = thread;
    }

    // ============ CONSULTA DE ESTADO ============

    /**
     * Verifica si la simulación está activa (running y no paused)
     */
    public boolean isActive() {
        return running.get() && !paused.get();
    }

    /**
     * Verifica si la simulación está en ejecución (puede estar pausada)
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Verifica si la simulación está pausada
     */
    public boolean isPaused() {
        return paused.get();
    }

    /**
     * Verifica si la simulación está completada
     */
    public boolean isCompleted() {
        return completed.get();
    }

    /**
     * Verifica si la sesión debe ser eliminada (más de 2 horas de antigüedad)
     */
    public boolean isStale() {
        return createdAt.plusHours(2).isBefore(LocalDateTime.now());
    }

    /**
     * Obtiene el snapshot más reciente (inmutable, thread-safe)
     */
    public SimulationSnapshot getLatestSnapshot() {
        return latestSnapshot.get();
    }

    /**
     * Obtiene el estado actual
     */
    public SimulationSnapshot.SimulationStatus getCurrentStatus() {
        return status.get();
    }

    /**
     * Obtiene información de debug
     */
    public String getDebugInfo() {
        return String.format(
            "Session[id=%s, status=%s, iteration=%d, processed=%d/%d, fitness=%.2f, running=%s, paused=%s]",
            sessionName,
            status.get(),
            currentIteration.get(),
            processedOrders.get(),
            totalOrders.get(),
            currentFitness.get(),
            running.get(),
            paused.get()
        );
    }

    @Override
    public String toString() {
        return sessionName;
    }
}
