package com.proyecto.backend.planificador.service;

import com.proyecto.backend.planificador.semanal.dto.request.PlanificacionRequest;
import com.proyecto.backend.planificador.semanal.dto.response.PlanificacionResponse;
import com.proyecto.backend.planificador.semanal.dto.sse.EventoTickDTO;
import com.proyecto.backend.planificador.semanal.dto.sse.SimulacionEstadoDTO;
import com.proyecto.backend.planificador.semanal.service.AlgoritmoGeneticoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orquestador de simulación incremental con SSE
 * 
 * Ejecuta el algoritmo genético de forma incremental:
 * - Tick 1: Planifica [0-5 min]
 * - Tick 2: Planifica [0-10 min]
 * - Tick 3: Planifica [0-15 min]
 * ...
 * 
 * Envía resultados en tiempo real mediante Server-Sent Events (SSE)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulacionOrchestrator {
    
    private final AlgoritmoGeneticoService algoritmoGeneticoService;
    
    // Clientes SSE conectados (thread-safe)
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    
    // Estado de la simulación
    private volatile boolean activa = false;
    private Thread simulacionThread;
    
    // Parámetros de la simulación actual
    private LocalDate fechaInicio;
    private LocalDateTime inicioSimulacion;
    private int minutoActual = 0;
    private int saltoMinutos = 5;  // Sa (step algorithm)
    private int tickActual = 0;
    private int tamanioPoblacion = 50;
    private int maxGeneraciones = 200;
    
    // Configuración
    private static final long INTERVALO_TICK_MS = 1000;  // 1 segundo entre ticks
    private static final int LIMITE_MINUTOS = 1440;      // 24 horas
    private static final long SSE_TIMEOUT = 30 * 60 * 1000; // 30 minutos
    
    /**
     * Inicia la simulación incremental
     * 
     * @param fecha Fecha de inicio de la planificación
     * @param saltoMinutos Incremento en minutos por cada tick (Sa)
     * @param tamanioPoblacion Tamaño de la población del algoritmo genético
     * @param maxGeneraciones Máximo de generaciones del algoritmo genético
     */
    public void iniciarSimulacion(LocalDate fecha, int saltoMinutos, int tamanioPoblacion, int maxGeneraciones) {
        if (activa) {
            throw new IllegalStateException("Ya hay una simulación activa");
        }
        
        // Inicializar estado
        this.fechaInicio = fecha;
        this.inicioSimulacion = LocalDateTime.now();
        this.saltoMinutos = saltoMinutos;
        this.tamanioPoblacion = tamanioPoblacion;
        this.maxGeneraciones = maxGeneraciones;
        this.minutoActual = 0;
        this.tickActual = 0;
        this.activa = true;
        
        // Crear y arrancar thread de simulación
        simulacionThread = new Thread(() -> {
            try {
                ejecutarLoopSimulacion();
            } catch (InterruptedException e) {
                log.info("⏸️ Simulación interrumpida");
            } catch (Exception e) {
                log.error("❌ Error en simulación", e);
                detenerSimulacion();
            }
        });
        
        simulacionThread.start();
        
        log.info("🚀 Simulación iniciada: fecha={}, saltoMinutos={}, población={}, generaciones={}", 
            fecha, saltoMinutos, tamanioPoblacion, maxGeneraciones);
    }
    
    /**
     * Loop principal de la simulación
     * Se ejecuta en un thread separado
     */
    private void ejecutarLoopSimulacion() throws InterruptedException {
        while (activa && minutoActual < LIMITE_MINUTOS) {
            long inicioTick = System.currentTimeMillis();
            tickActual++;
            
            // 1. Incrementar ventana temporal
            minutoActual += saltoMinutos;
            
            log.info("⏱️ Tick {}: Procesando ventana [0-{} min]", tickActual, minutoActual);
            
            // 2. Construir request dinámico
            PlanificacionRequest request = construirRequest();
            
            // 3. Ejecutar algoritmo genético
            PlanificacionResponse response = algoritmoGeneticoService.planificar(request);
            
            long duracionTick = System.currentTimeMillis() - inicioTick;
            
            // 4. Calcular progreso
            double progreso = (double) minutoActual / LIMITE_MINUTOS;
            boolean completada = minutoActual >= LIMITE_MINUTOS;
            
            // 5. Crear evento para SSE
            EventoTickDTO evento = EventoTickDTO.builder()
                .tick(tickActual)
                .minutoActual(minutoActual)
                .saltoMinutos(saltoMinutos)
                .fechaInicio(fechaInicio)
                .planificacion(response)
                .tiempoEjecucionMs(duracionTick)
                .completada(completada)
                .progreso(progreso)
                .build();
            
            // 6. Broadcast a todos los clientes conectados
            broadcastEvento(evento);
            
            log.info("📤 Tick {} completado en {}ms - {} clientes notificados - Progreso: {:.1f}%", 
                tickActual, duracionTick, emitters.size(), progreso * 100);
            
            // 7. Esperar antes del siguiente tick
            Thread.sleep(INTERVALO_TICK_MS);
        }
        
        // Simulación completada
        if (minutoActual >= LIMITE_MINUTOS) {
            log.info("✅ Simulación completada: {} minutos procesados ({} horas)", 
                minutoActual, minutoActual / 60.0);
        }
        
        detenerSimulacion();
    }
    
    /**
     * Construye el PlanificacionRequest dinámicamente según el minuto actual
     * 
     * Fórmula clave: factorK = minutoActual / saltoMinutos
     * 
     * Ejemplo:
     * - minutoActual = 10, saltoMinutos = 5 → factorK = 2
     * - El algoritmo procesará pedidos desde 0 hasta 10 minutos
     */
    private PlanificacionRequest construirRequest() {
        PlanificacionRequest request = new PlanificacionRequest();
        request.setFecha(fechaInicio);
        
        // 🔥 CLAVE: Calcular factorK dinámicamente
        // factorK determina el rango temporal: [0, minutoActual]
        int factorK = minutoActual / saltoMinutos;
        request.setFactorK(factorK);
        
        // Configurar parámetros del algoritmo genético
        PlanificacionRequest.ParametrosGenetico params = new PlanificacionRequest.ParametrosGenetico();
        params.setSaltoAlgoritmoMinutos(saltoMinutos);
        params.setTamanioPoblacion(tamanioPoblacion);
        params.setMaxGeneraciones(maxGeneraciones);
        request.setParametrosGenetico(params);
        
        log.debug("📝 Request: fecha={}, factorK={}, ventana=[0-{} min]", 
            fechaInicio, factorK, minutoActual);
        
        return request;
    }
    
    /**
     * Registra un nuevo cliente SSE
     * 
     * @return SseEmitter para enviar eventos
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
        
        // Enviar estado actual inmediatamente
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name("estado")
                .data(obtenerEstado());
            emitter.send(event);
        } catch (IOException e) {
            log.error("Error enviando estado inicial", e);
            emitters.remove(emitter);
        }
        
        return emitter;
    }
    
    /**
     * Envía un evento a todos los clientes conectados
     * 
     * @param evento Evento a enviar
     */
    private void broadcastEvento(EventoTickDTO evento) {
        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name("tick")
                    .data(evento);
                emitter.send(event);
            } catch (IOException e) {
                log.error("Error enviando evento a cliente, se eliminará", e);
                emitters.remove(emitter);
            }
        }
    }
    
    /**
     * Detiene la simulación actual
     */
    public void detenerSimulacion() {
        if (!activa) {
            log.warn("No hay simulación activa para detener");
            return;
        }
        
        activa = false;
        
        if (simulacionThread != null && simulacionThread.isAlive()) {
            simulacionThread.interrupt();
        }
        
        // Enviar evento de finalización a todos los clientes
        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name("finalizado")
                    .data("Simulación detenida");
                emitter.send(event);
                emitter.complete();
            } catch (IOException e) {
                log.error("Error enviando evento de finalización", e);
            }
        }
        
        emitters.clear();
        
        log.info("🛑 Simulación detenida");
    }
    
    /**
     * Obtiene el estado actual de la simulación
     * 
     * @return Estado de la simulación
     */
    public SimulacionEstadoDTO obtenerEstado() {
        double progreso = activa ? (double) minutoActual / LIMITE_MINUTOS : 0.0;
        
        return SimulacionEstadoDTO.builder()
            .activa(activa)
            .fechaInicio(fechaInicio)
            .inicioSimulacion(inicioSimulacion)
            .minutoActual(minutoActual)
            .saltoMinutos(saltoMinutos)
            .tickActual(tickActual)
            .clientesConectados(emitters.size())
            .progreso(progreso)
            .limiteMinutos(LIMITE_MINUTOS)
            .build();
    }
    
    /**
     * Verifica si hay una simulación activa
     * 
     * @return true si hay una simulación en curso
     */
    public boolean estaActiva() {
        return activa;
    }
}
