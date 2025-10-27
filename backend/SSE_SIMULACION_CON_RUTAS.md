# 📡 SSE Simulación con Rutas de Solución

## 🎯 Descripción

El endpoint `/api/simulacion/stream` ahora envía **rutas de solución** en cada tick, permitiendo graficar todos los vuelos activos e inactivos en tiempo real en el simulador.

---

## 📊 Estructura del Evento SSE

### Evento Completo (cada segundo)

```json
{
  "horaSimulada": "2025-01-01T10:00:00",
  "tiempoRealTranscurridoMs": 10000,
  "activa": true,
  "tickActual": 10,
  "estadoDescripcion": "ACTIVA",
  "timeScale": 10.0,
  
  "rutasSolucion": [
    {
      "id": 1,
      "originCode": "SPIM",
      "destinationCode": "EDDI",
      
      // Coordenadas de origen (fijas)
      "origenLatitud": -12.0219,
      "origenLongitud": -77.1143,
      
      // Coordenadas de destino (fijas)
      "destinoLatitud": 52.4731,
      "destinoLongitud": 13.4040,
      
      // Coordenadas actuales (cambian en tiempo real)
      "currentLatitude": 15.4532,
      "currentLongitude": -45.2341,
      
      // Estado del vuelo
      "enVuelo": true,
      "progress": 45.5,
      
      // Información adicional
      "speed": 900,
      "altitude": 35000,
      "totalPackages": 350,
      "regionOrigen": "America",
      "regionDestino": "Europa"
    },
    // ... más rutas
  ]
}
```

---

## 🗺️ Uso en Frontend para Graficar

### Conectar al SSE

```javascript
const simulacionSSE = new EventSource('http://localhost:8080/api/simulacion/stream');

simulacionSSE.onmessage = (event) => {
  const data = JSON.parse(event.data);
  
  // 1. Actualizar información de simulación
  document.getElementById('hora-simulada').textContent = data.horaSimulada;
  document.getElementById('tick-actual').textContent = data.tickActual;
  
  // 2. Graficar todas las rutas en el mapa
  graficarRutas(data.rutasSolucion);
};
```

---

## ✈️ Función para Graficar Rutas en Leaflet

```javascript
// Almacenar marcadores y líneas por ID de ruta
const marcadoresVuelos = new Map();
const lineasRutas = new Map();

function graficarRutas(rutas) {
  // Limpiar marcadores antiguos que ya no existen
  const idsActuales = new Set(rutas.map(r => r.id));
  
  for (let [id, marcador] of marcadoresVuelos) {
    if (!idsActuales.has(id)) {
      map.removeLayer(marcador);
      marcadoresVuelos.delete(id);
    }
  }
  
  // Procesar cada ruta
  rutas.forEach(ruta => {
    // Crear/actualizar línea de la ruta (origen → destino)
    if (!lineasRutas.has(ruta.id)) {
      const linea = L.polyline([
        [ruta.origenLatitud, ruta.origenLongitud],
        [ruta.destinoLatitud, ruta.destinoLongitud]
      ], {
        color: ruta.enVuelo ? 'blue' : 'gray',
        weight: 2,
        opacity: ruta.enVuelo ? 0.7 : 0.3,
        dashArray: ruta.enVuelo ? null : '5, 10'
      }).addTo(map);
      
      lineasRutas.set(ruta.id, linea);
    } else {
      // Actualizar estilo según estado
      const linea = lineasRutas.get(ruta.id);
      linea.setStyle({
        color: ruta.enVuelo ? 'blue' : 'gray',
        opacity: ruta.enVuelo ? 0.7 : 0.3
      });
    }
    
    // Crear/actualizar marcador del avión
    if (ruta.enVuelo) {
      const posicion = [ruta.currentLatitude, ruta.currentLongitude];
      
      if (!marcadoresVuelos.has(ruta.id)) {
        // Crear nuevo marcador
        const marcador = L.marker(posicion, {
          icon: L.icon({
            iconUrl: 'avion.png',
            iconSize: [32, 32],
            iconAnchor: [16, 16]
          })
        }).addTo(map);
        
        // Popup con información
        marcador.bindPopup(`
          <b>Vuelo ${ruta.originCode} → ${ruta.destinationCode}</b><br>
          Progreso: ${ruta.progress.toFixed(1)}%<br>
          Velocidad: ${ruta.speed} km/h<br>
          Altitud: ${ruta.altitude} ft<br>
          Paquetes: ${ruta.totalPackages}
        `);
        
        marcadoresVuelos.set(ruta.id, marcador);
      } else {
        // Actualizar posición existente
        const marcador = marcadoresVuelos.get(ruta.id);
        marcador.setLatLng(posicion);
        
        // Actualizar popup
        marcador.setPopupContent(`
          <b>Vuelo ${ruta.originCode} → ${ruta.destinationCode}</b><br>
          Progreso: ${ruta.progress.toFixed(1)}%<br>
          Velocidad: ${ruta.speed} km/h<br>
          Altitud: ${ruta.altitude} ft<br>
          Paquetes: ${ruta.totalPackages}
        `);
      }
    } else {
      // Vuelo no activo, remover marcador si existe
      if (marcadoresVuelos.has(ruta.id)) {
        map.removeLayer(marcadoresVuelos.get(ruta.id));
        marcadoresVuelos.delete(ruta.id);
      }
    }
  });
}
```

---

## 🎨 Ejemplo HTML Completo

```html
<!DOCTYPE html>
<html>
<head>
  <title>Simulador de Vuelos</title>
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    #map { height: 600px; }
    .panel {
      padding: 15px;
      background: #f5f5f5;
      margin: 10px 0;
      border-radius: 5px;
    }
    .info-item {
      display: inline-block;
      margin-right: 20px;
    }
  </style>
</head>
<body>
  <h1>🛫 Simulador de Vuelos en Tiempo Real</h1>
  
  <!-- Panel de Control -->
  <div class="panel">
    <h3>🎮 Control</h3>
    <button onclick="iniciar()">▶️ Iniciar</button>
    <button onclick="pausar()">⏸️ Pausar</button>
    <button onclick="reanudar()">▶️ Reanudar</button>
    <button onclick="detener()">⏹️ Detener</button>
  </div>
  
  <!-- Panel de Estado -->
  <div class="panel">
    <h3>📊 Estado de Simulación</h3>
    <div class="info-item">
      <strong>Hora Simulada:</strong> 
      <span id="hora-simulada">-</span>
    </div>
    <div class="info-item">
      <strong>Tick:</strong> 
      <span id="tick-actual">0</span>
    </div>
    <div class="info-item">
      <strong>Estado:</strong> 
      <span id="estado">DETENIDA</span>
    </div>
    <div class="info-item">
      <strong>Vuelos Activos:</strong> 
      <span id="vuelos-activos">0</span>
    </div>
  </div>
  
  <!-- Mapa -->
  <div id="map"></div>
  
  <script>
    // Inicializar mapa
    const map = L.map('map').setView([0, 0], 2);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap'
    }).addTo(map);
    
    // Almacenar elementos del mapa
    const marcadoresVuelos = new Map();
    const lineasRutas = new Map();
    let simulacionSSE = null;
    
    // Función para graficar rutas
    function graficarRutas(rutas) {
      // Limpiar marcadores de vuelos que ya no existen
      const idsActuales = new Set(rutas.map(r => r.id));
      
      for (let [id, marcador] of marcadoresVuelos) {
        if (!idsActuales.has(id)) {
          map.removeLayer(marcador);
          marcadoresVuelos.delete(id);
        }
      }
      
      // Contar vuelos activos
      let vuelosActivos = 0;
      
      // Procesar cada ruta
      rutas.forEach(ruta => {
        // Dibujar línea de ruta (origen → destino)
        if (!lineasRutas.has(ruta.id)) {
          const linea = L.polyline([
            [ruta.origenLatitud, ruta.origenLongitud],
            [ruta.destinoLatitud, ruta.destinoLongitud]
          ], {
            color: ruta.enVuelo ? '#007bff' : '#6c757d',
            weight: 2,
            opacity: ruta.enVuelo ? 0.7 : 0.3,
            dashArray: ruta.enVuelo ? null : '5, 10'
          }).addTo(map);
          
          lineasRutas.set(ruta.id, linea);
        } else {
          lineasRutas.get(ruta.id).setStyle({
            color: ruta.enVuelo ? '#007bff' : '#6c757d',
            opacity: ruta.enVuelo ? 0.7 : 0.3
          });
        }
        
        // Dibujar avión si está en vuelo
        if (ruta.enVuelo) {
          vuelosActivos++;
          const pos = [ruta.currentLatitude, ruta.currentLongitude];
          
          if (!marcadoresVuelos.has(ruta.id)) {
            const marcador = L.marker(pos, {
              icon: L.divIcon({
                html: '✈️',
                iconSize: [30, 30],
                className: 'avion-marker'
              })
            }).addTo(map);
            
            marcador.bindPopup(`
              <b>✈️ ${ruta.originCode} → ${ruta.destinationCode}</b><br>
              <b>Progreso:</b> ${ruta.progress.toFixed(1)}%<br>
              <b>Velocidad:</b> ${ruta.speed} km/h<br>
              <b>Altitud:</b> ${ruta.altitude} ft<br>
              <b>Paquetes:</b> ${ruta.totalPackages}
            `);
            
            marcadoresVuelos.set(ruta.id, marcador);
          } else {
            marcadoresVuelos.get(ruta.id).setLatLng(pos);
          }
        } else {
          if (marcadoresVuelos.has(ruta.id)) {
            map.removeLayer(marcadoresVuelos.get(ruta.id));
            marcadoresVuelos.delete(ruta.id);
          }
        }
      });
      
      document.getElementById('vuelos-activos').textContent = vuelosActivos;
    }
    
    // Funciones de control
    async function iniciar() {
      // Conectar SSE
      simulacionSSE = new EventSource('http://localhost:8080/api/simulacion/stream');
      
      simulacionSSE.onmessage = (event) => {
        const data = JSON.parse(event.data);
        
        // Actualizar UI
        document.getElementById('hora-simulada').textContent = data.horaSimulada;
        document.getElementById('tick-actual').textContent = data.tickActual;
        document.getElementById('estado').textContent = data.estadoDescripcion;
        
        // Graficar rutas
        if (data.rutasSolucion) {
          graficarRutas(data.rutasSolucion);
        }
      };
      
      // Iniciar simulación
      await fetch('http://localhost:8080/api/simulacion/iniciar', { method: 'POST' });
    }
    
    async function pausar() {
      await fetch('http://localhost:8080/api/simulacion/pausar', { method: 'POST' });
    }
    
    async function reanudar() {
      await fetch('http://localhost:8080/api/simulacion/reanudar', { method: 'POST' });
    }
    
    async function detener() {
      await fetch('http://localhost:8080/api/simulacion/detener', { method: 'POST' });
      if (simulacionSSE) simulacionSSE.close();
    }
  </script>
</body>
</html>
```

---

## 📋 Estructura de Datos de Rutas

### Campos Disponibles por Ruta:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Long | ID único de la ruta |
| `originCode` | String | Código ICAO origen (ej: "SPIM") |
| `destinationCode` | String | Código ICAO destino (ej: "EDDI") |
| `origenLatitud` | Double | Latitud origen (fija) |
| `origenLongitud` | Double | Longitud origen (fija) |
| `destinoLatitud` | Double | Latitud destino (fija) |
| `destinoLongitud` | Double | Longitud destino (fija) |
| `currentLatitude` | Double | **Latitud actual** (cambia cada segundo) |
| `currentLongitude` | Double | **Longitud actual** (cambia cada segundo) |
| `enVuelo` | Boolean | ¿Está volando actualmente? |
| `progress` | Double | Progreso del vuelo (0-100%) |
| `speed` | Integer | Velocidad en km/h |
| `altitude` | Integer | Altitud en pies |
| `totalPackages` | Integer | Total de paquetes transportados |
| `regionOrigen` | String | Región de origen |
| `regionDestino` | String | Región de destino |

---

## 🎯 Diferencias entre SSE

### `/api/simulacion/stream` (Nuevo - Mejorado)
- ✅ Envía **todas las rutas** en cada tick
- ✅ Incluye rutas activas e inactivas
- ✅ Información completa de simulación
- ✅ **Ideal para el simulador principal**

### `/api/vuelos/stream` (Individual)
- ✅ Envía solo **un vuelo específico**
- ✅ Más lightweight
- ✅ Cierra automáticamente al completar
- ✅ Ideal para tracking individual

---

## 💡 Casos de Uso

### Usar `/api/simulacion/stream` cuando:
- Quieres ver **todos los vuelos** simultáneamente
- Necesitas sincronizar con la simulación global
- Requieres el estado completo del sistema

### Usar `/api/vuelos/stream` cuando:
- Solo te interesa **un vuelo específico**
- Quieres un stream dedicado
- No necesitas información de simulación

---

## ✨ Resumen

Con `/api/simulacion/stream` ahora recibes:
1. ⏰ **Tiempos**: Hora simulada y tiempo real
2. 📊 **Estado**: Tick actual, si está activa, etc.
3. ✈️ **Rutas**: TODAS las rutas con coordenadas actualizadas en tiempo real

¡Tu simulador puede graficar todos los vuelos activos en el mapa! 🗺️🚀
