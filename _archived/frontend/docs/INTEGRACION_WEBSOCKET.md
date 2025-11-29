# 🚀 Integración WebSocket y API

## 📁 Estructura de archivos

```
src/config/
├── api.js           # ✅ HTTP REST + SSE (Server-Sent Events)
├── websocket.js     # ✅ WebSocket bidireccional
├── ejemplos.js      # 📚 Ejemplos de uso
└── README.md        # 📖 Documentación completa
```

---

## 🔧 Configuración rápida

### 1. Variables de entorno (`.env`)
```properties
REACT_APP_API_URL=http://127.0.0.1:8000
```

### 2. Importaciones

#### Para API HTTP/REST:
```javascript
import { 
  getAirports, 
  iniciarSimulacion, 
  detenerSimulacion 
} from './config/api';
```

#### Para WebSocket:
```javascript
import { conectarWebSocket } from './config/websocket';
```

---

## 🎯 Casos de uso

### ✈️ Caso 1: Solo recibir datos (SSE)
**Usa:** `api.js` → `conectarStreamSimulacion()`

```javascript
const eventSource = conectarStreamSimulacion(
  (data) => console.log(data)
);
```

**Ventajas:**
- ✅ Reconexión automática
- ✅ Más simple
- ✅ Compatible con proxies

**Desventajas:**
- ❌ Solo recibe datos (no envía)

---

### 🔄 Caso 2: Comunicación bidireccional (WebSocket)
**Usa:** `websocket.js` → `conectarWebSocket()`

```javascript
const ws = conectarWebSocket(
  (data) => console.log(data)
);

// Enviar datos al servidor
ws.enviar({ comando: 'pausar' });
```

**Ventajas:**
- ✅ Bidireccional (enviar y recibir)
- ✅ Más rápido
- ✅ Tiempo real

**Desventajas:**
- ❌ Requiere manejar reconexión manualmente
- ❌ Algunos proxies pueden bloquearlo

---

### 🎨 Caso 3: Mixto (Recomendado)
**Usa:** `api.js` para control + `websocket.js` para datos

```javascript
// Control con HTTP
await iniciarSimulacion();

// Datos en tiempo real con WebSocket
const ws = conectarWebSocket((data) => {
  // Actualizar UI con datos en tiempo real
});
```

**Ventajas:**
- ✅ Lo mejor de ambos mundos
- ✅ Control HTTP confiable
- ✅ Datos en tiempo real

---

## 📊 Comparación

| Feature | SSE (api.js) | WebSocket (websocket.js) |
|---------|--------------|---------------------------|
| **Protocolo** | HTTP | WS/WSS |
| **Dirección** | Servidor → Cliente | ↔️ Bidireccional |
| **Reconexión** | 🟢 Automática | 🟡 Manual |
| **Enviar datos** | ❌ No | ✅ Sí |
| **Compatibilidad** | 🟢 Alta | 🟡 Media |
| **Uso típico** | Notificaciones, logs | Chat, control en tiempo real |

---

## 🛠️ Endpoints del Backend

### HTTP REST (api.js)
```
POST   /api/simulacion/iniciar
POST   /api/simulacion/pausar
POST   /api/simulacion/reanudar
POST   /api/simulacion/detener
GET    /api/simulacion/estado
GET    /api/aeropuertos/listar
GET    /api/vuelos/listar
```

### SSE (api.js)
```
GET    /api/simulacion/stream   (text/event-stream)
```

### WebSocket (websocket.js)
```
WS     /api/simulacion/ws
```

---

## 📝 Ejemplo completo en SimuladorSemanal

```javascript
import React, { useEffect, useRef, useState } from 'react';
import { iniciarSimulacion, detenerSimulacion } from '../config/api';
import { conectarWebSocket } from '../config/websocket';

function SimuladorSemanal() {
  const [isConnected, setIsConnected] = useState(false);
  const [tickActual, setTickActual] = useState(0);
  const [rutas, setRutas] = useState([]);
  const wsRef = useRef(null);

  // Conectar WebSocket
  useEffect(() => {
    wsRef.current = conectarWebSocket(
      (data) => {
        setTickActual(data.tickActual || 0);
        setRutas(data.rutasSolucion || []);
      },
      (error) => console.error(error),
      () => setIsConnected(true),
      () => setIsConnected(false)
    );

    return () => wsRef.current?.cerrar();
  }, []);

  // Iniciar con HTTP
  const handleIniciar = async () => {
    await iniciarSimulacion();
  };

  // Detener con HTTP
  const handleDetener = async () => {
    await detenerSimulacion();
  };

  return (
    <div>
      <p>Estado WS: {isConnected ? '🟢' : '🔴'}</p>
      <p>Tick: {tickActual}</p>
      <button onClick={handleIniciar}>Iniciar</button>
      <button onClick={handleDetener}>Detener</button>
    </div>
  );
}
```

---

## 🐛 Troubleshooting

### WebSocket no conecta
1. ✅ Verificar que backend esté corriendo
2. ✅ Verificar URL en `.env`
3. ✅ Verificar CORS en backend
4. ✅ Abrir consola y buscar errores de WebSocket

### SSE no recibe datos
1. ✅ Verificar endpoint `/api/simulacion/stream`
2. ✅ Backend debe responder con `Content-Type: text/event-stream`
3. ✅ Verificar que el servidor envíe datos en formato correcto

### Error CORS
```java
// Backend Spring Boot
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("*");
            }
        };
    }
}
```

---

## 📚 Más información

Ver `src/config/README.md` para documentación completa y más ejemplos.
