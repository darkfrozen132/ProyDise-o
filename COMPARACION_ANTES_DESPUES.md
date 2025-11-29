# 🔄 ANTES vs DESPUÉS - Comparación Visual

## 🏗️ Arquitectura General

### ❌ ANTES (Tu código actual - COMPLEJO)

```
Backend:
┌─────────────────────────────────────┐
│  PlanificacionWebSocketHandler      │ ← Handler manual
│  - Gestión manual de sesiones      │
│  - Envío directo de mensajes       │
│  - Sin tipado claro                 │
└─────────────────────────────────────┘

Frontend:
┌─────────────────────────────────────┐
│  websocket.js                       │ ← Implementación 1
│  + PlanificacionService.js          │ ← Implementación 2
│  + STOMP directo en componente      │ ← Implementación 3
│  = CONFUSIÓN TOTAL                  │
└─────────────────────────────────────┘
```

### ✅ DESPUÉS (Patrón CODIGO_APARTE - LIMPIO)

```
Backend:
┌─────────────────────────────────────┐
│  SimulationController (REST)        │ ← Endpoints REST
│  ├─ POST /api/simulations           │
│  ├─ GET /{id}/status                │
│  └─ DELETE /{id}                    │
├─────────────────────────────────────┤
│  SimulationService                  │ ← Lógica de negocio
│  └─ SimpMessagingTemplate           │ ← Envío STOMP
├─────────────────────────────────────┤
│  DTOs Tipados                       │
│  ├─ SimulationMessage               │
│  ├─ SimulationSnapshot              │
│  └─ SimulationSegment               │
└─────────────────────────────────────┘

Frontend:
┌─────────────────────────────────────┐
│  useSimulacion Hook                 │ ← ¡UNA SOLA implementación!
│  ├─ WebSocket STOMP                 │
│  ├─ Estado centralizado             │
│  ├─ Funciones: iniciar/pausar/...  │
│  └─ Exporta todo lo necesario       │
├─────────────────────────────────────┤
│  SimuladorSemanal Component         │ ← Componente simple
│  └─ Usa el hook (3 líneas!)        │
└─────────────────────────────────────┘
```

---

## 💻 Código: Backend

### ❌ ANTES - PlanificacionWebSocketHandler

```java
@Component
public class PlanificacionWebSocketHandler extends TextWebSocketHandler {
    
    // ❌ Gestión manual de sesiones
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // ❌ Parsing manual de JSON
        String payload = message.getPayload();
        // ❌ Lógica mezclada con comunicación
        // ❌ Difícil de testear
        // ❌ Sin tipado
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // ❌ Gestión manual de conexiones
        sessions.put(session.getId(), session);
    }
    
    // ❌ Envío manual complicado
    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            // Manejo de errores...
        }
    }
}
```

### ✅ DESPUÉS - SimulationController + Service

```java
// ✅ Controller REST limpio
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    
    private final SimulationService simulationService;
    
    @PostMapping
    public ResponseEntity<SimulationStartResponse> start(
            @RequestBody SimulationStartRequest request) {
        // ✅ Delegación clara
        SimulationStartResponse response = simulationService.startSimulation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @DeleteMapping("/{simulationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String simulationId) {
        // ✅ Cancelación simple
        simulationService.cancel(UUID.fromString(simulationId));
    }
}

// ✅ Service con STOMP
@Service
public class SimulationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    private void runSimulation(SimulationSession session, ...) {
        try {
            // Lógica de negocio...
            SimulationSnapshot snapshot = createSnapshot();
            
            // ✅ Envío SIMPLE y TIPADO
            messagingTemplate.convertAndSend(
                "/topic/simulations/" + session.id,
                SimulationMessage.progress(session.id, snapshot)
            );
            
        } catch (Exception ex) {
            // ✅ Manejo de errores claro
            messagingTemplate.convertAndSend(
                "/topic/simulations/" + session.id,
                SimulationMessage.error(session.id, ex.getMessage())
            );
        }
    }
}
```

**Diferencias:**
- ✅ Separación REST vs WebSocket
- ✅ DTOs tipados (no strings)
- ✅ SimpMessagingTemplate (framework hace el trabajo)
- ✅ Fácil de testear
- ✅ Código más corto y claro

---

## 💻 Código: Frontend

### ❌ ANTES - Múltiples implementaciones

```javascript
// Archivo 1: websocket.js
export const conectarWebSocketPlanificacion = (onMensaje) => {
    const socket = new SockJS('http://localhost:8000/ws');
    const stompClient = new Client({
        webSocketFactory: () => socket,
        // ... lógica compleja ...
    });
    // ❌ Retorna objeto complicado
    return { enviar, iniciarPlanificacion, ... };
};

// Archivo 2: PlanificacionService.js
class PlanificacionService {
    constructor() {
        this.stompClient = null;
        // ❌ Otra implementación diferente
    }
    // ❌ Lógica duplicada
}

// Archivo 3: SimuladorSemanal.js
const SimuladorSemanal = () => {
    // ❌ Tres refs diferentes
    const wsPlanificacionRef = useRef(null);
    const stompClientRef = useRef(null);
    const wsSemanlaRef = useRef(null);
    
    // ❌ useEffect complicado
    useEffect(() => {
        const conectar = async () => {
            // ❌ Lógica mezclada
            // ❌ Difícil de mantener
        };
    }, [deps complicadas]);
    
    // ❌ Funciones dispersas
    const iniciarPlanificacion = () => { /* ... */ };
    const handleWebSocketMessage = () => { /* ... */ };
    // ... 200 líneas más ...
};
```

### ✅ DESPUÉS - Hook único

```javascript
// useSimulacion.js - ¡UNA SOLA implementación!
export const useSimulacion = () => {
    // ✅ Estado centralizado y claro
    const [simulationId, setSimulationId] = useState(null);
    const [stompClient, setStompClient] = useState(null);
    const [status, setStatus] = useState('idle');
    const [latestProgress, setLatestProgress] = useState(null);
    
    // ✅ WebSocket en un solo useEffect
    useEffect(() => {
        if (!simulationId) return;
        
        const client = new Client({
            webSocketFactory: () => new SockJS(WS_ENDPOINT),
            onConnect: () => {
                client.subscribe(`/topic/simulations/${simulationId}`, (message) => {
                    const simMessage = JSON.parse(message.body);
                    // ✅ Procesamiento claro
                    setLatestProgress(simMessage.snapshot);
                    if (simMessage.type === 'COMPLETED') {
                        setStatus('completed');
                    }
                });
            }
        });
        
        client.activate();
        return () => client.deactivate();
    }, [simulationId]);
    
    // ✅ Funciones simples
    const iniciar = useCallback(async (params) => {
        const response = await fetch(`${API}/simulations`, {
            method: 'POST',
            body: JSON.stringify(params)
        });
        const data = await response.json();
        setSimulationId(data.simulationId);  // ← Esto activa WebSocket
    }, []);
    
    const terminar = useCallback(async () => {
        if (simulationId) {
            await fetch(`${API}/simulations/${simulationId}`, { method: 'DELETE' });
        }
        stompClient?.deactivate();
        // Reset state...
    }, [simulationId, stompClient]);
    
    // ✅ Retorno limpio
    return {
        estaActivo: status === 'running',
        latestProgress,
        kpis,
        reloj,
        iniciar,
        pausar,
        terminar
    };
};

// SimuladorSemanal.js - ¡SÚPER SIMPLE!
const SimuladorSemanal = () => {
    // ✅ Una línea para todo!
    const {
        estaActivo,
        latestProgress,
        kpis,
        reloj,
        iniciar,
        pausar,
        terminar
    } = useSimulacion();
    
    // ✅ Lógica del componente clara
    const handleIniciar = async () => {
        await iniciar({
            startDate: fechaInicio,
            endDate: fechaFin
        });
    };
    
    return (
        <div>
            <button onClick={handleIniciar}>Iniciar</button>
            <button onClick={pausar}>Pausar</button>
            <button onClick={terminar}>Terminar</button>
            <div>KPIs: {kpis.entregas} entregas</div>
            <MapaVuelos datos={latestProgress} />
        </div>
    );
};
```

**Diferencias:**
- ✅ 1 implementación vs 3 diferentes
- ✅ Estado centralizado en hook
- ✅ Componente solo 50 líneas (antes 2000+)
- ✅ Fácil de testear
- ✅ Fácil de mantener

---

## 📊 Flujo de Datos

### ❌ ANTES (Confuso)

```
Frontend                          Backend
┌────────────────┐               ┌─────────────────┐
│  websocket.js  │──────?───────▶│  WebSocket      │
│  (Opción 1)    │               │  Handler        │
└────────────────┘               └─────────────────┘
        ↓ ¿Cuál usar?                    ↓
┌────────────────┐                       ↓
│ Planificacion  │──────?───────▶        ↓
│ Service        │                       ↓
│  (Opción 2)    │                       ↓
└────────────────┘                       ↓
        ↓                                ↓
┌────────────────┐                       ↓
│  STOMP directo │──────?───────▶        ↓
│  (Opción 3)    │                       ↓
└────────────────┘                       ↓
        ↓                                ↓
    ¿¿¿???                          Respuesta
```

### ✅ DESPUÉS (Claro)

```
Frontend                                    Backend
┌──────────────────────────────────┐      ┌────────────────────┐
│  useSimulacion Hook              │      │  REST API          │
│  ┌────────────────────────────┐  │      │  /api/simulations  │
│  │ 1. iniciar() →             │──┼─POST─▶│                    │
│  └────────────────────────────┘  │      └────────────────────┘
│                ↓                  │             ↓
│  ┌────────────────────────────┐  │      ┌────────────────────┐
│  │ 2. setSimulationId(uuid)   │  │      │  SimulationService │
│  └────────────────────────────┘  │      │  ├─ Ejecuta AG     │
│                ↓                  │      │  └─ Envía mensajes │
│  ┌────────────────────────────┐  │      └────────────────────┘
│  │ 3. WebSocket se activa     │  │             ↓
│  │    subscribe(...uuid)      │◀─┼──STOMP────▶│ /topic/       │
│  └────────────────────────────┘  │             │ simulations/  │
│                ↓                  │             │ {uuid}        │
│  ┌────────────────────────────┐  │             ↓
│  │ 4. Recibe mensajes         │  │      Progress → Snapshot
│  │    setLatestProgress()     │◀─┼──────────────┘
│  └────────────────────────────┘  │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│  SimuladorSemanal Component      │
│  └─ Renderiza datos del hook    │
└──────────────────────────────────┘
```

---

## 📈 Métricas de Mejora

| Aspecto                    | ANTES | DESPUÉS | Mejora |
|----------------------------|-------|---------|--------|
| **Archivos WebSocket**     | 3     | 1       | 66% ↓  |
| **Líneas de código**       | ~800  | ~300    | 62% ↓  |
| **Implementaciones**       | 3     | 1       | 66% ↓  |
| **Complejidad ciclomática**| Alta  | Baja    | ✅     |
| **Facilidad de test**      | Baja  | Alta    | ✅     |
| **Mantenibilidad**         | Baja  | Alta    | ✅     |
| **Curva de aprendizaje**   | Alta  | Baja    | ✅     |

---

## 🎯 Beneficios Clave

### ✅ Simplicidad
- **Antes:** 3 formas diferentes de hacer lo mismo
- **Después:** 1 forma clara y estándar

### ✅ Mantenibilidad
- **Antes:** Cambio requiere tocar 3 archivos
- **Después:** Cambio en 1 solo lugar

### ✅ Testabilidad
- **Antes:** Difícil mockear WebSockets manuales
- **Después:** Hook fácil de testear con Jest

### ✅ Escalabilidad
- **Antes:** Agregar funcionalidad = más confusión
- **Después:** Agregar funcionalidad = extender hook

### ✅ Profesionalismo
- **Antes:** Código "casero"
- **Después:** Patrón estándar de la industria

---

## 📚 Próximos Pasos

1. **Lee** el análisis completo → `ANALISIS_CODIGO_APARTE.md`
2. **Sigue** el plan de migración → `PLAN_MIGRACION_COMPLETO.md`
3. **Implementa** paso a paso (3-4 horas)
4. **Disfruta** código limpio y profesional 🎉

---

**¿Convencido de migrar?** 🚀

El esfuerzo vale la pena. Tu código será:
- ✅ Más fácil de mantener
- ✅ Más fácil de entender
- ✅ Más fácil de extender
- ✅ Más profesional

**¡Empieza ahora con `PLAN_MIGRACION_COMPLETO.md`!**
