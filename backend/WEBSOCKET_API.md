# 🔌 WebSocket API - Planificación y Tracking de Vuelos

## 📋 Información General

Este documento describe **dos servicios complementarios**:

1. **🎯 WebSocket de Planificación** → Decisiones del Algoritmo Genético (qué pedidos van en qué vuelos)
2. **📡 SSE de Tracking** → Posiciones en tiempo real de los aviones (para graficar en el mapa)

---

## 🎯 Servicio 1: WebSocket de Planificación

**Endpoint:** `ws://localhost:8000/ws/planificacion`

**Protocolo:** WebSocket nativo (no STOMP)

**Formato:** JSON

**Propósito:** Recibir progreso de la planificación (asignación de pedidos a vuelos)

---

## 🚀 Conexión

### URL de Conexión
```javascript
const ws = new WebSocket('ws://localhost:8000/ws/planificacion');
```

### Eventos de Conexión
```javascript
ws.onopen = () => {
    console.log('✅ WebSocket conectado');
};

ws.onclose = (event) => {
    console.log('🔌 WebSocket desconectado', event);
};

ws.onerror = (error) => {
    console.error('❌ Error en WebSocket', error);
};

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('📩 Mensaje recibido:', data);
};
```

---

## 📤 Mensajes del Cliente → Servidor

### 1. Iniciar Planificación

**Acción:** `iniciar`

**Payload mínimo:**
```json
{
  "accion": "iniciar",
  "fecha": "2025-01-02",
  "factorK": 5
}
```

**Payload completo (con parámetros de velocidad):**
```json
{
  "accion": "iniciar",
  "fecha": "2025-01-02",
  "factorK": 5,
  "tamanioPoblacion": 20,
  "maxGeneraciones": 20,
  "limiteGeneracionesSinMejora": 10
}
```

**Parámetros:**

| Campo | Tipo | Requerido | Default | Descripción |
|-------|------|-----------|---------|-------------|
| `accion` | string | ✅ Sí | - | Debe ser "iniciar" |
| `fecha` | string (YYYY-MM-DD) | ✅ Sí | - | Fecha de inicio de la planificación |
| `factorK` | integer | ✅ Sí | - | Factor de amplificación temporal (recomendado: **5**) |
| `tamanioPoblacion` | integer | ❌ No | 20 | Tamaño de población del AG (↓ = más rápido) |
| `maxGeneraciones` | integer | ❌ No | 20 | Máximo de generaciones (↓ = más rápido) |
| `limiteGeneracionesSinMejora` | integer | ❌ No | 10 | Límite de generaciones sin mejora (↓ = más rápido) |

**Ejemplo de código:**
```javascript
function iniciarPlanificacion() {
    const request = {
        accion: "iniciar",
        fecha: "2025-01-02",
        factorK: 5,  // ⚠️ IMPORTANTE: Usar K=5
        // Parámetros opcionales para velocidad
        tamanioPoblacion: 10,      // ⚡ Más rápido
        maxGeneraciones: 10,        // ⚡ Más rápido
        limiteGeneracionesSinMejora: 5  // ⚡ Más rápido
    };
    
    ws.send(JSON.stringify(request));
}
```

### 2. Pausar Planificación (futuro)

```json
{
  "accion": "pausar"
}
```

### 3. Reanudar Planificación (futuro)

```json
{
  "accion": "reanudar"
}
```

### 4. Cancelar Planificación (futuro)

```json
{
  "accion": "cancelar"
}
```

---

## 📥 Mensajes del Servidor → Cliente

### 1. Conexión Exitosa

```json
{
  "tipo": "conexion",
  "mensaje": "Conexión establecida",
  "timestamp": "2025-01-02T10:30:00"
}
```

### 2. Progreso de Planificación

**Tipo:** `progreso`

**Estructura:**
```json
{
  "tipo": "progreso",
  "tiempoSimulacionActual": "2025-01-02T00:00",
  "proximoTiempo": "2025-01-02T01:10",
  "avanceSimuladoMin": 70,
  "ejecucionNumero": 1,
  "duracionRealMs": 2345,
  "fechaInicioVuelos": "2025-01-02",
  "fechaFinalVuelos": "2025-01-05",
  "solucion": {
    "vuelos": [
      {
        "id": 123,
        "codigoVuelo": "SKBO-SEQM-001",
        "origen": "SKBO",
        "destino": "SEQM",
        "fechaInicial": "2025-01-02",
        "fechaFinal": "2025-01-04",
        "horaSalida": "14:30",
        "horaLlegada": "16:00",
        "pedidos": [
          {
            "idPedido": 4646752,
            "clienteId": "000000001",
            "aeropuertoDestino": "SEQM",
            "cantidadProductos": 2,
            "fechaLimite": "2025-01-02T01:02"
          }
        ],
        "cargaTotal": 150,
        "capacidadMaxima": 300,
        "utilizacion": 50.0
      }
    ],
    "metadata": {
      "totalVuelos": 2,
      "totalPedidos": 5,
      "fitness": 1400.0
    }
  }
}
```

**Campos principales:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `tipo` | string | Siempre "progreso" |
| `tiempoSimulacionActual` | string (ISO DateTime) | Tiempo actual de la simulación |
| `proximoTiempo` | string (ISO DateTime) | Próximo tiempo simulado (actual + Sc) |
| `avanceSimuladoMin` | integer | Minutos que avanzó la simulación (Sc = K × Sa) |
| `ejecucionNumero` | integer | Número de iteración actual |
| `duracionRealMs` | integer | Tiempo real que tardó esta iteración (ms) |
| `fechaInicioVuelos` | string (YYYY-MM-DD) | Primera fecha de los vuelos planificados |
| `fechaFinalVuelos` | string (YYYY-MM-DD) | Última fecha de los vuelos planificados |
| `solucion` | object | Solución completa con vuelos y pedidos |

**Estructura de un vuelo:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | integer | ID del vuelo en BD |
| `codigoVuelo` | string | Código único del vuelo |
| `origen` | string | Código ICAO del aeropuerto origen |
| `destino` | string | Código ICAO del aeropuerto destino |
| `fechaInicial` | string | Fecha de salida |
| `fechaFinal` | string | Fecha de llegada |
| `horaSalida` | string | Hora de salida (HH:mm) |
| `horaLlegada` | string | Hora de llegada (HH:mm) |
| `pedidos` | array | Lista de pedidos asignados |
| `cargaTotal` | integer | Carga total del vuelo |
| `capacidadMaxima` | integer | Capacidad máxima del avión |
| `utilizacion` | float | Porcentaje de utilización (0-100) |

**Estructura de un pedido:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `idPedido` | integer | ID del pedido en BD |
| `clienteId` | string | ID del cliente |
| `aeropuertoDestino` | string | Código ICAO del destino |
| `cantidadProductos` | integer | Cantidad de productos |
| `fechaLimite` | string (ISO DateTime) | Fecha límite de entrega |

### 3. Error

```json
{
  "tipo": "error",
  "mensaje": "Descripción del error",
  "timestamp": "2025-01-02T10:30:00"
}
```

**Errores comunes:**

| Mensaje | Causa |
|---------|-------|
| "Faltan parámetros: fecha y factorK son obligatorios" | No se enviaron fecha o factorK |
| "Error en planificación: ..." | Error durante la ejecución del AG |

---

## 🎯 Ejemplo Completo de Integración

### HTML
```html
<!DOCTYPE html>
<html>
<head>
    <title>Planificación WebSocket</title>
</head>
<body>
    <h1>Planificación de Vuelos en Tiempo Real</h1>
    
    <button onclick="conectar()">Conectar</button>
    <button onclick="iniciarPlanificacion()">Iniciar Planificación</button>
    <button onclick="desconectar()">Desconectar</button>
    
    <div id="status"></div>
    <div id="resultados"></div>
    
    <script src="app.js"></script>
</body>
</html>
```

### JavaScript (app.js)
```javascript
let ws = null;

function conectar() {
    ws = new WebSocket('ws://localhost:8080/ws/planificacion');
    
    ws.onopen = () => {
        console.log('✅ Conectado al WebSocket');
        document.getElementById('status').innerHTML = '✅ Conectado';
    };
    
    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log('📩 Mensaje recibido:', data);
        
        if (data.tipo === 'progreso') {
            mostrarProgreso(data);
        } else if (data.tipo === 'error') {
            mostrarError(data);
        }
    };
    
    ws.onclose = () => {
        console.log('🔌 Desconectado del WebSocket');
        document.getElementById('status').innerHTML = '🔌 Desconectado';
    };
    
    ws.onerror = (error) => {
        console.error('❌ Error:', error);
        document.getElementById('status').innerHTML = '❌ Error de conexión';
    };
}

function iniciarPlanificacion() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('Primero debes conectarte');
        return;
    }
    
    const request = {
        accion: "iniciar",
        fecha: "2025-01-02",
        factorK: 5,  // ⚠️ IMPORTANTE: Usar K=5 (Sc = 5 × 5 = 25 minutos)
        // Parámetros opcionales para velocidad
        tamanioPoblacion: 20,
        maxGeneraciones: 20,
        limiteGeneracionesSinMejora: 10
    };
    
    ws.send(JSON.stringify(request));
    console.log('📤 Solicitud enviada:', request);
}

function mostrarProgreso(data) {
    const html = `
        <div class="iteracion">
            <h3>Iteración #${data.ejecucionNumero}</h3>
            <p>⏰ Tiempo simulado: ${data.tiempoSimulacionActual}</p>
            <p>⏱️ Duración real: ${data.duracionRealMs}ms</p>
            <p>📦 Vuelos planificados: ${data.solucion?.vuelos?.length || 0}</p>
            <p>📦 Total pedidos: ${data.solucion?.metadata?.totalPedidos || 0}</p>
            <p>📊 Fitness: ${data.solucion?.metadata?.fitness || 0}</p>
            
            <h4>Vuelos:</h4>
            <ul>
                ${data.solucion?.vuelos?.map(v => `
                    <li>
                        ${v.codigoVuelo}: ${v.origen} → ${v.destino}
                        (${v.pedidos.length} pedidos, 
                        utilización: ${v.utilizacion.toFixed(1)}%)
                    </li>
                `).join('') || 'Sin vuelos'}
            </ul>
        </div>
    `;
    
    document.getElementById('resultados').innerHTML = html + 
        document.getElementById('resultados').innerHTML;
}

function mostrarError(data) {
    alert('❌ Error: ' + data.mensaje);
}

function desconectar() {
    if (ws) {
        ws.close();
        ws = null;
    }
}

// Auto-conectar al cargar la página
window.addEventListener('load', conectar);
```

---

## 🎨 Ejemplo con React

### Hook personalizado
```javascript
// useWebSocket.js
import { useState, useEffect, useRef } from 'react';

export const useWebSocket = (url) => {
    const [isConnected, setIsConnected] = useState(false);
    const [lastMessage, setLastMessage] = useState(null);
    const [iteraciones, setIteraciones] = useState([]);
    const ws = useRef(null);

    useEffect(() => {
        ws.current = new WebSocket(url);

        ws.current.onopen = () => {
            console.log('✅ WebSocket conectado');
            setIsConnected(true);
        };

        ws.current.onmessage = (event) => {
            const data = JSON.parse(event.data);
            setLastMessage(data);
            
            if (data.tipo === 'progreso') {
                setIteraciones(prev => [data, ...prev]);
            }
        };

        ws.current.onclose = () => {
            console.log('🔌 WebSocket desconectado');
            setIsConnected(false);
        };

        ws.current.onerror = (error) => {
            console.error('❌ Error WebSocket:', error);
        };

        return () => {
            if (ws.current) {
                ws.current.close();
            }
        };
    }, [url]);

    const iniciarPlanificacion = (fecha, factorK, opciones = {}) => {
        if (ws.current && ws.current.readyState === WebSocket.OPEN) {
            const request = {
                accion: "iniciar",
                fecha,
                factorK,
                ...opciones
            };
            ws.current.send(JSON.stringify(request));
        }
    };

    return {
        isConnected,
        lastMessage,
        iteraciones,
        iniciarPlanificacion
    };
};
```

### Componente React
```javascript
// PlanificacionComponent.jsx
import React from 'react';
import { useWebSocket } from './useWebSocket';

const PlanificacionComponent = () => {
    const { isConnected, iteraciones, iniciarPlanificacion } = useWebSocket(
        'ws://localhost:8080/ws/planificacion'
    );

    const handleIniciar = () => {
        iniciarPlanificacion('2025-01-02', 14, {
            tamanioPoblacion: 20,
            maxGeneraciones: 20,
            limiteGeneracionesSinMejora: 10
        });
    };

    return (
        <div className="planificacion-container">
            <h1>Planificación de Vuelos</h1>
            
            <div className="status">
                {isConnected ? '✅ Conectado' : '🔌 Desconectado'}
            </div>
            
            <button onClick={handleIniciar} disabled={!isConnected}>
                Iniciar Planificación
            </button>
            
            <div className="iteraciones">
                {iteraciones.map((iter, index) => (
                    <div key={index} className="iteracion-card">
                        <h3>Iteración #{iter.ejecucionNumero}</h3>
                        <p>⏰ Tiempo: {iter.tiempoSimulacionActual}</p>
                        <p>⏱️ Duración: {iter.duracionRealMs}ms</p>
                        <p>📦 Vuelos: {iter.solucion?.vuelos?.length || 0}</p>
                        <p>📦 Pedidos: {iter.solucion?.metadata?.totalPedidos || 0}</p>
                        
                        <div className="vuelos">
                            {iter.solucion?.vuelos?.map((vuelo, vIndex) => (
                                <div key={vIndex} className="vuelo">
                                    <strong>{vuelo.codigoVuelo}</strong>
                                    <span>{vuelo.origen} → {vuelo.destino}</span>
                                    <span>{vuelo.pedidos.length} pedidos</span>
                                    <span>{vuelo.utilizacion.toFixed(1)}%</span>
                                </div>
                            ))}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default PlanificacionComponent;
```

---

## ⚙️ Configuración de Velocidad

### Presets Recomendados

**🐢 Calidad Máxima (más lento, ~3-5 segundos/iteración)**
```json
{
  "tamanioPoblacion": 50,
  "maxGeneraciones": 50,
  "limiteGeneracionesSinMejora": 20
}
```

**⚖️ Balance (recomendado, ~2-3 segundos/iteración)**
```json
{
  "tamanioPoblacion": 20,
  "maxGeneraciones": 20,
  "limiteGeneracionesSinMejora": 10
}
```

**⚡ Ultra Rápido (menos preciso, ~0.5-1 segundo/iteración)**
```json
{
  "tamanioPoblacion": 10,
  "maxGeneraciones": 10,
  "limiteGeneracionesSinMejora": 5
}
```

---

## 🔄 Ciclo de Vida

```
┌─────────────────────────────────────────────────────┐
│ 1. Cliente conecta WebSocket                       │
│    ws://localhost:8080/ws/planificacion            │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│ 2. Servidor envía confirmación de conexión         │
│    { tipo: "conexion", mensaje: "..." }            │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│ 3. Cliente envía solicitud de inicio               │
│    { accion: "iniciar", fecha: "...", factorK: 14 }│
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│ 4. Servidor ejecuta iteración #1                   │
│    - Carga pedidos en ventana [00:00, 01:10]       │
│    - Ejecuta algoritmo genético                    │
│    - Avanza tiempo simulado: 00:00 → 01:10         │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│ 5. Servidor envía progreso #1                      │
│    { tipo: "progreso", ejecucionNumero: 1, ... }   │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│ 6. Servidor ejecuta iteración #2                   │
│    - Carga pedidos en ventana [01:10, 02:20]       │
│    - Ejecuta algoritmo genético                    │
│    - Avanza tiempo simulado: 01:10 → 02:20         │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│ 7. Servidor envía progreso #2                      │
│    { tipo: "progreso", ejecucionNumero: 2, ... }   │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
              ...
               │
               ▼
┌─────────────────────────────────────────────────────┐
│ N. Cliente cierra conexión                         │
│    - Todos los pedidos vuelven a estado PENDIENTE  │
└─────────────────────────────────────────────────────┘
```

---

## 📊 Fórmulas y Conceptos

### Factor K (factorK)
**Definición:** Multiplicador de amplificación temporal

**Valores típicos:**
- K=1: Operación día a día (lenta)
- **K=5: Simulación estándar (recomendado)** ⭐
- K=14: Simulación 3 días (rápida)
- K=75: Simulación hasta colapso (avanzado)

### Salto de Algoritmo (Sa)
**Definición:** Intervalo de tiempo entre ejecuciones del AG

**Valor:** 5 minutos (fijo)

### Salto de Consumo (Sc)
**Definición:** Tiempo simulado que avanza en cada iteración

**Fórmula:** `Sc = K × Sa`

**Ejemplo:** Con **K=5** y Sa=5min → **Sc=25 minutos**

**Interpretación:** Cada iteración avanza **25 minutos** en tiempo simulado

### Ventana de Búsqueda
**Definición:** Rango temporal para buscar pedidos en cada iteración

**Fórmula:** `[tiempoActual, tiempoActual + Sc]`

**Ejemplo con K=5:** 
- Si tiempo actual es 01:00 → ventana [01:00, 01:25] (25 minutos)
- Si tiempo actual es 01:25 → ventana [01:25, 01:50] (25 minutos)

---

## 🛠️ Troubleshooting

### Problema: WebSocket no conecta

**Causa:** Backend no está corriendo o puerto incorrecto

**Solución:**
```bash
# Verificar que el backend esté corriendo
curl http://localhost:8080/actuator/health

# O iniciar el backend
mvn spring-boot:run
```

### Problema: No recibo mensajes de progreso

**Causa:** Formato de solicitud incorrecto

**Solución:** Verificar que el JSON tenga los campos requeridos:
```javascript
{
  "accion": "iniciar",  // ✅ String
  "fecha": "2025-01-02",  // ✅ String en formato YYYY-MM-DD
  "factorK": 14  // ✅ Number
}
```

### Problema: Error "Faltan parámetros"

**Causa:** No se envió `fecha` o `factorK`

**Solución:**
```javascript
// ❌ Incorrecto
{ "accion": "iniciar" }

// ✅ Correcto
{ "accion": "iniciar", "fecha": "2025-01-02", "factorK": 14 }
```

### Problema: Planificación muy lenta

**Solución:** Reducir parámetros del AG:
```javascript
{
  "accion": "iniciar",
  "fecha": "2025-01-02",
  "factorK": 14,
  "tamanioPoblacion": 10,  // ⚡ Reduce tiempo
  "maxGeneraciones": 10,    // ⚡ Reduce tiempo
  "limiteGeneracionesSinMejora": 5  // ⚡ Reduce tiempo
}
```

---

## 📝 Notas Importantes

1. **Reconexión:** Si la conexión se cierra, todos los pedidos vuelven a estado `PENDIENTE` automáticamente

2. **Múltiples clientes:** Cada conexión WebSocket mantiene su propio estado de simulación independiente

3. **Límite de iteraciones:** La planificación continúa hasta que no haya más pedidos o el frontend cierre la conexión

4. **Formato de fechas:** Siempre usar formato ISO 8601 (`YYYY-MM-DD` para fechas, `YYYY-MM-DDTHH:MM` para datetime)

5. **Persistencia:** Los vuelos y pedidos se guardan en la base de datos después de cada iteración

---

## � Servicio 2: SSE de Tracking (Posiciones en Tiempo Real)

### 🎯 Propósito

El **SSE (Server-Sent Events) de Tracking** proporciona las **coordenadas en tiempo real** de los aviones para graficarlos en el mapa.

**Diferencia clave:**
- 🎯 **WebSocket de Planificación** → Qué pedidos van en qué vuelos (decisiones del AG)
- 📡 **SSE de Tracking** → Dónde están los aviones ahora (coordenadas para el mapa)

---

### 🔌 Conexión al SSE

**Endpoint:** `http://localhost:8000/api/vuelos/stream`

**Protocolo:** Server-Sent Events (EventSource)

**Formato:** JSON en eventos `vuelo-update`

**Endpoints adicionales:**
- `POST /api/vuelos/tracking/iniciar` → Iniciar streaming de coordenadas
- `POST /api/vuelos/tracking/detener` → Detener streaming
- `GET /api/vuelos/tracking/estado` → Ver estado del tracking
- `GET /api/vuelos/activos` → Obtener vuelos activos

---

### 💻 Ejemplo JavaScript Vanilla

```javascript
// Conectar al SSE de tracking
const eventSource = new EventSource('http://localhost:8000/api/vuelos/stream');

// Escuchar actualizaciones de vuelos
eventSource.addEventListener('vuelo-update', (event) => {
    const tracking = JSON.parse(event.data);
    
    console.log('✈️ Actualización de vuelo:', tracking);
    
    // Graficar en el mapa
    actualizarAvionEnMapa(tracking);
});

// Manejar errores
eventSource.onerror = (error) => {
    console.error('❌ Error en SSE:', error);
    eventSource.close();
};

// Función para graficar en el mapa
function actualizarAvionEnMapa(tracking) {
    const { 
        vueloId,
        currentLatitude,
        currentLongitude,
        progress,
        estado,
        origenCodigoICAO,
        destinoCodigoICAO
    } = tracking;
    
    // Aquí usas tu librería de mapas (Leaflet, Google Maps, etc.)
    // Ejemplo conceptual:
    // map.updateMarker(vueloId, currentLatitude, currentLongitude);
    // map.updateProgress(vueloId, progress);
}

// Cerrar conexión cuando sea necesario
function detenerTracking() {
    eventSource.close();
    console.log('🔌 SSE cerrado');
}
```

---

### ⚛️ Ejemplo React Hook

```javascript
import { useEffect, useRef, useState } from 'react';

export function useVueloTracking() {
    const [vuelos, setVuelos] = useState({});
    const eventSourceRef = useRef(null);
    
    useEffect(() => {
        // Conectar al SSE
        eventSourceRef.current = new EventSource('http://localhost:8000/api/vuelos/stream');
        
        // Escuchar actualizaciones
        eventSourceRef.current.addEventListener('vuelo-update', (event) => {
            const tracking = JSON.parse(event.data);
            
            setVuelos(prev => ({
                ...prev,
                [tracking.vueloId]: tracking
            }));
        });
        
        // Manejar errores
        eventSourceRef.current.onerror = (error) => {
            console.error('❌ Error en SSE:', error);
        };
        
        // Cleanup al desmontar
        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
        };
    }, []);
    
    return vuelos;
}

// Uso en componente
function MapaVuelos() {
    const vuelos = useVueloTracking();
    
    return (
        <div className="mapa">
            {Object.values(vuelos).map(vuelo => (
                <Marcador
                    key={vuelo.vueloId}
                    lat={vuelo.currentLatitude}
                    lng={vuelo.currentLongitude}
                    progress={vuelo.progress}
                    estado={vuelo.estado}
                    origen={vuelo.origenCodigoICAO}
                    destino={vuelo.destinoCodigoICAO}
                />
            ))}
        </div>
    );
}
```

---

### 📦 Estructura del Mensaje `vuelo-update`

```json
{
    "vueloId": 12345,
    "planDeVueloId": 789,
    "origenCodigoICAO": "EBCI",
    "destinoCodigoICAO": "SPIM",
    "currentLatitude": -12.0219,
    "currentLongitude": -77.1143,
    "progress": 45.2,
    "estado": "EN_VUELO",
    "fechaInicio": "2025-01-02T08:00:00",
    "fechaFin": "2025-01-02T16:00:00",
    "pedidos": [
        {
            "idPedido": 4655645,
            "cantidad": 2,
            "aeropuertoDestino": "SPIM"
        }
    ]
}
```

**Campos importantes para graficar:**
- `currentLatitude`, `currentLongitude` → Posición actual del avión
- `progress` → Porcentaje del viaje completado (0-100)
- `estado` → `PROGRAMADO`, `EN_VUELO`, `COMPLETADO`
- `origenCodigoICAO`, `destinoCodigoICAO` → Para dibujar la ruta

---

### 🔄 Integración Completa: Planificación + Tracking

```javascript
class SistemaPlanificacionYTracking {
    constructor() {
        this.ws = null;
        this.eventSource = null;
        this.vuelosActivos = new Map();
    }
    
    // 1️⃣ Iniciar WebSocket de Planificación
    iniciarPlanificacion() {
        this.ws = new WebSocket('ws://localhost:8000/ws/planificacion');
        
        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            
            if (data.tipo === 'progreso') {
                console.log('📊 Progreso AG:', data);
                this.actualizarUI(data);
            }
            
            if (data.tipo === 'completado') {
                console.log('✅ Planificación completada');
                // Los vuelos ya fueron guardados en BD
                // El tracking los detectará automáticamente
            }
        };
        
        this.ws.onopen = () => {
            const request = {
                accion: "iniciar",
                fecha: "2025-01-02",
                factorK: 5
            };
            this.ws.send(JSON.stringify(request));
        };
    }
    
    // 2️⃣ Iniciar SSE de Tracking
    iniciarTracking() {
        this.eventSource = new EventSource('http://localhost:8000/api/tracking/stream');
        
        this.eventSource.addEventListener('vuelo-update', (event) => {
            const tracking = JSON.parse(event.data);
            
            // Guardar en mapa para referencia rápida
            this.vuelosActivos.set(tracking.vueloId, tracking);
            
            // Graficar en el mapa
            this.actualizarAvionEnMapa(tracking);
        });
    }
    
    // 3️⃣ Actualizar mapa con coordenadas
    actualizarAvionEnMapa(tracking) {
        const { vueloId, currentLatitude, currentLongitude, progress, estado } = tracking;
        
        if (estado === 'EN_VUELO') {
            // Tu código para actualizar el mapa
            // map.updatePlane(vueloId, currentLatitude, currentLongitude, progress);
        }
    }
    
    // 4️⃣ Actualizar UI con progreso del AG
    actualizarUI(data) {
        // Mostrar métricas del AG
        document.getElementById('generacion').textContent = data.metricas?.generacionActual;
        document.getElementById('fitness').textContent = data.metricas?.mejorFitness;
    }
    
    // 5️⃣ Detener todo
    detener() {
        if (this.ws) {
            this.ws.send(JSON.stringify({ accion: "detener" }));
            this.ws.close();
        }
        if (this.eventSource) {
            this.eventSource.close();
        }
    }
    
    // 6️⃣ Iniciar todo
    iniciar() {
        this.iniciarPlanificacion();
        this.iniciarTracking();
    }
}

// Uso
const sistema = new SistemaPlanificacionYTracking();
sistema.iniciar();
```

---

### 📊 Flujo de Datos Completo

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND (React/JS)                       │
└─────────────────────────────────────────────────────────────┘
                 ▲                            ▲
                 │                            │
                 │ WebSocket                  │ SSE
                 │ (Planificación)           │ (Tracking)
                 │                            │
┌────────────────┴──────────┐   ┌────────────┴─────────────┐
│  PlanificacionWSHandler    │   │  VueloTrackingService    │
│  - Ejecuta AG              │   │  - Lee vuelos de BD      │
│  - Asigna pedidos          │   │  - Calcula coordenadas   │
│  - Guarda en BD            │   │  - Stream SSE            │
└────────────────────────────┘   └──────────────────────────┘
                 │                            │
                 └──────────────┬─────────────┘
                                ▼
                    ┌────────────────────────┐
                    │   Base de Datos MySQL  │
                    │   - vuelo_pedido       │
                    │   - pedido             │
                    │   - plan_de_vuelo      │
                    └────────────────────────┘
```

**Flujo:**
1. Frontend envía `iniciar` por WebSocket
2. Backend ejecuta AG y guarda vuelos en BD
3. Frontend recibe progreso del AG por WebSocket
4. **Simultáneamente**, SSE lee vuelos de BD y envía coordenadas
5. Frontend grafica aviones con datos del SSE

---

### ⚠️ Notas Importantes

1. **Dos conexiones separadas:**
   - WebSocket para planificación (eventos puntuales del AG)
   - SSE para tracking (stream continuo de coordenadas)

2. **El SSE funciona automáticamente:**
   - No necesitas "iniciarlo" manualmente
   - Lee los vuelos de la BD y calcula coordenadas
   - Envía updates cada X segundos

3. **Los datos se complementan:**
   - WebSocket → Decisiones del AG (qué pedidos en qué vuelos)
   - SSE → Posiciones en tiempo real (dónde está cada avión)

4. **Para graficar necesitas:**
   - Librería de mapas (Leaflet, Google Maps, Mapbox, etc.)
   - `currentLatitude` y `currentLongitude` del SSE
   - Opcional: `progress` para mostrar % del viaje

---

## �🔗 Enlaces Relacionados

- [Documentación del Algoritmo Genético](ALGORITMO_GENETICO_ESPAÑOL.md)
- [Ciclo de Vida del Tracking](CICLO_VIDA_TRACKING.md)
- [Mejoras de Rendimiento](MEJORAS_RENDIMIENTO.md)
- [Sincronización de Tiempo](SINCRONIZACION_TIEMPO_VUELOS.md)

---

**Versión:** 2.0  
**Última actualización:** 19 de noviembre de 2025  
**Estado:** ✅ Producción (Planificación + Tracking)


EJEMPLO
{
    "tipo": "completado",
    "mensaje": "Planificación #1 completada",
    "metricas": null,
    "solucion": {
        "vuelos": [
            {
                "fechaInicial": "2025-01-02 01:05",
                "fechaFinal": "2025-01-02 17:51",
                "origenCodigoICAO": "EBCI",
                "destinoCodigoICAO": "SCEL",
                "pedidos": [
                    {
                        "idPedido": 4655645,
                        "cantidad": 2
                    }
                ],
                "totalPaquetes": 2
            },
            {
                "fechaInicial": "2025-01-01 22:23",
                "fechaFinal": "2025-01-02 06:15",
                "origenCodigoICAO": "EBCI",
                "destinoCodigoICAO": "OAKB",
                "pedidos": [
                    {
                        "idPedido": 4655646,
                        "cantidad": 2
                    }
                ],
                "totalPaquetes": 2
            }
        ],
        "totalVuelos": 2,
        "totalPedidos": 2
    },
    "datos": {
        "duracionRealMs": 5147,
        "proximoTiempo": "2025-01-02T00:25",
        "ejecucionNumero": 1,
        "tiempoSimulacionActual": "2025-01-02T00:00",
        "pedidosPlanificados": 0,
        "avanceSimuladoMin": 25
    }
}