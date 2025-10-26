# 🛫 Sistema de Tracking de Vuelos en Tiempo Real

## 📋 Descripción

Sistema de seguimiento en tiempo real de vuelos usando **Server-Sent Events (SSE)** que simula el recorrido de un vuelo desde Lima (Perú) hasta Berlín (Alemania).

## 🗂️ Estructura de Base de Datos

### Tabla: `rutas_solucion`
Almacena las rutas de vuelo con sus coordenadas origen/destino.

### Tabla: `vuelo_pedidos`
Almacena los pedidos individuales transportados en cada vuelo.

## 🚀 Endpoints API

### 1. **Iniciar Streaming de Vuelo**
```http
POST /api/vuelos/tracking/iniciar
```

**Respuesta:**
```json
{
  "mensaje": "Tracking de vuelos iniciado exitosamente",
  "streaming": true,
  "clientesConectados": 1
}
```

---

### 2. **Conectarse al Stream SSE**
```http
GET /api/vuelos/stream
```

**Headers:**
```
Accept: text/event-stream
```

**Evento recibido cada segundo:**
```json
{
  "id": 1,
  "originCode": "SPIM",
  "destinationCode": "EDDI",
  "currentLatitude": -8.5432,
  "currentLongitude": -65.2341,
  "progress": 35.5,
  "speed": 900,
  "altitude": 35000,
  "totalPackages": 350
}
```

---

### 3. **Detener Streaming**
```http
POST /api/vuelos/tracking/detener
```

**Respuesta:**
```json
{
  "mensaje": "Tracking de vuelos detenido exitosamente",
  "streaming": false
}
```

---

### 4. **Consultar Estado del Tracking**
```http
GET /api/vuelos/tracking/estado
```

**Respuesta:**
```json
{
  "streaming": true,
  "clientesConectados": 2
}
```

---

## 📡 Implementación en Frontend

### JavaScript Vanilla / React

```javascript
// Conectar al SSE
const eventSource = new EventSource('http://localhost:8080/api/vuelos/stream');

eventSource.addEventListener('vuelo-update', (event) => {
  const data = JSON.parse(event.data);
  
  console.log('📍 Posición actual:', data.currentLatitude, data.currentLongitude);
  console.log('✈️ Progreso:', data.progress.toFixed(2) + '%');
  
  // Actualizar mapa o visualización
  actualizarMapa(data.currentLatitude, data.currentLongitude);
});

eventSource.onerror = (error) => {
  console.error('❌ Error en SSE:', error);
  eventSource.close();
};

// Iniciar tracking
fetch('http://localhost:8080/api/vuelos/tracking/iniciar', {
  method: 'POST'
})
.then(res => res.json())
.then(data => console.log('🚀 Tracking iniciado:', data));

// Detener tracking
function detenerTracking() {
  fetch('http://localhost:8080/api/vuelos/tracking/detener', {
    method: 'POST'
  })
  .then(() => eventSource.close());
}
```

---

## 🗺️ Ejemplo con Google Maps / Leaflet

### Con Leaflet
```javascript
// Inicializar mapa
const map = L.map('map').setView([-12.0219, -77.1143], 3);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

// Marcador del avión
let avionMarker = L.marker([-12.0219, -77.1143]).addTo(map);
let routeLine = L.polyline([], { color: 'blue' }).addTo(map);
const coordenadas = [];

// Conectar a SSE
const eventSource = new EventSource('http://localhost:8080/api/vuelos/stream');

eventSource.addEventListener('vuelo-update', (event) => {
  const data = JSON.parse(event.data);
  
  const latLng = [data.currentLatitude, data.currentLongitude];
  
  // Actualizar marcador
  avionMarker.setLatLng(latLng);
  
  // Agregar coordenada a la línea de ruta
  coordenadas.push(latLng);
  routeLine.setLatLngs(coordenadas);
  
  // Centrar mapa
  map.panTo(latLng);
  
  // Actualizar información
  document.getElementById('progreso').textContent = data.progress.toFixed(1) + '%';
  document.getElementById('velocidad').textContent = data.speed + ' km/h';
  document.getElementById('altitud').textContent = data.altitude + ' ft';
});
```

---

## 🎯 Datos Hardcodeados

Al iniciar la aplicación, se crea automáticamente:

**Vuelo:** Lima (SPIM) → Berlín (EDDI)
- 🛫 **Origen:** Lima, Perú (-12.0219, -77.1143)
- 🛬 **Destino:** Berlín, Alemania (52.4731, 13.4040)
- ⏱️ **Duración simulada:** ~100 segundos (1 update/seg)
- 📦 **Paquetes:** 350 unidades
- ✈️ **Velocidad:** 900 km/h
- 📏 **Altitud:** 35,000 ft

**Pedidos incluidos:**
1. PED-LIMA-001: 150 paquetes
2. PED-LIMA-002: 100 paquetes
3. PED-LIMA-003: 100 paquetes

---

## 🔧 Funcionamiento

1. **Al iniciar la app:** Se crea la ruta en BD automáticamente
2. **Llamar a `/tracking/iniciar`:** Comienza la simulación
3. **El servidor envía eventos SSE cada 1 segundo** con las coordenadas interpoladas
4. **El frontend recibe y grafica** la posición en tiempo real
5. **Al completar:** El vuelo llega a Berlín tras 100 pasos

---

## 📝 Notas

- ✅ El SSE no tiene timeout (conexión persistente)
- ✅ Soporta múltiples clientes simultáneos
- ✅ Las coordenadas se interpolan linealmente entre origen y destino
- ✅ Thread-safe usando `CopyOnWriteArrayList`
- ✅ Manejo robusto de desconexiones

---

## 🎨 Sugerencias de Visualización

1. **Mapa animado** con trayectoria del vuelo
2. **Panel de información** mostrando velocidad, altitud, progreso
3. **Lista de pedidos** en tránsito
4. **Gráfico de progreso** (barra o circular)
5. **ETA estimado** según el progreso actual

¡Listo para integrar en tu frontend! 🚀
