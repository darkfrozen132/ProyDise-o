# 🔗 Integración Simulación + Tracking de Vuelos

## 📋 Descripción

El sistema ahora integra **automáticamente** el tracking de vuelos con la simulación principal, de modo que ambos SSE funcionan simultáneamente.

---

## 🚀 Funcionamiento Automático

### Cuando inicias la simulación:

```http
POST /api/simulacion/iniciar
```

**Se ejecuta automáticamente:**
1. ✅ Inicia la simulación (tick cada 1 segundo)
2. ✅ **Inicia el tracking de vuelos SSE automáticamente**
3. ✅ Ambos sistemas comienzan a enviar eventos en tiempo real

---

### Cuando detienes la simulación:

```http
POST /api/simulacion/detener
```

**Se ejecuta automáticamente:**
1. ✅ Detiene la simulación
2. ✅ **Detiene el tracking de vuelos SSE automáticamente**
3. ✅ Resetea el estado de ambos sistemas

---

## 📡 SSE Streams Activos

### 1. **Stream de Simulación** (Original)
```http
GET /api/simulacion/stream
```

**Eventos recibidos:**
```json
{
  "horaSimulada": "2025-01-01T10:00:00",
  "tiempoRealTranscurridoMs": 5000,
  "activa": true,
  "tickActual": 5,
  "estadoDescripcion": "ACTIVA",
  "timeScale": 10.0
}
```

---

### 2. **Stream de Tracking de Vuelos** (Nuevo - Automático)
```http
GET /api/vuelos/stream
```

**Eventos recibidos durante el vuelo:**
```json
{
  "id": 1,
  "originCode": "SPIM",
  "destinationCode": "EDDI",
  "currentLatitude": 15.4532,
  "currentLongitude": -45.2341,
  "progress": 45.5,
  "speed": 900,
  "altitude": 35000,
  "totalPackages": 350
}
```

**⭐ Evento final cuando el vuelo llega (100% completado):**
```json
{
  "id": 1,
  "originCode": "SPIM",
  "destinationCode": "EDDI",
  "currentLatitude": 52.4731,
  "currentLongitude": 13.4040,
  "progress": 100.0,
  "speed": 900,
  "altitude": 35000,
  "totalPackages": 350,
  "completed": true,
  "message": "Vuelo completado - Llegada a EDDI"
}
```

**Después del evento final:**
- ✅ El servidor espera 2 segundos
- ✅ Cierra automáticamente todas las conexiones SSE
- ✅ El frontend recibe el cierre de conexión

---

## 💻 Implementación en Frontend

### JavaScript/React - Conectar ambos SSE

```javascript
let simulacionSSE = null;
let trackingSSE = null;

// Función para iniciar simulación
async function iniciarSimulacion() {
  try {
    // 1. Conectar al SSE de simulación
    simulacionSSE = new EventSource('http://localhost:8080/api/simulacion/stream');
    
    simulacionSSE.addEventListener('simulacion-estado', (event) => {
      const data = JSON.parse(event.data);
      console.log('📊 Estado Simulación:', data);
      actualizarPanelSimulacion(data);
    });
    
    // 2. Conectar al SSE de tracking de vuelos
    trackingSSE = new EventSource('http://localhost:8080/api/vuelos/stream');
    
    trackingSSE.addEventListener('vuelo-update', (event) => {
      const data = JSON.parse(event.data);
      console.log('✈️ Posición Vuelo:', data);
      actualizarMapaVuelo(data);
      
      // ⭐ Detectar cuando el vuelo se completa
      if (data.completed === true) {
        console.log('🎉 Vuelo completado!', data.message);
        mostrarNotificacion('Vuelo completado', data.message);
      }
    });
    
    // ⭐ Manejar cierre de conexión automático
    trackingSSE.onerror = (error) => {
      console.log('🔌 Conexión SSE cerrada - Vuelo finalizado');
      trackingSSE.close();
    };
    
    // 3. Iniciar simulación (esto inicia automáticamente ambos sistemas)
    const response = await fetch('http://localhost:8080/api/simulacion/iniciar', {
      method: 'POST'
    });
    
    const result = await response.json();
    console.log('✅ Simulación iniciada:', result);
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

// Función para detener simulación
async function detenerSimulacion() {
  try {
    // Detener simulación (esto detiene automáticamente el tracking también)
    await fetch('http://localhost:8080/api/simulacion/detener', {
      method: 'POST'
    });
    
    // Cerrar conexiones SSE
    if (simulacionSSE) simulacionSSE.close();
    if (trackingSSE) trackingSSE.close();
    
    console.log('✅ Simulación detenida');
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

// Actualizar panel de simulación
function actualizarPanelSimulacion(data) {
  document.getElementById('hora-simulada').textContent = data.horaSimulada;
  document.getElementById('tick-actual').textContent = data.tickActual;
  document.getElementById('estado').textContent = data.estadoDescripcion;
}

// Actualizar mapa con posición del vuelo
function actualizarMapaVuelo(data) {
  // Actualizar marcador en mapa (Leaflet/Google Maps)
  const latLng = [data.currentLatitude, data.currentLongitude];
  avionMarker.setLatLng(latLng);
  
  // Actualizar información del vuelo
  document.getElementById('progreso').textContent = data.progress.toFixed(1) + '%';
  document.getElementById('velocidad').textContent = data.speed + ' km/h';
  document.getElementById('altitud').textContent = data.altitude + ' ft';
}
```

---

## 🗺️ Ejemplo Completo con Leaflet

```html
<!DOCTYPE html>
<html>
<head>
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    #map { height: 500px; }
    .panel { padding: 20px; background: #f0f0f0; margin: 10px 0; }
  </style>
</head>
<body>
  <!-- Panel de Control -->
  <div class="panel">
    <h2>🎮 Control de Simulación</h2>
    <button onclick="iniciarSimulacion()">▶️ Iniciar</button>
    <button onclick="pausarSimulacion()">⏸️ Pausar</button>
    <button onclick="detenerSimulacion()">⏹️ Detener</button>
  </div>
  
  <!-- Panel de Estado -->
  <div class="panel">
    <h2>📊 Estado Simulación</h2>
    <p>Hora Simulada: <span id="hora-simulada">-</span></p>
    <p>Tick Actual: <span id="tick-actual">0</span></p>
    <p>Estado: <span id="estado">DETENIDA</span></p>
  </div>
  
  <!-- Panel de Vuelo -->
  <div class="panel">
    <h2>✈️ Estado del Vuelo</h2>
    <p>Progreso: <span id="progreso">0%</span></p>
    <p>Velocidad: <span id="velocidad">0</span></p>
    <p>Altitud: <span id="altitud">0</span></p>
  </div>
  
  <!-- Mapa -->
  <div id="map"></div>
  
  <script>
    // Inicializar mapa
    const map = L.map('map').setView([-12.0219, -77.1143], 3);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
    
    // Marcador del avión
    let avionMarker = L.marker([-12.0219, -77.1143]).addTo(map);
    let routeLine = L.polyline([], { color: 'blue' }).addTo(map);
    const coordenadas = [];
    
    let simulacionSSE = null;
    let trackingSSE = null;
    
    async function iniciarSimulacion() {
      // Conectar SSE de simulación
      simulacionSSE = new EventSource('http://localhost:8080/api/simulacion/stream');
      simulacionSSE.addEventListener('simulacion-estado', (event) => {
        const data = JSON.parse(event.data);
        document.getElementById('hora-simulada').textContent = data.horaSimulada;
        document.getElementById('tick-actual').textContent = data.tickActual;
        document.getElementById('estado').textContent = data.estadoDescripcion;
      });
      
      // Conectar SSE de tracking
      trackingSSE = new EventSource('http://localhost:8080/api/vuelos/stream');
      trackingSSE.addEventListener('vuelo-update', (event) => {
        const data = JSON.parse(event.data);
        const latLng = [data.currentLatitude, data.currentLongitude];
        
        // Actualizar marcador
        avionMarker.setLatLng(latLng);
        coordenadas.push(latLng);
        routeLine.setLatLngs(coordenadas);
        map.panTo(latLng);
        
        // Actualizar info
        document.getElementById('progreso').textContent = data.progress.toFixed(1) + '%';
        document.getElementById('velocidad').textContent = data.speed + ' km/h';
        document.getElementById('altitud').textContent = data.altitude + ' ft';
        
        // ⭐ Detectar vuelo completado
        if (data.completed === true) {
          console.log('🎉 ' + data.message);
          alert('✅ ' + data.message);
        }
      });
      
      // ⭐ Manejar cierre de conexión
      trackingSSE.onerror = () => {
        console.log('🔌 Tracking finalizado - Conexión cerrada');
        trackingSSE.close();
      };
      
      // Iniciar simulación (inicia tracking automáticamente)
      await fetch('http://localhost:8080/api/simulacion/iniciar', { method: 'POST' });
    }
    
    async function pausarSimulacion() {
      await fetch('http://localhost:8080/api/simulacion/pausar', { method: 'POST' });
    }
    
    async function detenerSimulacion() {
      await fetch('http://localhost:8080/api/simulacion/detener', { method: 'POST' });
      if (simulacionSSE) simulacionSSE.close();
      if (trackingSSE) trackingSSE.close();
    }
  </script>
</body>
</html>
```

---

## 📝 Logs del Backend

Cuando inicias la simulación, verás en los logs:

```
🚀 Iniciando simulación...
   TIME_SCALE: 10.0 horas/segundo
   INTERVALO_TICK: 1000ms
   TIEMPO_PROCESAMIENTO: 800ms
✅ Simulación iniciada en hora simulada: 2025-01-01T00:00
✈️ Tracking de vuelos iniciado automáticamente
🚀 Streaming de coordenadas iniciado
📍 Iniciando vuelo desde (-12.0219, -77.1143) hasta (52.4731, 13.404)
```

---

## 🎯 Ventajas de la Integración

1. ✅ **Un solo botón** inicia todo (simulación + tracking)
2. ✅ **Sincronización automática** entre sistemas
3. ✅ **Detención coordinada** de ambos streams
4. ✅ **Gestión simplificada** del ciclo de vida
5. ✅ **Menos endpoints** que invocar manualmente

---

## 🔧 Endpoints Disponibles

### Simulación (Control Principal)
- `POST /api/simulacion/iniciar` - Inicia todo (simulación + tracking)
- `POST /api/simulacion/pausar` - Pausa simulación
- `POST /api/simulacion/reanudar` - Reanuda simulación
- `POST /api/simulacion/detener` - Detiene todo (simulación + tracking)
- `GET /api/simulacion/stream` - SSE de estado simulación

### Tracking de Vuelos (También disponible independiente)
- `GET /api/vuelos/stream` - SSE de coordenadas vuelo
- `POST /api/vuelos/tracking/iniciar` - Iniciar solo tracking (opcional)
- `POST /api/vuelos/tracking/detener` - Detener solo tracking (opcional)
- `GET /api/vuelos/tracking/estado` - Estado del tracking

---

## ✨ Resumen

- 🎮 **Inicia simulación** → Se inicia tracking automáticamente
- ⏹️ **Detén simulación** → Se detiene tracking automáticamente
- 📡 **Dos SSE streams** funcionando en paralelo
- 🗺️ **Frontend recibe** ambos tipos de eventos simultáneamente

¡Tu aplicación ahora tiene simulación en tiempo real con tracking de vuelos integrado! 🚀✈️
