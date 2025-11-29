# 📋 CÓDIGO LISTO PARA COPIAR - Quick Reference

Este archivo contiene bloques de código listos para copiar directamente.

---

## 🔧 Backend - Java

### 1. SimulationController.java

```java
package com.proyecto.backend.simulation.controller;

import com.proyecto.backend.simulation.dto.*;
import com.proyecto.backend.simulation.service.SimulationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/simulations")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    public ResponseEntity<SimulationStartResponse> start(
            @RequestBody SimulationStartRequest request) {
        SimulationStartResponse response = simulationService.startSimulation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{simulationId}/status")
    public SimulationStatus getStatus(@PathVariable String simulationId) {
        return simulationService.getStatus(UUID.fromString(simulationId));
    }

    @DeleteMapping("/{simulationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String simulationId) {
        simulationService.cancel(UUID.fromString(simulationId));
    }
}
```

---

### 2. DTOs - Records Java

**SimulationMessage.java**
```java
package com.proyecto.backend.simulation.dto;

public record SimulationMessage(
    String simulationId,
    SimulationMessageType type,
    SimulationSnapshot snapshot,
    String error
) {
    public static SimulationMessage progress(String simulationId, SimulationSnapshot snapshot) {
        return new SimulationMessage(simulationId, SimulationMessageType.PROGRESS, snapshot, null);
    }

    public static SimulationMessage completed(String simulationId, SimulationSnapshot snapshot) {
        return new SimulationMessage(simulationId, SimulationMessageType.COMPLETED, snapshot, null);
    }

    public static SimulationMessage error(String simulationId, String error) {
        return new SimulationMessage(simulationId, SimulationMessageType.ERROR, null, error);
    }
}
```

**SimulationMessageType.java**
```java
package com.proyecto.backend.simulation.dto;

public enum SimulationMessageType {
    PROGRESS,
    COMPLETED,
    ERROR
}
```

**SimulationSnapshot.java**
```java
package com.proyecto.backend.simulation.dto;

import java.time.Instant;
import java.util.List;

public record SimulationSnapshot(
    String simulationId,
    int processedOrders,
    int totalOrders,
    double fitness,
    Instant generatedAt,
    List<SimulationOrderPlan> orderPlans
) {}
```

**SimulationOrderPlan.java**
```java
package com.proyecto.backend.simulation.dto;

import java.util.List;

public record SimulationOrderPlan(
    String orderId,
    long slackMinutes,
    List<SimulationRoute> routes
) {}
```

**SimulationRoute.java**
```java
package com.proyecto.backend.simulation.dto;

import java.util.List;

public record SimulationRoute(
    int quantity,
    long slackMinutes,
    List<SimulationSegment> segments
) {}
```

**SimulationSegment.java**
```java
package com.proyecto.backend.simulation.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SimulationSegment(
    String flightId,
    String origin,
    String destination,
    LocalDate date,
    int quantity,
    Instant departureUtc,
    Instant arrivalUtc
) {}
```

**SimulationStartRequest.java**
```java
package com.proyecto.backend.simulation.dto;

import java.time.LocalDateTime;

public record SimulationStartRequest(
    LocalDateTime startDate,
    LocalDateTime endDate,
    Integer windowMinutes,
    Integer factorK,
    Integer tamanioPoblacion,
    Integer maxGeneraciones
) {}
```

**SimulationStartResponse.java**
```java
package com.proyecto.backend.simulation.dto;

public record SimulationStartResponse(
    String simulationId
) {}
```

**SimulationStatus.java**
```java
package com.proyecto.backend.simulation.dto;

public record SimulationStatus(
    String simulationId,
    int processedOrders,
    int totalOrders,
    boolean completed,
    boolean cancelled,
    String error,
    SimulationSnapshot lastSnapshot
) {}
```

---

### 3. SimulationService.java (Esqueleto)

```java
package com.proyecto.backend.simulation.service;

import com.proyecto.backend.simulation.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class SimulationService {

    private static final String TOPIC_PREFIX = "/topic/simulations/";
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<UUID, SimulationSession> sessions = new ConcurrentHashMap<>();

    public SimulationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public SimulationStartResponse startSimulation(SimulationStartRequest request) {
        UUID simulationId = UUID.randomUUID();
        log.info("[SIM:{}] Iniciando simulación", simulationId);
        
        SimulationSession session = new SimulationSession(simulationId);
        sessions.put(simulationId, session);
        
        executorService.submit(() -> runSimulation(session, request));
        
        return new SimulationStartResponse(simulationId.toString());
    }

    public SimulationStatus getStatus(UUID simulationId) {
        SimulationSession session = sessions.get(simulationId);
        if (session == null) {
            throw new RuntimeException("Simulación no encontrada: " + simulationId);
        }
        return session.toStatus();
    }

    public void cancel(UUID simulationId) {
        SimulationSession session = sessions.get(simulationId);
        if (session == null) {
            throw new RuntimeException("Simulación no encontrada: " + simulationId);
        }
        session.cancel();
        log.info("[SIM:{}] Simulación cancelada", simulationId);
    }

    private void runSimulation(SimulationSession session, SimulationStartRequest request) {
        try {
            log.info("[SIM:{}] Ejecutando algoritmo genético...", session.id);
            
            // TODO: INTEGRAR TU ALGORITMO GENÉTICO AQUÍ
            
            for (int gen = 1; gen <= 10; gen++) {
                if (session.cancelled.get()) {
                    log.warn("[SIM:{}] Simulación cancelada", session.id);
                    break;
                }
                
                Thread.sleep(1000);
                
                SimulationSnapshot snapshot = new SimulationSnapshot(
                    session.id.toString(),
                    gen, 10,
                    Math.random() * 100,
                    Instant.now(),
                    List.of()  // TODO: Tus order plans reales
                );
                
                messagingTemplate.convertAndSend(
                    topic(session.id),
                    SimulationMessage.progress(session.id.toString(), snapshot)
                );
            }
            
            session.complete();
            SimulationSnapshot finalSnapshot = createFinalSnapshot(session.id);
            
            messagingTemplate.convertAndSend(
                topic(session.id),
                SimulationMessage.completed(session.id.toString(), finalSnapshot)
            );
            
            log.info("[SIM:{}] ✅ Completada", session.id);
            
        } catch (Exception ex) {
            session.error(ex.getMessage());
            log.error("[SIM:{}] ❌ Error: {}", session.id, ex.getMessage(), ex);
            
            messagingTemplate.convertAndSend(
                topic(session.id),
                SimulationMessage.error(session.id.toString(), ex.getMessage())
            );
        }
    }

    private String topic(UUID simulationId) {
        return TOPIC_PREFIX + simulationId;
    }
    
    private SimulationSnapshot createFinalSnapshot(UUID simulationId) {
        return new SimulationSnapshot(
            simulationId.toString(),
            10, 10, 95.5,
            Instant.now(),
            List.of()  // TODO: Datos reales
        );
    }

    private static class SimulationSession {
        private final UUID id;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile boolean completed = false;
        private volatile String error = null;

        SimulationSession(UUID id) {
            this.id = id;
        }

        void cancel() { this.cancelled.set(true); }
        void complete() { this.completed = true; }
        void error(String message) { this.error = message; }

        SimulationStatus toStatus() {
            return new SimulationStatus(
                id.toString(), 0, 0,
                completed, cancelled.get(),
                error, null
            );
        }
    }
}
```

---

## 🎨 Frontend - JavaScript/React

### 4. useSimulacion.js (Hook completo)

```javascript
import { useState, useEffect, useCallback, useMemo } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BACKEND_URL = 'http://localhost:8000';
const WS_ENDPOINT = `${BACKEND_URL}/ws`;
const TOPIC_PREFIX = '/topic/simulations/';
const DEFAULT_SPEED = 1000;

export const useSimulacion = () => {
  const [simulationId, setSimulationId] = useState(null);
  const [stompClient, setStompClient] = useState(null);
  const [status, setStatus] = useState('idle');
  const [latestProgress, setLatestProgress] = useState(null);
  const [finalSnapshot, setFinalSnapshot] = useState(null);
  const [visibleSnapshot, setVisibleSnapshot] = useState(null);
  const [hasSnapshots, setHasSnapshots] = useState(false);
  const [tiempoSimulado, setTiempoSimulado] = useState(null);
  const [simSpeed, setSimSpeed] = useState(DEFAULT_SPEED);
  const [animPaused, setAnimPaused] = useState(false);
  const [segmentosVuelo, setSegmentosVuelo] = useState(new Map());

  // WebSocket Connection
  useEffect(() => {
    if (!simulationId) return;

    console.log(`[WS] Conectando a: ${simulationId}`);

    const socket = new SockJS(WS_ENDPOINT);
    const client = new Client({
      webSocketFactory: () => socket,
      debug: (str) => {
        if (str.includes('ERROR') || str.includes('CONNECTED')) {
          console.log('[STOMP]', str);
        }
      },
      reconnectDelay: 5000,
      
      onConnect: () => {
        console.log('✅ WebSocket Conectado');
        setStatus('running');
        
        const topic = `${TOPIC_PREFIX}${simulationId}`;
        client.subscribe(topic, (message) => {
          try {
            const simMessage = JSON.parse(message.body);
            console.log('[WS] Mensaje:', simMessage);
            
            if (simMessage.snapshot) {
              setLatestProgress(simMessage.snapshot);
              setVisibleSnapshot(simMessage.snapshot);
              setHasSnapshots(true);
            }
            
            if (simMessage.type === 'COMPLETED') {
              console.log('🎉 Completada');
              setStatus('completed');
              if (simMessage.snapshot) {
                setFinalSnapshot(simMessage.snapshot);
              }
            } else if (simMessage.type === 'ERROR') {
              console.error('❌ Error:', simMessage.error);
              setStatus('error');
              client.deactivate();
            }
          } catch (error) {
            console.error('[WS] Parse error:', error);
          }
        });
      },
      
      onStompError: (frame) => {
        console.error('❌ STOMP Error:', frame);
        setStatus('error');
      },
      
      onDisconnect: () => {
        console.log('🔌 Desconectado');
        setStatus(prev => prev === 'completed' ? 'completed' : 'idle');
      }
    });
    
    setStompClient(client);
    client.activate();
    
    return () => {
      client.deactivate();
      setStompClient(null);
    };
  }, [simulationId]);

  // Iniciar simulación
  const iniciar = useCallback(async (params) => {
    try {
      console.log('[SIM] Iniciando:', params);
      
      setSegmentosVuelo(new Map());
      setHasSnapshots(false);
      setAnimPaused(false);
      setLatestProgress(null);
      setFinalSnapshot(null);
      setVisibleSnapshot(null);
      setStatus('idle');
      
      const response = await fetch(`${BACKEND_URL}/api/simulations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(params)
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      
      const data = await response.json();
      console.log('[SIM] ✅ Iniciada:', data.simulationId);
      setSimulationId(data.simulationId);
      
    } catch (error) {
      console.error('[SIM] ❌ Error:', error);
      setStatus('error');
      throw error;
    }
  }, []);

  // Pausar/reanudar
  const pausar = useCallback(() => {
    setAnimPaused(prev => !prev);
  }, []);

  // Terminar
  const terminar = useCallback(async () => {
    if (simulationId) {
      try {
        await fetch(`${BACKEND_URL}/api/simulations/${simulationId}`, {
          method: 'DELETE'
        });
      } catch (error) {
        console.error('[SIM] Error al cancelar:', error);
      }
    }
    
    stompClient?.deactivate();
    setSimulationId(null);
    setStompClient(null);
    setLatestProgress(null);
    setFinalSnapshot(null);
    setVisibleSnapshot(null);
    setSegmentosVuelo(new Map());
    setStatus('idle');
    setTiempoSimulado(null);
    setHasSnapshots(false);
    setAnimPaused(false);
  }, [stompClient, simulationId]);

  // KPIs
  const kpis = useMemo(() => {
    const snapshot = visibleSnapshot || finalSnapshot || latestProgress;
    if (!snapshot?.orderPlans) return { entregas: 0, retrasados: 0 };
    
    return {
      entregas: snapshot.orderPlans.filter(p => p.slackMinutes > 0).length,
      retrasados: snapshot.orderPlans.filter(p => p.slackMinutes <= 0).length
    };
  }, [visibleSnapshot, finalSnapshot, latestProgress]);

  // Reloj
  const reloj = useMemo(() => {
    const snapshot = latestProgress || finalSnapshot;
    if (!snapshot) return '0 / 0 Órdenes';
    return `${snapshot.processedOrders} / ${snapshot.totalOrders} Órdenes`;
  }, [latestProgress, finalSnapshot]);

  return {
    isLoading: false,
    isStarting: status === 'idle' && simulationId !== null,
    isError: status === 'error',
    estaActivo: status === 'running',
    estaVisualizando: hasSnapshots,
    status,
    snapshotFinal: finalSnapshot,
    snapshotVisible: visibleSnapshot,
    snapshotProgreso: latestProgress,
    tiempoSimulado,
    hasSnapshots,
    kpis,
    reloj,
    simSpeed,
    setSimSpeed,
    iniciar,
    pausar,
    terminar
  };
};
```

---

### 5. SimuladorSemanal.js (Ejemplo de uso)

```javascript
import React, { useState } from 'react';
import { useSimulacion } from '../../../hooks/useSimulacion';

const SimuladorSemanal = () => {
  const {
    kpis,
    reloj,
    isLoading,
    isStarting,
    isError,
    estaActivo,
    estaVisualizando,
    iniciar,
    pausar,
    terminar,
    snapshotVisible,
    hasSnapshots,
    tiempoSimulado,
    simSpeed,
    setSimSpeed,
    status
  } = useSimulacion();

  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');

  const handleIniciarSimulacion = async () => {
    if (!fechaInicio) {
      alert('Selecciona una fecha de inicio');
      return;
    }

    try {
      await iniciar({
        startDate: fechaInicio,
        endDate: fechaFin || undefined,
        windowMinutes: 5,
        factorK: 5,
        tamanioPoblacion: 10,
        maxGeneraciones: 10
      });
      
      console.log('✅ Simulación iniciada');
    } catch (error) {
      console.error('❌ Error:', error);
      alert(`Error: ${error.message}`);
    }
  };

  if (isLoading) {
    return <div>Cargando datos base...</div>;
  }

  if (isError) {
    return <div className="error">Error al cargar datos.</div>;
  }

  return (
    <div className="simulador-container">
      {/* Panel de Control */}
      <div className="panel-control">
        <h1>Simulación Semanal</h1>
        
        <div className="controles">
          <input
            type="datetime-local"
            value={fechaInicio}
            onChange={(e) => setFechaInicio(e.target.value)}
            disabled={estaActivo}
          />
          
          <input
            type="datetime-local"
            value={fechaFin}
            onChange={(e) => setFechaFin(e.target.value)}
            disabled={estaActivo}
            placeholder="Opcional"
          />
          
          <button 
            onClick={handleIniciarSimulacion}
            disabled={estaActivo || isStarting}
          >
            {isStarting ? 'Iniciando...' : 'Iniciar Simulación'}
          </button>
          
          <button 
            onClick={pausar} 
            disabled={!estaActivo && !estaVisualizando}
          >
            Pausar
          </button>
          
          <button onClick={terminar}>
            Terminar
          </button>
        </div>
        
        {/* KPIs */}
        <div className="kpis">
          <div>✅ A tiempo: <strong>{kpis.entregas}</strong></div>
          <div>⚠️ Retrasados: <strong>{kpis.retrasados}</strong></div>
          <div>📊 Progreso: <strong>{reloj}</strong></div>
        </div>
        
        {/* Velocidad */}
        <div className="velocidad">
          <label>Velocidad de simulación (x):</label>
          <input
            type="number"
            min={1}
            max={10000}
            value={simSpeed}
            onChange={(e) => setSimSpeed(Number(e.target.value))}
          />
        </div>
        
        {/* Tiempo Simulado */}
        {tiempoSimulado && (
          <div className="tiempo-simulado">
            <span>
              📅 {tiempoSimulado.toLocaleDateString()}
            </span>
            <span>
              🕒 {tiempoSimulado.toLocaleTimeString()}
            </span>
          </div>
        )}
      </div>
      
      {/* Mapa */}
      <div className="mapa-container">
        {/* Aquí va tu componente de mapa */}
        {snapshotVisible && (
          <div>
            <h3>Snapshot Actual</h3>
            <pre>{JSON.stringify(snapshotVisible, null, 2)}</pre>
          </div>
        )}
      </div>
    </div>
  );
};

export default SimuladorSemanal;
```

---

## 🚀 Comandos Quick Start

### Backend

```bash
# Navegar al backend
cd backend

# Compilar (verifica que todo esté correcto)
mvn clean compile

# Ejecutar
mvn spring-boot:run
```

### Frontend

```bash
# Navegar al frontend
cd front

# Instalar dependencias (si no lo has hecho)
npm install @stomp/stompjs sockjs-client

# Iniciar desarrollo
npm start
```

---

## ✅ Checklist de Implementación

### Backend
```
[ ] Crear SimulationController.java
[ ] Crear todos los DTOs (9 archivos)
[ ] Refactorizar SimulationService.java
[ ] Verificar WebSocketConfig.java
[ ] mvn clean compile → Sin errores
[ ] mvn spring-boot:run → Servidor en puerto 8000
```

### Frontend
```
[ ] Crear hooks/useSimulacion.js
[ ] Actualizar SimuladorSemanal.js
[ ] Eliminar config/websocket.js
[ ] Eliminar services/PlanificacionService.js
[ ] npm install (dependencias)
[ ] npm start → Sin errores
[ ] Abrir http://localhost:3000
```

### Testing
```
[ ] Backend logs muestran [SIM:uuid]
[ ] Frontend conecta WebSocket
[ ] Mensajes se reciben en consola
[ ] KPIs se actualizan
[ ] Botón Terminar funciona
[ ] Sin errores en consola
```

---

**¡Copia, pega y ejecuta!** 🚀

Todos estos bloques están listos para usar directamente.
