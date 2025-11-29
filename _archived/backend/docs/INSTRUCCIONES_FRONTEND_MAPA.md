# 🗺️ INSTRUCCIONES: Integrar Visualizador de Mapa con Backend

## 📋 Resumen
Tu backend YA está enviando todos los datos necesarios para pintar rutas en un mapa. Solo necesitas **leer y procesar** estos datos en tu frontend.

---

## 🔌 PASO 1: Conectar WebSocket y Cargar Aeropuertos

### 1.1 Cargar coordenadas de aeropuertos (IMPORTANTE)
```javascript
// Hacer esto ANTES de conectar WebSocket
async function cargarAeropuertos() {
    const response = await fetch('http://localhost:8000/api/aeropuertos/listar');
    const aeropuertos = await response.json();
    
    // Crear diccionario: codigo ICAO -> coordenadas
    const aeropuertosMap = {};
    aeropuertos.forEach(aero => {
        aeropuertosMap[aero.codigoICAO] = {
            lat: aero.latitud,
            lon: aero.longitud,
            ciudad: aero.ciudad,
            pais: aero.pais
        };
    });
    
    return aeropuertosMap;
}

// Ejemplo de uso:
const aeropuertosMap = await cargarAeropuertos();
console.log(aeropuertosMap);
// Resultado:
// {
//   "SPIM": { lat: -12.0219, lon: -77.1143, ciudad: "Lima", pais: "Peru" },
//   "SCIE": { lat: -33.3929, lon: -70.7858, ciudad: "Santiago", pais: "Chile" },
//   ...
// }
```

### 1.2 Conectar WebSocket
```javascript
const socket = new SockJS('http://localhost:8000/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('✅ Conectado');
});
```

---

## 🚀 PASO 2: Iniciar Simulación

```javascript
fetch('http://localhost:8000/api/simulations/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        fecha: "2025-01-02",
        factorK: 5,
        tamanioPoblacion: 10,
        maxGeneraciones: 10,
        limiteGeneracionesSinMejora: 5
    })
})
.then(res => res.json())
.then(data => {
    const sessionId = data.sessionId; // ← GUARDAR ESTE ID
    suscribirseAlProgreso(sessionId);
});
```

---

## 📡 PASO 3: Suscribirse y Recibir Rutas

```javascript
function suscribirseAlProgreso(sessionId) {
    stompClient.subscribe(`/topic/simulations/${sessionId}`, (mensaje) => {
        const progreso = JSON.parse(mensaje.body);
        
        if (progreso.tipo === "PROGRESO_AG" && progreso.solucion) {
            const rutas = progreso.solucion.rutas;
            pintarRutasEnMapa(rutas, aeropuertosMap);
        }
    });
}
```

---

## 🗺️ PASO 4: Pintar Rutas en el Mapa

### Estructura de datos que recibes:

```javascript
// progreso.solucion.rutas es un array de objetos:
[
  {
    "pedidoId": 123,
    "vueloId": "SPIM-SCIE-002",
    "origen": "SPIM",      // Código ICAO
    "destino": "SCIE",     // Código ICAO
    "salida": "2025-01-02T05:30",
    "llegada": "2025-01-02T07:15",
    "duracionHoras": 1.75,
    "distanciaKm": 850.5
  },
  // ... más rutas
]
```

### Función para pintar en mapa:

```javascript
function pintarRutasEnMapa(rutas, aeropuertosMap) {
    rutas.forEach(ruta => {
        // 1. Obtener coordenadas de origen y destino
        const origen = aeropuertosMap[ruta.origen];
        const destino = aeropuertosMap[ruta.destino];
        
        if (!origen || !destino) {
            console.warn(`⚠️ Coordenadas no encontradas para ${ruta.origen} → ${ruta.destino}`);
            return;
        }
        
        // 2. Datos completos para pintar
        const rutaConCoordenadas = {
            pedidoId: ruta.pedidoId,
            vueloId: ruta.vueloId,
            origen: {
                codigo: ruta.origen,
                lat: origen.lat,
                lon: origen.lon,
                ciudad: origen.ciudad,
                pais: origen.pais
            },
            destino: {
                codigo: ruta.destino,
                lat: destino.lat,
                lon: destino.lon,
                ciudad: destino.ciudad,
                pais: destino.pais
            },
            salida: ruta.salida,
            llegada: ruta.llegada,
            duracionHoras: ruta.duracionHoras,
            distanciaKm: ruta.distanciaKm
        };
        
        // 3. AQUÍ PINTAS EN TU MAPA
        // Ejemplo con Leaflet:
        // L.polyline([
        //     [origen.lat, origen.lon],
        //     [destino.lat, destino.lon]
        // ], {color: 'blue', weight: 2}).addTo(mapa);
        
        // Ejemplo con Google Maps:
        // new google.maps.Polyline({
        //     path: [
        //         {lat: origen.lat, lng: origen.lon},
        //         {lat: destino.lat, lng: destino.lon}
        //     ],
        //     strokeColor: '#0000FF',
        //     strokeWeight: 2
        // }).setMap(mapa);
        
        console.log("📍 Ruta:", rutaConCoordenadas);
    });
}
```

---

## 📊 EJEMPLO COMPLETO DE INTEGRACIÓN

```javascript
// ============= VARIABLES GLOBALES =============
let stompClient = null;
let aeropuertosMap = {};
let miMapa = null; // Tu instancia de mapa (Leaflet, Google Maps, etc.)

// ============= FLUJO PRINCIPAL =============
async function inicializar() {
    // 1. Cargar aeropuertos
    aeropuertosMap = await cargarAeropuertos();
    console.log("✅ Aeropuertos cargados:", Object.keys(aeropuertosMap).length);
    
    // 2. Conectar WebSocket
    conectarWebSocket();
}

function conectarWebSocket() {
    const socket = new SockJS('http://localhost:8000/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    
    stompClient.connect({}, () => {
        console.log("✅ WebSocket conectado");
        iniciarSimulacion();
    });
}

function iniciarSimulacion() {
    fetch('http://localhost:8000/api/simulations/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            fecha: "2025-01-02",
            factorK: 5,
            tamanioPoblacion: 10,
            maxGeneraciones: 10,
            limiteGeneracionesSinMejora: 5
        })
    })
    .then(res => res.json())
    .then(data => {
        const sessionId = data.sessionId;
        console.log("🚀 Simulación iniciada:", sessionId);
        suscribirseAlProgreso(sessionId);
    });
}

function suscribirseAlProgreso(sessionId) {
    stompClient.subscribe(`/topic/simulations/${sessionId}`, (mensaje) => {
        const progreso = JSON.parse(mensaje.body);
        
        console.log(`📊 Generación ${progreso.generacion}/${progreso.maxGeneraciones}`);
        console.log(`⭐ Fitness: ${progreso.mejorFitness}`);
        
        if (progreso.solucion && progreso.solucion.rutas) {
            pintarRutasEnMapa(progreso.solucion.rutas);
        }
    });
}

function pintarRutasEnMapa(rutas) {
    // Limpiar rutas anteriores
    // miMapa.clearLayers(); // (depende de tu librería)
    
    rutas.forEach(ruta => {
        const origen = aeropuertosMap[ruta.origen];
        const destino = aeropuertosMap[ruta.destino];
        
        if (!origen || !destino) return;
        
        // ✨ AQUÍ VA TU CÓDIGO DE PINTADO ✨
        console.log(`Pintar línea: ${origen.ciudad} (${origen.lat}, ${origen.lon}) → ${destino.ciudad} (${destino.lat}, ${destino.lon})`);
        
        // Ejemplo: agregar marcador de origen
        // L.marker([origen.lat, origen.lon])
        //     .bindPopup(`${origen.ciudad} (${ruta.origen})`)
        //     .addTo(miMapa);
        
        // Ejemplo: agregar línea de ruta
        // L.polyline([
        //     [origen.lat, origen.lon],
        //     [destino.lat, destino.lon]
        // ], {color: 'blue', weight: 2}).addTo(miMapa);
    });
}

// ============= INICIAR =============
inicializar();
```

---

## 🔍 DEBUGGING: Ver datos en consola del navegador

Abre el visualizador HTML (`visualizador-ag.html`) y:

1. **Abre la consola** (F12 → Tab "Console")
2. **Conecta y ejecuta simulación**
3. **Verás logs como:**

```
📍 Cargando aeropuertos desde API...
✅ 30 aeropuertos cargados: {SPIM: {...}, SCIE: {...}, ...}
🔌 Conectando al servidor...
✅ Conectado exitosamente
🚀 Iniciando simulación...
✅ Simulación iniciada con ID: 20116d22-7694-4664-ac03-06523c92063c
📡 Suscribiéndose al canal: /topic/simulations/20116d22...
📥 Mensaje recibido: {tipo: "PROGRESO_AG", generacion: 1, ...}

═══════════════════════════════════════════
🗺️  DATOS PARA TU MAPA (array de rutas):
═══════════════════════════════════════════
[
  {
    "pedidoId": 123,
    "vueloId": "SPIM-SCIE-002",
    "origen": {
      "codigo": "SPIM",
      "lat": -12.0219,
      "lon": -77.1143,
      "ciudad": "Lima",
      "pais": "Peru"
    },
    "destino": {
      "codigo": "SCIE",
      "lat": -33.3929,
      "lon": -70.7858,
      "ciudad": "Santiago",
      "pais": "Chile"
    },
    "salida": "2025-01-02T05:30",
    "llegada": "2025-01-02T07:15",
    "duracionHoras": 1.75,
    "distanciaKm": 850.5
  }
]
═══════════════════════════════════════════
Total de 18 rutas
═══════════════════════════════════════════
💡 TIP: Accede a los datos escribiendo 'window.ultimasRutas' en la consola
```

4. **Copia el JSON** directamente desde la consola
5. **Usa `window.ultimasRutas`** para acceder al array completo

---

## 📦 FORMATO DE DATOS FINAL

Tu frontend recibe este array **cada vez que se completa una generación**:

```javascript
window.ultimasRutas = [
  {
    pedidoId: 123,
    vueloId: "SPIM-SCIE-002",
    origen: {
      codigo: "SPIM",
      lat: -12.0219,
      lon: -77.1143,
      ciudad: "Lima",
      pais: "Peru"
    },
    destino: {
      codigo: "SCIE",
      lat: -33.3929,
      lon: -70.7858,
      ciudad: "Santiago",
      pais: "Chile"
    },
    salida: "2025-01-02T05:30",
    llegada: "2025-01-02T07:15",
    duracionHoras: 1.75,
    distanciaKm: 850.5
  },
  // ... más rutas
];
```

---

## ✅ Checklist para tu Frontend

- [ ] ¿Llamas a `GET /api/aeropuertos/listar` al inicio?
- [ ] ¿Creas un diccionario `aeropuertosMap[codigoICAO] = {lat, lon}`?
- [ ] ¿Conectas al WebSocket en `/ws`?
- [ ] ¿Guardas el `sessionId` del endpoint `/start`?
- [ ] ¿Te suscribes al topic correcto `/topic/simulations/{sessionId}`?
- [ ] ¿Parseas el JSON: `JSON.parse(mensaje.body)`?
- [ ] ¿Accedes a las rutas con `progreso.solucion.rutas`?
- [ ] ¿Combinas ruta.origen/destino con aeropuertosMap para obtener coordenadas?
- [ ] ¿Pintas en el mapa usando las coordenadas?

---

## 🎯 RESUMEN

1. **Cargar aeropuertos**: `GET /api/aeropuertos/listar`
2. **Conectar WebSocket**: `SockJS('http://localhost:8000/ws')`
3. **Iniciar simulación**: `POST /api/simulations/start`
4. **Suscribirse**: `/topic/simulations/{sessionId}`
5. **Leer rutas**: `progreso.solucion.rutas[]`
6. **Combinar con coordenadas**: `aeropuertosMap[ruta.origen]`
7. **Pintar en mapa**: `L.polyline([origen, destino])`

---

¡Todo listo! Solo falta que tu frontend use estos datos para pintar en el mapa 🗺️✨
