# 📊 Arquitectura del Sistema de Simulación

## 🎯 Vista General

```
┌────────────────────────────────────────────────────────────────────┐
│                         CAPA DE PRESENTACIÓN                        │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐            │
│  │  React App   │  │   Vue.js     │  │  HTML/JS     │            │
│  │              │  │   Cliente    │  │   Cliente    │            │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘            │
│         │                  │                  │                     │
│         └──────────────────┴──────────────────┘                     │
│                            │                                        │
│                    SockJS + STOMP.js                               │
└────────────────────────────┼──────────────────────────────────────┘
                             │
                             │ WebSocket/HTTP
                             │
┌────────────────────────────┼──────────────────────────────────────┐
│                    CAPA DE APLICACIÓN                              │
│                            ▼                                        │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │              Spring Boot WebSocket Gateway                  │   │
│  │                                                             │   │
│  │  ┌──────────────────┐         ┌────────────────────────┐  │   │
│  │  │ WebSocketConfig  │◄────────┤ SimpMessagingTemplate  │  │   │
│  │  │  - /ws endpoint  │         │  - STOMP broadcaster   │  │   │
│  │  │  - /topic broker │         └────────────────────────┘  │   │
│  │  └──────────────────┘                                      │   │
│  └────────────────────────────────────────────────────────────┘   │
│                            │                                        │
│                            ▼                                        │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │            SimulationController (REST API)                  │   │
│  │                                                             │   │
│  │  POST /api/simulations/start          → Inicia simulación  │   │
│  │  POST /api/simulations/{id}/cancel    → Cancela simulación │   │
│  │  POST /api/simulations/{id}/pause     → Pausa simulación   │   │
│  │  POST /api/simulations/{id}/resume    → Reanuda simulación │   │
│  │  GET  /api/simulations/{id}           → Estado actual      │   │
│  │  GET  /api/simulations                → Lista sesiones     │   │
│  └─────────────────────┬──────────────────────────────────────┘   │
│                        │                                            │
│                        ▼                                            │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                  SimulationService                          │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────┐  │   │
│  │  │ ConcurrentHashMap<UUID, SimulationSession>          │  │   │
│  │  │  - Gestión de múltiples simulaciones simultáneas    │  │   │
│  │  │  - Thread-safe state management                     │  │   │
│  │  └─────────────────────────────────────────────────────┘  │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────┐  │   │
│  │  │ ExecutorService (Virtual Threads - Java 21)         │  │   │
│  │  │  - newVirtualThreadPerTaskExecutor()                │  │   │
│  │  │  - Alta concurrencia sin overhead                   │  │   │
│  │  └─────────────────────────────────────────────────────┘  │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────┐  │   │
│  │  │ @Scheduled Cleanup (cada hora)                      │  │   │
│  │  │  - Elimina sesiones > 2 horas                       │  │   │
│  │  │  - Previene Memory Leaks                            │  │   │
│  │  └─────────────────────────────────────────────────────┘  │   │
│  └─────────────────────┬──────────────────────────────────────┘   │
│                        │                                            │
│                        ▼                                            │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                  SimulationSession                          │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────┐  │   │
│  │  │ Estado Atómico (Thread-Safe)                        │  │   │
│  │  │  - AtomicBoolean running/paused/completed           │  │   │
│  │  │  - AtomicInteger currentIteration/processedOrders   │  │   │
│  │  │  - AtomicReference<SimulationSnapshot>              │  │   │
│  │  │  - AtomicReference<Double> fitness                  │  │   │
│  │  └─────────────────────────────────────────────────────┘  │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────┐  │   │
│  │  │ Control de Ciclo de Vida                            │  │   │
│  │  │  - start() / pause() / resume() / cancel()          │  │   │
│  │  │  - complete() / error()                             │  │   │
│  │  └─────────────────────────────────────────────────────┘  │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────┐  │   │
│  │  │ Throttling (500ms)                                  │  │   │
│  │  │  - shouldSendUpdate()                               │  │   │
│  │  │  - Evita saturar el frontend                        │  │   │
│  │  └─────────────────────────────────────────────────────┘  │   │
│  └─────────────────────┬──────────────────────────────────────┘   │
│                        │                                            │
└────────────────────────┼────────────────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────────────┐
│                    CAPA DE LÓGICA DE NEGOCIO                        │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │              GeneticAlgorithm (Interfaz)                    │   │
│  │                                                             │   │
│  │  execute(GAContext) → GAResult                             │   │
│  │                                                             │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │   │
│  │  │ GAContext    │  │ GAResult     │  │ GAMetrics    │    │   │
│  │  │ - orders     │  │ - routes     │  │ - gens       │    │   │
│  │  │ - flights    │  │ - fitness    │  │ - time       │    │   │
│  │  │ - config     │  │ - metrics    │  │ - evals      │    │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘    │   │
│  └─────────────────────┬──────────────────────────────────────┘   │
│                        │                                            │
│                        ▼                                            │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │         GeneticAlgorithmImpl (Implementación)               │   │
│  │                                                             │   │
│  │  1. Inicializar población                                  │   │
│  │  2. Evaluar fitness                                        │   │
│  │  3. Selección (torneo/ruleta)                              │   │
│  │  4. Crossover (OX, PMX, etc.)                              │   │
│  │  5. Mutación (swap, insert, etc.)                          │   │
│  │  6. Reemplazo generacional                                 │   │
│  │  7. Criterio de parada                                     │   │
│  └─────────────────────┬──────────────────────────────────────┘   │
└────────────────────────┼────────────────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────────────┐
│                    CAPA DE PERSISTENCIA                             │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                  Spring Data JPA                            │   │
│  │                                                             │   │
│  │  ┌──────────────────┐  ┌────────────────────────────────┐ │   │
│  │  │ PedidoRepository │  │ PlanDeVueloRepository          │ │   │
│  │  │  - findByEstado()│  │  - findAll()                   │ │   │
│  │  └──────────────────┘  │  - findByOrigen()              │ │   │
│  │                        │  - findByDestino()             │ │   │
│  │                        └────────────────────────────────┘ │   │
│  └─────────────────────┬──────────────────────────────────────┘   │
└────────────────────────┼────────────────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────────────┐
│                      BASE DE DATOS                                  │
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐      │
│  │   pedidos      │  │ planesdevuelo  │  │  aeropuertos   │      │
│  │                │  │                │  │                │      │
│  │  - id          │  │  - id          │  │  - codigo_icao │      │
│  │  - anio        │  │  - origen      │  │  - nombre      │      │
│  │  - mes         │  │  - destino     │  │  - latitud     │      │
│  │  - dia         │  │  - hora_salida │  │  - longitud    │      │
│  │  - hora        │  │  - hora_llegada│  │                │      │
│  │  - minuto      │  │  - capacidad   │  │                │      │
│  │  - destino_id  │  │                │  │                │      │
│  │  - cantidad    │  │                │  │                │      │
│  │  - cliente_id  │  │                │  │                │      │
│  │  - estado      │  │                │  │                │      │
│  └────────────────┘  └────────────────┘  └────────────────┘      │
└────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Flujo de Datos Completo

### 1. Inicio de Simulación

```
Usuario (Frontend)
    │
    │ 1. POST /api/simulations/start
    │    { fecha: "2025-01-02", factorK: 5 }
    ▼
SimulationController
    │
    │ 2. startSimulation(request)
    ▼
SimulationService
    │
    ├─► 3. Crear SimulationSession (UUID)
    │
    ├─► 4. Cargar datos de BD (sin bloqueo transaccional)
    │      - PedidoRepository.findByEstado("PENDIENTE")
    │      - PlanDeVueloRepository.findAll()
    │
    ├─► 5. Lanzar Virtual Thread
    │      virtualThreadExecutor.submit(() -> runSimulation())
    │
    └─► 6. Retornar UUID al cliente
         { success: true, sessionId: "123e4567...", 
           subscriptionTopic: "/topic/simulations/123e4567..." }
```

### 2. Ejecución de Simulación (Virtual Thread)

```
SimulationService.runSimulation()
    │
    │ Loop: mientras (running && iteración < máximo)
    │
    ├─► 1. Verificar si está pausado
    │      while (paused) { Thread.sleep(100) }
    │
    ├─► 2. Buscar pedidos en ventana de tiempo
    │      [currentTime, currentTime + Sc]
    │
    ├─► 3. Ejecutar Algoritmo Genético
    │      GAResult = geneticAlgorithm.execute(context)
    │      - Población inicial
    │      - Evolución
    │      - Selección
    │      - Crossover
    │      - Mutación
    │
    ├─► 4. Actualizar métricas de sesión
    │      session.updateMetrics(iteration, processed, fitness)
    │
    ├─► 5. Enviar actualización (con throttling)
    │      if (session.shouldSendUpdate()) {
    │          SimulationSnapshot snapshot = buildSnapshot()
    │          messagingTemplate.convertAndSend(topic, snapshot)
    │      }
    │
    ├─► 6. Avanzar tiempo simulado
    │      currentTime += Sc minutos
    │
    └─► 7. Repetir hasta completar o cancelar
```

### 3. Recepción en Frontend

```
Frontend (STOMP Client)
    │
    │ 1. Conectar WebSocket
    │    new SockJS('http://localhost:8000/ws')
    │
    │ 2. Suscribirse al tópico
    │    stompClient.subscribe('/topic/simulations/123e4567...')
    │
    │ 3. Recibir actualizaciones
    │    onMessage(snapshot) {
    │        - Actualizar métricas UI
    │        - Actualizar progreso
    │        - Renderizar rutas
    │    }
    │
    │ 4. Manejar completado/error
    │    if (snapshot.status === 'COMPLETED') {
    │        - Mostrar resultados finales
    │        - Desuscribirse
    │    }
```

---

## 🎯 Características Clave

### ✅ Concurrencia Sin Bloqueo
- **Virtual Threads (Java 21)**: Miles de threads sin overhead
- **Sin @Transactional**: No bloquea BD durante cálculos largos
- **ConcurrentHashMap**: Gestión thread-safe de sesiones

### ✅ Comunicación en Tiempo Real
- **STOMP sobre WebSocket**: Protocolo estándar con ACK/NACK
- **SockJS Fallback**: HTTP Long-Polling automático
- **Throttling (500ms)**: Evita saturar el frontend

### ✅ Gestión de Estado Robusto
- **AtomicBoolean/AtomicInteger**: Primitivas atómicas thread-safe
- **Snapshots Inmutables**: Records de Java para datos inmutables
- **No hay Race Conditions**: Diseño cuidadoso sin locks

### ✅ Escalabilidad
- **Múltiples Simulaciones**: Cada una en su propio thread
- **Limpieza Automática**: @Scheduled cada hora
- **Prevención de Memory Leaks**: Sesiones > 2 horas se eliminan

---

## 📊 Métricas de Rendimiento

### Throughput Esperado
- **Simulaciones concurrentes**: 100+ simultáneas
- **Iteraciones por segundo**: 2-10 (según AG)
- **Latencia WebSocket**: < 10ms
- **Memoria por sesión**: ~1-5 MB

### Escalado Horizontal
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Server 1  │     │   Server 2  │     │   Server 3  │
│  (20 sims)  │     │  (20 sims)  │     │  (20 sims)  │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       └───────────────────┴───────────────────┘
                           │
                    Load Balancer
                           │
                   ┌───────┴───────┐
                   │   Frontend    │
                   └───────────────┘
```

---

## 🔧 Configuración Recomendada

### JVM Options
```bash
java -XX:+UseZGC \
     --enable-preview \
     -Xmx4G \
     -Xms2G \
     -XX:MaxDirectMemorySize=1G \
     -jar backend.jar
```

### application.properties
```properties
# WebSocket
spring.websocket.message-size-limit=512KB
spring.websocket.send-buffer-size-limit=1MB
spring.websocket.send-time-limit=20000

# Thread Pool
spring.task.scheduling.pool.size=5
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=50

# JPA
spring.jpa.show-sql=false
spring.jpa.open-in-view=false
```

---

**Documento vivo - Se actualiza con cada mejora del sistema**
