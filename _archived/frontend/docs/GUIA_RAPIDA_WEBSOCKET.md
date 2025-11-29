# 🚀 Guía Rápida - WebSocket Frontend

## ✅ Estado Actual

### Archivos Implementados
- ✅ `src/config/websocket.js` - Cliente WebSocket completo
- ✅ `src/config/api.js` - Funciones HTTP de control
- ✅ `src/pages/simulacion/Simulador/SimuladorSemanal.js` - Panel de pruebas

### Funciones Disponibles

#### Control HTTP (api.js)
```javascript
import { 
  consultarEstadoWebSocket,  // GET /api/websocket/estado
  activarWebSocket,           // GET /api/websocket/activar
  desactivarWebSocket,        // GET /api/websocket/desactivar
  enviarMensajePruebaWS      // GET /api/websocket/test?mensaje=...
} from './config/api';
```

#### Cliente WebSocket (websocket.js)
```javascript
import { 
  conectarWebSocket,  // Función para conectar
  useWebSocket       // Hook de React
} from './config/websocket';
```

---

## 🎯 Cómo Usar (3 formas)

### Opción 1: Panel de Pruebas (Ya implementado)
1. Iniciar servidor: `npm start`
2. Ir a: http://localhost:3000/operaciones/simulador-semanal
3. Buscar panel amarillo "🔌 Prueba WebSocket"
4. Hacer clic en los botones:
   - **Consultar Estado** → Ver si backend está activo
   - **Activar Backend** → Activar WebSocket en servidor
   - **Conectar** → Abrir conexión WebSocket
   - **Enviar Mensaje** → Probar envío

---

### Opción 2: Función Básica
```javascript
import { conectarWebSocket } from './config/websocket';

// 1. Conectar
const ws = conectarWebSocket(
  (data) => console.log('Mensaje:', data),  // onMessage
  (err) => console.error('Error:', err),     // onError
  () => console.log('Conectado'),            // onOpen
  () => console.log('Desconectado')          // onClose
);

// 2. Enviar mensaje
ws.enviar({ tipo: 'test', mensaje: 'Hola' });

// 3. Verificar estado
if (ws.estaConectado()) {
  console.log('Conectado!');
}

// 4. Cerrar
ws.cerrar();
```

---

### Opción 3: Hook de React (Recomendado)
```javascript
import React, { useState } from 'react';
import { useWebSocket } from './config/websocket';

function MiComponente() {
  const [mensajes, setMensajes] = useState([]);
  
  const { ws, isConnected } = useWebSocket(
    (data) => {
      console.log('📨 Nuevo mensaje:', data);
      setMensajes(prev => [...prev, data]);
    },
    (error) => {
      console.error('❌ Error:', error);
    }
  );

  return (
    <div>
      <h3>Estado: {isConnected ? '🟢 Conectado' : '🔴 Desconectado'}</h3>
      
      <button 
        onClick={() => ws?.enviar({ test: true })}
        disabled={!isConnected}
      >
        Enviar Test
      </button>

      <div>
        <h4>Mensajes recibidos:</h4>
        {mensajes.map((msg, i) => (
          <div key={i}>{JSON.stringify(msg)}</div>
        ))}
      </div>
    </div>
  );
}
```

---

## 🔄 Integración con Simulación

### Ejemplo: Actualizar vuelos en tiempo real

```javascript
import { conectarWebSocket } from './config/websocket';

function SimuladorSemanal() {
  const [flights, setFlights] = useState([]);
  const [horaSimulada, setHoraSimulada] = useState(null);
  const wsRef = useRef(null);

  const conectarSimulacion = () => {
    const ws = conectarWebSocket(
      (data) => {
        // Procesar mensajes del backend
        switch(data.tipo) {
          case 'simulacion':
            // Actualizar tiempo
            setHoraSimulada(data.data.horaSimulada);
            setTickActual(data.data.tickActual);
            break;
            
          case 'vuelos':
            // Actualizar vuelos en el mapa
            const vuelosConvertidos = data.data.vuelos.map(ruta => ({
              id: ruta.idRuta,
              currentLat: ruta.origenLatitud + (ruta.destinoLatitud - ruta.origenLatitud) * ruta.progress,
              currentLng: ruta.origenLongitud + (ruta.destinoLongitud - ruta.origenLongitud) * ruta.progress,
              progress: ruta.progress,
              packages: ruta.totalPackages,
              // ... más campos
            }));
            setFlights(vuelosConvertidos);
            break;
            
          case 'aeropuertos':
            // Actualizar aeropuertos
            updateAirports(data.data.aeropuertos);
            break;
            
          case 'alerta':
            // Mostrar alerta
            showNotification(data.data.mensaje, data.data.nivel);
            break;
        }
      },
      (error) => console.error('Error WS:', error),
      () => console.log('Simulación conectada'),
      () => console.log('Simulación desconectada')
    );
    
    wsRef.current = ws;
  };

  // Cleanup
  useEffect(() => {
    return () => {
      if (wsRef.current) {
        wsRef.current.cerrar();
      }
    };
  }, []);

  return (
    <div>
      <button onClick={conectarSimulacion}>
        Iniciar Simulación con WebSocket
      </button>
      {/* Mapa con flights */}
    </div>
  );
}
```

---

## 📋 Checklist de Uso

### Antes de empezar:
- [ ] Backend corriendo en `http://localhost:8000`
- [ ] Frontend corriendo en `http://localhost:3000`
- [ ] Verificar `.env`: `REACT_APP_API_URL=http://127.0.0.1:8000`

### Flujo recomendado:
1. [ ] **Consultar estado** del WebSocket en backend
   ```javascript
   const estado = await consultarEstadoWebSocket();
   ```

2. [ ] **Activar** si está inactivo
   ```javascript
   if (!estado.activo) {
     await activarWebSocket();
   }
   ```

3. [ ] **Conectar** el cliente WebSocket
   ```javascript
   const ws = conectarWebSocket(...);
   ```

4. [ ] **Recibir mensajes** y actualizar UI
   ```javascript
   onMessage: (data) => {
     // Actualizar estados de React
   }
   ```

5. [ ] **Desconectar** al finalizar
   ```javascript
   ws.cerrar();
   ```

---

## 🐛 Solución Rápida de Problemas

### No se conecta el WebSocket

**Verificar backend:**
```bash
curl http://localhost:8000/api/websocket/estado
```

Si da error `Connection refused`:
- Backend no está corriendo
- Iniciar backend con `java -jar backend.jar` o similar

**Activar WebSocket:**
```bash
curl http://localhost:8000/api/websocket/activar
```

---

### WebSocket conecta pero no llegan mensajes

**Desde navegador (F12 → Console):**
```javascript
// 1. Verificar estado
const estado = await fetch('http://localhost:8000/api/websocket/estado').then(r => r.json());
console.log(estado);

// 2. Si no está activo, activar
await fetch('http://localhost:8000/api/websocket/activar');

// 3. Enviar mensaje de prueba desde backend
await fetch('http://localhost:8000/api/websocket/test?mensaje=Hola');
```

---

### WebSocket se desconecta solo

**Agregar reconexión automática:**
```javascript
const conectarConReintento = (intentos = 3) => {
  const ws = conectarWebSocket(
    onMessage,
    onError,
    onOpen,
    (event) => {
      console.log('Desconectado:', event.code);
      
      // Reconectar si no fue cierre normal
      if (event.code !== 1000 && intentos > 0) {
        console.log(`Reconectando en 3s (${intentos} intentos restantes)`);
        setTimeout(() => {
          conectarConReintento(intentos - 1);
        }, 3000);
      }
    }
  );
};
```

---

## 🎨 Formato de Mensajes del Backend

### Simulación
```json
{
  "tipo": "simulacion",
  "data": {
    "horaSimulada": "2024-11-15T10:30:00",
    "tickActual": 1580,
    "activa": true
  }
}
```

### Vuelos
```json
{
  "tipo": "vuelos",
  "data": {
    "vuelos": [
      {
        "idRuta": "R001",
        "origenCodigo": "SKBO",
        "destinoCodigo": "SKCL",
        "progress": 0.65,
        "totalPackages": 120
      }
    ]
  }
}
```

### Aeropuertos
```json
{
  "tipo": "aeropuertos",
  "data": {
    "aeropuertos": [
      {
        "codigoICAO": "SKBO",
        "ciudad": "Bogota",
        "paquetes": 350,
        "capacidad": 430
      }
    ]
  }
}
```

---

## 📞 Recursos

- **Documentación completa**: `DOCUMENTACION_WEBSOCKET.md`
- **Panel de pruebas**: http://localhost:3000/operaciones/simulador-semanal
- **Logs del frontend**: Abrir consola (F12) y buscar 🔌 📡 ✅ ❌

---

**Tip**: Usa el panel de pruebas primero para verificar que todo funciona antes de integrar en tu código.

**Versión**: 1.0.0  
**Actualizado**: 19 nov 2025
