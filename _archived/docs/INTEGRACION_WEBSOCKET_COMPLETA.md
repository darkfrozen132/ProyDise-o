# 🚀 GUÍA COMPLETA: Integración WebSocket + REST para Simulación en Tiempo Real

## 📋 Tabla de Contenidos

1. [Resumen del Sistema](#resumen-del-sistema)
2. [Arquitectura](#arquitectura)
3. [Archivos Implementados](#archivos-implementados)
4. [Flujo de Comunicación](#flujo-de-comunicación)
5. [Cómo Usar](#cómo-usar)
6. [Troubleshooting](#troubleshooting)

---

## 🎯 Resumen del Sistema

Este sistema implementa una **comunicación bidireccional en tiempo real** entre el frontend (React) y el backend (Spring Boot) para la simulación de rutas de paquetes.

### Tecnologías Clave:
- **Frontend**: React + SockJS-Client + STOMP.js
- **Backend**: Spring Boot + WebSocket + STOMP
- **Protocolo**: REST (control) + WebSocket (datos en tiempo real)

---

## 🏗️ Arquitectura

```
FRONTEND                          BACKEND
━━━━━━━━                          ━━━━━━━

Simulador.js                      Spring Boot
    ↓                                 ↓
SimulationService.js              /api/simulations (REST)
    ↓                                 ↓
STOMP + SockJS    ←―――――→        /ws (WebSocket)
                                      ↓
/topic/simulations/{id}  ←――――  SimulationService
```

**Flujo Simple:**
1. **Iniciar**: POST `/api/simulations` → recibe `sessionId`
2. **Conectar**: WebSocket a `/ws`
3. **Escuchar**: Subscribe a `/topic/simulations/{sessionId}`
4. **Cancelar**: POST `/api/simulations/{sessionId}/cancel`

---

## 📂 Archivos Implementados

### ✅ 1. `src/config/api.js`
**Propósito**: Configuración centralizada de URLs

```javascript
// URLs configurables
const API_BASE_URL = "http://localhost:8000";
const REST_API_URL = `${API_BASE_URL}/api`;
const WS_URL = `${API_BASE_URL}/ws`;
```

**Ajustar según tu entorno**:
- Desarrollo local: `http://localhost:8000`
- Producción: Usa variable de entorno `REACT_APP_BACKEND_URL`

---

### ✅ 2. `src/services/SimulationService.js`
**Propósito**: Servicio que encapsula toda la lógica de comunicación

#### Métodos Principales:

| Método | Descripción | Retorno |
|--------|-------------|---------|
| `connect()` | Conecta al WebSocket | `Promise<void>` |
| `startSimulation(windowMinutes)` | Inicia simulación (POST + Subscribe) | `Promise<string>` (ID) |
| `cancelSimulation()` | Cancela simulación activa | `Promise<void>` |
| `disconnect()` | Limpia conexión y recursos | `void` |

#### Callbacks Disponibles:

```javascript
simulationService
  .onMessage((data) => {
    // Recibe snapshots cada 500ms
  })
  .onError((error) => {
    // Maneja errores
  })
  .onConnected(() => {
    // Se conectó exitosamente
  })
  .onDisconnected(() => {
    // Se desconectó
  })
  .onSimulationComplete((finalData) => {
    // Simulación terminó
  });
```

---

### ✅ 3. `src/pages/simulacion/Monitoreo/Simulador_NEW.js`
**Propósito**: Componente React principal (El Cerebro)

#### Características:

- ✅ **Conecta automáticamente** al montar el componente
- ✅ **Desconecta automáticamente** al desmontar
- ✅ **Maneja el ciclo completo**: Conectar → Iniciar → Escuchar → Detener
- ✅ **Actualiza UI en tiempo real**: Progreso, stats, rutas, logs
- ✅ **Manejo robusto de errores**: Alertas y logs detallados

#### Estados Manejados:

| Estado | Descripción |
|--------|-------------|
| `IDLE` | Listo para iniciar |
| `CONNECTING` | Conectando al WebSocket |
| `RUNNING` | Simulación en curso |
| `COMPLETED` | Simulación terminada exitosamente |
| `CANCELLED` | Simulación cancelada por el usuario |
| `ERROR` | Error en la conexión o simulación |

---

## 🔄 Flujo de Comunicación (Paso a Paso)

### 📌 Paso 1: Montar el Componente

```javascript
// Al montar, el componente:
useEffect(() => {
  simulationService.connect();  // Conecta WebSocket
}, []);
```

### 📌 Paso 2: Usuario Click en "Iniciar Simulación"

```javascript
const handleStart = async () => {
  // 1. Hace POST a /api/simulations
  const id = await simulationService.startSimulation(60);
  
  // 2. Automáticamente se suscribe a /topic/simulations/{id}
  // (esto lo hace internamente el servicio)
};
```

### 📌 Paso 3: Backend Envía Snapshots

```javascript
// Cada 500ms, el backend envía:
{
  "status": "RUNNING",
  "processedOrders": 150,
  "totalOrders": 5000,
  "currentFitness": 1234.56,
  "routes": [
    {
      "id": "route-1",
      "stops": [
        { "lat": 40.7128, "lng": -74.0060, "code": "JFK" },
        { "lat": 51.5074, "lng": -0.1278, "code": "LHR" }
      ],
      "fitness": 123.45
    }
  ]
}
```

### 📌 Paso 4: Frontend Recibe y Actualiza UI

```javascript
const handleMessage = (data) => {
  setProgress((data.processedOrders / data.totalOrders) * 100);
  setStats({...});
  setRoutes(data.routes);
  // UI se actualiza automáticamente
};
```

### 📌 Paso 5: Usuario Cancela o Simulación Termina

```javascript
// Si usuario cancela:
await simulationService.cancelSimulation();

// Si backend termina:
// Backend envía: { "status": "COMPLETED", ... }
// Frontend detecta y ejecuta callback onSimulationComplete
```

### 📌 Paso 6: Desmontar Componente

```javascript
useEffect(() => {
  return () => {
    simulationService.disconnect();  // Limpia todo
  };
}, []);
```

---

## 🎮 Cómo Usar

### 1️⃣ **Configurar el Backend**

Asegúrate de que tu backend esté corriendo en `http://localhost:8000` con:
- ✅ Endpoint REST: `POST /api/simulations`
- ✅ Endpoint WebSocket: `/ws` (SockJS + STOMP)
- ✅ Topic: `/topic/simulations/{id}`

### 2️⃣ **Instalar Dependencias**

```bash
cd front
npm install sockjs-client @stomp/stompjs
```

### 3️⃣ **Usar el Nuevo Componente**

**Opción A: Reemplazar archivo existente**
```bash
mv src/pages/simulacion/Monitoreo/Simulador.js src/pages/simulacion/Monitoreo/Simulador_OLD.js
mv src/pages/simulacion/Monitoreo/Simulador_NEW.js src/pages/simulacion/Monitoreo/Simulador.js
mv src/pages/simulacion/Monitoreo/Simulador_NEW.css src/pages/simulacion/Monitoreo/Simulador.css
```

**Opción B: Importar directamente**
```javascript
import Simulador from './pages/simulacion/Monitoreo/Simulador_NEW';
```

### 4️⃣ **Iniciar la Aplicación**

```bash
npm start
```

### 5️⃣ **Probar la Simulación**

1. Abre el navegador en `http://localhost:3000`
2. Navega al Simulador
3. Verifica que el estado sea "🟢 Conectado"
4. Click en "🚀 Iniciar Simulación"
5. Observa la barra de progreso actualizarse en tiempo real
6. Verifica que las rutas aparezcan en el mapa
7. Los logs mostrarán cada snapshot recibido

---

## 🐛 Troubleshooting

### ❌ Problema: "Error al conectar con el servidor"

**Causa**: Backend no está corriendo o URL incorrecta

**Solución**:
1. Verifica que el backend esté en `http://localhost:8000`
2. Revisa `src/config/api.js` y ajusta la URL
3. Verifica CORS en el backend:
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

---

### ❌ Problema: "No se recibió un ID válido del servidor"

**Causa**: El backend no devuelve el ID en la respuesta del POST

**Solución**:
1. Verifica la respuesta del endpoint `POST /api/simulations`:
   ```json
   {
     "simulationId": "abc-123",  // o "id" o "uuid"
     ...
   }
   ```
2. El servicio busca automáticamente: `data.simulationId || data.id || data.uuid`

---

### ❌ Problema: "No se reciben mensajes del WebSocket"

**Causa**: No se está suscribiendo al topic correcto

**Solución**:
1. Verifica en la consola del navegador el log: `📡 Suscribiéndose al tópico: /topic/simulations/{id}`
2. Verifica que el backend esté enviando a ese mismo topic:
   ```java
   messagingTemplate.convertAndSend(
       "/topic/simulations/" + simulationId,
       snapshot
   );
   ```

---

### ❌ Problema: "Las rutas no se muestran en el mapa"

**Causa**: El formato de las rutas no coincide con lo esperado

**Solución**:
1. El componente espera:
   ```json
   {
     "routes": [
       {
         "id": "route-1",
         "stops": [
           { "latitude": 40.7128, "longitude": -74.0060 }
         ]
       }
     ]
   }
   ```
2. Ajusta el código en `Simulador.js` según tu formato:
   ```javascript
   const lat = stop.latitude || stop.lat || 0;
   const lng = stop.longitude || stop.lng || 0;
   ```

---

## 🎉 ¡Listo!

Ahora tienes un sistema completo de simulación en tiempo real con:
- ✅ Conexión WebSocket robusta
- ✅ Manejo automático de reconexión
- ✅ UI reactiva y actualizada en tiempo real
- ✅ Logs detallados para debugging
- ✅ Limpieza automática de recursos

---

## 📞 Soporte

Si tienes problemas:
1. Revisa los logs del navegador (F12 → Console)
2. Revisa los logs del backend
3. Verifica que el formato de datos coincida entre frontend y backend
4. Usa las herramientas de desarrollo de React para ver el estado en tiempo real

---

**Desarrollado con ❤️ para integración perfecta entre Frontend y Backend**
