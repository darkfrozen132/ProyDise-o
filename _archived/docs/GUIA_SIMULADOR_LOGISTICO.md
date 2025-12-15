# 🚀 SIMULADOR LOGÍSTICO EN TIEMPO REAL - GUÍA COMPLETA

## 📋 Tabla de Contenidos

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Instalación](#instalación)
3. [Estructura de Archivos](#estructura-de-archivos)
4. [Integración en tu Aplicación](#integración-en-tu-aplicación)
5. [Configuración](#configuración)
6. [Uso](#uso)
7. [API Reference](#api-reference)
8. [Troubleshooting](#troubleshooting)

---

## 🎯 Resumen Ejecutivo

Sistema completo de simulación logística en tiempo real que integra:
- **REST API** para control de simulaciones (iniciar/pausar/cancelar)
- **WebSocket STOMP** para actualizaciones en tiempo real
- **React Hooks** para gestión de estado
- **UI moderna** con animaciones y diseño responsivo

### Características Principales

✅ **Arquitectura Clean**: Servicio → Hook → Componente  
✅ **Singleton WebSocket**: Una única conexión reutilizable  
✅ **Reconexión Automática**: Hasta 5 intentos  
✅ **Estado Reactivo**: UI actualizada en tiempo real  
✅ **Manejo de Errores**: Robusto y con feedback visual  
✅ **Logs en Tiempo Real**: Consola de eventos  
✅ **Diseño Responsivo**: Mobile, tablet y desktop  

---

## 📦 Instalación

### 1. Instalar Dependencias

```bash
cd /home/leoncio/Documentos/GitHub/ProyDise-o/front
npm install sockjs-client @stomp/stompjs
```

### 2. Verificar Archivos Creados

Los siguientes archivos fueron creados:

```
src/
├── services/
│   └── SimulacionLogisticaService.js    # Servicio de comunicación WebSocket + REST
├── hooks/
│   └── useSimulacionLogistica.js        # Hook personalizado para estado
└── pages/
    └── simulacion/
        ├── SimuladorLogistico.js         # Componente principal UI
        └── SimuladorLogistico.css        # Estilos profesionales
```

---

## 🏗️ Estructura de Archivos

### SimulacionLogisticaService.js

**Propósito**: Gestiona toda la comunicación con el backend

**Características**:
- Singleton pattern (una única instancia)
- Conexión WebSocket STOMP + SockJS
- Llamadas REST API
- Reconexión automática
- Sistema de callbacks para eventos

**Métodos públicos**:
```javascript
simulacionService.connect()                    // Conectar WebSocket
simulacionService.iniciarSimulacion(fecha)     // Iniciar simulación
simulacionService.cancelarSimulacion()         // Cancelar simulación
simulacionService.pausarSimulacion()           // Pausar simulación
simulacionService.reanudarSimulacion()         // Reanudar simulación
simulacionService.disconnect()                 // Desconectar WebSocket

// Callbacks
simulacionService
  .onProgresoAG(callback)
  .onSnapshot(callback)
  .onCompleted(callback)
  .onError(callback)
  .onConnected(callback)
  .onDisconnected(callback)
```

---

### useSimulacionLogistica.js

**Propósito**: Hook personalizado que encapsula toda la lógica de estado

**Estado retornado**:
```javascript
const {
  // Estado general
  estado,              // 'IDLE' | 'CONNECTING' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'CANCELLED' | 'ERROR'
  estaConectado,       // boolean
  sessionId,           // string | null
  error,               // object | null
  
  // Progreso del AG
  progreso: {
    generacionActual,
    maxGeneraciones,
    porcentaje,
    mejorFitness,
    fitnessPromedio,
    pedidosProcesados,
    pedidosTotales,
    fechaSimulada,
    tiempoTranscurrido
  },
  
  // Solución
  solucion: {
    rutas: [],
    metricas: {
      totalRutas,
      totalVuelos,
      costoTotal,
      tiempoPromedioEntrega
    }
  },
  
  // Snapshot
  snapshot: {
    iteration,
    simulatedTime,
    processedOrders,
    totalOrders
  },
  
  // Logs
  logs: [],          // Array de objetos {id, tipo, mensaje, timestamp}
  
  // Stats calculados
  stats: {
    estaActivo,
    puedeIniciar,
    puedeCancelar,
    puedePausar,
    puedeReanudar,
    porcentajeCompleto,
    generaciones,
    pedidos,
    totalRutas,
    totalVuelos,
    costoTotal
  },
  
  // Acciones
  iniciar,           // (fecha: string) => Promise<void>
  cancelar,          // () => Promise<void>
  pausar,            // () => Promise<void>
  reanudar,          // () => Promise<void>
  reiniciar,         // () => void
  conectar           // () => Promise<void>
} = useSimulacionLogistica();
```

---

### SimuladorLogistico.js

**Propósito**: Componente React con UI completa

**Características**:
- Panel de control con input de fecha
- Botones de inicio/pausa/reanudar/cancelar
- Barra de progreso animada
- Métricas del algoritmo genético
- Lista de rutas generadas
- Consola de logs en tiempo real
- Diseño responsivo

---

## 🔌 Integración en tu Aplicación

### Opción 1: Integración Directa (Recomendado)

Importa el componente en tu router o página:

```javascript
// En tu archivo de rutas (e.g., App.js o Router.js)
import SimuladorLogistico from './pages/simulacion/SimuladorLogistico';

function App() {
  return (
    <Router>
      <Routes>
        {/* Otras rutas... */}
        <Route path="/simulador" element={<SimuladorLogistico />} />
      </Routes>
    </Router>
  );
}
```

### Opción 2: Integrar en Página Existente

Si ya tienes una página de simulación, puedes usar solo el hook:

```javascript
import { useSimulacionLogistica } from '../../hooks/useSimulacionLogistica';

function TuComponenteExistente() {
  const {
    estado,
    progreso,
    solucion,
    iniciar,
    cancelar
  } = useSimulacionLogistica();

  // Tu UI personalizada aquí
  return (
    <div>
      <button onClick={() => iniciar('2025-01-15')}>
        Iniciar
      </button>
      
      <div>Progreso: {progreso.porcentaje}%</div>
      
      <button onClick={cancelar}>
        Cancelar
      </button>
    </div>
  );
}
```

### Opción 3: Usar Solo el Servicio

Para integraciones más específicas:

```javascript
import { simulacionService } from '../../services/SimulacionLogisticaService';

// Conectar
await simulacionService.connect();

// Configurar callbacks
simulacionService
  .onProgresoAG((data) => {
    console.log('Progreso:', data);
  })
  .onCompleted((data) => {
    console.log('Completado:', data);
  });

// Iniciar simulación
const sessionId = await simulacionService.iniciarSimulacion('2025-01-15');

// Cancelar
await simulacionService.cancelarSimulacion();

// Desconectar
simulacionService.disconnect();
```

---

## ⚙️ Configuración

### Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto:

```env
# URL del backend (opcional, por defecto http://localhost:8000)
REACT_APP_BACKEND_URL=http://localhost:8000

# O en producción
# REACT_APP_BACKEND_URL=https://api.tudominio.com
```

### Configuración del Servicio

Si necesitas cambiar configuraciones, edita `SimulacionLogisticaService.js`:

```javascript
this.config = {
  baseURL: process.env.REACT_APP_BACKEND_URL || 'http://localhost:8000',
  wsEndpoint: '/ws',
  reconnectDelay: 5000,           // Tiempo entre reconexiones (ms)
  heartbeatIncoming: 4000,        // Heartbeat del servidor
  heartbeatOutgoing: 4000,        // Heartbeat del cliente
  maxReconnectAttempts: 5,        // Máximo de intentos de reconexión
};
```

---

## 🎮 Uso

### Flujo Básico

1. **Usuario abre la aplicación**
   - El componente se monta
   - El WebSocket se conecta automáticamente
   - Estado: `IDLE`

2. **Usuario ingresa fecha y hace click en "Iniciar Simulación"**
   - Se envía POST a `/api/simulations/start` con `{fecha, factorK: 5}`
   - Backend responde con `sessionId`
   - Se suscribe al topic `/topic/simulations/{sessionId}`
   - Estado: `RUNNING`

3. **Backend envía actualizaciones**
   - Cada generación del AG: mensaje tipo `PROGRESO_AG`
   - Cada N segundos: mensaje tipo `Snapshot`
   - UI se actualiza automáticamente

4. **Simulación termina**
   - Backend envía mensaje con `status: 'COMPLETED'`
   - Estado: `COMPLETED`
   - Se desconecta del topic
   - Botón "Iniciar Nueva Simulación" se habilita

### Controles Durante la Simulación

| Acción | Endpoint | Efecto |
|--------|----------|--------|
| **Pausar** | `POST /api/simulations/{sessionId}/pause` | Pausa el AG, mantiene WebSocket |
| **Reanudar** | `POST /api/simulations/{sessionId}/resume` | Continúa el AG |
| **Cancelar** | `POST /api/simulations/{sessionId}/cancel` | Detiene y limpia todo |

---

## 📡 API Reference

### REST API

#### Iniciar Simulación

```http
POST http://localhost:8000/api/simulations/start
Content-Type: application/json

{
  "fecha": "2025-01-15",
  "factorK": 5
}
```

**Response:**
```json
{
  "sessionId": "5c79a813-2e16-4d30-9c9e-639e445a5619",
  "mensaje": "Simulación iniciada exitosamente",
  "topicUrl": "/topic/simulations/5c79a813-2e16-4d30-9c9e-639e445a5619"
}
```

#### Cancelar Simulación

```http
POST http://localhost:8000/api/simulations/{sessionId}/cancel
```

### WebSocket

**URL**: `ws://localhost:8000/ws`  
**Protocolo**: STOMP sobre SockJS  
**Topic**: `/topic/simulations/{sessionId}`

**Tipos de mensajes recibidos**:

1. **Progreso del AG** (`tipo: "PROGRESO_AG"`)
```json
{
  "sessionId": "...",
  "tipo": "PROGRESO_AG",
  "generacion": 15,
  "maxGeneraciones": 20,
  "progreso": 75.0,
  "mejorFitness": 1245.67,
  "solucion": {
    "rutas": [...],
    "metricas": {...}
  }
}
```

2. **Snapshot** (`status: "RUNNING"`)
```json
{
  "sessionId": "...",
  "status": "RUNNING",
  "iteration": 42,
  "processedOrders": 150,
  "totalOrders": 4440
}
```

3. **Completado** (`status: "COMPLETED"`)
```json
{
  "sessionId": "...",
  "status": "COMPLETED",
  "iteration": 888,
  "processedOrders": 4440,
  "totalOrders": 4440
}
```

4. **Error** (`tipo: "ERROR"`)
```json
{
  "sessionId": "...",
  "tipo": "ERROR",
  "mensaje": "Error al procesar pedidos..."
}
```

---

## 🐛 Troubleshooting

### Problema: "WebSocket no se conecta"

**Síntomas**: Estado siempre en `CONNECTING` o `ERROR`

**Soluciones**:
1. Verifica que el backend esté corriendo en `http://localhost:8000`
2. Verifica CORS en el backend:
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
3. Revisa la consola del navegador (F12) para errores

---

### Problema: "No se reciben mensajes"

**Síntomas**: Simulación inicia pero no hay actualizaciones

**Soluciones**:
1. Verifica que el backend esté enviando mensajes al topic correcto:
   ```java
   messagingTemplate.convertAndSend(
       "/topic/simulations/" + sessionId,
       progresoDTO
   );
   ```
2. Verifica en la consola del navegador que veas:
   ```
   📡 Suscribiéndose al topic: /topic/simulations/{sessionId}
   ✅ Suscripción exitosa
   ```
3. Verifica que el `sessionId` sea el mismo en backend y frontend

---

### Problema: "Error al iniciar simulación"

**Síntomas**: Error al hacer click en "Iniciar Simulación"

**Soluciones**:
1. Verifica la fecha ingresada (debe ser formato `YYYY-MM-DD`)
2. Verifica la respuesta del servidor:
   ```javascript
   // Debe contener:
   {
     "sessionId": "uuid-valido",
     "mensaje": "...",
     "topicUrl": "..."
   }
   ```
3. Revisa logs del backend para errores

---

### Problema: "UI no se actualiza"

**Síntomas**: Mensajes llegan pero UI no cambia

**Soluciones**:
1. Verifica que los callbacks estén registrados:
   ```javascript
   useEffect(() => {
     simulacionService
       .onProgresoAG(manejarProgresoAG)
       .onSnapshot(manejarSnapshot);
   }, []);
   ```
2. Verifica que los estados se estén actualizando en React DevTools
3. Asegúrate de que el componente no se esté desmontando

---

### Problema: "Memoria crece indefinidamente"

**Síntomas**: Después de varias simulaciones, la aplicación se vuelve lenta

**Soluciones**:
1. Verifica que se esté llamando `disconnect()` al desmontar:
   ```javascript
   useEffect(() => {
     return () => {
       simulacionService.disconnect();
     };
   }, []);
   ```
2. Limita el número de logs:
   ```javascript
   const maxLogs = 50; // Ya está configurado
   ```

---

## 🎨 Personalización

### Cambiar Colores

Edita `SimuladorLogistico.css`:

```css
/* Cambiar el gradiente principal */
.simulador-container {
  background: linear-gradient(135deg, #TU_COLOR_1 0%, #TU_COLOR_2 100%);
}

/* Cambiar el gradiente de los paneles */
.panel-header {
  background: linear-gradient(135deg, #TU_COLOR_1 0%, #TU_COLOR_2 100%);
}
```

### Agregar Nuevos Campos

Si el backend envía campos adicionales, agrégalos al estado:

```javascript
// En useSimulacionLogistica.js
const [progreso, setProgreso] = useState({
  // ... campos existentes ...
  tuNuevoCampo: 0  // ← Agregar aquí
});

// En manejarProgresoAG
setProgreso({
  // ... campos existentes ...
  tuNuevoCampo: data.tuNuevoCampo  // ← Mapear aquí
});
```

---

## 📊 Diagrama de Flujo

```
Usuario ingresa fecha
       ↓
Click "Iniciar Simulación"
       ↓
POST /api/simulations/start {fecha, factorK: 5}
       ↓
Backend inicia AG en hilo separado
       ↓
Response: {sessionId}
       ↓
Frontend se suscribe: /topic/simulations/{sessionId}
       ↓
Backend envía actualizaciones cada generación
       ↓
Frontend recibe ProgresoAGDTO
       ↓
Actualiza estado React
       ↓
UI se re-renderiza automáticamente
       ↓
AG completa todas las generaciones
       ↓
Backend envía: {status: "COMPLETED"}
       ↓
Frontend se desuscribe
       ↓
Botón "Nueva Simulación" se habilita
```

---

## 🚀 Próximos Pasos

1. **Probar en desarrollo**
   ```bash
   npm start
   ```

2. **Abrir en navegador**
   ```
   http://localhost:3000/simulador
   ```

3. **Verificar backend corriendo**
   ```bash
   curl http://localhost:8000/api/simulations/start
   ```

4. **Iniciar primera simulación**
   - Ingresa fecha
   - Click "Iniciar Simulación"
   - Observa progreso en tiempo real

5. **Revisar logs**
   - Abre DevTools (F12)
   - Ve a Console
   - Deberías ver logs detallados

---

## 📞 Soporte

Si encuentras problemas:

1. Revisa esta guía completa
2. Revisa la consola del navegador (F12 → Console)
3. Revisa los logs del backend
4. Verifica que los formatos JSON coincidan

---

## ✅ Checklist de Integración

- [ ] Dependencias instaladas (`sockjs-client`, `@stomp/stompjs`)
- [ ] Backend corriendo en `http://localhost:8000`
- [ ] CORS configurado en backend
- [ ] Endpoint `/ws` disponible
- [ ] Endpoint `/api/simulations/start` disponible
- [ ] Component importado en router
- [ ] Variable de entorno configurada (opcional)
- [ ] Primera prueba exitosa

---

**¡Listo para simular! 🚀✨**

Creado con ❤️ para integración perfecta entre Frontend y Backend
