# Migración Completa: SSE → STOMP WebSocket

## 📋 Resumen Ejecutivo

Se ha eliminado completamente el sistema SSE (Server-Sent Events) y estandarizado toda la arquitectura de comunicación en tiempo real sobre **STOMP WebSocket**.

---

## 🗑️ Archivos Eliminados

### Controllers SSE
- ❌ `SimulacionController.java` (planificador/semanal/controller)
- ❌ `VueloTrackingController.java` (controller)

### Services SSE
- ❌ `SimulacionOrchestrator.java` (planificador/service)
- ❌ `VueloTrackingService.java` (service)

### DTOs SSE
- ❌ `EventoTickDTO.java` (planificador/semanal/dto/sse)
- ❌ `SimulacionEstadoDTO.java` (planificador/semanal/dto/sse)
- ❌ `ProgresoAGDTO.java` (websocket/dto - versión antigua)

---

## ✅ Archivos Creados

### 1. SimulationStartResponse.java
**Ubicación:** `com.proyecto.backend.simulation.dto`

```java
@Data
@Builder
public class SimulationStartResponse {
    private String sessionId;        // UUID de la sesión
    private String mensaje;          // Mensaje descriptivo
    private String topicUrl;         // Topic STOMP para subscribirse
}
```

**Uso:** Respuesta del endpoint `POST /api/simulations/start`

---

### 2. ProgresoAGDTO.java (Nuevo)
**Ubicación:** `com.proyecto.backend.simulation.dto`

```java
@Data
@Builder
public class ProgresoAGDTO {
    // Identificación
    @JsonProperty("sessionId") private String sessionId;
    @JsonProperty("tipo") private String tipo;  // "PROGRESO_AG", "SIMULACION_COMPLETADA", "ERROR"
    
    // Progreso del AG
    @JsonProperty("generacion") private Integer generacion;
    @JsonProperty("maxGeneraciones") private Integer maxGeneraciones;
    @JsonProperty("progreso") private Double progreso;          // 0-100%
    @JsonProperty("mejorFitness") private Double mejorFitness;
    @JsonProperty("fitnessPromedio") private Double fitnessPromedio;
    
    // Solución actual
    @JsonProperty("solucion") private PlanificacionResponseSimple solucion;
    
    // Contexto temporal
    @JsonProperty("fechaSimulada") private LocalDateTime fechaSimulada;
    @JsonProperty("timestamp") private LocalDateTime timestamp;
    
    // Métricas
    @JsonProperty("pedidosProcesados") private Integer pedidosProcesados;
    @JsonProperty("pedidosTotales") private Integer pedidosTotales;
    @JsonProperty("mensaje") private String mensaje;
}
```

**Características:**
- Campos en español con `@JsonProperty`
- Compatible con frontend JavaScript
- Incluye solución completa en cada update

---

### 3. WebSocketService.java
**Ubicación:** `com.proyecto.backend.simulation.service`

```java
@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    
    // Enviar a topic específico
    public <T> void sendMessage(String destination, T payload);
    
    // Enviar a simulación específica
    public <T> void sendToSimulation(String sessionId, T payload);
    
    // Obtener URL del topic
    public String getSimulationTopic(String sessionId);
}
```

**Beneficios:**
- Abstracción sobre `SimpMessagingTemplate`
- Logging centralizado
- Fácil de mockear en tests
- Manejo de errores sin detener AG

---

## 🔄 Archivos Modificados

### 1. SimulationController.java
**Cambios:**
```java
@RequiredArgsConstructor
public class SimulationController {
    private final SimulationService simulationService;
    private final WebSocketService webSocketService;  // ✅ NUEVO
    
    @PostMapping("/start")
    public ResponseEntity<SimulationStartResponse> startSimulation(@Valid @RequestBody SimulationRequest request) {
        UUID sessionId = simulationService.startSimulation(request);
        String topicUrl = webSocketService.getSimulationTopic(sessionId.toString());
        
        return ResponseEntity.ok(SimulationStartResponse.builder()
                .sessionId(sessionId.toString())
                .mensaje("Simulación iniciada exitosamente")
                .topicUrl(topicUrl)
                .build());
    }
}
```

---

### 2. SimulationService.java
**Cambios:**
```java
@RequiredArgsConstructor
public class SimulationService {
    private final PedidoRepository pedidoRepository;
    private final PlanDeVueloRepository planDeVueloRepository;
    private final WebSocketService webSocketService;  // ✅ CAMBIO: antes SimpMessagingTemplate
    
    private void sendMessage(UUID sessionId, SimulationSnapshot snapshot) {
        webSocketService.sendToSimulation(sessionId.toString(), snapshot);
        log.trace("📤 Mensaje enviado a /topic/simulations/{}", sessionId);
    }
}
```

**Beneficios:**
- Desacoplado de Spring Messaging
- Más fácil de testear
- Logging centralizado

---

### 3. AlgoritmoGeneticoService.java
**Cambios:**
```java
// Imports
import com.proyecto.backend.simulation.dto.ProgresoAGDTO;  // ✅ NUEVO DTO

// Método actualizado
private void enviarProgresoConSolucion(
        int generacion, double fitness, Solution solucion,
        long inicioMs, int totalPedidos, WorldTemporal worldTemporal,
        java.util.function.Consumer<ProgresoAGDTO> callback) {
    
    double progreso = (generacion * 100.0) / MAX_GENERACIONES;
    PlanificacionResponseSimple response = convertirAResponseSimple(solucion, worldTemporal);

    callback.accept(ProgresoAGDTO.builder()
            .tipo("PROGRESO_AG")
            .generacion(generacion)
            .maxGeneraciones(MAX_GENERACIONES)
            .progreso(progreso)
            .mejorFitness(fitness)
            .fitnessPromedio(fitness)  // TODO: calcular promedio real
            .solucion(response)
            .pedidosProcesados(totalPedidos)
            .pedidosTotales(totalPedidos)
            .timestamp(LocalDateTime.now())
            .build());
}
```

**Características:**
- Usa nuevo DTO estandarizado
- Callback mechanism preservado
- Compatible con WebSocketService

---

## 🏗️ Arquitectura Final

### REST Control Endpoints
```
POST   /api/simulations/start     → SimulationStartResponse
POST   /api/simulations/{id}/cancel
POST   /api/simulations/{id}/pause
POST   /api/simulations/{id}/resume
GET    /api/simulations/{id}      → SimulationSnapshot
GET    /api/simulations           → Lista de sesiones activas
GET    /api/simulations/health    → Health check
```

### STOMP WebSocket
```
Conexión:  ws://localhost:8000/ws (SockJS fallback automático)
Broker:    /topic/*
App Dest:  /app/*
Topic:     /topic/simulations/{sessionId}
```

### Flujo de Datos

```
┌─────────┐                                    ┌─────────┐
│ Cliente │                                    │ Servidor│
└────┬────┘                                    └────┬────┘
     │                                              │
     │  POST /api/simulations/start                │
     ├─────────────────────────────────────────────>│
     │                                              │
     │  SimulationStartResponse                    │
     │  { sessionId, topicUrl }                    │
     │<─────────────────────────────────────────────┤
     │                                              │
     │  STOMP CONNECT ws://localhost:8000/ws       │
     ├─────────────────────────────────────────────>│
     │                                              │
     │  SUBSCRIBE /topic/simulations/{sessionId}   │
     ├─────────────────────────────────────────────>│
     │                                              │
     │  SimulationSnapshot (cada actualización)    │
     │<─────────────────────────────────────────────┤
     │                                              │
     │  ProgresoAGDTO (desde AG cada generación)   │
     │<─────────────────────────────────────────────┤
     │                                              │
     │  POST /api/simulations/{id}/cancel          │
     ├─────────────────────────────────────────────>│
     │                                              │
     │  OK + cierre de topic                       │
     │<─────────────────────────────────────────────┤
     │                                              │
```

---

## 🎯 Componentes Clave

### WebSocketConfig
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### Frontend Integration (test-simulation-stomp.html)
```javascript
// Conexión STOMP
const socket = new SockJS('http://localhost:8000/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Conectado:', frame);
    
    // Subscribirse al topic de la sesión
    stompClient.subscribe('/topic/simulations/' + sessionId, function(message) {
        const data = JSON.parse(message.body);
        console.log('Actualización recibida:', data);
        
        // Procesar SimulationSnapshot o ProgresoAGDTO
        if (data.tipo === 'PROGRESO_AG') {
            actualizarProgresoAG(data);
        } else {
            actualizarSimulacion(data);
        }
    });
});
```

---

## ✅ Checklist de Verificación

- [x] Todos los archivos SSE eliminados
- [x] SimulationStartResponse creado
- [x] ProgresoAGDTO (nuevo) creado
- [x] WebSocketService implementado
- [x] SimulationController actualizado
- [x] SimulationService refactorizado
- [x] AlgoritmoGeneticoService actualizado
- [x] Compilación exitosa
- [x] WebSocketConfig configurado
- [x] Frontend compatible (test-simulation-stomp.html)

---

## 🚀 Próximos Pasos

### 1. Testing
- [ ] Test unitario de WebSocketService
- [ ] Test de integración del flujo completo
- [ ] Test de SimulationController con nuevo DTO

### 2. Mejoras Opcionales
- [ ] Calcular `fitnessPromedio` real en ProgresoAGDTO
- [ ] Añadir `sessionId` a ProgresoAGDTO
- [ ] Implementar reconexión automática en frontend
- [ ] Añadir heartbeat/keepalive para conexiones largas

### 3. Producción
- [ ] Configurar CORS específicos (no "*")
- [ ] Usar broker externo (RabbitMQ/ActiveMQ) en lugar de simple broker
- [ ] Añadir autenticación a WebSocket
- [ ] Implementar rate limiting

---

## 📝 Notas Técnicas

### ¿Por qué STOMP en lugar de SSE?

| Característica | SSE | STOMP WebSocket |
|---------------|-----|-----------------|
| **Dirección** | Unidireccional (servidor → cliente) | Bidireccional |
| **Protocolo** | HTTP | WebSocket + STOMP |
| **Fallback** | No (solo HTTP) | Sí (SockJS) |
| **Multiplexión** | No | Sí (múltiples topics) |
| **ACK/NACK** | No | Sí |
| **Spring Support** | Básico | Nativo |
| **Escalabilidad** | Limitada | Alta (con broker externo) |

### Ventajas de WebSocketService

1. **Abstracción limpia:** Oculta detalles de Spring Messaging
2. **Testeable:** Fácil de mockear en tests
3. **Error handling:** No detiene el AG si falla el envío
4. **Logging centralizado:** Todos los mensajes trackeados
5. **Future-proof:** Fácil cambiar implementación interna

---

## 📚 Referencias

- [Spring WebSocket STOMP](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [SockJS Client](https://github.com/sockjs/sockjs-client)
- [STOMP Protocol](https://stomp.github.io/)

---

**Fecha de migración:** 2025-11-26  
**Versión:** 2.0  
**Estado:** ✅ COMPLETADO
