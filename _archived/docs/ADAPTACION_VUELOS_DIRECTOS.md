# 🔧 ADAPTACIÓN: Vuelos Directos (Sin SubRutas)

## 📋 Problema

El backend está enviando un formato **diferente** al esperado:

### ❌ **Formato Esperado Original:**
```json
{
  "solucion": {
    "rutas": [
      {
        "pedidoId": 123,
        "subRutas": [
          {
            "origen": "KJFK",
            "destino": "LFPG",
            "vuelo": "AF001"
          }
        ]
      }
    ]
  }
}
```

### ✅ **Formato Real del Backend:**
```json
{
  "solucion": {
    "vuelos": [
      {
        "origenCodigoICAO": "EBCI",
        "destinoCodigoICAO": "OOMS",
        "fechaInicial": "2025-01-01 22:44",
        "fechaFinal": "2025-01-02 06:41",
        "pedidos": [
          {
            "idPedido": 4655679,
            "cantidad": 2
          }
        ],
        "totalPaquetes": 2
      }
    ]
  }
}
```

---

## ✅ Solución Implementada

### 1. **Nueva Función: `procesarVuelosDirectos`**

Creada para manejar el formato **vuelos directos** (sin subRutas):

```javascript
const procesarVuelosDirectos = useCallback((vuelos) => {
  console.log(`🔍 procesarVuelosDirectos - Recibidos ${vuelos?.length || 0} vuelos`);
  
  const nuevosVuelos = [];

  vuelos.forEach((vuelo, index) => {
    // Buscar aeropuertos por código ICAO
    const origen = airports.find(a => 
      a.code.toUpperCase() === vuelo.origenCodigoICAO.toUpperCase()
    );
    const destino = airports.find(a => 
      a.code.toUpperCase() === vuelo.destinoCodigoICAO.toUpperCase()
    );

    // Validación
    if (!origen || !destino) {
      console.error(`❌ Aeropuerto no encontrado`);
      return;
    }

    // Crear vuelo con progreso 0.5 (mitad del recorrido)
    const progress = 0.5;
    const currentLat = origen.lat + (destino.lat - origen.lat) * progress;
    const currentLng = origen.lng + (destino.lng - origen.lng) * progress;

    const nuevoVuelo = {
      id: `WS-${vuelo.pedidos?.[0]?.idPedido || index}-${Date.now()}`,
      origin: { code: vuelo.origenCodigoICAO, lat: origen.lat, lng: origen.lng },
      destination: { code: vuelo.destinoCodigoICAO, lat: destino.lat, lng: destino.lng },
      progress: 0.5,
      altitude: 35000,
      speed: 850,
      status: 'active',
      currentLat,
      currentLng,
      aircraftColor: '#10b981', // 🟢 Verde
      rotation: bearingDegrees(...),
      packageCapacity: vuelo.totalPaquetes,
      packageType: 'WS',
      fechaInicial: vuelo.fechaInicial,
      fechaFinal: vuelo.fechaFinal
    };

    nuevosVuelos.push(nuevoVuelo);
  });

  // Reemplazar todos los vuelos
  setFlights(nuevosVuelos);
  setFlightsInAir(nuevosVuelos.filter(v => v.status === 'active').length);
}, [airports]);
```

### 2. **Actualización de `procesarMensajeSimulacion`**

Ahora detecta automáticamente el formato:

```javascript
const procesarMensajeSimulacion = useCallback((datos) => {
  if (datos.tipo === 'PROGRESO_AG') {
    // Actualizar progreso...
    
    // 🔥 NUEVO: Detectar formato automáticamente
    if (datos.solucion?.vuelos && datos.solucion.vuelos.length > 0) {
      // Formato NUEVO: vuelos directos
      console.log(`✈️ Procesando ${datos.solucion.vuelos.length} vuelos directos...`);
      procesarVuelosDirectos(datos.solucion.vuelos);
    } 
    else if (datos.solucion?.rutas) {
      // Formato ANTIGUO: rutas con subRutas (fallback)
      console.log(`✈️ Procesando ${datos.solucion.rutas.length} rutas...`);
      procesarRutasSimulacion(datos.solucion.rutas);
    }
  }
}, [agregarMensaje, procesarVuelosDirectos, procesarRutasSimulacion]);
```

---

## 📊 Mapeo de Campos

| Campo Backend | Campo Frontend | Descripción |
|---------------|----------------|-------------|
| `origenCodigoICAO` | `origin.code` | Código ICAO origen (4 letras) |
| `destinoCodigoICAO` | `destination.code` | Código ICAO destino (4 letras) |
| `fechaInicial` | `fechaInicial` | Hora de salida |
| `fechaFinal` | `fechaFinal` | Hora de llegada |
| `totalPaquetes` | `packageCapacity` | Cantidad de paquetes |
| `pedidos[0].idPedido` | `pedidoId` | ID del pedido |

---

## 🎨 Características Visuales

**Vuelos WebSocket (del backend):**
- 🟢 **Color verde** (`#10b981`)
- **ID:** `WS-{pedidoId}-{timestamp}-{random}`
- **Progress:** Fijo en `0.5` (mitad del recorrido)
- **Status:** `'active'` (siempre activo)
- **Altitud:** `35000` pies
- **Velocidad:** `850` km/h

---

## 🔍 Logs en Consola

Cuando recibas el mensaje, verás:

```
📨 Mensaje recibido: {tipo: "PROGRESO_AG", generacion: 0, ...}
🧬 Progreso AG - Generación 0/20
✈️ Procesando 15 vuelos directos...

🔍 procesarVuelosDirectos - Recibidos 15 vuelos
📍 Aeropuertos disponibles: 45

✈️ Vuelo 1/15
   Origen: EBCI → Destino: OOMS
   Paquetes: 2
   ✅ Origen: EBCI [50.90, 4.48]
   ✅ Destino: OOMS [23.59, 58.28]
   ✅ Vuelo creado en posición: [37.24, 31.38]

✈️ Vuelo 2/15
   Origen: EBCI → Destino: SUAA
   Paquetes: 4
   ...

📊 ============================================
📊 Total de vuelos WebSocket creados: 15
📊 ============================================

🗺️ Primer vuelo (ejemplo): {
  id: "WS-4655679-1732604317000-0.12345",
  from: "EBCI",
  to: "OOMS",
  position: [37.24, 31.38],
  color: "#10b981",
  packages: 2
}

🔄 Reemplazando flights array con 15 vuelos nuevos
✈️ Vuelos activos: 15

🗺️ DynamicMarkers - Recibidos 15 vuelos, activeView: flights
✈️ Añadiendo 15 vuelos al mapa
  ✈️ Vuelo 1: WS-4655679-... - Color: #10b981 - Pos: [37.24, 31.38]
  ✈️ Vuelo 2: WS-4655669-... - Color: #10b981 - Pos: [25.45, -32.87]
  ...
✅ Total marcadores de vuelos añadidos: 15
```

---

## ✅ Resultado Esperado

Ahora deberías ver:

1. ✅ **15 aviones verdes** (🟢) en el mapa
2. ✅ Cada avión en la **mitad del recorrido** entre origen y destino
3. ✅ Logs detallados en la consola
4. ✅ Panel de progreso mostrando **Generación 0/20**
5. ✅ **Fitness: 1500** en las métricas

---

## 🐛 Troubleshooting

### ❌ **Si NO ves vuelos:**

**1. Verifica que los aeropuertos existan:**
```javascript
// En consola del navegador:
console.log('Aeropuertos:', airports.length);
console.log('Primer aeropuerto:', airports[0]);
```

**2. Busca errores en la consola:**
```
❌ Aeropuerto ORIGEN no encontrado: "EBCI"
```
**Solución:** Asegúrate de que la base de datos de aeropuertos incluya ese código ICAO.

**3. Verifica el mensaje recibido:**
```javascript
// En consola:
📨 Mensaje recibido: {tipo: "PROGRESO_AG", solucion: {...}}
```
Si `solucion` es `null` o `vuelos` está vacío, el backend no está enviando vuelos.

---

## 📋 Ejemplo Completo de Mensaje Válido

```json
{
  "sessionId": "a81ac6b7-67a6-448a-8705-735aa4325c24",
  "tipo": "PROGRESO_AG",
  "generacion": 0,
  "maxGeneraciones": 20,
  "progreso": 0,
  "mejorFitness": 1500,
  "solucion": {
    "vuelos": [
      {
        "fechaInicial": "2025-01-01 22:44",
        "fechaFinal": "2025-01-02 06:41",
        "origenCodigoICAO": "EBCI",
        "destinoCodigoICAO": "OOMS",
        "pedidos": [{"idPedido": 4655679, "cantidad": 2}],
        "totalPaquetes": 2
      }
    ],
    "totalVuelos": 15,
    "totalPedidos": 15
  },
  "pedidosProcesados": 15,
  "pedidosTotales": 15
}
```

---

## 🚀 Próximos Pasos

1. **Recargar la página** (Ctrl+Shift+R)
2. **Conectar WebSocket** (botón morado)
3. **Iniciar simulación** (botón verde)
4. **Verificar consola** para ver los logs
5. **Observar el mapa** - deberías ver 15 aviones verdes 🟢

---

## 📊 Métricas de Éxito

- ✅ **Console muestra:** "Total de vuelos WebSocket creados: 15"
- ✅ **Console muestra:** "Total marcadores de vuelos añadidos: 15"
- ✅ **Mapa muestra:** 15 aviones verdes
- ✅ **Panel muestra:** Generación 0/20, Fitness: 1500

---

**Fecha:** 26 de noviembre de 2025  
**Versión:** 2.0 - Vuelos directos (sin subRutas)
