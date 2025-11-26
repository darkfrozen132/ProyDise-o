# 🚀 PROMPT PARA INTEGRACIÓN FRONTEND - Sistema de Simulación de Logística

## 📋 Contexto

El backend tiene un sistema de simulación logística en tiempo real que usa **WebSocket STOMP** para enviar actualizaciones. Tu tarea es integrar este sistema en el frontend.

---

## 🎯 Objetivo

Crear una interfaz que:
1. Inicie una simulación mediante REST API
2. Se conecte al WebSocket STOMP
3. Reciba actualizaciones en tiempo real
4. Muestre el progreso del algoritmo genético
5. Visualice las rutas de la mejor solución

---

## 🔌 API REST - Control de Simulación

### 1. Iniciar Simulación

**Endpoint:** `POST /api/simulations/start`

**Request Body:**
```json
{
  "fecha": "2025-01-02",
  "factorK": 5
}
```

**Campos:**
- `fecha` (string ISO date): Fecha de inicio de la simulación
- `factorK` (number): Factor de expansión temporal (entre 1 y 1000)

**Response exitoso (200 OK):**
```json
{
  "sessionId": "5c79a813-2e16-4d30-9c9e-639e445a5619",
  "mensaje": "Simulación iniciada exitosamente",
  "topicUrl": "/topic/simulations/5c79a813-2e16-4d30-9c9e-639e445a5619"
}
```

**Response error (500):**
```json
{
  "sessionId": null,
  "mensaje": "Error: <descripción del error>",
  "topicUrl": null
}
```

### 2. Cancelar Simulación

**Endpoint:** `POST /api/simulations/{sessionId}/cancel`

**Response:**
```json
{
  "success": true,
  "message": "Simulación cancelada exitosamente",
  "sessionId": "5c79a813-2e16-4d30-9c9e-639e445a5619",
  "subscriptionTopic": "/topic/simulations/5c79a813-2e16-4d30-9c9e-639e445a5619"
}
```

### 3. Pausar Simulación

**Endpoint:** `POST /api/simulations/{sessionId}/pause`

### 4. Reanudar Simulación

**Endpoint:** `POST /api/simulations/{sessionId}/resume`

---

## 🔄 WebSocket STOMP - Actualizaciones en Tiempo Real

### Configuración de Conexión

**URL WebSocket:** `ws://localhost:8000/ws`

**Protocolo:** STOMP sobre SockJS

**Topic de suscripción:** `/topic/simulations/{sessionId}`

### Dependencias NPM

```bash
npm install sockjs-client @stomp/stompjs
```

### Código de Integración (JavaScript/TypeScript)

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class SimulationClient {
  constructor() {
    this.stompClient = null;
    this.sessionId = null;
  }

  // 1. Iniciar simulación
  async startSimulation(fecha, factorK) {
    const response = await fetch('http://localhost:8000/api/simulations/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fecha, factorK })
    });

    const data = await response.json();
    
    if (data.sessionId) {
      this.sessionId = data.sessionId;
      this.connectWebSocket(data.sessionId);
      return data;
    } else {
      throw new Error(data.mensaje);
    }
  }

  // 2. Conectar WebSocket
  connectWebSocket(sessionId) {
    const socket = new SockJS('http://localhost:8000/ws');
    
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      onConnect: () => {
        console.log('✅ WebSocket conectado');
        
        // Suscribirse al topic de la simulación
        this.stompClient.subscribe(
          `/topic/simulations/${sessionId}`,
          this.handleMessage.bind(this)
        );
      },
      
      onStompError: (frame) => {
        console.error('❌ Error STOMP:', frame.headers['message']);
        console.error('Detalles:', frame.body);
      },
      
      onWebSocketError: (error) => {
        console.error('❌ Error WebSocket:', error);
      }
    });

    this.stompClient.activate();
  }

  // 3. Manejar mensajes recibidos
  handleMessage(message) {
    const data = JSON.parse(message.body);
    
    console.log('📨 Mensaje recibido:', data);
    
    // Identificar tipo de mensaje
    if (data.tipo === 'PROGRESO_AG') {
      this.handleProgresoAG(data);
    } else if (data.status === 'RUNNING' || data.status === 'COMPLETED') {
      this.handleSimulationSnapshot(data);
    } else if (data.tipo === 'ERROR') {
      this.handleError(data);
    }
  }

  // 4. Manejar progreso del Algoritmo Genético
  handleProgresoAG(data) {
    console.log(`🧬 Generación ${data.generacion}/${data.maxGeneraciones}`);
    console.log(`📊 Progreso: ${data.progreso.toFixed(2)}%`);
    console.log(`💯 Mejor fitness: ${data.mejorFitness}`);
    console.log(`📦 Pedidos procesados: ${data.pedidosProcesados}/${data.pedidosTotales}`);
    
    // Actualizar UI con el progreso
    this.updateProgressBar(data.progreso);
    this.updateMetrics({
      generacion: data.generacion,
      maxGeneraciones: data.maxGeneraciones,
      fitness: data.mejorFitness,
      pedidosProcesados: data.pedidosProcesados,
      pedidosTotales: data.pedidosTotales
    });
    
    // Si hay solución, mostrarla
    if (data.solucion) {
      this.displaySolution(data.solucion);
    }
  }

  // 5. Manejar snapshot de simulación
  handleSimulationSnapshot(data) {
    console.log(`🎮 Simulación: ${data.status}`);
    console.log(`⏱️  Tiempo simulado: ${data.simulatedTime}`);
    console.log(`📊 Iteración: ${data.iteration}`);
    
    if (data.status === 'COMPLETED') {
      console.log('✅ Simulación completada');
      this.disconnect();
    }
  }

  // 6. Manejar errores
  handleError(data) {
    console.error('❌ Error en simulación:', data.mensaje);
    alert(`Error: ${data.mensaje}`);
  }

  // 7. Cancelar simulación
  async cancelSimulation() {
    if (!this.sessionId) return;
    
    await fetch(`http://localhost:8000/api/simulations/${this.sessionId}/cancel`, {
      method: 'POST'
    });
    
    this.disconnect();
  }

  // 8. Desconectar WebSocket
  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
      console.log('🔌 WebSocket desconectado');
    }
  }

  // Métodos auxiliares (implementar según tu UI)
  updateProgressBar(percentage) {
    // Actualizar barra de progreso visual
    document.getElementById('progress-bar').style.width = percentage + '%';
  }

  updateMetrics(metrics) {
    // Actualizar métricas en pantalla
    document.getElementById('generacion').textContent = 
      `${metrics.generacion}/${metrics.maxGeneraciones}`;
    document.getElementById('fitness').textContent = 
      metrics.fitness.toFixed(2);
    document.getElementById('pedidos').textContent = 
      `${metrics.pedidosProcesados}/${metrics.pedidosTotales}`;
  }

  displaySolution(solucion) {
    // Mostrar la mejor solución encontrada
    console.log('🎯 Mejor solución:', solucion);
    // Renderizar rutas, métricas, etc.
  }
}

// Uso
const client = new SimulationClient();

// Iniciar simulación
client.startSimulation('2025-01-02', 5)
  .then(response => {
    console.log('Simulación iniciada:', response);
  })
  .catch(error => {
    console.error('Error al iniciar:', error);
  });
```

---

## 📦 Estructura de Mensajes WebSocket

### Tipo 1: Progreso del Algoritmo Genético (`ProgresoAGDTO`)

**Recibido cada vez que el AG completa una generación**

```json
{
  "sessionId": "5c79a813-2e16-4d30-9c9e-639e445a5619",
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
          },
          {
            "origen": "LFPG",
            "destino": "EGLL",
            "vuelo": "BA205",
            "horaSalida": "2025-01-03T08:00:00",
            "horaLlegada": "2025-01-03T09:30:00"
          }
        ]
      }
    ],
    "metricas": {
      "totalRutas": 150,
      "totalVuelos": 320,
      "costoTotal": 45000.0,
      "tiempoPromedioEntrega": 48.5
    }
  },
  "fechaSimulada": "2025-01-02T14:30:00",
  "timestamp": "2025-11-26T06:33:45.123",
  "pedidosProcesados": 150,
  "pedidosTotales": 4440,
  "mensaje": "Generación 15 completada"
}
```

### Tipo 2: Snapshot de Simulación (`SimulationSnapshot`)

**Recibido cada N segundos durante la ejecución**

```json
{
  "sessionId": "5c79a813-2e16-4d30-9c9e-639e445a5619",
  "status": "RUNNING",
  "iteration": 42,
  "simulatedTime": "2025-01-02T14:30:00",
  "processedOrders": 150,
  "totalOrders": 4440,
  "timestamp": "2025-11-26T06:33:45.123",
  "message": "Procesando pedidos...",
  "solution": {
    "routes": [...],
    "metadata": {
      "totalFlights": 320,
      "totalOrders": 150,
      "fitness": 1245.67
    }
  }
}
```

### Tipo 3: Simulación Completada

```json
{
  "sessionId": "5c79a813-2e16-4d30-9c9e-639e445a5619",
  "status": "COMPLETED",
  "iteration": 888,
  "processedOrders": 4440,
  "totalOrders": 4440,
  "timestamp": "2025-11-26T07:15:30.456",
  "message": "Simulación completada exitosamente",
  "solution": {
    "routes": [...],
    "metadata": {
      "totalFlights": 8500,
      "totalOrders": 4440,
      "fitness": 98765.43
    }
  }
}
```

### Tipo 4: Error

```json
{
  "sessionId": "5c79a813-2e16-4d30-9c9e-639e445a5619",
  "tipo": "ERROR",
  "mensaje": "Error al procesar pedidos: timeout en base de datos",
  "timestamp": "2025-11-26T06:35:00.000"
}
```

---

## 🎨 Ejemplo de UI Sugerida

### Componentes Recomendados

1. **Panel de Control**
   - Botón "Iniciar Simulación"
   - Inputs: Fecha, Factor K
   - Botón "Cancelar Simulación"

2. **Panel de Progreso**
   - Barra de progreso (0-100%)
   - Generación actual / Total generaciones
   - Mejor fitness actual
   - Pedidos procesados / Total pedidos
   - Tiempo simulado actual

3. **Panel de Visualización**
   - Mapa con rutas de la mejor solución
   - Lista de vuelos generados
   - Gráfica de evolución del fitness

4. **Panel de Logs**
   - Consola con mensajes en tiempo real
   - Historial de generaciones

---

## 🧪 Testing con HTML Vanilla

Si quieres probar rápidamente sin framework:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Test Simulación</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>
    <style>
        body { font-family: Arial; padding: 20px; }
        .panel { border: 1px solid #ccc; padding: 15px; margin: 10px 0; }
        button { padding: 10px 20px; margin: 5px; }
        #logs { 
            height: 400px; 
            overflow-y: auto; 
            background: #f5f5f5; 
            padding: 10px;
            font-family: monospace;
            font-size: 12px;
        }
        .progress-bar {
            width: 100%;
            height: 30px;
            background: #eee;
            border-radius: 5px;
            overflow: hidden;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #4CAF50, #8BC34A);
            width: 0%;
            transition: width 0.3s;
            text-align: center;
            line-height: 30px;
            color: white;
        }
    </style>
</head>
<body>
    <h1>🚀 Test de Simulación de Logística</h1>
    
    <div class="panel">
        <h2>Control</h2>
        <label>Fecha: <input type="date" id="fecha" value="2025-01-02"></label>
        <label>Factor K: <input type="number" id="factorK" value="5" min="1" max="1000"></label>
        <br><br>
        <button onclick="iniciarSimulacion()">▶️ Iniciar Simulación</button>
        <button onclick="cancelarSimulacion()">⏹️ Cancelar</button>
    </div>
    
    <div class="panel">
        <h2>Progreso</h2>
        <div class="progress-bar">
            <div class="progress-fill" id="progress">0%</div>
        </div>
        <p>Generación: <span id="generacion">-</span></p>
        <p>Mejor Fitness: <span id="fitness">-</span></p>
        <p>Pedidos: <span id="pedidos">-</span></p>
        <p>SessionId: <span id="sessionId">-</span></p>
    </div>
    
    <div class="panel">
        <h2>Logs</h2>
        <div id="logs"></div>
    </div>

    <script>
        let stompClient = null;
        let sessionId = null;

        function log(mensaje) {
            const logs = document.getElementById('logs');
            const timestamp = new Date().toLocaleTimeString();
            logs.innerHTML += `[${timestamp}] ${mensaje}<br>`;
            logs.scrollTop = logs.scrollHeight;
        }

        async function iniciarSimulacion() {
            const fecha = document.getElementById('fecha').value;
            const factorK = parseInt(document.getElementById('factorK').value);
            
            log('🚀 Iniciando simulación...');
            
            try {
                const response = await fetch('http://localhost:8000/api/simulations/start', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ fecha, factorK })
                });
                
                const data = await response.json();
                
                if (data.sessionId) {
                    sessionId = data.sessionId;
                    document.getElementById('sessionId').textContent = sessionId;
                    log(`✅ ${data.mensaje}`);
                    log(`📡 Topic: ${data.topicUrl}`);
                    conectarWebSocket(sessionId);
                } else {
                    log(`❌ Error: ${data.mensaje}`);
                }
            } catch (error) {
                log(`❌ Error: ${error.message}`);
            }
        }

        function conectarWebSocket(sessionId) {
            const socket = new SockJS('http://localhost:8000/ws');
            
            stompClient = new StompJs.Client({
                webSocketFactory: () => socket,
                reconnectDelay: 5000,
                
                onConnect: () => {
                    log('✅ WebSocket conectado');
                    
                    stompClient.subscribe(`/topic/simulations/${sessionId}`, (message) => {
                        const data = JSON.parse(message.body);
                        manejarMensaje(data);
                    });
                },
                
                onStompError: (frame) => {
                    log(`❌ Error STOMP: ${frame.headers['message']}`);
                }
            });

            stompClient.activate();
        }

        function manejarMensaje(data) {
            if (data.tipo === 'PROGRESO_AG') {
                log(`🧬 Gen ${data.generacion}/${data.maxGeneraciones} - Fitness: ${data.mejorFitness.toFixed(2)}`);
                
                document.getElementById('progress').style.width = data.progreso + '%';
                document.getElementById('progress').textContent = data.progreso.toFixed(1) + '%';
                document.getElementById('generacion').textContent = 
                    `${data.generacion}/${data.maxGeneraciones}`;
                document.getElementById('fitness').textContent = 
                    data.mejorFitness.toFixed(2);
                document.getElementById('pedidos').textContent = 
                    `${data.pedidosProcesados}/${data.pedidosTotales}`;
                    
            } else if (data.status === 'COMPLETED') {
                log('✅ Simulación completada');
                desconectar();
            } else {
                log(`📨 Mensaje: ${JSON.stringify(data, null, 2)}`);
            }
        }

        async function cancelarSimulacion() {
            if (!sessionId) {
                log('⚠️ No hay simulación activa');
                return;
            }
            
            log('⏹️ Cancelando simulación...');
            
            await fetch(`http://localhost:8000/api/simulations/${sessionId}/cancel`, {
                method: 'POST'
            });
            
            desconectar();
        }

        function desconectar() {
            if (stompClient) {
                stompClient.deactivate();
                log('🔌 WebSocket desconectado');
            }
        }
    </script>
</body>
</html>
```

---

## 🔍 Debugging

### Verificar conexión WebSocket

```javascript
// En la consola del navegador
console.log('Estado STOMP:', stompClient.connected);
console.log('SessionId:', sessionId);
```

### Ver mensajes raw

```javascript
stompClient.subscribe(`/topic/simulations/${sessionId}`, (message) => {
  console.log('📨 Mensaje raw:', message.body);
  const data = JSON.parse(message.body);
  console.log('📦 Datos parseados:', data);
});
```

### Verificar endpoint REST

```bash
curl -X POST http://localhost:8000/api/simulations/start \
  -H "Content-Type: application/json" \
  -d '{"fecha":"2025-01-02","factorK":5}'
```

---

## 📚 Referencias

- **SockJS Client:** https://github.com/sockjs/sockjs-client
- **STOMP.js:** https://stomp-js.github.io/stomp-websocket/
- **Backend URL:** http://localhost:8000
- **WebSocket Endpoint:** ws://localhost:8000/ws

---

## ✅ Checklist de Integración

- [ ] Instalar dependencias (sockjs-client, @stomp/stompjs)
- [ ] Implementar llamada POST a `/api/simulations/start`
- [ ] Guardar `sessionId` de la respuesta
- [ ] Conectar a WebSocket usando SockJS
- [ ] Suscribirse al topic `/topic/simulations/{sessionId}`
- [ ] Implementar handler para mensajes tipo `PROGRESO_AG`
- [ ] Implementar handler para mensajes tipo `SimulationSnapshot`
- [ ] Actualizar UI con progreso en tiempo real
- [ ] Implementar botón de cancelación
- [ ] Manejar desconexión y reconexión
- [ ] Añadir manejo de errores
- [ ] Probar con datos reales

---

## 💡 Tips

1. **Usa nombres en español:** Los campos JSON están en español (fecha, factorK, generacion, etc.)
2. **Maneja la reconexión:** El cliente STOMP reconecta automáticamente con `reconnectDelay`
3. **Filtra mensajes:** Usa el campo `tipo` para diferenciar tipos de mensajes
4. **Muestra progreso visual:** Usa barras de progreso, gráficas, animaciones
5. **Guarda historial:** Almacena las mejores soluciones de cada generación
6. **Optimiza renders:** No actualices la UI en cada mensaje si son muy frecuentes

---

¡Éxito con la integración! 🚀
