# 🚀 Guía de Integración WebSocket STOMP - Package Planner

## 📋 Tabla de Contenidos

1. [Introducción](#introducción)
2. [Arquitectura](#arquitectura)
3. [Instalación](#instalación)
4. [Uso Básico](#uso-básico)
5. [Uso Avanzado](#uso-avanzado)
6. [API Reference](#api-reference)
7. [Ejemplos Completos](#ejemplos-completos)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

---

## 🎯 Introducción

Esta guía te enseña cómo integrar la funcionalidad de **Simulación en Vivo** del backend `package-planner` en tu aplicación React. La solución utiliza:

- ✅ **REST API** para control (iniciar/cancelar simulaciones)
- ✅ **WebSockets (STOMP)** para datos en tiempo real
- ✅ **SockJS** como fallback para compatibilidad
- ✅ **React Hooks** para integración declarativa

### Contrato del Backend

```
📍 REST Endpoints:
   POST   /api/simulations              → Iniciar simulación
   POST   /api/simulations/{id}/cancel  → Cancelar simulación

🔌 WebSocket:
   Endpoint: /ws (SockJS + STOMP)
   Tópico:   /topic/simulations/{simulationId}
```

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────┐
│                  Componente React                    │
│  ┌──────────────────────────────────────────────┐  │
│  │         useSimulation() Hook                 │  │
│  │  • Estado reactivo                           │  │
│  │  • Métodos simplificados                     │  │
│  │  • Limpieza automática                       │  │
│  └────────────────┬─────────────────────────────┘  │
│                   │                                  │
│  ┌────────────────▼─────────────────────────────┐  │
│  │       SimulationService                      │  │
│  │  • Gestión de conexión STOMP                │  │
│  │  • Suscripción a tópicos dinámicos          │  │
│  │  • Reconexión automática                    │  │
│  │  • Llamadas REST                            │  │
│  └────────────────┬─────────────────────────────┘  │
└───────────────────┼──────────────────────────────────┘
                    │
      ┌─────────────▼──────────────┐
      │    Backend (Spring Boot)   │
      │  • REST Controllers        │
      │  • WebSocket Handler       │
      │  • STOMP Broker            │
      └────────────────────────────┘
```

---

## 📦 Instalación

### 1. Instalar Dependencias

```bash
npm install sockjs-client @stomp/stompjs
```

### 2. Estructura de Archivos

Los siguientes archivos ya fueron creados en tu proyecto:

```
src/
├── services/
│   └── SimulationService.js     # ✅ Servicio principal
├── hooks/
│   └── useSimulation.js         # ✅ Hook de React
└── components/
    ├── SimuladorEnVivo.js       # ✅ Componente de ejemplo
    └── SimuladorEnVivo.css      # ✅ Estilos
```

### 3. Configurar URL del Backend

Crea o edita tu archivo `.env`:

```bash
# .env
REACT_APP_BACKEND_URL=http://localhost:8080
```

---

## 🚀 Uso Básico

### Opción 1: Usar el Hook (Recomendado)

El hook `useSimulation` es la forma más simple de integrar la simulación:

```jsx
import React from 'react';
import { useSimulation } from './hooks/useSimulation';

function MiComponente() {
  const {
    // Estado
    isConnected,
    isSubscribed,
    status,
    progress,
    routes,
    
    // Métodos
    startSimulation,
    cancelSimulation
  } = useSimulation({
    autoConnect: true,    // Conectar automáticamente
    windowMinutes: 60     // Ventana de tiempo por defecto
  });

  return (
    <div>
      <h2>Estado: {status}</h2>
      <p>Progreso: {progress.toFixed(1)}%</p>
      
      <button onClick={() => startSimulation()}>
        Iniciar
      </button>
      
      <button onClick={() => cancelSimulation()}>
        Detener
      </button>
      
      <ul>
        {routes.map((route, i) => (
          <li key={i}>Ruta {i + 1}</li>
        ))}
      </ul>
    </div>
  );
}
```

### Opción 2: Usar el Servicio Directamente

Si necesitas más control, usa el servicio directamente:

```jsx
import React, { useEffect, useState } from 'react';
import SimulationService from './services/SimulationService';

function MiComponente() {
  const [service] = useState(() => new SimulationService());
  const [data, setData] = useState(null);

  useEffect(() => {
    // Configurar callbacks
    service
      .onMessage((snapshot) => {
        console.log('Datos recibidos:', snapshot);
        setData(snapshot);
      })
      .onError((error) => {
        console.error('Error:', error);
      });

    // Conectar y iniciar
    const init = async () => {
      await service.connect();
      await service.startSimulation(60);
    };

    init();

    // Limpieza al desmontar
    return () => {
      service.disconnect();
    };
  }, [service]);

  return (
    <div>
      {data && (
        <div>
          <p>Estado: {data.status}</p>
          <p>Progreso: {data.processedOrders}/{data.totalOrders}</p>
        </div>
      )}
    </div>
  );
}
```

---

## 🎨 Uso Avanzado

### Integrar con tu SimuladorSemanal Existente

Aquí te muestro cómo integrar el nuevo sistema en tu componente `SimuladorSemanal.js`:

```jsx
import React, { useState, useEffect } from 'react';
import { useSimulation } from '../hooks/useSimulation';

const SimuladorSemanal = () => {
  const {
    isConnected,
    isSubscribed,
    status,
    progress,
    routes,
    processedOrders,
    totalOrders,
    currentFitness,
    startSimulation,
    cancelSimulation,
    error
  } = useSimulation({ autoConnect: true });

  const [windowMinutes, setWindowMinutes] = useState(60);

  // Actualizar el mapa cuando lleguen nuevas rutas
  useEffect(() => {
    if (routes.length > 0) {
      actualizarMapa(routes);
    }
  }, [routes]);

  const actualizarMapa = (rutas) => {
    // Convertir rutas del backend al formato de tu mapa
    const flights = rutas.map(convertirRutaAVuelo);
    setFlights(flights);
  };

  const convertirRutaAVuelo = (ruta) => {
    // Tu lógica de conversión existente
    return {
      id: ruta.id,
      origin: { /* ... */ },
      destination: { /* ... */ },
      // ...
    };
  };

  return (
    <div>
      {/* Indicador de conexión */}
      <div className="connection-status">
        {isConnected ? '🟢 Conectado' : '🔴 Desconectado'}
      </div>

      {/* Controles */}
      <div className="controls">
        <input
          type="number"
          value={windowMinutes}
          onChange={(e) => setWindowMinutes(Number(e.target.value))}
        />
        <button onClick={() => startSimulation(windowMinutes)}>
          Iniciar
        </button>
        <button onClick={() => cancelSimulation()}>
          Detener
        </button>
      </div>

      {/* Barra de progreso */}
      <div className="progress">
        <div className="progress-bar" style={{ width: `${progress}%` }} />
        <span>{processedOrders} / {totalOrders}</span>
      </div>

      {/* Mapa (tu implementación existente) */}
      <MapContainer>
        {/* ... */}
      </MapContainer>
    </div>
  );
};
```

### Manejo de Múltiples Simulaciones

Si necesitas ejecutar varias simulaciones simultáneas:

```jsx
import React, { useState } from 'react';
import SimulationService from './services/SimulationService';

function MultiSimulador() {
  const [simulaciones, setSimulaciones] = useState([]);

  const crearNuevaSimulacion = async () => {
    const service = new SimulationService();
    
    service.onMessage((data) => {
      // Actualizar estado de esta simulación
      setSimulaciones(prev => 
        prev.map(sim => 
          sim.id === data.simulationId 
            ? { ...sim, data } 
            : sim
        )
      );
    });

    await service.connect();
    const id = await service.startSimulation(60);
    
    setSimulaciones(prev => [...prev, { id, service, data: null }]);
  };

  return (
    <div>
      <button onClick={crearNuevaSimulacion}>
        + Nueva Simulación
      </button>
      
      {simulaciones.map(sim => (
        <div key={sim.id}>
          <h3>Simulación {sim.id}</h3>
          {sim.data && (
            <p>Progreso: {sim.data.progress}%</p>
          )}
        </div>
      ))}
    </div>
  );
}
```

---

## 📚 API Reference

### SimulationService

#### Métodos

##### `connect(): Promise<void>`
Establece conexión WebSocket con el backend.

```javascript
await service.connect();
```

##### `startSimulation(windowMinutes): Promise<string>`
Inicia una nueva simulación y se suscribe automáticamente.

```javascript
const simulationId = await service.startSimulation(60);
```

**Parámetros:**
- `windowMinutes` (number): Ventana de tiempo en minutos (default: 60)

**Retorna:** ID de la simulación

##### `subscribe(simulationId): Promise<void>`
Se suscribe manualmente a un tópico específico.

```javascript
await service.subscribe('abc-123-def-456');
```

##### `cancelSimulation(): Promise<void>`
Cancela la simulación actual.

```javascript
await service.cancelSimulation();
```

##### `disconnect(): void`
Desconecta y limpia todos los recursos.

```javascript
service.disconnect();
```

#### Callbacks

##### `onMessage(callback)`
Se ejecuta cada vez que llega un snapshot.

```javascript
service.onMessage((data) => {
  console.log('Status:', data.status);
  console.log('Progress:', data.processedOrders, '/', data.totalOrders);
  console.log('Routes:', data.routes.length);
});
```

**Formato del Snapshot:**
```typescript
{
  status: 'RUNNING' | 'COMPLETED' | 'CANCELLED',
  processedOrders: number,
  totalOrders: number,
  currentFitness: number,
  routes: Array<Route>
}
```

##### `onError(callback)`
Se ejecuta cuando ocurre un error.

```javascript
service.onError((error) => {
  console.error('Error:', error.message);
  // Mostrar notificación al usuario
});
```

##### `onConnected(callback)`
Se ejecuta cuando la conexión se establece.

```javascript
service.onConnected(() => {
  console.log('¡Conectado!');
});
```

##### `onDisconnected(callback)`
Se ejecuta cuando se pierde la conexión.

```javascript
service.onDisconnected(() => {
  console.log('Desconectado');
  // Intentar reconectar
});
```

##### `onSimulationComplete(callback)`
Se ejecuta cuando la simulación termina.

```javascript
service.onSimulationComplete((finalData) => {
  console.log('Simulación completada:', finalData);
  // Mostrar resumen final
});
```

#### Propiedades

##### `isConnected(): boolean`
Verifica si está conectado al WebSocket.

```javascript
if (service.isConnected()) {
  console.log('Conectado');
}
```

##### `isSubscribed(): boolean`
Verifica si está suscrito a una simulación.

```javascript
if (service.isSubscribed()) {
  console.log('Suscrito');
}
```

##### `getSimulationId(): string | null`
Obtiene el ID de la simulación actual.

```javascript
const id = service.getSimulationId();
```

---

### useSimulation Hook

#### Opciones

```javascript
const options = {
  autoConnect: true,    // Conectar automáticamente al montar
  windowMinutes: 60     // Ventana de tiempo por defecto
};

const { /* ... */ } = useSimulation(options);
```

#### Retorno

```javascript
const {
  // ===== Estado de Conexión =====
  isConnected: boolean,        // ¿Está conectado al WebSocket?
  isSubscribed: boolean,       // ¿Está suscrito a una simulación?
  isLoading: boolean,          // ¿Hay una operación en curso?
  error: string | null,        // Mensaje de error (si existe)
  
  // ===== Datos de Simulación =====
  simulationId: string | null, // ID de la simulación actual
  simulationData: object,      // Snapshot completo más reciente
  status: string,              // RUNNING, COMPLETED, CANCELLED
  progress: number,            // Progreso en porcentaje (0-100)
  routes: Array,               // Lista de rutas activas
  processedOrders: number,     // Pedidos procesados
  totalOrders: number,         // Total de pedidos
  currentFitness: number,      // Fitness actual
  
  // ===== Métodos =====
  connect: () => Promise<void>,
  startSimulation: (minutes?) => Promise<string>,
  cancelSimulation: () => Promise<void>,
  disconnect: () => void,
  reset: () => void,           // Limpiar estado
  
  // ===== Servicio Raw =====
  service: SimulationService   // Acceso directo al servicio
} = useSimulation();
```

---

## 💡 Ejemplos Completos

### Ejemplo 1: Componente Minimalista

```jsx
import React from 'react';
import { useSimulation } from './hooks/useSimulation';

function SimuladorMinimo() {
  const { 
    isConnected, 
    progress, 
    startSimulation, 
    cancelSimulation 
  } = useSimulation();

  return (
    <div style={{ padding: '20px' }}>
      <div style={{ marginBottom: '10px' }}>
        Estado: {isConnected ? '🟢 Conectado' : '🔴 Desconectado'}
      </div>
      
      <div style={{ marginBottom: '10px' }}>
        Progreso: {progress.toFixed(1)}%
      </div>
      
      <div style={{ 
        width: '100%', 
        height: '20px', 
        background: '#eee',
        borderRadius: '10px',
        overflow: 'hidden',
        marginBottom: '10px'
      }}>
        <div style={{
          width: `${progress}%`,
          height: '100%',
          background: '#4CAF50',
          transition: 'width 0.3s'
        }} />
      </div>
      
      <button onClick={() => startSimulation()}>Iniciar</button>
      <button onClick={() => cancelSimulation()}>Detener</button>
    </div>
  );
}

export default SimuladorMinimo;
```

### Ejemplo 2: Con Material-UI

```jsx
import React from 'react';
import { 
  Box, 
  Button, 
  LinearProgress, 
  Typography, 
  Chip 
} from '@mui/material';
import { PlayArrow, Stop } from '@mui/icons-material';
import { useSimulation } from './hooks/useSimulation';

function SimuladorMUI() {
  const {
    isConnected,
    status,
    progress,
    processedOrders,
    totalOrders,
    startSimulation,
    cancelSimulation
  } = useSimulation();

  const getStatusColor = () => {
    switch (status) {
      case 'RUNNING': return 'primary';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'warning';
      default: return 'default';
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ mb: 2, display: 'flex', gap: 1, alignItems: 'center' }}>
        <Chip 
          label={isConnected ? 'Conectado' : 'Desconectado'}
          color={isConnected ? 'success' : 'error'}
          size="small"
        />
        {status && (
          <Chip 
            label={status}
            color={getStatusColor()}
            size="small"
          />
        )}
      </Box>

      <Box sx={{ mb: 2 }}>
        <Typography variant="body2" color="text.secondary">
          {processedOrders} / {totalOrders} pedidos procesados
        </Typography>
        <LinearProgress 
          variant="determinate" 
          value={progress}
          sx={{ mt: 1 }}
        />
      </Box>

      <Box sx={{ display: 'flex', gap: 1 }}>
        <Button
          variant="contained"
          color="success"
          startIcon={<PlayArrow />}
          onClick={() => startSimulation()}
          disabled={status === 'RUNNING'}
        >
          Iniciar
        </Button>
        <Button
          variant="contained"
          color="error"
          startIcon={<Stop />}
          onClick={() => cancelSimulation()}
          disabled={status !== 'RUNNING'}
        >
          Detener
        </Button>
      </Box>
    </Box>
  );
}

export default SimuladorMUI;
```

### Ejemplo 3: Integración con Leaflet

```jsx
import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Polyline } from 'react-leaflet';
import { useSimulation } from './hooks/useSimulation';

function SimuladorConMapa() {
  const { routes, startSimulation } = useSimulation();

  // Convertir rutas del backend a formato Leaflet
  const polylines = routes.map(route => ({
    positions: [
      [route.origin.lat, route.origin.lng],
      [route.destination.lat, route.destination.lng]
    ],
    color: route.status === 'active' ? 'blue' : 'gray'
  }));

  useEffect(() => {
    // Iniciar automáticamente
    startSimulation(60);
  }, []);

  return (
    <div>
      <MapContainer 
        center={[0, 0]} 
        zoom={2} 
        style={{ height: '600px' }}
      >
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        
        {polylines.map((line, i) => (
          <Polyline 
            key={i}
            positions={line.positions}
            color={line.color}
            weight={2}
          />
        ))}
      </MapContainer>
      
      <div style={{ padding: '20px' }}>
        <p>Rutas activas: {routes.length}</p>
      </div>
    </div>
  );
}

export default SimuladorConMapa;
```

---

## 🐛 Troubleshooting

### Problema 1: "No se puede conectar al WebSocket"

**Síntoma:** Error `ERR_CONNECTION_REFUSED` o timeout

**Soluciones:**

1. Verificar que el backend esté corriendo:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. Verificar la URL en `.env`:
   ```bash
   REACT_APP_BACKEND_URL=http://localhost:8080
   ```

3. Verificar CORS en el backend (Spring Boot):
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

### Problema 2: "No se recibe el ID de simulación"

**Síntoma:** Error `No se recibió un ID de simulación válido del servidor`

**Solución:** El servicio busca automáticamente en estos campos:
- `data.simulationId`
- `data.id`
- `data.uuid`

Verifica que tu backend devuelva el ID en alguno de estos campos.

**Backend correcto:**
```json
{
  "simulationId": "abc-123-def-456",
  "status": "STARTED"
}
```

### Problema 3: "No llegan mensajes al tópico"

**Síntoma:** Conectado y suscrito, pero sin datos

**Diagnóstico:**

1. Activa el modo debug:
   ```javascript
   // En desarrollo
   process.env.NODE_ENV = 'development';
   ```

2. Verifica el tópico en la consola:
   ```
   📡 Suscribiéndose al tópico: /topic/simulations/abc-123
   ```

3. Verifica que el backend esté publicando en el tópico correcto:
   ```java
   messagingTemplate.convertAndSend(
     "/topic/simulations/" + simulationId,
     snapshot
   );
   ```

### Problema 4: "Múltiples conexiones abiertas"

**Síntoma:** Múltiples websockets abiertos simultáneamente

**Causa:** React Strict Mode ejecuta efectos dos veces en desarrollo

**Solución:** Asegúrate de tener cleanup:
```jsx
useEffect(() => {
  const service = new SimulationService();
  
  // Tu código...
  
  return () => {
    service.disconnect(); // ✅ IMPORTANTE
  };
}, []);
```

### Problema 5: "Error 401 Unauthorized"

**Síntoma:** Error de autenticación al conectar

**Solución:** Si tu backend requiere autenticación, agrega headers:

```javascript
// En SimulationService.js, modifica el método startSimulation:
const response = await fetch(`${REST_BASE_URL}/api/simulations`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}` // ✅ Agregar token
  },
  body: JSON.stringify({ windowMinutes })
});
```

---

## ✅ Best Practices

### 1. Siempre Limpiar Recursos

```jsx
useEffect(() => {
  const service = new SimulationService();
  
  // ...
  
  return () => {
    service.disconnect(); // ✅ Evita memory leaks
  };
}, []);
```

### 2. Manejar Errores Gracefully

```jsx
const { error } = useSimulation();

// Mostrar error al usuario
{error && (
  <div className="alert alert-danger">
    ⚠️ {error}
    <button onClick={handleRetry}>Reintentar</button>
  </div>
)}
```

### 3. Deshabilitar Botones Durante Operaciones

```jsx
const { isLoading, status } = useSimulation();

<button 
  onClick={startSimulation}
  disabled={isLoading || status === 'RUNNING'}
>
  {isLoading ? 'Iniciando...' : 'Iniciar'}
</button>
```

### 4. Usar Estado Derivado

```jsx
const { status, processedOrders, totalOrders } = useSimulation();

// Calcular métricas derivadas
const isComplete = status === 'COMPLETED';
const remainingOrders = totalOrders - processedOrders;
const estimatedTime = (remainingOrders / 10) * 60; // segundos
```

### 5. Optimizar Renders con useMemo

```jsx
const routeMarkers = useMemo(() => {
  return routes.map(route => ({
    lat: route.origin.lat,
    lng: route.origin.lng,
    // ...
  }));
}, [routes]);
```

### 6. Logging Condicional

```jsx
if (process.env.NODE_ENV === 'development') {
  console.log('Debug:', data);
}
```

### 7. Validar Datos del Backend

```jsx
service.onMessage((data) => {
  if (!data || !data.status) {
    console.warn('Datos inválidos:', data);
    return;
  }
  
  // Procesar datos válidos
  setSimulationData(data);
});
```

---

## 🎓 Próximos Pasos

1. ✅ **Lee esta guía completa**
2. ✅ **Prueba el componente de ejemplo** (`SimuladorEnVivo.js`)
3. ✅ **Integra el hook en tu componente** (`SimuladorSemanal.js`)
4. ✅ **Conecta el mapa con las rutas** del WebSocket
5. ✅ **Personaliza los estilos** según tu diseño
6. ✅ **Agrega manejo de errores** robusto
7. ✅ **Prueba en producción** con datos reales

---

## 📞 Soporte

Si tienes problemas:

1. Revisa la consola del navegador (F12)
2. Verifica los logs del backend
3. Revisa la sección de [Troubleshooting](#troubleshooting)
4. Activa el modo debug en `SimulationService.js`

---

## 📄 Licencia

Este código es parte del proyecto MoraPack Dashboard.

---

**¡Feliz Codificación! 🚀**
