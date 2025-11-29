# 📡 Documentación WebSocket - Integración Backend → Frontend

## 🔗 Configuración Base

### URLs
- **HTTP API:** `http://127.0.0.1:8000/api`
- **WebSocket:** `ws://127.0.0.1:8000/api/websocket/conectar`

---

## 📋 Endpoints HTTP (Control del WebSocket)

### 1. Consultar Estado
```bash
GET /api/websocket/estado
```

**Respuesta:**
```json
{
  "activo": true,
  "mensaje": "WebSocket está activo"
}
```

**Uso en Frontend:**
```javascript
import { consultarEstadoWebSocket } from './config/api';

const estado = await consultarEstadoWebSocket();
console.log(estado.activo); // true o false
```

---

### 2. Activar WebSocket
```bash
GET /api/websocket/activar
```

**Respuesta:**
```json
{
  "mensaje": "WebSocket activado"
}
```

**Uso en Frontend:**
```javascript
import { activarWebSocket } from './config/api';

await activarWebSocket();
```

---

### 3. Desactivar WebSocket
```bash
GET /api/websocket/desactivar
```

**Respuesta:**
```json
{
  "mensaje": "WebSocket desactivado"
}
```

**Uso en Frontend:**
```javascript
import { desactivarWebSocket } from './config/api';

await desactivarWebSocket();
```

---

### 4. Enviar Mensaje de Prueba
```bash
GET /api/websocket/test?mensaje={texto}
```

**Respuesta:**
```json
{
  "enviado": true,
  "mensaje": "Texto enviado"
}
```

**Uso en Frontend:**
```javascript
import { enviarMensajePruebaWS } from './config/api';

await enviarMensajePruebaWS("Hola desde React");
```

---

## 🔌 Conexión WebSocket

### Endpoint
```
ws://127.0.0.1:8000/api/websocket/conectar
```

### Implementación en Frontend

#### Opción 1: Función Directa
```javascript
import { conectarWebSocket } from './config/websocket';

// Conectar al WebSocket
const ws = conectarWebSocket(
  // onMessage - cuando llega un mensaje
  (data) => {
    console.log('📨 Mensaje recibido:', data);
    
    // Procesar según tipo
    if (data.tipo === 'actualizacion') {
      // Actualizar estado
    }
  },
  
  // onError - cuando hay error
  (error) => {
    console.error('❌ Error WebSocket:', error);
  },
  
  // onOpen - cuando se conecta
  () => {
    console.log('✅ WebSocket conectado');
  },
  
  // onClose - cuando se desconecta
  (event) => {
    console.log('🔌 WebSocket desconectado:', event.code);
  }
);

// Métodos disponibles:
ws.enviar({ tipo: 'comando', accion: 'pausar' });  // Enviar mensaje
ws.estaConectado();  // Verificar estado
ws.cerrar();  // Cerrar conexión
```

#### Opción 2: Hook de React (Recomendado)
```javascript
import { useWebSocket } from './config/websocket';

function MiComponente() {
  const { ws, isConnected } = useWebSocket(
    (data) => {
      console.log('Mensaje:', data);
      // Actualizar estado de React aquí
    },
    (error) => {
      console.error('Error:', error);
    }
  );

  return (
    <div>
      <p>Estado: {isConnected ? '🟢 Conectado' : '🔴 Desconectado'}</p>
      <button 
        onClick={() => ws?.enviar({ test: true })}
        disabled={!isConnected}
      >
        Enviar Mensaje
      </button>
    </div>
  );
}
```

---

## 📡 Formato de Mensajes

### Del Servidor → Cliente

El backend envía mensajes en formato JSON con esta estructura:

#### 1. Actualización de Simulación
```json
{
  "tipo": "simulacion",
  "timestamp": "2024-11-19T19:13:00",
  "data": {
    "horaSimulada": "2024-11-15T10:30:00",
    "tickActual": 1580,
    "tiempoRealMs": 158000,
    "activa": true,
    "pausada": false,
    "timeScale": 1000
  }
}
```

#### 2. Actualización de Vuelos
```json
{
  "tipo": "vuelos",
  "timestamp": "2024-11-19T19:13:00",
  "data": {
    "vuelos": [
      {
        "idRuta": "R001",
        "origenCodigo": "SKBO",
        "destinoCodigo": "SKCL",
        "origenLatitud": 4.7011,
        "origenLongitud": -74.1469,
        "destinoLatitud": 3.5431,
        "destinoLongitud": -76.3816,
        "progress": 0.65,
        "enVuelo": true,
        "totalPackages": 120,
        "capacidad": 200
      }
    ],
    "totalVuelos": 45,
    "vuelosActivos": 28
  }
}
```

#### 3. Actualización de Aeropuertos
```json
{
  "tipo": "aeropuertos",
  "timestamp": "2024-11-19T19:13:00",
  "data": {
    "aeropuertos": [
      {
        "codigoICAO": "SKBO",
        "ciudad": "Bogota",
        "capacidadAlmacen": 430,
        "capacidadDisponible": 80,
        "paquetes": 350,
        "ocupacion": 81.4
      }
    ]
  }
}
```

#### 4. Alertas
```json
{
  "tipo": "alerta",
  "timestamp": "2024-11-19T19:13:00",
  "data": {
    "nivel": "warning",
    "mensaje": "Aeropuerto SKBO alcanzó 90% de capacidad",
    "aeropuerto": "SKBO"
  }
}
```

#### 5. Mensaje de Prueba
```json
{
  "tipo": "test",
  "timestamp": "2024-11-19T19:13:00",
  "mensaje": "Hola desde backend",
  "datos": "Información adicional"
}
```

---

## 🎯 Implementación Completa en SimuladorSemanal

### Paso 1: Importar funciones
```javascript
import { 
  consultarEstadoWebSocket,
  activarWebSocket,
  desactivarWebSocket,
  enviarMensajePruebaWS
} from '../../../config/api';
import { conectarWebSocket } from '../../../config/websocket';
```

### Paso 2: Agregar estados
```javascript
const [wsConectado, setWsConectado] = useState(false);
const [wsActivo, setWsActivo] = useState(false);
const [mensajesWS, setMensajesWS] = useState([]);
const wsRef = useRef(null);
```

### Paso 3: Función de conexión
```javascript
const handleConectarWS = () => {
  if (wsRef.current) {
    console.warn('⚠️ WebSocket ya está conectado');
    return;
  }

  const ws = conectarWebSocket(
    // Manejar mensajes
    (data) => {
      console.log('📨 Mensaje recibido:', data);
      
      // Procesar según tipo de mensaje
      switch(data.tipo) {
        case 'simulacion':
          // Actualizar tiempo de simulación
          setHoraSimulada(data.data.horaSimulada);
          setTickActual(data.data.tickActual);
          setTiempoRealMs(data.data.tiempoRealMs);
          break;
          
        case 'vuelos':
          // Actualizar vuelos en el mapa
          const vuelosConvertidos = data.data.vuelos.map(ruta => 
            convertirRutaAVuelo(ruta)
          );
          setFlights(vuelosConvertidos);
          break;
          
        case 'aeropuertos':
          // Actualizar información de aeropuertos
          updateAirports(data.data.aeropuertos);
          break;
          
        case 'alerta':
          // Mostrar alerta al usuario
          showAlert(data.data.mensaje, data.data.nivel);
          break;
          
        case 'test':
          // Mensaje de prueba
          console.log('Mensaje de prueba:', data.mensaje);
          break;
      }
      
      // Guardar en historial
      setMensajesWS(prev => [...prev.slice(-50), data]);
    },
    
    // Error
    (error) => {
      console.error('❌ Error WebSocket:', error);
      setWsConectado(false);
    },
    
    // Conectado
    () => {
      console.log('✅ WebSocket conectado!');
      setWsConectado(true);
    },
    
    // Desconectado
    () => {
      console.log('🔌 WebSocket desconectado');
      setWsConectado(false);
      wsRef.current = null;
    }
  );
  
  wsRef.current = ws;
};
```

### Paso 4: Cleanup
```javascript
useEffect(() => {
  return () => {
    if (wsRef.current) {
      console.log('🔌 Cerrando WebSocket al desmontar...');
      wsRef.current.cerrar();
    }
  };
}, []);
```

---

## 🔄 Flujo de Trabajo Recomendado

### Inicialización
```javascript
// 1. Verificar si el backend tiene WebSocket activo
const estado = await consultarEstadoWebSocket();

if (!estado.activo) {
  // 2. Activar si no está activo
  await activarWebSocket();
}

// 3. Conectar el cliente
handleConectarWS();
```

### Durante la Simulación
```javascript
// El WebSocket envía actualizaciones automáticamente cada segundo (o según configuración)
// Solo necesitas escuchar y procesar los mensajes en onMessage
```

### Finalización
```javascript
// 1. Desconectar cliente
handleDesconectarWS();

// 2. (Opcional) Desactivar en backend
await desactivarWebSocket();
```

---

## 🎨 Panel de Control UI

Ya implementado en `SimuladorSemanal.js`:

```jsx
<div style={{ background: '#fff3cd', padding: '15px', borderRadius: '8px' }}>
  <h4>🔌 WebSocket Control</h4>
  
  {/* Indicadores */}
  <div style={{ display: 'flex', gap: '20px', marginBottom: '15px' }}>
    <span style={{ color: wsConectado ? '#28a745' : '#dc3545' }}>
      ● {wsConectado ? 'Conectado' : 'Desconectado'}
    </span>
    <span style={{ color: wsActivo ? '#28a745' : '#6c757d' }}>
      Backend: {wsActivo ? 'Activo' : 'Inactivo'}
    </span>
    <span>Mensajes: {mensajesWS.length}</span>
  </div>

  {/* Botones */}
  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
    <button onClick={handleConectarWS} disabled={wsConectado}>
      Conectar
    </button>
    <button onClick={handleDesconectarWS} disabled={!wsConectado}>
      Desconectar
    </button>
    <button onClick={handleActivarWS}>
      Activar Backend
    </button>
    <button onClick={handleDesactivarWS}>
      Desactivar Backend
    </button>
    <button onClick={handleConsultarEstadoWS}>
      Consultar Estado
    </button>
    <button onClick={handleEnviarMensajeWS}>
      Enviar Prueba
    </button>
  </div>

  {/* Últimos mensajes */}
  {mensajesWS.length > 0 && (
    <div style={{ marginTop: '15px', maxHeight: '100px', overflow: 'auto' }}>
      {mensajesWS.slice(-5).map((msg, idx) => (
        <div key={idx}>{JSON.stringify(msg)}</div>
      ))}
    </div>
  )}
</div>
```

---

## 🐛 Troubleshooting

### Problema: ERR_CONNECTION_REFUSED

**Causa:** El backend no está corriendo o el puerto es incorrecto.

**Solución:**
```bash
# Verificar que el backend esté corriendo
curl http://localhost:8000/api/websocket/estado

# Verificar .env
cat .env | grep REACT_APP_API_URL
```

---

### Problema: WebSocket se conecta pero no llegan mensajes

**Causa:** El WebSocket está desactivado en el backend.

**Solución:**
```bash
# Activar en el backend
curl http://localhost:8000/api/websocket/activar

# O desde el frontend
await activarWebSocket();
```

---

### Problema: WebSocket se desconecta constantemente

**Causa:** Timeout del servidor o red inestable.

**Solución:** Implementar reconexión automática:
```javascript
const handleConectarWS = (intentosReconexion = 3) => {
  // ... código de conexión ...
  
  // En onClose, agregar:
  (event) => {
    console.log('Desconectado:', event.code);
    
    if (event.code !== 1000 && intentosReconexion > 0) {
      console.log(`Reconectando... (${intentosReconexion} intentos restantes)`);
      setTimeout(() => {
        handleConectarWS(intentosReconexion - 1);
      }, 3000);
    }
  }
};
```

---

## ✅ Checklist de Implementación

- [x] `src/config/websocket.js` - Lógica WebSocket
- [x] `src/config/api.js` - Funciones HTTP de control
- [x] Estados en SimuladorSemanal
- [x] Función `handleConectarWS()`
- [x] Panel de control UI
- [x] Cleanup en useEffect
- [ ] **Integrar mensajes con estados de simulación**
- [ ] **Actualizar mapa en tiempo real**
- [ ] **Agregar reconexión automática**
- [ ] **Optimizar rendimiento con throttling**

---

## 🚀 Próximos Pasos

1. **Reemplazar SSE por WebSocket** en la simulación principal
2. **Agregar reconexión automática** con exponential backoff
3. **Optimizar actualizaciones del mapa** con `requestAnimationFrame`
4. **Agregar indicadores visuales** de conexión en el mapa
5. **Implementar sistema de notificaciones** para alertas

---

## 📞 Soporte

**Archivos importantes:**
- `src/config/websocket.js` - Cliente WebSocket
- `src/config/api.js` - API HTTP
- `src/pages/simulacion/Simulador/SimuladorSemanal.js` - Implementación completa

**Logs útiles:**
- Abrir consola del navegador (F12)
- Buscar mensajes con emojis: 🔌 📨 ✅ ❌

**Panel de pruebas:**
- Ir a: http://localhost:3000/operaciones/simulador-semanal
- Buscar el panel amarillo "🔌 Prueba WebSocket"

---

**Versión:** 1.0.0  
**Fecha:** 19 de noviembre de 2025  
**Backend:** http://127.0.0.1:8000
