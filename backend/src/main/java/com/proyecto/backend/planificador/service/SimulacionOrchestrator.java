package com.proyecto.backend.planificador.service;

import com.proyecto.backend.planificador.dto.SimulacionEstadoDTO;
import com.proyecto.backend.service.VueloTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orquestador de la simulación en tiempo real
 * 
 * Constantes principales:
 * - TIME_SCALE: Cuántos minutos simulados equivalen a 1 segundo real (default: 60.0)
 * - INTERVALO_TICK_MS: Cada cuántos milisegundos se ejecuta un tick (default: 1000ms)
 * - TIEMPO_PROCESAMIENTO_MS: Tiempo reservado para que el algoritmo procese (default: 800ms)
 * 
 * Funcionamiento:
 * - Cada 1 segundo real = 60 minutos simulados (1 hora simulada)
 * - Ejecuta el algoritmo genético cada tick
 * - Permite pausar/reanudar/detener la simulación
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimulacionOrchestrator {

    // ⭐ Servicio de tracking de vuelos
    private final VueloTrackingService vueloTrackingService;

    // ==================== CONSTANTES DE SIMULACIÓN ====================
    
    /**
     * TIME_SCALE: Cuántos minutos simulados equivalen a 1 segundo real
     * Ejemplo: 60.0 significa que cada segundo real = 60 minutos simulados (1 hora)
     *          120.0 significa que cada segundo real = 120 minutos simulados (2 horas)
     */
    private static final double TIME_SCALE = 60.0;  // ⭐ 1 segundo real = 1 hora simulada
    
    /**
     * INTERVALO_TICK_MS: Cada cuántos milisegundos se ejecuta un tick de simulación
     * 1000ms = 1 segundo real
     */
    private static final long INTERVALO_TICK_MS = 1000;
    
    /**
     * TIEMPO_PROCESAMIENTO_MS: Tiempo reservado para que el algoritmo procese
     * Debe ser menor que INTERVALO_TICK_MS para evitar solapamientos
     */
    private static final long TIEMPO_PROCESAMIENTO_MS = 800;
    
    // ==================== ESTADO DE LA SIMULACIÓN ====================
    
    private final AtomicBoolean simulacionActiva = new AtomicBoolean(false);
    private final AtomicInteger tickActual = new AtomicInteger(0);
    private final AtomicLong tiempoInicioMs = new AtomicLong(0);
    
    private LocalDateTime horaSimulacionActual;
    
    // Estadísticas
    private final AtomicInteger totalPedidosProcesados = new AtomicInteger(0);
    private final AtomicInteger pedidosATiempo = new AtomicInteger(0);
    private final AtomicInteger pedidosTarde = new AtomicInteger(0);
    
    // ⭐ Controller para enviar eventos SSE
    private com.proyecto.backend.planificador.controller.SimulacionController simulacionController;
    
    /**
     * Método para inyectar el controller (evita dependencia circular)
     */
    public void setSimulacionController(com.proyecto.backend.planificador.controller.SimulacionController controller) {
        this.simulacionController = controller;
    }
    
    // ==================== SCHEDULER ====================
    
    /**
     * Ejecuta un tick de simulación cada INTERVALO_TICK_MS milisegundos
     * fixedDelay asegura que no se solapen ejecuciones
     */
    @Scheduled(fixedDelayString = "#{${simulacion.intervalo.tick.ms:1000}}")
    public void tick() {
        if (!simulacionActiva.get()) {
            return; // Simulación pausada
        }

        try {
            long inicioTick = System.currentTimeMillis();
            int tick = tickActual.incrementAndGet();

            log.info("═══════════════════════════════════════════════════════════");
            log.info("TICK #{} - Hora Simulada: {} (Tiempo Real: {}ms)", 
                     tick, horaSimulacionActual, getTiempoRealTranscurrido());
            log.info("═══════════════════════════════════════════════════════════");

            // 1. Ejecutar algoritmo genético (aquí llamarías a tu servicio)
            ejecutarAlgoritmo();

            // 2. Avanzar el tiempo simulado
            avanzarTiempoSimulado();

            // 3. Verificar tiempo de procesamiento
            long tiempoProcesamiento = System.currentTimeMillis() - inicioTick;
            
            if (tiempoProcesamiento > TIEMPO_PROCESAMIENTO_MS) {
                log.warn("⚠️ El procesamiento tomó {}ms (límite: {}ms)", 
                         tiempoProcesamiento, TIEMPO_PROCESAMIENTO_MS);
            } else {
                log.debug("✅ Procesamiento completado en {}ms", tiempoProcesamiento);
            }

            // 4. ⭐ Enviar estado actualizado a todos los clientes conectados por SSE
            broadcastEstado();

        } catch (Exception e) {
            log.error("❌ Error en tick de simulación", e);
        }
    }

    // ==================== CONTROL DE SIMULACIÓN ====================
    
    /**
     * Inicia la simulación
     */
    public synchronized void iniciar() {
        if (simulacionActiva.get()) {
            log.warn("La simulación ya está en ejecución");
            throw new IllegalStateException("La simulación ya está activa");
        }

        log.info("🚀 Iniciando simulación...");
        log.info("   TIME_SCALE: {} minutos/segundo", TIME_SCALE);
        log.info("   INTERVALO_TICK: {}ms", INTERVALO_TICK_MS);
        log.info("   TIEMPO_PROCESAMIENTO: {}ms", TIEMPO_PROCESAMIENTO_MS);

        // Inicializar estado
        tickActual.set(0);
        tiempoInicioMs.set(System.currentTimeMillis());
        horaSimulacionActual = LocalDateTime.of(2025, 1, 1, 0, 0); // Día 1, hora 0
        
        // Resetear estadísticas
        totalPedidosProcesados.set(0);
        pedidosATiempo.set(0);
        pedidosTarde.set(0);
        
        simulacionActiva.set(true);
        log.info("✅ Simulación iniciada en hora simulada: {}", horaSimulacionActual);
        
        // ⭐ Iniciar tracking de vuelos automáticamente
        try {
            vueloTrackingService.iniciarStreaming();
            log.info("✈️ Tracking de vuelos iniciado automáticamente");
        } catch (Exception e) {
            log.error("❌ Error al iniciar tracking de vuelos", e);
        }
        
        // ⭐ Enviar estado inicial por SSE
        broadcastEstado();
    }

    /**
     * Pausa la simulación
     */
    public synchronized void pausar() {
        if (!simulacionActiva.get()) {
            log.warn("La simulación ya está pausada");
            throw new IllegalStateException("La simulación ya está pausada");
        }

        simulacionActiva.set(false);
        log.info("⏸️ Simulación pausada en tick #{} - Hora simulada: {}", 
                 tickActual.get(), horaSimulacionActual);
        
        // ⭐ Enviar estado pausado por SSE
        broadcastEstado();
    }

    /**
     * Reanuda la simulación
     */
    public synchronized void reanudar() {
        if (simulacionActiva.get()) {
            log.warn("La simulación ya está en ejecución");
            throw new IllegalStateException("La simulación ya está activa");
        }

        simulacionActiva.set(true);
        log.info("▶️ Simulación reanudada desde tick #{} - Hora simulada: {}", 
                 tickActual.get(), horaSimulacionActual);
        
        // ⭐ Enviar estado reanudado por SSE
        broadcastEstado();
    }

    /**
     * Detiene completamente la simulación y resetea el estado
     */
    public synchronized void detener() {
        simulacionActiva.set(false);
        tickActual.set(0);
        tiempoInicioMs.set(0);
        horaSimulacionActual = null;
        
        // Resetear estadísticas
        totalPedidosProcesados.set(0);
        pedidosATiempo.set(0);
        pedidosTarde.set(0);
        
        // ⭐ Detener tracking de vuelos automáticamente
        try {
            vueloTrackingService.detenerStreaming();
            log.info("✈️ Tracking de vuelos detenido automáticamente");
        } catch (Exception e) {
            log.error("❌ Error al detener tracking de vuelos", e);
        }
        
        log.info("⏹️ Simulación detenida y reseteada");
        
        // ⭐ Enviar estado detenido por SSE
        broadcastEstado();
    }

    // ==================== LÓGICA INTERNA ====================
    
    /**
     * Ejecuta el algoritmo genético
     * AQUÍ DEBES INTEGRAR TU ALGORITMO GENÉTICO
     */
    private void ejecutarAlgoritmo() {
        try {
            long inicio = System.currentTimeMillis();
            
            // TODO: Llamar a tu servicio del algoritmo genético
            // Ejemplo:
            // Solucion solucion = algoritmoGeneticoService.ejecutar();
            // actualizarEstadisticas(solucion);
            
            log.debug("🧬 Algoritmo genético simulado");
            
            // Simular procesamiento
            Thread.sleep(100);
            
            long duracion = System.currentTimeMillis() - inicio;
            log.debug("🧬 Algoritmo ejecutado en {}ms", duracion);
            
        } catch (Exception e) {
            log.error("❌ Error al ejecutar algoritmo genético", e);
        }
    }

    /**
     * Avanza el tiempo simulado según TIME_SCALE
     */
    private void avanzarTiempoSimulado() {
        if (horaSimulacionActual == null) {
            return;
        }
        
        // Cada tick avanza TIME_SCALE minutos
        horaSimulacionActual = horaSimulacionActual.plusMinutes((long) TIME_SCALE);
        
        log.debug("🕐 Tiempo simulado avanzado a: {} (Día {}, Hora {})", 
                 horaSimulacionActual,
                 horaSimulacionActual.getDayOfMonth(),
                 horaSimulacionActual.getHour());
    }

    /**
     * Actualiza estadísticas después de ejecutar el algoritmo
     */
    public void actualizarEstadisticas(int pedidosProcesados, int aTiempo, int tarde) {
        totalPedidosProcesados.addAndGet(pedidosProcesados);
        pedidosATiempo.addAndGet(aTiempo);
        pedidosTarde.addAndGet(tarde);
    }

    // ==================== CONSULTAS ====================
    
    /**
     * Verifica si la simulación está activa
     */
    public boolean estaActiva() {
        return simulacionActiva.get();
    }

    /**
     * Obtiene el tick actual
     */
    public int getTickActual() {
        return tickActual.get();
    }

    /**
     * Obtiene la hora simulada actual
     */
    public LocalDateTime getHoraSimulacionActual() {
        return horaSimulacionActual;
    }

    /**
     * Calcula cuánto tiempo real ha transcurrido en milisegundos
     */
    public long getTiempoRealTranscurrido() {
        if (tiempoInicioMs.get() == 0) {
            return 0;
        }
        return System.currentTimeMillis() - tiempoInicioMs.get();
    }

    /**
     * ⭐ Obtiene el DTO con los tiempos (para SSE)
     */
    public SimulacionEstadoDTO getEstadoDTO() {
        return SimulacionEstadoDTO.builder()
                .horaSimulada(horaSimulacionActual)
                .tiempoRealTranscurridoMs(getTiempoRealTranscurrido())
                .activa(simulacionActiva.get())
                .tickActual(tickActual.get())
                .estadoDescripcion(simulacionActiva.get() ? "ACTIVA" : "PAUSADA")
                .timeScale(TIME_SCALE)
                .rutasSolucion(vueloTrackingService.obtenerTodasLasRutas())  // ⭐ Incluir rutas
                .build();
    }

    /**
     * ⭐ Envía el estado actual a todos los clientes conectados por SSE
     */
    private void broadcastEstado() {
        if (simulacionController != null) {
            simulacionController.broadcastEstado(getEstadoDTO());
        }
    }

    /**
     * Obtiene las constantes de configuración
     */
    public double getTimeScale() {
        return TIME_SCALE;
    }

    public long getIntervaloTickMs() {
        return INTERVALO_TICK_MS;
    }

    public long getTiempoProcesamientoMs() {
        return TIEMPO_PROCESAMIENTO_MS;
    }
}
