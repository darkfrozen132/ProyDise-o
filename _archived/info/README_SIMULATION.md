# 🚀 Sistema de Simulación de Logística con WebSockets

## 📋 Descripción

Sistema profesional de simulación de logística basado en Spring Boot 3.5.6 y Java 21, con comunicación en tiempo real mediante WebSocket/STOMP.

### Características Principales

✅ **Arquitectura Moderna**
- Java 21 con Virtual Threads para alta concurrencia
- Spring Boot 3.5.6 con WebSocket/STOMP
- Gestión de estado thread-safe con AtomicBoolean/AtomicReference

✅ **Simulaciones Concurrentes**
- Múltiples simulaciones simultáneas
- Cada simulación en su propio Virtual Thread
- Sin bloqueo de base de datos durante cálculos

✅ **Comunicación en Tiempo Real**
- WebSocket con protocolo STOMP
- Actualizaciones throttled (500ms) para no saturar el frontend
- SockJS fallback automático

✅ **Gestión de Recursos**
- Limpieza automática de sesiones antiguas (@Scheduled)
- Control de ciclo de vida (start/pause/resume/cancel)
- Prevención de Memory Leaks

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                     FRONTEND (Browser)                       │
│                 SockJS + STOMP.js Client                     │
└─────────────────────────────────────────────────────────────┘
                            ▲ │
                            │ │ WebSocket/STOMP
                            │ ▼
┌─────────────────────────────────────────────────────────────┐
│                   SPRING BOOT BACKEND                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         SimulationController (REST API)             │    │
│  └─────────────────────────────────────────────────────┘    │
│                            │                                 │
│  ┌─────────────────────────────────────────────────────┐    │
│  │           SimulationService (Core Logic)            │    │
│  │  - ConcurrentHashMap<UUID, SimulationSession>      │    │
│  │  - Virtual Thread Executor                          │    │
│  │  - SimpMessagingTemplate (STOMP)                    │    │
│  └─────────────────────────────────────────────────────┘    │
│                            │                                 │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         SimulationSession (State Management)         │    │
│  │  - AtomicBoolean running/paused/completed           │    │
│  │  - AtomicInteger metrics                            │    │
│  │  - AtomicReference<SimulationSnapshot>              │    │
│  └─────────────────────────────────────────────────────┘    │
│                            │                                 │
│  ┌─────────────────────────────────────────────────────┐    │
│  │      WebSocketConfig (STOMP Configuration)          │    │
│  │  - Endpoint: /ws                                    │    │
│  │  - Broker: /topic                                   │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   DATABASE (MySQL/PostgreSQL)                │
│  - pedidos (Orders)                                          │
│  - planesdevuelo (Flight Plans)                              │
│  - aeropuertos (Airports)                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Estructura del Proyecto

```
backend/
├── src/main/java/com/proyecto/backend/
│   ├── simulation/
│   │   ├── controller/
│   │   │   └── SimulationController.java      # REST API endpoints
│   │   ├── service/
│   │   │   └── SimulationService.java         # Core simulation logic
│   │   ├── session/
│   │   │   └── SimulationSession.java         # Session state management
│   │   └── dto/
│   │       ├── SimulationRequest.java         # Request DTOs
│   │       ├── SimulationSnapshot.java        # Response DTOs
│   │       └── WebSocketMessage.java          # WebSocket messages
│   ├── config/
│   │   └── WebSocketConfig.java               # WebSocket/STOMP config
│   ├── model/
│   │   ├── Pedido.java                        # Order entity
│   │   ├── PlanDeVuelo.java                   # Flight plan entity
│   │   └── Aeropuerto.java                    # Airport entity
│   └── repository/
│       ├── PedidoRepository.java
│       ├── PlanDeVueloRepository.java
│       └── AeropuertoRepository.java
├── test-simulation-client.html                # Cliente de prueba
└── README_SIMULATION.md                       # Este archivo
```

---

## 🚀 Quick Start

### 1. Prerrequisitos

- Java 21+
- Maven 3.8+
- MySQL 8.0+ (o PostgreSQL)
- Spring Boot 3.5.6

### 2. Configuración

**application.properties**
```properties
# Puerto del servidor
server.port=8000

# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/package_planner
spring.datasource.username=root
spring.datasource.password=yourpassword

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# CORS (desarrollo)
spring.web.cors.allowed-origins=*
```

### 3. Ejecutar el Backend

```bash
mvn clean install
mvn spring-boot:run
```

### 4. Probar con el Cliente HTML

```bash
# Abrir en el navegador
open test-simulation-client.html
```

---

## 📡 API REST

### Base URL
```
http://localhost:8000/api/simulations
```

### Endpoints

#### 1. Iniciar Simulación
```http
POST /api/simulations/start
Content-Type: application/json

{
  "accion": "iniciar",
  "fecha": "2025-01-02",
  "factorK": 5,
  "tamanioPoblacion": 20,
  "maxGeneraciones": 20,
  "limiteGeneracionesSinMejora": 10
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Simulación iniciada exitosamente",
  "sessionId": "123e4567-e89b-12d3-a456-426614174000",
  "subscriptionTopic": "/topic/simulations/123e4567-e89b-12d3-a456-426614174000"
}
```

#### 2. Cancelar Simulación
```http
POST /api/simulations/{sessionId}/cancel
```

#### 3. Pausar Simulación
```http
POST /api/simulations/{sessionId}/pause
```

#### 4. Reanudar Simulación
```http
POST /api/simulations/{sessionId}/resume
```

#### 5. Obtener Estado
```http
GET /api/simulations/{sessionId}
```

#### 6. Listar Sesiones Activas
```http
GET /api/simulations
```

#### 7. Health Check
```http
GET /api/simulations/health
```

---

## 🔌 WebSocket STOMP

### Conexión

```javascript
// 1. Conectar al WebSocket
const socket = new SockJS('http://localhost:8000/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
    console.log('✅ Conectado');
    
    // 2. Suscribirse al tópico de la simulación
    const sessionId = '123e4567-e89b-12d3-a456-426614174000';
    stompClient.subscribe(`/topic/simulations/${sessionId}`, (message) => {
        const data = JSON.parse(message.body);
        console.log('📊 Actualización:', data);
    });
});
```

### Mensajes Recibidos

#### Progreso de Simulación
```json
{
  "simulationId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "RUNNING",
  "currentSimulationTime": "2025-01-02T10:30",
  "nextSimulationTime": "2025-01-02T10:55",
  "advanceMinutes": 25,
  "iterationNumber": 5,
  "durationMs": 1234,
  "processedOrders": 15,
  "totalOrders": 100,
  "currentFitness": 850.5,
  "bestFitness": 920.3,
  "timestamp": "2025-01-02T10:30:00"
}
```

#### Simulación Completada
```json
{
  "simulationId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "COMPLETED",
  "processedOrders": 100,
  "totalOrders": 100,
  "bestFitness": 950.8,
  "timestamp": "2025-01-02T11:00:00"
}
```

#### Error
```json
{
  "simulationId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "ERROR",
  "errorMessage": "Error al ejecutar algoritmo genético",
  "timestamp": "2025-01-02T10:35:00"
}
```

---

## ⚙️ Parámetros de Configuración

### Factor K (factorK)
**Definición:** Multiplicador de amplificación temporal

**Valores recomendados:**
- **K=1**: Operación día a día (muy lenta, máxima precisión)
- **K=5**: Simulación estándar (recomendado) ⭐
- **K=14**: Simulación rápida
- **K=75**: Simulación hasta colapso (testing)

**Fórmula:** `Salto de Consumo (Sc) = K × 5 minutos`

### Parámetros del Algoritmo Genético

| Parámetro | Default | Descripción |
|-----------|---------|-------------|
| `tamanioPoblacion` | 20 | Tamaño de la población (↓ = más rápido) |
| `maxGeneraciones` | 20 | Máximo de generaciones (↓ = más rápido) |
| `limiteGeneracionesSinMejora` | 10 | Límite sin mejora (↓ = más rápido) |

### Presets Recomendados

**🐢 Máxima Calidad (3-5 segundos/iteración)**
```json
{
  "factorK": 1,
  "tamanioPoblacion": 50,
  "maxGeneraciones": 50,
  "limiteGeneracionesSinMejora": 20
}
```

**⚖️ Balanceado (1-2 segundos/iteración)** ⭐
```json
{
  "factorK": 5,
  "tamanioPoblacion": 20,
  "maxGeneraciones": 20,
  "limiteGeneracionesSinMejora": 10
}
```

**⚡ Ultra Rápido (0.5-1 segundo/iteración)**
```json
{
  "factorK": 14,
  "tamanioPoblacion": 10,
  "maxGeneraciones": 10,
  "limiteGeneracionesSinMejora": 5
}
```

---

## 🔧 Integración con Frontend

### React Hook

```javascript
import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

export function useSimulation() {
    const [snapshot, setSnapshot] = useState(null);
    const stompClient = useRef(null);

    useEffect(() => {
        const socket = new SockJS('http://localhost:8000/ws');
        stompClient.current = Stomp.over(socket);

        stompClient.current.connect({}, () => {
            console.log('✅ WebSocket conectado');
        });

        return () => {
            if (stompClient.current) {
                stompClient.current.disconnect();
            }
        };
    }, []);

    const startSimulation = async (config) => {
        const response = await fetch('http://localhost:8000/api/simulations/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });

        const data = await response.json();
        
        if (data.success) {
            stompClient.current.subscribe(
                `/topic/simulations/${data.sessionId}`,
                (message) => {
                    setSnapshot(JSON.parse(message.body));
                }
            );
        }

        return data;
    };

    return { snapshot, startSimulation };
}
```

### Vue.js Composable

```javascript
import { ref, onMounted, onUnmounted } from 'vue';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

export function useSimulation() {
    const snapshot = ref(null);
    let stompClient = null;

    onMounted(() => {
        const socket = new SockJS('http://localhost:8000/ws');
        stompClient = Stomp.over(socket);
        stompClient.connect({}, () => {
            console.log('✅ WebSocket conectado');
        });
    });

    onUnmounted(() => {
        if (stompClient) {
            stompClient.disconnect();
        }
    });

    const startSimulation = async (config) => {
        const response = await fetch('http://localhost:8000/api/simulations/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });

        const data = await response.json();
        
        if (data.success) {
            stompClient.subscribe(
                `/topic/simulations/${data.sessionId}`,
                (message) => {
                    snapshot.value = JSON.parse(message.body);
                }
            );
        }

        return data;
    };

    return { snapshot, startSimulation };
}
```

---

## 🔍 Troubleshooting

### Problema: WebSocket no conecta

**Solución:**
```bash
# Verificar que el backend esté corriendo
curl http://localhost:8000/api/simulations/health

# Verificar logs
tail -f backend.log
```

### Problema: Simulación muy lenta

**Solución:** Reducir parámetros del AG
```json
{
  "tamanioPoblacion": 10,
  "maxGeneraciones": 10,
  "limiteGeneracionesSinMejora": 5
}
```

### Problema: Memory Leak

**Solución:** Las sesiones se limpian automáticamente cada hora. Para forzar limpieza:
- Las sesiones con más de 2 horas se eliminan automáticamente
- Implementado en `SimulationService.cleanupStaleSessions()`

---

## 📊 Monitoreo

### Logs

```bash
# Ver logs en tiempo real
tail -f backend.log | grep "Simulation"

# Filtrar por nivel
tail -f backend.log | grep "ERROR"
```

### Métricas

El servicio expone métricas en:
```
GET /api/simulations
```

Respuesta:
```json
{
  "totalSessions": 3,
  "sessions": {
    "123e4567...": {
      "sessionName": "SIM-123e4567",
      "status": "RUNNING",
      "createdAt": "2025-01-02T10:00:00",
      "isRunning": true,
      "isPaused": false
    }
  }
}
```

---

## 🧪 Testing

### Test Manual con cURL

```bash
# 1. Iniciar simulación
curl -X POST http://localhost:8000/api/simulations/start \
  -H "Content-Type: application/json" \
  -d '{
    "accion": "iniciar",
    "fecha": "2025-01-02",
    "factorK": 5,
    "tamanioPoblacion": 20,
    "maxGeneraciones": 20
  }'

# 2. Obtener estado
curl http://localhost:8000/api/simulations/{sessionId}

# 3. Cancelar
curl -X POST http://localhost:8000/api/simulations/{sessionId}/cancel
```

---

## 🚀 Próximos Pasos

### TODO: Integración con Algoritmo Genético Real

Actualmente, el servicio usa un stub del AG. Para integrar el algoritmo real:

1. **Crear interfaz `GeneticAlgorithm`**
```java
public interface GeneticAlgorithm {
    SimulationResult execute(
        List<Pedido> orders,
        List<PlanDeVuelo> flights,
        GAConfiguration config
    );
}
```

2. **Implementar en `SimulationService`**
```java
@Autowired
private GeneticAlgorithm geneticAlgorithm;

private SimulationResult runGeneticAlgorithm(...) {
    return geneticAlgorithm.execute(orders, flights, config);
}
```

3. **Mapear rutas reales en `buildSolutionData()`**

---

## 📝 Notas Técnicas

### Virtual Threads (Java 21)

El servicio usa Virtual Threads para ejecutar simulaciones:
```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

**Ventajas:**
- Miles de threads concurrentes sin overhead
- Scheduling automático por la JVM
- Perfecto para operaciones I/O-bound

### Throttling

Para evitar saturar el frontend, las actualizaciones se envían cada 500ms:
```java
private static final long THROTTLE_MS = 500;

if (session.shouldSendUpdate()) {
    sendProgressUpdate(...);
}
```

### Thread Safety

Todo el estado de la sesión usa primitivas atómicas:
- `AtomicBoolean` para flags
- `AtomicInteger` para contadores
- `AtomicReference` para objetos inmutables

---

## 📞 Soporte

Para preguntas o problemas, consultar:
- Documentación de Spring WebSocket: https://docs.spring.io/spring-framework/reference/web/websocket.html
- STOMP Protocol: https://stomp.github.io/

---

**Versión:** 1.0  
**Última actualización:** 26 de noviembre de 2025  
**Estado:** ✅ Listo para integración con AG real
