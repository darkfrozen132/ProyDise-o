# 🌐 Guía de Uso - WebSocket STOMP en SimuladorSemanal

## 📋 Índice
1. [Resumen](#resumen)
2. [Características Implementadas](#características-implementadas)
3. [Cómo Usar](#cómo-usar)
4. [Arquitectura](#arquitectura)
5. [Estados de la Simulación](#estados-de-la-simulación)
6. [Tipos de Mensajes](#tipos-de-mensajes)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Resumen

Se ha integrado **WebSocket STOMP** en `SimuladorSemanal.js` para recibir actualizaciones en tiempo real de la simulación logística del backend Spring Boot.

### ✅ Qué se implementó:

- ✅ Conexión WebSocket STOMP con SockJS
- ✅ Integración con API REST (`/api/simulations/start`)
- ✅ Suscripción automática a topics personalizados
- ✅ Procesamiento de mensajes en tiempo real
- ✅ Visualización de progreso del Algoritmo Genético
- ✅ Graficación automática de rutas en el mapa
- ✅ Log de eventos en tiempo real
- ✅ Control completo (iniciar, cancelar, limpiar)

---

## 🚀 Características Implementadas

### 1. Panel de Control WebSocket STOMP

Un nuevo panel morado con degradado que incluye:

- **Botón "Conectar WebSocket"**: Establece conexión con el backend
- **Botón "Iniciar Simulación Semanal"**: Envía POST a `/api/simulations/start` y se suscribe al topic
- **Botón "Cancelar Simulación"**: Detiene la simulación en curso
- **Botón "Limpiar Todo"**: Resetea el estado (mensajes, vuelos, progreso)
- **Botón "Desconectar"**: Cierra la conexión WebSocket

### 2. Indicador de Estado en Tiempo Real

- 🟢 **CONECTADO**: WebSocket activo
- 🔴 **DESCONECTADO**: Sin conexión

### 3. Progreso del Algoritmo Genético

Panel blanco con métricas en tiempo real:

- **Barra de progreso visual** (0-100%)
- **Generación actual / Total generaciones**
- **Mejor fitness encontrado**
- **Fitness promedio**
- **Pedidos procesados / Total pedidos**

### 4. Log de Eventos

Consola de mensajes con código de colores:

- 🟢 Verde: Mensajes de éxito
- 🔴 Rojo: Errores
- 🟡 Amarillo: Advertencias
- 🔵 Azul: Información general

---

## 📖 Cómo Usar

### Paso 1: Seleccionar Fecha

```
1. En el campo "Fecha de Inicio", selecciona una fecha (ej: 2025-01-02)
2. El sistema usará Factor K = 5 automáticamente
```

### Paso 2: Conectar WebSocket

```
1. Click en "🔌 Conectar WebSocket"
2. Espera a que el indicador se ponga verde: "CONECTADO"
3. Verás el mensaje: "✅ Conexión WebSocket establecida"
```

### Paso 3: Iniciar Simulación

```
1. Click en "🚀 Iniciar Simulación Semanal"
2. El sistema:
   - Envía POST a http://localhost:8000/api/simulations/start
   - Recibe un sessionId del backend
   - Se suscribe automáticamente a /topic/simulations/{sessionId}
3. Empezarás a recibir mensajes en tiempo real
```

### Paso 4: Observar el Progreso

```
- La barra de progreso se actualiza automáticamente
- Las métricas del AG se actualizan cada generación
- Los vuelos aparecen en el mapa en tiempo real (verde = AG)
- El log muestra todos los eventos
```

### Paso 5: Cancelar o Limpiar

```
- Click en "🛑 Cancelar Simulación" para detener en cualquier momento
- Click en "🧹 Limpiar Todo" para resetear el estado
```

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                  SimuladorSemanal.js                        │
│                                                             │
│  1. Usuario selecciona fecha: "2025-01-02"                 │
│  2. Click en "Iniciar Simulación Semanal"                  │
│                                                             │
│  3. POST http://localhost:8000/api/simulations/start       │
│     Body: { fecha: "2025-01-02", factorK: 5 }              │
│                                                             │
│  4. Response: { sessionId: "abc-123", topicUrl: "..." }    │
│                                                             │
│  5. stompClient.subscribe("/topic/simulations/abc-123")    │
│                                                             │
│  6. Backend envía mensajes cada N segundos:                │
│     - PROGRESO_AG: Info del algoritmo genético             │
│     - RUNNING: Snapshot de la simulación                   │
│     - COMPLETED: Simulación terminada                      │
│     - ERROR: Si algo falla                                 │
│                                                             │
│  7. procesarMensajeSimulacion(datos)                       │
│     - Actualiza progreso                                   │
│     - Grafica rutas en el mapa                             │
│     - Agrega mensajes al log                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Estados de la Simulación

| Estado | Descripción | Color |
|--------|-------------|-------|
| `disconnected` | WebSocket no conectado | Rojo |
| `connecting` | Conectando... | Amarillo |
| `connected` | WebSocket activo, listo para simular | Verde |
| `running` | Simulación en curso | Azul pulsante |
| `completed` | Simulación terminada exitosamente | Verde |
| `cancelled` | Simulación cancelada por el usuario | Amarillo |
| `error` | Error en la conexión o simulación | Rojo |

---

## 📨 Tipos de Mensajes

### 1. Mensaje de Progreso del AG

```json
{
  "sessionId": "abc-123",
  "tipo": "PROGRESO_AG",
  "generacion": 15,
  "maxGeneraciones": 20,
  "progreso": 75.0,
  "mejorFitness": 1245.67,
  "fitnessPromedio": 890.23,
  "solucion": {
    "rutas": [
      {
        "pedidoId": 123,
        "origen": "KJFK",
        "destino": "EGLL",
        "subRutas": [
          {
            "origen": "KJFK",
            "destino": "LFPG",
            "vuelo": "AF001",
            "horaSalida": "2025-01-02T10:00:00",
            "horaLlegada": "2025-01-02T22:00:00"
          }
        ]
      }
    ],
    "metricas": {
      "totalRutas": 150,
      "totalVuelos": 320
    }
  },
  "pedidosProcesados": 150,
  "pedidosTotales": 4440
}
```

**Acción**: 
- Actualiza barra de progreso
- Muestra métricas del AG
- Grafica rutas en el mapa (color verde)

---

### 2. Mensaje de Snapshot de Simulación

```json
{
  "sessionId": "abc-123",
  "status": "RUNNING",
  "iteration": 42,
  "processedOrders": 150,
  "totalOrders": 4440,
  "message": "Procesando pedidos...",
  "solution": {
    "routes": [...],
    "metadata": {
      "totalFlights": 320,
      "fitness": 1245.67
    }
  }
}
```

**Acción**:
- Muestra progreso general
- Agrega mensaje al log

---

### 3. Mensaje de Completado

```json
{
  "sessionId": "abc-123",
  "status": "COMPLETED",
  "iteration": 888,
  "processedOrders": 4440,
  "totalOrders": 4440,
  "message": "Simulación completada exitosamente"
}
```

**Acción**:
- Marca estado como `completed`
- Muestra mensaje de éxito
- Desuscribe del topic

---

### 4. Mensaje de Error

```json
{
  "sessionId": "abc-123",
  "tipo": "ERROR",
  "mensaje": "Error al procesar pedidos: timeout en base de datos"
}
```

**Acción**:
- Marca estado como `error`
- Muestra alerta al usuario
- Agrega mensaje rojo al log

---

## 🐛 Troubleshooting

### ❌ Error: "WebSocket no conectado"

**Causa**: El botón "Iniciar Simulación" fue presionado sin conectar primero

**Solución**:
1. Click en "🔌 Conectar WebSocket"
2. Espera a ver "CONECTADO" en verde
3. Luego click en "🚀 Iniciar Simulación Semanal"

---

### ❌ Error: "No se recibió sessionId del servidor"

**Causa**: El backend no devolvió un sessionId válido

**Solución**:
1. Verifica que el backend esté corriendo en `http://localhost:8000`
2. Verifica la respuesta del endpoint:
   ```bash
   curl -X POST http://localhost:8000/api/simulations/start \
     -H "Content-Type: application/json" \
     -d '{"fecha":"2025-01-02","factorK":5}'
   ```
3. Debe retornar:
   ```json
   {
     "sessionId": "uuid-aquí",
     "mensaje": "...",
     "topicUrl": "/topic/simulations/uuid-aquí"
   }
   ```

---

### ❌ Error: No se grafican vuelos en el mapa

**Causa**: Los códigos ICAO de aeropuertos no coinciden

**Solución**:
1. Abre la consola del navegador (F12)
2. Busca mensajes: `⚠️ Aeropuertos no encontrados: ...`
3. Verifica que los códigos ICAO del backend coincidan con los de la base de datos:
   ```javascript
   // Ejemplo de búsqueda case-insensitive implementada:
   const origen = airports.find(a => 
     a.code.toUpperCase() === subRuta.origen.toUpperCase()
   );
   ```

---

### ❌ Error: "Error STOMP: ..."

**Causa**: Problema en la configuración del backend

**Solución**:
1. Verifica que el backend tenga configurado CORS:
   ```java
   @Configuration
   public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
       @Override
       public void registerStompEndpoints(StompEndpointRegistry registry) {
           registry.addEndpoint("/ws")
                   .setAllowedOrigins("http://localhost:3000")
                   .withSockJS();
       }
   }
   ```

2. Verifica que el topic esté configurado:
   ```java
   @Override
   public void configureMessageBroker(MessageBrokerRegistry config) {
       config.enableSimpleBroker("/topic");
       config.setApplicationDestinationPrefixes("/app");
   }
   ```

---

### 🔍 Logs útiles

En la consola del navegador verás:

```javascript
📡 Conectando WebSocket STOMP...
✅ WebSocket STOMP conectado
🚀 Iniciando simulación semanal con WebSocket STOMP...
📨 Respuesta del servidor: {...}
📡 Suscribiéndose a: /topic/simulations/abc-123
📨 Mensaje recibido: {...}
🧬 Progreso AG - Generación 15/20
✈️ Procesando 320 rutas...
✅ Procesados 320 vuelos del AG
```

Estos logs te ayudan a entender exactamente qué está pasando.

---

## 🎨 Personalización

### Cambiar colores de vuelos del AG

En `procesarRutasSimulacion()`:

```javascript
aircraftColor: '#10b981', // Verde actual
// Cambia a:
aircraftColor: '#3b82f6', // Azul
// o
aircraftColor: '#f59e0b', // Naranja
```

### Ajustar tamaño del log

En el panel de mensajes:

```javascript
maxHeight: '300px', // Altura actual
// Cambia a:
maxHeight: '500px', // Más alto
```

### Modificar formato de mensajes

En `agregarMensaje()` puedes cambiar cómo se formatean los mensajes del log.

---

## 📊 Métricas Visualizadas

1. **Generación**: Iteración actual del AG
2. **Mejor Fitness**: Mejor solución encontrada
3. **Fitness Promedio**: Promedio de la población
4. **Pedidos Procesados**: Progreso de la simulación

---

## 🔗 Endpoints Utilizados

| Método | Endpoint | Body | Uso |
|--------|----------|------|-----|
| POST | `/api/simulations/start` | `{fecha, factorK}` | Iniciar simulación |
| POST | `/api/simulations/{sessionId}/cancel` | - | Cancelar simulación |
| WS | `/ws` | - | Conexión WebSocket |
| TOPIC | `/topic/simulations/{sessionId}` | - | Recibir mensajes |

---

## ✅ Checklist de Funcionamiento

- [ ] Backend corriendo en `http://localhost:8000`
- [ ] Endpoint `/api/simulations/start` funcional
- [ ] WebSocket endpoint `/ws` configurado
- [ ] CORS habilitado para `http://localhost:3000`
- [ ] Frontend puede conectarse al WebSocket
- [ ] Se recibe sessionId al iniciar
- [ ] Los mensajes llegan correctamente
- [ ] Las rutas se grafican en el mapa
- [ ] El progreso se actualiza en tiempo real

---

## 🎉 Resultado Final

Con esta integración tendrás:

- ✅ **Simulación en tiempo real** del Algoritmo Genético
- ✅ **Visualización interactiva** de rutas en el mapa
- ✅ **Monitoreo completo** del progreso con métricas
- ✅ **Log detallado** de todos los eventos
- ✅ **Control total** desde la interfaz web
- ✅ **Arquitectura escalable** para futuras mejoras

---

**🚀 ¡Todo listo para simular tu logística en tiempo real!**

**Fecha de implementación**: 26 de noviembre de 2025  
**Versión**: 1.0 - WebSocket STOMP Integrado
