# 📊 Análisis Completo: CODIGO_APARTE vs Tu Proyecto

## 🎯 Objetivo
Analizar el código de CODIGO_APARTE y aplicar sus mejores prácticas a tu proyecto actual.

---

## 🏗️ Arquitectura Backend

### ✅ CODIGO_APARTE (REFERENCIA)

```java
// WebSocketConfig.java - ✅ PERFECTO
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // ← Soporte SockJS
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");  // ← Prefijo para topics
        registry.setApplicationDestinationPrefixes("/app");  // ← Prefijo para mensajes desde cliente
    }
}
```

**Características Clave:**
1. ✅ Endpoint único: `/ws`
2. ✅ SockJS habilitado para fallback
3. ✅ STOMP broker simple en memoria
4. ✅ Prefijos claros: `/topic` y `/app`

---

### 🔄 SimulationController - Patrón REST + WebSocket

```java
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    
    @PostMapping  // ← POST /api/simulations
    public ResponseEntity<SimulationStartResponse> start(@RequestBody SimulationStartRequest request) {
        SimulationStartResponse response = simulationService.startSimulation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{simulationId}/status")  // ← GET status
    public SimulationStatus status(@PathVariable UUID simulationId) {
        return simulationService.getStatus(simulationId);
    }

    @DeleteMapping("/{simulationId}")  // ← DELETE para cancelar
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID simulationId) {
        simulationService.cancel(simulationId);
    }
}
```

**Flujo:**
1. **POST** `/api/simulations` → Devuelve `{simulationId: "uuid"}`
2. **Cliente se suscribe** a `/topic/simulations/{uuid}`
3. **Backend envía mensajes** al topic automáticamente
4. **DELETE** `/api/simulations/{uuid}` para cancelar

---

### 📡 SimulationService - Envío de Mensajes STOMP

```java
@Service
public class SimulationService {
    
    private final SimpMessagingTemplate messagingTemplate;  // ← CLAVE!
    
    private static final String TOPIC_PREFIX = "/topic/simulations/";
    
    // Método para iniciar simulación
    public SimulationStartResponse startSimulation(SimulationStartRequest request) {
        UUID simulationId = UUID.randomUUID();
        
        // ... lógica de inicio ...
        
        // Ejecuta en otro hilo
        executorService.submit(() -> runSimulation(session, orders, world));
        
        return new SimulationStartResponse(simulationId.toString());
    }
    
    private void runSimulation(...) {
        try {
            for (Order order : orders) {
                // Procesar orden
                Individual best = processOrder(...);
                
                // Crear snapshot
                SimulationSnapshot snapshot = toSnapshot(...);
                
                // 📤 ENVIAR AL WEBSOCKET
                messagingTemplate.convertAndSend(
                    topic(session.id),  // /topic/simulations/{id}
                    SimulationMessage.progress(id, snapshot)
                );
            }
            
            // 🎉 SIMULACIÓN COMPLETADA
            messagingTemplate.convertAndSend(
                topic(session.id),
                SimulationMessage.completed(id, finalSnapshot)
            );
            
        } catch (Exception ex) {
            // ❌ ERROR
            messagingTemplate.convertAndSend(
                topic(session.id),
                SimulationMessage.error(id, ex.getMessage())
            );
        }
    }
    
    private String topic(UUID simulationId) {
        return TOPIC_PREFIX + simulationId;
    }
}
```

**Puntos Clave:**
1. ✅ Usa `SimpMessagingTemplate` para enviar mensajes
2. ✅ Topic dinámico: `/topic/simulations/{uuid}`
3. ✅ Mensajes tipados: `PROGRESS`, `COMPLETED`, `ERROR`
4. ✅ Ejecución asíncrona con `ExecutorService`

---

### 📦 DTOs - Estructura de Mensajes

```java
// SimulationMessage.java - Wrapper principal
public record SimulationMessage(
    String simulationId,
    SimulationMessageType type,  // PROGRESS, COMPLETED, ERROR
    SimulationSnapshot snapshot,
    String error
) {
    public static SimulationMessage progress(String id, SimulationSnapshot snapshot) {
        return new SimulationMessage(id, SimulationMessageType.PROGRESS, snapshot, null);
    }
    
    public static SimulationMessage completed(String id, SimulationSnapshot snapshot) {
        return new SimulationMessage(id, SimulationMessageType.COMPLETED, snapshot, null);
    }
    
    public static SimulationMessage error(String id, String error) {
        return new SimulationMessage(id, SimulationMessageType.ERROR, null, error);
    }
}

// SimulationSnapshot.java - Estado de la simulación
public record SimulationSnapshot(
    String simulationId,
    int processedOrders,
    int totalOrders,
    double fitness,
    Instant generatedAt,  // ← TIMESTAMP SIMULADO!
    List<SimulationOrderPlan> orderPlans
) {}

// SimulationSegment.java - Vuelo individual
public record SimulationSegment(
    String flightId,
    String origin,
    String destination,
    LocalDate date,
    int quantity,
    Instant departureUtc,  // ← TIMESTAMPS REALES
    Instant arrivalUtc
) {}
```

---

## 🎨 Arquitectura Frontend

### ✅ Hook `useSimulacion` - Gestión Completa del Estado

```typescript
export const useSimulacion = () => {
  // ========== ESTADO ==========
  const [simulationId, setSimulationId] = useState<string | null>(null);
  const [stompClient, setStompClient] = useState<Client | null>(null);
  const [status, setStatus] = useState<'idle' | 'running' | 'completed' | 'error'>('idle');
  
  // Snapshots
  const [latestProgress, setLatestProgress] = useState<SimulationSnapshot | null>(null);
  const [finalSnapshot, setFinalSnapshot] = useState<SimulationSnapshot | null>(null);
  const [visibleSnapshot, setVisibleSnapshot] = useState<SimulationSnapshot | null>(null);
  
  // Tiempo simulado
  const [tiempoSimulado, setTiempoSimulado] = useState<Date | null>(null);
  const [simSpeed, setSimSpeed] = useState(2000);  // 2000x velocidad
  
  // Vuelos
  const [segmentosVuelo, setSegmentosVuelo] = useState<Map<string, SegmentoVuelo>>(new Map());
  const [animPaused, setAnimPaused] = useState(false);
  
  // ========== WEBSOCKET CONNECTION ==========
  useEffect(() => {
    if (!simulationId) return;

    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',  // ← WebSocket nativo
      reconnectDelay: 5000,
      
      onConnect: () => {
        console.log('✅ WebSocket Conectado');
        setStatus('running');
        
        // 📡 SUSCRIBIRSE AL TOPIC
        client.subscribe(`/topic/simulations/${simulationId}`, (message) => {
          const simMessage: SimulationMessage = JSON.parse(message.body);
          
          if (simMessage.snapshot) {
            setLatestProgress(simMessage.snapshot);
            setVisibleSnapshot(simMessage.snapshot);
            mergeFlightSegments(simMessage.snapshot);
            setHasSnapshots(true);
          }
          
          if (simMessage.type === 'COMPLETED') {
            setStatus('completed');
          } else if (simMessage.type === 'ERROR') {
            console.error("❌ Error:", simMessage.error);
            setStatus('error');
            client.deactivate();
          }
        });
      },
      
      onDisconnect: () => {
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
  
  // ========== FUNCIONES ==========
  const iniciar = useCallback((payload: SimulationStartRequest) => {
    // 1. Resetear estado
    setSegmentosVuelo(new Map());
    setHasSnapshots(false);
    
    // 2. Llamar REST API
    simulacionService.startSimulation(payload)
      .then(response => {
        console.log('Simulación iniciada:', response.simulationId);
        setSimulationId(response.simulationId);  // ← Esto activa el WebSocket
        setStatus('running');
      })
      .catch(error => {
        console.error("Error:", error);
        setStatus('error');
      });
  }, []);
  
  const terminar = useCallback(async () => {
    if (simulationId) {
      await simulacionService.cancelSimulation(simulationId);
    }
    stompClient?.deactivate();
    // Resetear todo el estado...
  }, [stompClient, simulationId]);
  
  return {
    aeropuertos,
    vuelosEnMovimiento,
    activeSegments,
    isLoading,
    estaActivo: status === 'running',
    iniciar,
    pausar,
    terminar,
    kpis,
    reloj,
    status,
  };
};
```

**Características Clave:**
1. ✅ Hook personalizado que encapsula TODA la lógica
2. ✅ WebSocket con `@stomp/stompjs` (nativo WebSocket)
3. ✅ Gestión completa del ciclo de vida
4. ✅ Animación en tiempo real con interpolación
5. ✅ Separación clara de responsabilidades

---

### 🗺️ Componente MapaVuelos

```typescript
// MapaVuelos.tsx - Visualización Leaflet
export const MapaVuelos = ({ 
  aeropuertos, 
  activeSegments, 
  vuelosEnMovimiento,
  filtroHubActivo 
}) => {
  
  // Renderiza aeropuertos como marcadores
  {aeropuertos.map(aeropuerto => (
    <Marker 
      key={aeropuerto.id}
      position={[aeropuerto.latitude, aeropuerto.longitude]}
      icon={customIcon}
    >
      <Popup>{aeropuerto.name}</Popup>
    </Marker>
  ))}
  
  // Renderiza rutas activas
  {activeSegments.map(segment => {
    const origen = aeropuertos.find(a => a.id === segment.origin);
    const destino = aeropuertos.find(a => a.id === segment.destination);
    
    return (
      <Polyline
        key={segment.id}
        positions={[[origen.lat, origen.lon], [destino.lat, destino.lon]]}
        color={segment.retrasado ? 'red' : 'blue'}
      />
    );
  })}
  
  // Renderiza aviones en movimiento
  {vuelosEnMovimiento.map(vuelo => (
    <Marker
      key={vuelo.id}
      position={[vuelo.latActual, vuelo.lonActual]}
      icon={avionIcon}
    >
      <Tooltip>
        {vuelo.origen} → {vuelo.destino}
        <br />Progreso: {vuelo.progreso}%
      </Tooltip>
    </Marker>
  ))}
  
};
```

---

## 🔑 Diferencias Clave con Tu Proyecto

### ❌ Tu Proyecto Actual

1. **Backend:**
   - ❌ Endpoint WebSocket separado: `/ws/planificacion`
   - ❌ Handler manual: `PlanificacionWebSocketHandler`
   - ❌ No usa `SimpMessagingTemplate`
   - ❌ Envío directo de mensajes sin tipado

2. **Frontend:**
   - ❌ Múltiples implementaciones WebSocket
   - ❌ Lógica dispersa en varios archivos
   - ❌ `websocket.js` obsoleto
   - ❌ No hay hook centralizado

### ✅ CODIGO_APARTE (MEJOR)

1. **Backend:**
   - ✅ Endpoint único: `/ws`
   - ✅ REST + WebSocket combinados
   - ✅ `SimpMessagingTemplate` para envíos
   - ✅ DTOs tipados y claros

2. **Frontend:**
   - ✅ Hook `useSimulacion` centralizado
   - ✅ WebSocket nativo con `@stomp/stompjs`
   - ✅ Animación interpolada en tiempo real
   - ✅ Separación de responsabilidades

---

## 🚀 Plan de Migración

### 1. Backend
- [ ] Crear `SimulationController` similar a CODIGO_APARTE
- [ ] Refactorizar `SimulacionService` para usar `SimpMessagingTemplate`
- [ ] Crear DTOs claros: `SimulationMessage`, `SimulationSnapshot`
- [ ] Eliminar `PlanificacionWebSocketHandler` manual

### 2. Frontend
- [ ] Crear hook `useSimulacion` inspirado en CODIGO_APARTE
- [ ] Eliminar `websocket.js` obsoleto
- [ ] Migrar `SimuladorSemanal.js` para usar el hook
- [ ] Simplificar lógica de animación

### 3. Limpieza
- [ ] Eliminar `visualizador-ag.html`
- [ ] Eliminar archivos de documentación obsoletos
- [ ] Consolidar toda la lógica en el hook

---

## 📝 Conclusión

CODIGO_APARTE usa un patrón **mucho más limpio y profesional**:

1. **Backend:** REST API para control + WebSocket STOMP para streaming
2. **Frontend:** Hook personalizado que encapsula toda la complejidad
3. **Separación:** Lógica de negocio separada de la visualización
4. **Tipado:** DTOs claros y bien definidos
5. **Simplicidad:** Una sola forma de hacer las cosas

**Recomendación:** Migrar tu proyecto a este patrón completamente.
