# 📦 Ejemplos de Código - WebSocket

Ejemplos listos para copiar y pegar en tu aplicación.

---

## 1️⃣ Ejemplo Básico - Conectar y Recibir Mensajes

```javascript
import { conectarWebSocket } from './config/websocket';

// Estado
let miWebSocket = null;

// Conectar
function iniciarWebSocket() {
  miWebSocket = conectarWebSocket(
    // Cuando llega un mensaje
    (data) => {
      console.log('📨 Mensaje recibido:', data);
      document.getElementById('mensajes').innerHTML += 
        `<div>${JSON.stringify(data)}</div>`;
    },
    
    // Si hay error
    (error) => {
      console.error('❌ Error:', error);
      alert('Error en WebSocket');
    },
    
    // Cuando se conecta
    () => {
      console.log('✅ Conectado');
      document.getElementById('estado').textContent = '🟢 Conectado';
    },
    
    // Cuando se desconecta
    () => {
      console.log('🔴 Desconectado');
      document.getElementById('estado').textContent = '🔴 Desconectado';
      miWebSocket = null;
    }
  );
}

// Desconectar
function detenerWebSocket() {
  if (miWebSocket) {
    miWebSocket.cerrar();
  }
}

// Enviar mensaje
function enviarMensaje() {
  if (miWebSocket && miWebSocket.estaConectado()) {
    miWebSocket.enviar({ tipo: 'test', mensaje: 'Hola!' });
  }
}
```

**HTML:**
```html
<div id="estado">🔴 Desconectado</div>
<button onclick="iniciarWebSocket()">Conectar</button>
<button onclick="detenerWebSocket()">Desconectar</button>
<button onclick="enviarMensaje()">Enviar</button>
<div id="mensajes"></div>
```

---

## 2️⃣ Ejemplo con React - Hook useWebSocket

```javascript
import React, { useState } from 'react';
import { useWebSocket } from './config/websocket';

function ComponenteWebSocket() {
  const [mensajes, setMensajes] = useState([]);
  
  // El hook maneja automáticamente la conexión
  const { ws, isConnected } = useWebSocket(
    (data) => {
      // Agregar mensaje a la lista
      setMensajes(prev => [...prev, data]);
    },
    (error) => {
      console.error('Error:', error);
    }
  );

  return (
    <div style={{ padding: '20px' }}>
      <h2>WebSocket Demo</h2>
      
      {/* Estado */}
      <div style={{ marginBottom: '20px' }}>
        Estado: {isConnected ? '🟢 Conectado' : '🔴 Desconectado'}
      </div>

      {/* Botón para enviar */}
      <button 
        onClick={() => ws?.enviar({ tipo: 'test', data: new Date().toISOString() })}
        disabled={!isConnected}
        style={{
          padding: '10px 20px',
          background: isConnected ? '#007bff' : '#ccc',
          color: 'white',
          border: 'none',
          borderRadius: '5px',
          cursor: isConnected ? 'pointer' : 'not-allowed'
        }}
      >
        Enviar Mensaje
      </button>

      {/* Lista de mensajes */}
      <div style={{ marginTop: '20px' }}>
        <h3>Mensajes recibidos: {mensajes.length}</h3>
        <div style={{ 
          maxHeight: '300px', 
          overflow: 'auto',
          border: '1px solid #ddd',
          padding: '10px',
          borderRadius: '5px',
          fontFamily: 'monospace',
          fontSize: '12px'
        }}>
          {mensajes.map((msg, idx) => (
            <div key={idx} style={{ 
              padding: '5px',
              borderBottom: '1px solid #eee',
              marginBottom: '5px'
            }}>
              {JSON.stringify(msg, null, 2)}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default ComponenteWebSocket;
```

---

## 3️⃣ Ejemplo Completo - SimuladorSemanal con WebSocket

```javascript
import React, { useState, useEffect, useRef } from 'react';
import { conectarWebSocket } from '../../../config/websocket';
import {
  consultarEstadoWebSocket,
  activarWebSocket,
  desactivarWebSocket
} from '../../../config/api';

function SimuladorSemanal() {
  // Estados
  const [flights, setFlights] = useState([]);
  const [horaSimulada, setHoraSimulada] = useState(null);
  const [tickActual, setTickActual] = useState(0);
  const [wsConectado, setWsConectado] = useState(false);
  const [wsActivo, setWsActivo] = useState(false);
  const wsRef = useRef(null);

  // Función para convertir ruta del backend a vuelo
  const convertirRutaAVuelo = (ruta) => {
    const deltaLat = ruta.destinoLatitud - ruta.origenLatitud;
    const deltaLng = ruta.destinoLongitud - ruta.origenLongitud;
    
    return {
      id: ruta.idRuta,
      currentLat: ruta.origenLatitud + (deltaLat * ruta.progress),
      currentLng: ruta.origenLongitud + (deltaLng * ruta.progress),
      progress: ruta.progress,
      origin: { code: ruta.origenCodigo },
      destination: { code: ruta.destinoCodigo },
      currentPackages: ruta.totalPackages,
      packageCapacity: ruta.capacidad,
      status: ruta.enVuelo ? 'active' : 'landed',
      rotation: Math.atan2(deltaLng, deltaLat) * (180 / Math.PI)
    };
  };

  // Conectar WebSocket
  const handleConectarWS = async () => {
    try {
      // 1. Verificar estado del backend
      const estado = await consultarEstadoWebSocket();
      console.log('Estado WS backend:', estado);
      
      // 2. Activar si no está activo
      if (!estado.activo) {
        await activarWebSocket();
        console.log('WebSocket activado en backend');
      }
      
      // 3. Conectar cliente
      const ws = conectarWebSocket(
        // onMessage - Procesar mensajes
        (data) => {
          console.log('📨 Mensaje WS:', data);
          
          switch(data.tipo) {
            case 'simulacion':
              // Actualizar tiempo
              setHoraSimulada(data.data.horaSimulada);
              setTickActual(data.data.tickActual);
              break;
              
            case 'vuelos':
              // Actualizar vuelos
              const vuelosNuevos = data.data.vuelos.map(convertirRutaAVuelo);
              setFlights(vuelosNuevos);
              console.log(`✈️ Actualizados ${vuelosNuevos.length} vuelos`);
              break;
              
            case 'aeropuertos':
              // Actualizar aeropuertos
              console.log('🏢 Actualizando aeropuertos:', data.data.aeropuertos);
              break;
              
            case 'alerta':
              // Mostrar alerta
              console.warn('⚠️ Alerta:', data.data.mensaje);
              break;
              
            default:
              console.log('Mensaje desconocido:', data);
          }
        },
        
        // onError
        (error) => {
          console.error('❌ Error WebSocket:', error);
          setWsConectado(false);
        },
        
        // onOpen
        () => {
          console.log('✅ WebSocket conectado');
          setWsConectado(true);
        },
        
        // onClose
        (event) => {
          console.log('🔌 WebSocket desconectado:', event.code);
          setWsConectado(false);
          wsRef.current = null;
        }
      );
      
      wsRef.current = ws;
      
    } catch (error) {
      console.error('Error al conectar:', error);
      alert('Error al conectar WebSocket: ' + error.message);
    }
  };

  // Desconectar WebSocket
  const handleDesconectarWS = () => {
    if (wsRef.current) {
      wsRef.current.cerrar();
      setWsConectado(false);
    }
  };

  // Consultar estado
  const handleConsultarEstado = async () => {
    try {
      const estado = await consultarEstadoWebSocket();
      setWsActivo(estado.activo);
      alert(`WebSocket backend: ${estado.activo ? 'Activo' : 'Inactivo'}\n${estado.mensaje}`);
    } catch (error) {
      alert('Error al consultar estado: ' + error.message);
    }
  };

  // Cleanup al desmontar
  useEffect(() => {
    return () => {
      if (wsRef.current) {
        console.log('Limpiando WebSocket...');
        wsRef.current.cerrar();
      }
    };
  }, []);

  return (
    <div style={{ padding: '20px' }}>
      <h1>Simulador Semanal con WebSocket</h1>
      
      {/* Panel de control WebSocket */}
      <div style={{
        background: '#fff3cd',
        border: '1px solid #ffc107',
        borderRadius: '8px',
        padding: '15px',
        marginBottom: '20px'
      }}>
        <h3>🔌 Control WebSocket</h3>
        
        {/* Indicadores */}
        <div style={{ display: 'flex', gap: '20px', marginBottom: '15px' }}>
          <span style={{ 
            color: wsConectado ? '#28a745' : '#dc3545',
            fontWeight: '600' 
          }}>
            ● Cliente: {wsConectado ? 'Conectado' : 'Desconectado'}
          </span>
          <span style={{ 
            color: wsActivo ? '#28a745' : '#6c757d',
            fontWeight: '600' 
          }}>
            Backend: {wsActivo ? 'Activo' : 'Inactivo'}
          </span>
        </div>

        {/* Botones */}
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button 
            onClick={handleConectarWS}
            disabled={wsConectado}
            style={{
              padding: '8px 16px',
              background: wsConectado ? '#e9ecef' : '#007bff',
              color: wsConectado ? '#6c757d' : 'white',
              border: 'none',
              borderRadius: '5px',
              cursor: wsConectado ? 'not-allowed' : 'pointer'
            }}
          >
            Conectar
          </button>
          
          <button 
            onClick={handleDesconectarWS}
            disabled={!wsConectado}
            style={{
              padding: '8px 16px',
              background: !wsConectado ? '#e9ecef' : '#dc3545',
              color: !wsConectado ? '#6c757d' : 'white',
              border: 'none',
              borderRadius: '5px',
              cursor: !wsConectado ? 'not-allowed' : 'pointer'
            }}
          >
            Desconectar
          </button>
          
          <button 
            onClick={handleConsultarEstado}
            style={{
              padding: '8px 16px',
              background: '#17a2b8',
              color: 'white',
              border: 'none',
              borderRadius: '5px',
              cursor: 'pointer'
            }}
          >
            Consultar Estado
          </button>
        </div>
      </div>

      {/* Info de simulación */}
      <div style={{
        background: '#f8f9fa',
        borderRadius: '8px',
        padding: '15px',
        marginBottom: '20px'
      }}>
        <p><strong>Hora Simulada:</strong> {horaSimulada || 'N/A'}</p>
        <p><strong>Tick Actual:</strong> {tickActual}</p>
        <p><strong>Vuelos Activos:</strong> {flights.length}</p>
      </div>

      {/* Mapa o lista de vuelos */}
      <div style={{
        border: '1px solid #ddd',
        borderRadius: '8px',
        padding: '15px',
        minHeight: '400px',
        background: 'white'
      }}>
        <h3>Vuelos en el Mapa</h3>
        {flights.length > 0 ? (
          <ul>
            {flights.map(flight => (
              <li key={flight.id}>
                {flight.origin.code} → {flight.destination.code} ({(flight.progress * 100).toFixed(1)}%)
              </li>
            ))}
          </ul>
        ) : (
          <p style={{ color: '#6c757d' }}>
            No hay vuelos activos. Conecta el WebSocket para recibir datos.
          </p>
        )}
      </div>
    </div>
  );
}

export default SimuladorSemanal;
```

---

## 4️⃣ Ejemplo - Reconexión Automática

```javascript
import { conectarWebSocket } from './config/websocket';

let intentosReconexion = 0;
const MAX_INTENTOS = 5;
const RETRASO_BASE = 2000; // 2 segundos

function conectarConReintento() {
  const ws = conectarWebSocket(
    onMessage,
    onError,
    () => {
      console.log('✅ Conectado');
      intentosReconexion = 0; // Resetear contador
    },
    (event) => {
      console.log('Desconectado. Código:', event.code);
      
      // Solo reconectar si no fue cierre intencional (1000)
      if (event.code !== 1000 && intentosReconexion < MAX_INTENTOS) {
        intentosReconexion++;
        
        // Exponential backoff: 2s, 4s, 8s, 16s, 32s
        const retraso = RETRASO_BASE * Math.pow(2, intentosReconexion - 1);
        
        console.log(`Reconectando en ${retraso/1000}s (intento ${intentosReconexion}/${MAX_INTENTOS})`);
        
        setTimeout(() => {
          conectarConReintento();
        }, retraso);
      } else if (intentosReconexion >= MAX_INTENTOS) {
        console.error('❌ Máximo de intentos alcanzado. Reconexión manual requerida.');
        alert('No se pudo reconectar al WebSocket. Por favor, recarga la página.');
      }
    }
  );
  
  return ws;
}
```

---

## 5️⃣ Ejemplo - Throttling (Optimización)

```javascript
import { useRef, useCallback } from 'react';

function useThrottledWebSocket(delay = 1000) {
  const lastUpdate = useRef(Date.now());
  const pendingData = useRef(null);
  const timeoutRef = useRef(null);

  const handleMessage = useCallback((data, callback) => {
    const now = Date.now();
    
    // Si ya pasó el delay, actualizar inmediatamente
    if (now - lastUpdate.current >= delay) {
      callback(data);
      lastUpdate.current = now;
      pendingData.current = null;
    } else {
      // Guardar datos pendientes
      pendingData.current = data;
      
      // Programar actualización
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      
      timeoutRef.current = setTimeout(() => {
        if (pendingData.current) {
          callback(pendingData.current);
          lastUpdate.current = Date.now();
          pendingData.current = null;
        }
      }, delay - (now - lastUpdate.current));
    }
  }, [delay]);

  return handleMessage;
}

// Uso:
function MiComponente() {
  const [vuelos, setVuelos] = useState([]);
  
  // Solo actualizar máximo 1 vez por segundo
  const actualizarVuelosThrottled = useThrottledWebSocket(1000);

  const { ws, isConnected } = useWebSocket(
    (data) => {
      actualizarVuelosThrottled(data, (throttledData) => {
        if (throttledData.tipo === 'vuelos') {
          setVuelos(throttledData.data.vuelos);
        }
      });
    },
    (error) => console.error(error)
  );

  // ...
}
```

---

## 📋 Resumen de Funciones Disponibles

### conectarWebSocket(onMessage, onError, onOpen, onClose)
Conecta al WebSocket y retorna instancia con métodos:
- `ws.enviar(mensaje)` - Enviar mensaje al servidor
- `ws.estaConectado()` - Verificar si está conectado
- `ws.cerrar()` - Cerrar conexión

### useWebSocket(onMessage, onError)
Hook de React que retorna:
- `ws` - Instancia del WebSocket
- `isConnected` - Estado de conexión (boolean)

### Funciones HTTP de Control
- `consultarEstadoWebSocket()` - Verificar estado backend
- `activarWebSocket()` - Activar en backend
- `desactivarWebSocket()` - Desactivar en backend
- `enviarMensajePruebaWS(mensaje)` - Enviar prueba

---

**Tip**: Copia el ejemplo que mejor se adapte a tu caso de uso y personalízalo según tus necesidades.

**Archivos**: Todos los ejemplos asumen que tienes `src/config/websocket.js` y `src/config/api.js` configurados correctamente.
