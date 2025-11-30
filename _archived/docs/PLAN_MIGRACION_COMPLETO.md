# 🎯 PLAN DE MIGRACIÓN COMPLETO - Backend y Frontend

## 📋 Resumen Ejecutivo

Migrarás tu proyecto actual para usar el patrón de **CODIGO_APARTE**:
- ✅ Backend: REST API + WebSocket STOMP con `SimpMessagingTemplate`
- ✅ Frontend: Hook `useSimulacion` centralizado
- ✅ Eliminar código duplicado y obsoleto
- ✅ Visualización solo en React (no más HTML standalone)

---

## 🔧 FASE 1: Backend - Refactorización

### 1.1 Crear SimulationController (Nuevo)

**Archivo:** `backend/src/main/java/com/proyecto/backend/simulation/controller/SimulationController.java`

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

    /**
     * Inicia una nueva simulación
     * POST /api/simulations
     */
    @PostMapping
    public ResponseEntity<SimulationStartResponse> start(
            @RequestBody SimulationStartRequest request) {
        SimulationStartResponse response = simulationService.startSimulation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Obtiene el estado actual de una simulación
     * GET /api/simulations/{simulationId}/status
     */
    @GetMapping("/{simulationId}/status")
    public SimulationStatus getStatus(@PathVariable String simulationId) {
        return simulationService.getStatus(UUID.fromString(simulationId));
    }

    /**
     * Cancela una simulación en curso
     * DELETE /api/simulations/{simulationId}
     */
    @DeleteMapping("/{simulationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String simulationId) {
        simulationService.cancel(UUID.fromString(simulationId));
    }
}
```

---

### 1.2 Refactorizar SimulationService

**Archivo:** `backend/src/main/java/com/proyecto/backend/simulation/service/SimulationService.java`

**CAMBIOS CLAVE:**

```java
package com.proyecto.backend.simulation.service;

import com.proyecto.backend.simulation.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;  // ← CLAVE
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class SimulationService {

    private static final String TOPIC_PREFIX = "/topic/simulations/";
    
    private final SimpMessagingTemplate messagingTemplate;  // ← AGREGAR
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<UUID, SimulationSession> sessions = new ConcurrentHashMap<>();

    // CONSTRUCTOR
    public SimulationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Inicia una nueva simulación
     */
    public SimulationStartResponse startSimulation(SimulationStartRequest request) {
        UUID simulationId = UUID.randomUUID();
        
        log.info("[SIM:{}] Iniciando simulación con parámetros: {}", simulationId, request);
        
        SimulationSession session = new SimulationSession(simulationId);
        sessions.put(simulationId, session);
        
        // Ejecutar en hilo separado
        executorService.submit(() -> runSimulation(session, request));
        
        return new SimulationStartResponse(simulationId.toString());
    }

    /**
     * Obtiene el estado de una simulación
     */
    public SimulationStatus getStatus(UUID simulationId) {
        SimulationSession session = sessions.get(simulationId);
        if (session == null) {
            throw new RuntimeException("Simulación no encontrada: " + simulationId);
        }
        return session.toStatus();
    }

    /**
     * Cancela una simulación
     */
    public void cancel(UUID simulationId) {
        SimulationSession session = sessions.get(simulationId);
        if (session == null) {
            throw new RuntimeException("Simulación no encontrada: " + simulationId);
        }
        session.cancel();
        log.info("[SIM:{}] Simulación cancelada", simulationId);
    }

    /**
     * Ejecuta la simulación (lógica principal)
     */
    private void runSimulation(SimulationSession session, SimulationStartRequest request) {
        try {
            log.info("[SIM:{}] Ejecutando algoritmo genético...", session.id);
            
            // TODO: Aquí va tu lógica de Algoritmo Genético
            // Por cada generación/progreso:
            
            for (int gen = 1; gen <= 10; gen++) {  // Ejemplo
                
                if (session.cancelled.get()) {
                    log.warn("[SIM:{}] Simulación cancelada en generación {}", session.id, gen);
                    break;
                }
                
                // Simular procesamiento
                Thread.sleep(1000);
                
                // Crear snapshot del progreso
                SimulationSnapshot snapshot = new SimulationSnapshot(
                    session.id.toString(),
                    gen,                    // processedOrders
                    10,                     // totalOrders
                    Math.random() * 100,    // fitness
                    Instant.now(),          // generatedAt
                    createDummyOrderPlans() // orderPlans
                );
                
                // 📤 ENVIAR PROGRESO POR WEBSOCKET
                messagingTemplate.convertAndSend(
                    topic(session.id),
                    SimulationMessage.progress(session.id.toString(), snapshot)
                );
                
                log.debug("[SIM:{}] Progreso enviado: {}/{}", session.id, gen, 10);
            }
            
            // 🎉 SIMULACIÓN COMPLETADA
            session.complete();
            SimulationSnapshot finalSnapshot = createFinalSnapshot(session.id);
            
            messagingTemplate.convertAndSend(
                topic(session.id),
                SimulationMessage.completed(session.id.toString(), finalSnapshot)
            );
            
            log.info("[SIM:{}] ✅ Simulación completada exitosamente", session.id);
            
        } catch (Exception ex) {
            session.error(ex.getMessage());
            log.error("[SIM:{}] ❌ Error en simulación: {}", session.id, ex.getMessage(), ex);
            
            // 📤 ENVIAR ERROR POR WEBSOCKET
            messagingTemplate.convertAndSend(
                topic(session.id),
                SimulationMessage.error(session.id.toString(), ex.getMessage())
            );
        }
    }

    // Helper para crear topic
    private String topic(UUID simulationId) {
        return TOPIC_PREFIX + simulationId;
    }
    
    // Métodos auxiliares...
    private List<SimulationOrderPlan> createDummyOrderPlans() {
        // TODO: Implementar conversión de tus datos reales
        return List.of();
    }
    
    private SimulationSnapshot createFinalSnapshot(UUID simulationId) {
        // TODO: Implementar snapshot final
        return new SimulationSnapshot(
            simulationId.toString(),
            10, 10,
            95.5,
            Instant.now(),
            createDummyOrderPlans()
        );
    }

    // Clase interna para sesión
    private static class SimulationSession {
        private final UUID id;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile boolean completed = false;
        private volatile String error = null;

        SimulationSession(UUID id) {
            this.id = id;
        }

        void cancel() {
            this.cancelled.set(true);
        }

        void complete() {
            this.completed = true;
        }

        void error(String message) {
            this.error = message;
        }

        SimulationStatus toStatus() {
            return new SimulationStatus(
                id.toString(),
                0, 0,  // processed, total
                completed,
                cancelled.get(),
                error,
                null
            );
        }
    }
}
```

---

### 1.3 Crear DTOs (Nuevos)

**Archivo:** `backend/src/main/java/com/proyecto/backend/simulation/dto/SimulationMessage.java`

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

**Archivo:** `backend/src/main/java/com/proyecto/backend/simulation/dto/SimulationMessageType.java`

```java
package com.proyecto.backend.simulation.dto;

public enum SimulationMessageType {
    PROGRESS,
    COMPLETED,
    ERROR
}
```

**Archivo:** `backend/src/main/java/com/proyecto/backend/simulation/dto/SimulationSnapshot.java`

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

**Archivo:** `backend/src/main/java/com/proyecto/backend/simulation/dto/SimulationStartRequest.java`

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

**Archivo:** `backend/src/main/java/com/proyecto/backend/simulation/dto/SimulationStartResponse.java`

```java
package com.proyecto.backend.simulation.dto;

public record SimulationStartResponse(
    String simulationId
) {}
```

**Archivo:** `backend/src/main/java/com/proyecto/backend/simulation/dto/SimulationStatus.java`

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

### 1.4 Mantener WebSocketConfig (Ya lo tienes)

**Archivo:** `backend/src/main/java/com/proyecto/backend/config/WebSocketConfig.java`

```java
package com.proyecto.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

✅ **Este archivo ya debería existir y estar correcto.**

---

### 1.5 ELIMINAR Archivos Obsoletos del Backend

```bash
# Ejecutar en terminal
cd backend/src/main/java/com/proyecto/backend

# OPCIONAL: Eliminar PlanificacionWebSocketHandler (si ya no lo usas)
rm -v websocket/PlanificacionWebSocketHandler.java

# Verificar que no queden referencias
grep -r "PlanificacionWebSocketHandler" .
```

---

## 🎨 FASE 2: Frontend - Refactorización

### 2.1 Crear Hook `useSimulacion` (Nuevo)

**Archivo:** `front/src/hooks/useSimulacion.js`

```javascript
import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BACKEND_URL = 'http://localhost:8000';
const WS_ENDPOINT = `${BACKEND_URL}/ws`;
const TOPIC_PREFIX = '/topic/simulations/';
const DEFAULT_SPEED = 1000; // 1000x velocidad

export const useSimulacion = () => {
  // ========== ESTADO ==========
  const [simulationId, setSimulationId] = useState(null);
  const [stompClient, setStompClient] = useState(null);
  const [status, setStatus] = useState('idle'); // 'idle' | 'running' | 'completed' | 'error'
  
  // Snapshots
  const [latestProgress, setLatestProgress] = useState(null);
  const [finalSnapshot, setFinalSnapshot] = useState(null);
  const [visibleSnapshot, setVisibleSnapshot] = useState(null);
  const [hasSnapshots, setHasSnapshots] = useState(false);
  
  // Tiempo simulado
  const [tiempoSimulado, setTiempoSimulado] = useState(null);
  const [simSpeed, setSimSpeed] = useState(DEFAULT_SPEED);
  const [animPaused, setAnimPaused] = useState(false);
  
  // Vuelos
  const [segmentosVuelo, setSegmentosVuelo] = useState(new Map());
  
  // ========== WEBSOCKET CONNECTION ==========
  useEffect(() => {
    if (!simulationId) return;

    console.log(`[WS] Conectando a simulación: ${simulationId}`);

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
        
        // 📡 SUSCRIBIRSE AL TOPIC
        const topic = `${TOPIC_PREFIX}${simulationId}`;
        console.log(`[WS] Suscribiéndose a: ${topic}`);
        
        client.subscribe(topic, (message) => {
          try {
            const simMessage = JSON.parse(message.body);
            console.log('[WS] Mensaje recibido:', simMessage);
            
            if (simMessage.snapshot) {
              setLatestProgress(simMessage.snapshot);
              setVisibleSnapshot(simMessage.snapshot);
              mergeFlightSegments(simMessage.snapshot);
              setHasSnapshots(true);
            }
            
            if (simMessage.type === 'COMPLETED') {
              console.log('🎉 Simulación completada');
              setStatus('completed');
              if (simMessage.snapshot) {
                setFinalSnapshot(simMessage.snapshot);
              }
            } else if (simMessage.type === 'ERROR') {
              console.error('❌ Error en simulación:', simMessage.error);
              setStatus('error');
              client.deactivate();
            }
          } catch (error) {
            console.error('[WS] Error al parsear mensaje:', error);
          }
        });
      },
      
      onStompError: (frame) => {
        console.error('❌ STOMP Error:', frame);
        setStatus('error');
      },
      
      onWebSocketError: (error) => {
        console.error('❌ WebSocket Error:', error);
        setStatus('error');
      },
      
      onDisconnect: () => {
        console.log('🔌 WebSocket desconectado');
        setStatus(prev => prev === 'completed' ? 'completed' : 'idle');
      }
    });
    
    setStompClient(client);
    client.activate();
    
    return () => {
      console.log('[WS] Limpiando conexión...');
      client.deactivate();
      setStompClient(null);
    };
  }, [simulationId]);
  
  // ========== FUNCIONES ==========
  
  /**
   * Inicia una nueva simulación
   */
  const iniciar = useCallback(async (params) => {
    try {
      console.log('[SIM] Iniciando simulación:', params);
      
      // Resetear estado
      setSegmentosVuelo(new Map());
      setHasSnapshots(false);
      setAnimPaused(false);
      setLatestProgress(null);
      setFinalSnapshot(null);
      setVisibleSnapshot(null);
      setStatus('idle');
      
      // Llamar REST API
      const response = await fetch(`${BACKEND_URL}/api/simulations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          startDate: params.startDate,
          endDate: params.endDate,
          windowMinutes: params.windowMinutes || 5,
          factorK: params.factorK || 5,
          tamanioPoblacion: params.tamanioPoblacion || 10,
          maxGeneraciones: params.maxGeneraciones || 10
        })
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      console.log('[SIM] ✅ Simulación iniciada:', data.simulationId);
      
      // Esto activará el WebSocket en el useEffect
      setSimulationId(data.simulationId);
      
    } catch (error) {
      console.error('[SIM] ❌ Error al iniciar:', error);
      setStatus('error');
      throw error;
    }
  }, []);
  
  /**
   * Pausa/reanuda la animación
   */
  const pausar = useCallback(() => {
    setAnimPaused(prev => !prev);
    console.log('[SIM] Animación', animPaused ? 'reanudada' : 'pausada');
  }, [animPaused]);
  
  /**
   * Termina la simulación actual
   */
  const terminar = useCallback(async () => {
    if (simulationId) {
      try {
        console.log(`[SIM] Cancelando simulación: ${simulationId}`);
        await fetch(`${BACKEND_URL}/api/simulations/${simulationId}`, {
          method: 'DELETE'
        });
        console.log('[SIM] ✅ Simulación cancelada en backend');
      } catch (error) {
        console.error('[SIM] Error al cancelar:', error);
      }
    }
    
    // Resetear estado
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
    
    console.log('[SIM] Estado reseteado');
  }, [stompClient, simulationId]);
  
  // ========== PROCESAMIENTO DE VUELOS ==========
  
  const mergeFlightSegments = useCallback((snapshot) => {
    if (!snapshot?.orderPlans) return;
    
    setSegmentosVuelo(prev => {
      const next = new Map(prev);
      
      snapshot.orderPlans.forEach(plan => {
        plan.routes?.forEach(ruta => {
          ruta.segments?.forEach(segmento => {
            if (!segmento.departureUtc || !segmento.arrivalUtc) return;
            
            // ID único basado en vuelo + salida
            const id = `${segmento.flightId}-${segmento.departureUtc}`;
            
            const existing = next.get(id);
            const existingOrders = existing?.orderIds || [];
            const orderIds = existingOrders.includes(plan.orderId)
              ? existingOrders
              : [...existingOrders, plan.orderId];
            
            next.set(id, {
              id,
              flightId: segmento.flightId,
              origin: segmento.origin,
              destination: segmento.destination,
              departureUtc: segmento.departureUtc,
              arrivalUtc: segmento.arrivalUtc,
              orderIds,
              retrasado: plan.slackMinutes <= 0
            });
          });
        });
      });
      
      return next;
    });
  }, []);
  
  // ========== VUELOS ACTIVOS ==========
  
  const activeSegments = useMemo(() => {
    if (!tiempoSimulado) return [];
    
    const currentMs = tiempoSimulado.getTime();
    
    return Array.from(segmentosVuelo.values()).filter(segmento => {
      const depMs = Date.parse(segmento.departureUtc);
      const arrMs = Date.parse(segmento.arrivalUtc);
      
      if (isNaN(depMs) || isNaN(arrMs)) return false;
      
      // Vuelo está activo si el tiempo actual está entre salida y llegada
      return depMs <= currentMs && currentMs <= arrMs;
    });
  }, [segmentosVuelo, tiempoSimulado]);
  
  // ========== KPIs ==========
  
  const kpis = useMemo(() => {
    const snapshot = visibleSnapshot || finalSnapshot || latestProgress;
    if (!snapshot?.orderPlans) return { entregas: 0, retrasados: 0 };
    
    return {
      entregas: snapshot.orderPlans.filter(p => p.slackMinutes > 0).length,
      retrasados: snapshot.orderPlans.filter(p => p.slackMinutes <= 0).length
    };
  }, [visibleSnapshot, finalSnapshot, latestProgress]);
  
  // ========== RELOJ DE PROGRESO ==========
  
  const reloj = useMemo(() => {
    const snapshot = latestProgress || finalSnapshot;
    if (!snapshot) return '0 / 0 Órdenes';
    return `${snapshot.processedOrders} / ${snapshot.totalOrders} Órdenes`;
  }, [latestProgress, finalSnapshot]);
  
  // ========== RETORNO ==========
  
  return {
    // Estado
    isLoading: false,
    isStarting: status === 'idle' && simulationId !== null,
    isError: status === 'error',
    estaActivo: status === 'running',
    estaVisualizando: hasSnapshots,
    status,
    
    // Datos
    activeSegments,
    snapshotFinal: finalSnapshot,
    snapshotVisible: visibleSnapshot,
    snapshotProgreso: latestProgress,
    tiempoSimulado,
    hasSnapshots,
    
    // KPIs
    kpis,
    reloj,
    
    // Controles
    simSpeed,
    setSimSpeed,
    iniciar,
    pausar,
    terminar
  };
};
```

---

### 2.2 Actualizar SimuladorSemanal.js

**Archivo:** `front/src/pages/simulacion/Simulador/SimuladorSemanal.js`

**CAMBIOS PRINCIPALES:**

```javascript
// ANTES (Múltiples imports)
import { conectarWebSocketPlanificacion } from '../../../config/websocket';
// ... otros imports confusos

// DESPUÉS (Simple y claro)
import { useSimulacion } from '../../../hooks/useSimulacion';

const SimuladorSemanal = () => {
  // ANTES: Múltiples refs y estados confusos
  // const wsPlanificacionRef = useRef(null);
  // const stompClientRef = useRef(null);
  // const [wsConectado, setWsConectado] = useState(false);
  // ... etc

  // DESPUÉS: Un solo hook
  const {
    activeSegments,
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
    snapshotFinal,
    snapshotVisible,
    snapshotProgreso,
    hasSnapshots,
    tiempoSimulado,
    simSpeed,
    setSimSpeed,
    status
  } = useSimulacion();

  // Estado local simple
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');

  // ANTES: Conexión manual compleja
  // useEffect(() => {
  //   const conectarWebSocket = async () => { ... }
  // }, []);

  // DESPUÉS: Ya no necesitas conectar manualmente, el hook lo hace!

  // Iniciar simulación
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

  // Resto del componente...
  return (
    <div className="simulador-container">
      <div className="controls">
        <input
          type="datetime-local"
          value={fechaInicio}
          onChange={(e) => setFechaInicio(e.target.value)}
          disabled={estaActivo}
        />
        
        <button 
          onClick={handleIniciarSimulacion}
          disabled={estaActivo || isStarting}
        >
          {isStarting ? 'Iniciando...' : 'Iniciar Simulación'}
        </button>
        
        <button onClick={pausar} disabled={!estaActivo}>
          Pausar
        </button>
        
        <button onClick={terminar}>
          Terminar
        </button>
        
        <div className="kpis">
          <span>✅ A tiempo: {kpis.entregas}</span>
          <span>⚠️ Retrasados: {kpis.retrasados}</span>
          <span>{reloj}</span>
        </div>
      </div>
      
      <MapaVuelos 
        activeSegments={activeSegments}
        tiempoSimulado={tiempoSimulado}
      />
    </div>
  );
};
```

---

### 2.3 ELIMINAR Archivos Obsoletos del Frontend

```bash
# Ejecutar en terminal
cd front/src

# ELIMINAR websocket.js obsoleto
rm -v config/websocket.js

# ELIMINAR PlanificacionService.js duplicado
rm -v services/PlanificacionService.js

# Verificar que no queden referencias
grep -r "websocket.js" .
grep -r "PlanificacionService" .
```

---

## ✅ FASE 3: Verificación

### 3.1 Compilar Backend

```bash
cd backend
mvn clean compile

# Si hay errores, revisa:
# 1. Que todos los DTOs existan
# 2. Que SimulationService tenga SimpMessagingTemplate
# 3. Que SimulationController esté bien configurado
```

### 3.2 Compilar Frontend

```bash
cd front
npm install  # Asegúrate de tener @stomp/stompjs y sockjs-client
npm run build

# Si hay errores, revisa:
# 1. Que useSimulacion.js esté bien creado
# 2. Que SimuladorSemanal.js importe el hook
# 3. Que no queden referencias a websocket.js
```

### 3.3 Probar el Flujo

```bash
# Terminal 1: Backend
cd backend
mvn spring-boot:run

# Terminal 2: Frontend
cd front
npm start

# Navegador:
# 1. Ve a http://localhost:3000
# 2. Selecciona fecha de inicio
# 3. Click en "Iniciar Simulación"
# 4. Verifica en consola del navegador:
#    - ✅ WebSocket conectado
#    - 📡 Suscrito a /topic/simulations/{uuid}
#    - 📥 Mensajes recibidos
# 5. Verifica en consola del backend:
#    - [SIM:uuid] logs del progreso
```

---

## 📋 Checklist Final

### Backend
- [ ] `SimulationController.java` creado
- [ ] `SimulationService.java` refactorizado con `SimpMessagingTemplate`
- [ ] DTOs creados: `SimulationMessage`, `SimulationSnapshot`, etc.
- [ ] `WebSocketConfig.java` correcto
- [ ] `PlanificacionWebSocketHandler.java` eliminado (opcional)
- [ ] Backend compila sin errores
- [ ] Puerto 8000 disponible

### Frontend
- [ ] Hook `useSimulacion.js` creado
- [ ] `SimuladorSemanal.js` actualizado para usar el hook
- [ ] `websocket.js` eliminado
- [ ] `PlanificacionService.js` eliminado
- [ ] Frontend compila sin errores
- [ ] Dependencias instaladas: `@stomp/stompjs`, `sockjs-client`

### Testing
- [ ] Backend se inicia correctamente
- [ ] Frontend se inicia correctamente
- [ ] WebSocket se conecta al iniciar simulación
- [ ] Mensajes de progreso se reciben
- [ ] Mapa muestra vuelos en tiempo real
- [ ] Botón "Terminar" cancela correctamente
- [ ] No hay errores en consola

---

## 🚀 Próximos Pasos

1. **Integrar tu Algoritmo Genético Real**
   - Reemplazar el código dummy en `SimulationService.runSimulation()`
   - Crear snapshots reales con tus datos de vuelos

2. **Mejorar Visualización**
   - Añadir animación interpolada de aviones
   - Mostrar rutas en el mapa
   - Tooltips con información de vuelos

3. **Optimizaciones**
   - Ajustar velocidad de simulación
   - Implementar pausar/reanudar
   - Guardar historial de simulaciones

---

## 📚 Documentación de Referencia

- **Spring WebSocket STOMP**: https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html
- **@stomp/stompjs**: https://stomp-js.github.io/stomp-websocket/
- **SockJS**: https://github.com/sockjs/sockjs-client

---

**¿LISTO PARA EMPEZAR?** 🚀

Comienza con la **FASE 1.1** creando el `SimulationController` y avanza paso a paso.
