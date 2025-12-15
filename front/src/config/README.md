# Configuración de API y WebSocket

Este directorio contiene la configuración para comunicarse con el backend.

## Archivos

### 📡 `api.js` - HTTP y SSE (Server-Sent Events)
Maneja todas las peticiones HTTP (REST API) y SSE.

**Configuración:**
- URL base: `process.env.REACT_APP_API_URL` o fallback a `/api`
- Timeout: 10 segundos
- Headers: `Content-Type: application/json`

**Funciones disponibles:**

#### Aeropuertos
```javascript
import { getAirports } from './config/api';

const airports = await getAirports();
// Retorna: Array de aeropuertos transformados
```

#### Vuelos
```javascript
import { getFlights } from './config/api';

const flights = await getFlights(airports);
// Retorna: Array de vuelos transformados
```

#### Simulación (Control)
```javascript
import { 
  iniciarSimulacion, 
  pausarSimulacion, 
  reanudarSimulacion, 
  detenerSimulacion,
  obtenerEstadoSimulacion 
} from './config/api';

// Iniciar
const response = await iniciarSimulacion();

// Pausar
await pausarSimulacion();

// Reanudar
await reanudarSimulacion();

// Detener
await detenerSimulacion();

// Obtener estado actual (sin stream)
const estado = await obtenerEstadoSimulacion();
```

#### SSE (Server-Sent Events)
```javascript
import { conectarStreamSimulacion } from './config/api';

const eventSource = conectarStreamSimulacion(
  (data) => {
    // Callback cuando llegan datos
    console.log('Estado:', data);
    // data contiene: { tickActual, horaSimulada, timeScale, activa, rutasSolucion, ... }
  },
  (error) => {
    // Callback cuando hay error
    console.error('Error SSE:', error);
  }
);

// Para desconectar:
eventSource.close();
```

---

### 🔌 `websocket.js` - WebSocket (Bidireccional)
Maneja conexiones WebSocket para comunicación en tiempo real bidireccional.

**Configuración:**
- URL base: Convierte automáticamente `http://` a `ws://` y `https://` a `wss://`
- Endpoint: `/api/simulacion/ws`

**Funciones disponibles:**

#### Conexión básica
```javascript
import { conectarWebSocket } from './config/websocket';

const ws = conectarWebSocket(
  (data) => {
    // Callback cuando llegan datos del servidor
    console.log('Datos recibidos:', data);
  },
  (error) => {
    // Callback cuando hay error
    console.error('Error WebSocket:', error);
  },
  () => {
    // Callback cuando se conecta (opcional)
    console.log('Conectado!');
  },
  (event) => {
    // Callback cuando se desconecta (opcional)
    console.log('Desconectado. Código:', event.code);
  }
);

// Enviar mensaje al servidor
ws.enviar({ tipo: 'comando', accion: 'iniciar' });

// Verificar si está conectado
if (ws.estaConectado()) {
  ws.enviar({ mensaje: 'hola' });
}

// Cerrar conexión
ws.cerrar();
```

#### Uso en React
```javascript
import React, { useEffect, useRef, useState } from 'react';
import { conectarWebSocket } from './config/websocket';

function MiComponente() {
  const wsRef = useRef(null);
  const [isConnected, setIsConnected] = useState(false);
  const [data, setData] = useState(null);

  useEffect(() => {
    // Conectar al WebSocket
    wsRef.current = conectarWebSocket(
      (newData) => setData(newData),
      (error) => console.error(error),
      () => setIsConnected(true),
      () => setIsConnected(false)
    );

    // Limpiar al desmontar
    return () => {
      if (wsRef.current) {
        wsRef.current.cerrar();
      }
    };
  }, []);

  const enviarMensaje = () => {
    if (wsRef.current && wsRef.current.estaConectado()) {
      wsRef.current.enviar({ tipo: 'test', data: 'hola' });
    }
  };

  return (
    <div>
      <p>Estado: {isConnected ? 'Conectado' : 'Desconectado'}</p>
      <button onClick={enviarMensaje}>Enviar</button>
      <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>
  );
}
```

---

## Diferencias: SSE vs WebSocket

| Característica | SSE (api.js) | WebSocket (websocket.js) |
|----------------|--------------|---------------------------|
| **Dirección** | Unidireccional (Servidor → Cliente) | Bidireccional |
| **Protocolo** | HTTP | WS/WSS |
| **Reconexión** | Automática (navegador) | Manual |
| **Uso típico** | Recibir actualizaciones del servidor | Chat, control en tiempo real |
| **Enviar datos** | ❌ No | ✅ Sí |

---

## Configuración del Backend

### Variables de entorno (.env)
```properties
REACT_APP_API_URL=http://127.0.0.1:8000
```

### Endpoints requeridos en Spring Boot

#### HTTP/REST
- `POST /api/simulacion/iniciar`
- `POST /api/simulacion/pausar`
- `POST /api/simulacion/reanudar`
- `POST /api/simulacion/detener`
- `GET /api/simulacion/estado`
- `GET /api/aeropuertos/listar`
- `GET /api/vuelos/listar`

#### SSE
- `GET /api/simulacion/stream` (text/event-stream)

#### WebSocket
- `WS /api/simulacion/ws`

---

## Ejemplos de uso completo

### Ejemplo 1: Simulación con SSE
```javascript
import { iniciarSimulacion, conectarStreamSimulacion, detenerSimulacion } from './config/api';

// 1. Iniciar simulación
const response = await iniciarSimulacion();
console.log('Simulación iniciada:', response);

// 2. Conectar al stream
const eventSource = conectarStreamSimulacion(
  (estado) => {
    console.log('Tick:', estado.tickActual);
    console.log('Rutas:', estado.rutasSolucion);
    // Actualizar UI aquí
  },
  (error) => console.error(error)
);

// 3. Detener después de 1 minuto
setTimeout(async () => {
  eventSource.close();
  await detenerSimulacion();
}, 60000);
```

### Ejemplo 2: Simulación con WebSocket
```javascript
import { conectarWebSocket } from './config/websocket';

const ws = conectarWebSocket(
  (data) => {
    console.log('Estado de simulación:', data);
    // Actualizar UI
  },
  (error) => console.error(error),
  () => {
    // Cuando se conecta, enviar comando de inicio
    ws.enviar({ comando: 'iniciar' });
  }
);

// Enviar comando de pausa
setTimeout(() => {
  ws.enviar({ comando: 'pausar' });
}, 30000);

// Cerrar después de 1 minuto
setTimeout(() => {
  ws.enviar({ comando: 'detener' });
  ws.cerrar();
}, 60000);
```

---

## Troubleshooting

### Error: "WebSocket connection failed"
- Verifica que el backend esté corriendo
- Verifica la URL en `.env`
- Asegúrate de que el backend tenga CORS habilitado

### Error: "EventSource failed"
- Verifica que el endpoint `/api/simulacion/stream` esté configurado
- El backend debe responder con `Content-Type: text/event-stream`
- Asegúrate de que SSE esté habilitado en el backend

### Error: "Network Error" en axios
- Verifica la URL del backend en `.env`
- Asegúrate de que el backend tenga CORS habilitado para tu origen
