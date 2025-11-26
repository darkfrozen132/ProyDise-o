# 🎯 Sistema de Simulación Incremental con SSE

## 📋 Descripción General

Sistema de **simulación en tiempo real** que ejecuta el algoritmo genético de planificación de forma **incremental**, expandiendo la ventana temporal progresivamente y enviando resultados mediante **Server-Sent Events (SSE)**.

---

## 🔄 Concepto: Ventana Temporal Incremental

### **Funcionamiento Base**

```
Usuario envía:
  - fecha: "2025-01-15"
  - saltoMinutos: 5 (Sa)

Simulación ejecuta:

Tick 1:  Algoritmo planifica [0-5 min]     → Enviar por SSE
         ⏱️ Esperar 1 segundo
         
Tick 2:  Algoritmo planifica [0-10 min]    → Enviar por SSE
         ⏱️ Esperar 1 segundo
         
Tick 3:  Algoritmo planifica [0-15 min]    → Enviar por SSE
         ⏱️ Esperar 1 segundo
         
Tick 4:  Algoritmo planifica [0-20 min]    → Enviar por SSE
         ...
         
Tick N:  Algoritmo planifica [0-1440 min]  → Completado (24 horas)
```

### **Ventaja Clave**

✅ **Frontend ve la planificación evolucionando en tiempo real**  
✅ **El mapa se actualiza cada segundo con nuevas rutas**  
✅ **Usuario puede detener/pausar/reanudar la simulación**

---

## 🏗️ Arquitectura Completa

```
┌─────────────┐                    ┌──────────────────────────────────────┐
│   Frontend  │                    │           Backend                    │
└─────────────┘                    └──────────────────────────────────────┘
       │                                        │
       │ POST /iniciar                         │
       │ { fecha, saltoMinutos }               │
       ├──────────────────────────────────────>│ SimulacionOrchestrator
       │                                        │ ├─ Guardar estado inicial
       │ 200 OK                                 │ ├─ Crear thread simulación
       │<───────────────────────────────────────┤ └─ Retornar confirmación
       │                                        │
       │ GET /stream (SSE)                     │
       ├──────────────────────────────────────>│ Registrar cliente SSE
       │                                        │
       │<═══════════════════════════════════════│ Loop infinito (cada 1 seg):
       │ Event: tick_1                          │ ├─ minutoActual += saltoMinutos
       │ { minuto: 5, response: {...} }        │ ├─ Ejecutar algoritmo [0-minutoActual]
       │                                        │ ├─ Broadcast a todos los clientes
       │<═══════════════════════════════════════│ └─ Dormir 1 segundo
       │ Event: tick_2                          │
       │ { minuto: 10, response: {...} }       │
       │                                        │
       │<═══════════════════════════════════════│
       │ Event: tick_3                          │
       │ { minuto: 15, response: {...} }       │
       │                                        │
       │ POST /detener                          │
       ├──────────────────────────────────────>│ Detener simulación
       │ 200 OK                                 │ ├─ activa = false
       │<───────────────────────────────────────┤ └─ Interrupt thread
```

---

## 📂 Estructura de Archivos (Nuevos)

```
src/main/java/com/proyecto/backend/planificador/semanal/
│
├── controller/
│   ├── PlanificacionController.java          # ✅ Ya existe
│   └── SimulacionController.java             # 🆕 NUEVO (API REST + SSE)
│
├── service/
│   ├── AlgoritmoGeneticoService.java         # ✅ Ya existe
│   ├── WorldCacheService.java                # ✅ Ya existe
│   └── SimulacionOrchestrator.java           # 🆕 NUEVO (Motor del loop)
│
└── dto/
    └── sse/
        ├── SimulacionEstadoDTO.java          # 🆕 Estado actual
        └── EventoTickDTO.java                # 🆕 Datos enviados por SSE
```

---

## 🔧 Implementación Detallada

### **1. SimulacionOrchestrator.java** (Motor del Loop)

```java
package com.proyecto.backend.planificador.semanal.service;

import com.proyecto.backend.planificador.semanal.dto.request.PlanificacionRequest;
import com.proyecto.backend.planificador.semanal.dto.response.PlanificacionResponse;
import com.proyecto.backend.planificador.semanal.dto.sse.EventoTickDTO;
import com.proyecto.backend.planificador.semanal.dto.sse.SimulacionEstadoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    private int minutoActual = 0;
    private int saltoMinutos = 5;  // Sa (step algorithm)
    private int tickActual = 0;
    
    // Configuración
    private static final long INTERVALO_TICK_MS = 1000;  // 1 segundo entre ticks
    private static final int LIMITE_MINUTOS = 1440;      // 24 horas
    
    /**
     * Inicia la simulación incremental
     * 
     * @param fecha Fecha de inicio de la planificación
     * @param saltoMinutos Incremento en minutos por cada tick (Sa)
     */
    public void iniciarSimulacion(LocalDate fecha, int saltoMinutos) {
        if (activa) {
            throw new IllegalStateException("Ya hay una simulación activa");
        }
        
        // Inicializar estado
        this.fechaInicio = fecha;
        this.saltoMinutos = saltoMinutos;
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
        
        log.info("🚀 Simulación iniciada: fecha={}, saltoMinutos={}", fecha, saltoMinutos);
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
            
            // 4. Crear evento para SSE
            EventoTickDTO evento = EventoTickDTO.builder()
                .tick(tickActual)
                .minutoActual(minutoActual)
                .saltoMinutos(saltoMinutos)
                .fechaInicio(fechaInicio)
                .planificacion(response)
                .tiempoEjecucionMs(duracionTick)
                .build();
            
            // 5. Broadcast a todos los clientes conectados
            broadcastEvento(evento);
            
            log.info("📤 Tick {} completado en {}ms - {} clientes notificados", 
                tickActual, duracionTick, emitters.size());
            
            // 6. Esperar antes del siguiente tick
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
        params.setTamanioPoblacion(50);
        params.setMaxGeneraciones(200);
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
    public SseEmitter registrarCliente