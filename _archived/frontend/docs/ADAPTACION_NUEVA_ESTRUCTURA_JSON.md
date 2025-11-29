# 🔄 Adaptación a Nueva Estructura JSON

## 📋 Resumen de Cambios

Se ha adaptado el sistema híbrido de animación para trabajar con la **nueva estructura JSON** del backend que usa `SimulationMessage` con `snapshot` → `orderPlans` → `routes` → `segments`.

---

## 🆕 Nueva Estructura JSON del Backend

```json
{
  "simulationId": "uuid-de-la-simulacion",
  "type": "PROGRESS",
  "snapshot": {
    "simulationId": "uuid-de-la-simulacion",
    "processedOrders": 150,
    "totalOrders": 500,
    "fitness": 0.89,
    "generatedAt": "2025-01-15T10:30:00Z",
    "orderPlans": [
      {
        "orderId": "ORD-001",
        "slackMinutes": 45,
        "routes": [
          {
            "quantity": 10,
            "slackMinutes": 45,
            "segments": [
              {
                "flightId": "LIM-MIA-0800",
                "origin": "LIM",
                "destination": "MIA",
                "date": "2025-01-15",
                "quantity": 10,
                "departureUtc": "2025-01-15T13:00:00Z",
                "arrivalUtc": "2025-01-15T19:30:00Z"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

---

## 🛠️ Cambios Implementados

### 1. Nueva Función: `procesarSegmentsSnapshot()`

**Ubicación:** `SimuladorSemanal.js` línea ~1515

**Propósito:** Extraer y procesar todos los segments (vuelos) desde la estructura anidada del snapshot.

**Flujo de procesamiento:**
```
snapshot.orderPlans[]
  └─> orderPlan.routes[]
      └─> route.segments[]
          └─> segment (VUELO) ⭐
```

**Código clave:**
```javascript
const procesarSegmentsSnapshot = useCallback((snapshot) => {
  const currentAirports = airportsRef.current;
  const nuevosVuelos = [];
  
  // Iterar por cada orderPlan
  snapshot.orderPlans.forEach((orderPlan) => {
    const orderId = orderPlan.orderId;
    const orderSlackMinutes = orderPlan.slackMinutes;
    
    // Iterar por cada ruta del pedido
    orderPlan.routes.forEach((route) => {
      
      // Iterar por cada segment (VUELO)
      route.segments.forEach((segment) => {
        
        // Buscar aeropuertos
        const origen = currentAirports.find(a => 
          a.code.toUpperCase() === segment.origin.toUpperCase()
        );
        const destino = currentAirports.find(a => 
          a.code.toUpperCase() === segment.destination.toUpperCase()
        );
        
        // Crear objeto de vuelo con timestamps
        const nuevoVuelo = {
          id: `${segment.flightId}-${orderId}-${segmentIndex}`,
          flightId: segment.flightId,
          origin: { code: segment.origin, lat: origen.lat, lng: origen.lng },
          destination: { code: segment.destination, lat: destino.lat, lng: destino.lng },
          // ⏰ TIMESTAMPS para interpolación híbrida
          fechaInicial: segment.departureUtc,
          fechaFinal: segment.arrivalUtc,
          // Metadatos
          pedidoId: orderId,
          slackMinutes: orderSlackMinutes,
          status: orderSlackMinutes <= 0 ? 'retrasado' : 'active',
          packageCapacity: segment.quantity,
          // ... otros campos
        };
        
        nuevosVuelos.push(nuevoVuelo);
      });
    });
  });
  
  setFlights(nuevosVuelos);
}, []);
```

**Características:**
- ✅ Extrae `departureUtc` y `arrivalUtc` de cada segment
- ✅ Identifica vuelos retrasados por `slackMinutes <= 0`
- ✅ Asocia cada vuelo con su pedido (`orderId`)
- ✅ Usa `flightId` del backend como identificador
- ✅ Calcula rotación del avión automáticamente
- ✅ Compatible con sistema híbrido de animación

---

### 2. Modificado: `procesarMensajeSimulacion()`

**Cambios en detección de tipo de mensaje:**

#### Antes:
```javascript
if (datos.status === 'RUNNING') {
  if (datos.solution?.routes) {
    procesarRutasSnapshotRef.current(datos.solution.routes);
  }
}
```

#### Ahora:
```javascript
if (datos.type === 'PROGRESS' || datos.status === 'RUNNING') {
  if (datos.snapshot?.orderPlans) {
    // 🆕 Nueva estructura
    procesarSegmentsSnapshotRef.current(datos.snapshot);
  } else if (datos.solution?.routes) {
    // Fallback para formato antiguo
    procesarRutasSnapshotRef.current(datos.solution.routes);
  }
}
```

**Ventajas:**
- ✅ Detecta ambos tipos de mensajes: `type: "PROGRESS"` y `status: "RUNNING"`
- ✅ Prioriza nueva estructura (`snapshot.orderPlans`)
- ✅ Mantiene compatibilidad con formato antiguo (`solution.routes`)
- ✅ Muestra logs descriptivos según formato detectado

---

### 3. Añadido: Ref para nueva función

**Ubicación:** `SimuladorSemanal.js` línea ~546

```javascript
const procesarSegmentsSnapshotRef = useRef(null); // 🆕 Para nueva estructura JSON
```

---

### 4. Asignación del Ref

**Ubicación:** Después de la declaración de `procesarSegmentsSnapshot()`

```javascript
procesarSegmentsSnapshotRef.current = procesarSegmentsSnapshot;
```

**Propósito:** Evitar circular dependencies al llamar la función desde `procesarMensajeSimulacion`.

---

## 🎯 Mapeo de Campos: Backend → Frontend

| Campo Backend | Campo Frontend | Uso |
|---------------|----------------|-----|
| `segment.flightId` | `flightId` | Identificador único del vuelo |
| `segment.origin` | `origin.code` | Código ICAO del aeropuerto origen |
| `segment.destination` | `destination.code` | Código ICAO del aeropuerto destino |
| `segment.departureUtc` | `fechaInicial` | ⏰ Timestamp de despegue (interpolación) |
| `segment.arrivalUtc` | `fechaFinal` | ⏰ Timestamp de llegada (interpolación) |
| `segment.quantity` | `packageCapacity` | Cantidad de paquetes |
| `orderPlan.orderId` | `pedidoId` | ID del pedido asociado |
| `orderPlan.slackMinutes` | `slackMinutes` | Holgura (≤0 = retrasado) |
| `snapshot.generatedAt` | - | Tiempo simulado actual (logs) |

---

## 📊 Logs de Consola Esperados

### ✅ Con nueva estructura:

```
🎮 Simulación corriendo - Snapshot recibido
📊 Snapshot: 150/500 pedidos procesados
📈 Fitness: 0.8900
✈️ Procesando segments del snapshot...

🔍 procesarSegmentsSnapshot - Snapshot recibido
📍 Aeropuertos disponibles: 30
⏰ Tiempo simulado: 2025-01-15T10:30:00Z

📦 Pedido 1/5: ORD-001
   Holgura: 45 minutos ✅
   📍 Ruta 1: 2 segmentos

   ✈️ Segment 1: LIM-MIA-0800
      LIM → MIA
      Despegue: 2025-01-15T13:00:00Z
      Llegada:  2025-01-15T19:30:00Z
      Cantidad: 10 paquetes
      ✅ Origen: LIM [-12.02, -77.11]
      ✅ Destino: MIA [25.80, -80.29]
      ✅ Vuelo creado con interpolación temporal
      🎬 Sistema híbrido ACTIVADO

   ✈️ Segment 2: MIA-NYC-1200
      MIA → NYC
      Despegue: 2025-01-15T21:00:00Z
      Llegada:  2025-01-16T00:45:00Z
      Cantidad: 10 paquetes
      ✅ Origen: MIA [25.80, -80.29]
      ✅ Destino: NYC [40.64, -73.78]
      ✅ Vuelo creado con interpolación temporal
      🎬 Sistema híbrido ACTIVADO

📦 Pedido 2/5: ORD-002
   Holgura: -15 minutos ⚠️
   📍 Ruta 1: 1 segmentos

   ✈️ Segment 3: LIM-BOG-0600
      LIM → BOG
      Despegue: 2025-01-15T11:00:00Z
      Llegada:  2025-01-15T14:20:00Z
      Cantidad: 5 paquetes
      ✅ Origen: LIM [-12.02, -77.11]
      ✅ Destino: BOG [4.70, -74.15]
      ✅ Vuelo creado con interpolación temporal
      🎬 Sistema híbrido ACTIVADO

📊 ============================================
📊 Total de segments procesados: 3
📊 Total de vuelos creados: 3
🎬 Vuelos con interpolación temporal: 3/3
📊 ============================================

🗺️ Primer vuelo (ejemplo): {
  id: "LIM-MIA-0800-ORD-001-1",
  flightId: "LIM-MIA-0800",
  from: "LIM",
  to: "MIA",
  departure: "2025-01-15T13:00:00Z",
  arrival: "2025-01-15T19:30:00Z",
  orderId: "ORD-001",
  status: "active"
}

🔄 Reemplazando flights array con 3 vuelos nuevos

🗺️ DynamicMarkers - Recibidos 3 vuelos
✈️ Actualizando 3 vuelos en el mapa con sistema híbrido
  ✈️ Vuelo nuevo 1: LIM-MIA-0800-ORD-001-1 - Interpolación: true - Pos: [-12.021, -77.114]
  ✈️ Vuelo nuevo 2: MIA-NYC-1200-ORD-001-2 - Interpolación: true - Pos: [25.796, -80.287]
  ✈️ Vuelo nuevo 3: LIM-BOG-0600-ORD-002-3 - Interpolación: true - Pos: [-12.021, -77.114]
```

---

## 🎨 Comportamiento Visual Esperado

### 1. **Vuelos con holgura positiva** (`slackMinutes > 0`)
- Color: 🟢 Verde (`#10b981`)
- Estado: `active`
- Popup: Muestra holgura en minutos

### 2. **Vuelos retrasados** (`slackMinutes <= 0`)
- Color: 🔴 Rojo (`#ef4444`)
- Estado: `retrasado`
- Popup: Alerta de retraso

### 3. **Animación continua**
- Movimiento píxel a píxel usando `departureUtc` y `arrivalUtc`
- Interpolación temporal cada frame (60 FPS)
- Transiciones suaves con easing cúbico

### 4. **Múltiples segments por pedido**
- Cada segment es un vuelo independiente
- Varios aviones pueden compartir el mismo `orderId`
- Ejemplo: ORD-001 tiene 2 segments → 2 aviones en el mapa

---

## 🔄 Compatibilidad con Formato Antiguo

El sistema mantiene **retrocompatibilidad** con la estructura anterior:

```javascript
// Formato antiguo (SIGUE FUNCIONANDO)
{
  status: "RUNNING",
  solution: {
    routes: [...]
  }
}

// Formato nuevo (PRIORIDAD)
{
  type: "PROGRESS",
  snapshot: {
    orderPlans: [...]
  }
}
```

**Lógica de detección:**
1. ¿Tiene `snapshot.orderPlans`? → Usar `procesarSegmentsSnapshot()`
2. ¿Tiene `solution.routes`? → Usar `procesarRutasSnapshot()` (antiguo)
3. Ninguno → Mostrar advertencia

---

## 🧪 Testing del Sistema Adaptado

### Checklist de validación:

- [ ] **Backend envía snapshot**: ¿Contiene `orderPlans`?
- [ ] **Logs detectan estructura**: ¿Dice "Procesando segments del snapshot"?
- [ ] **Extracción de segments**: ¿Muestra cada segment con sus datos?
- [ ] **Timestamps presentes**: ¿Se ven `departureUtc` y `arrivalUtc`?
- [ ] **Sistema híbrido activado**: ¿Dice "Sistema híbrido ACTIVADO"?
- [ ] **Vuelos en mapa**: ¿Aparecen los aviones?
- [ ] **Movimiento continuo**: ¿Se mueven píxel a píxel?
- [ ] **Colores correctos**: ¿Verde para a tiempo, rojo para retrasado?
- [ ] **Popups informativos**: ¿Muestran orderId y slackMinutes?

---

## 🐛 Troubleshooting

### Problema: No aparecen vuelos en el mapa

**Causas posibles:**
1. Backend no envía `orderPlans` en el snapshot
2. Segments no tienen `departureUtc`/`arrivalUtc`
3. Códigos de aeropuerto no coinciden con la BD

**Solución:**
```javascript
// Verificar en consola:
// ✅ "📦 Pedido 1/X: ORD-XXX"
// ✅ "✈️ Segment X: XXXX-XXXX-XXXX"
// ❌ "⚠️ No hay orderPlans en el snapshot" → Backend no envía estructura correcta
```

---

### Problema: Vuelos sin movimiento

**Causas posibles:**
1. `departureUtc` o `arrivalUtc` son `null`
2. Timestamps en formato incorrecto
3. Tiempo simulado no avanza

**Solución:**
```javascript
// Verificar en consola:
// ✅ "Despegue: 2025-01-15T13:00:00Z"
// ✅ "Llegada: 2025-01-15T19:30:00Z"
// ❌ "Despegue: null" → Backend no envía timestamps
```

---

### Problema: Todos los vuelos en rojo

**Causa:** `slackMinutes` siempre negativo o cero

**Solución:**
- Revisar cálculo de holgura en el backend
- Verificar que `arrivalUtc < deadlineUtc`

---

## 📝 Ejemplo de Flujo Completo

```
1. Backend envía mensaje WebSocket:
   {
     "type": "PROGRESS",
     "snapshot": {
       "processedOrders": 150,
       "orderPlans": [
         {
           "orderId": "ORD-001",
           "slackMinutes": 45,
           "routes": [
             {
               "segments": [
                 {
                   "flightId": "LIM-MIA-0800",
                   "origin": "LIM",
                   "destination": "MIA",
                   "departureUtc": "2025-01-15T13:00:00Z",
                   "arrivalUtc": "2025-01-15T19:30:00Z",
                   "quantity": 10
                 }
               ]
             }
           ]
         }
       ]
     }
   }

2. procesarMensajeSimulacion() detecta:
   ✅ datos.type === "PROGRESS"
   ✅ datos.snapshot.orderPlans existe
   → Llama procesarSegmentsSnapshot()

3. procesarSegmentsSnapshot() extrae:
   ✅ orderId: "ORD-001"
   ✅ slackMinutes: 45
   ✅ segment.flightId: "LIM-MIA-0800"
   ✅ segment.origin: "LIM"
   ✅ segment.destination: "MIA"
   ✅ segment.departureUtc: "2025-01-15T13:00:00Z"
   ✅ segment.arrivalUtc: "2025-01-15T19:30:00Z"

4. Busca aeropuertos:
   ✅ LIM: [-12.0219, -77.1143]
   ✅ MIA: [25.7959, -80.2870]

5. Crea objeto vuelo:
   {
     id: "LIM-MIA-0800-ORD-001-1",
     flightId: "LIM-MIA-0800",
     origin: { code: "LIM", lat: -12.0219, lng: -77.1143 },
     destination: { code: "MIA", lat: 25.7959, lng: -80.2870 },
     fechaInicial: "2025-01-15T13:00:00Z",
     fechaFinal: "2025-01-15T19:30:00Z",
     pedidoId: "ORD-001",
     slackMinutes: 45,
     status: "active",
     packageCapacity: 10
   }

6. setFlights([nuevoVuelo])

7. DynamicMarkers detecta fechaInicial/fechaFinal:
   → Usa calculateInterpolatedPosition()

8. Update loop (60 FPS):
   currentTime = Date.now()
   progress = (currentTime - departureTime) / (arrivalTime - departureTime)
   lat = -12.0219 + (25.7959 - (-12.0219)) * progress
   lng = -77.1143 + (-80.2870 - (-77.1143)) * progress

9. Avión se mueve continuamente en el mapa 🎬
   Color: Verde (slackMinutes > 0)
   Movimiento: Suave, píxel a píxel
```

---

## 📚 Archivos Modificados

```
src/pages/simulacion/Simulador/SimuladorSemanal.js
├─ procesarSegmentsSnapshot() [NUEVO]
│  ├─ Extrae segments de orderPlans → routes → segments
│  ├─ Crea vuelos con timestamps
│  └─ Identifica retrasados por slackMinutes
│
├─ procesarSegmentsSnapshotRef [NUEVO]
│  └─ Ref para evitar circular dependencies
│
├─ procesarMensajeSimulacion() [MODIFICADO]
│  ├─ Detecta type: "PROGRESS"
│  ├─ Prioriza snapshot.orderPlans
│  └─ Mantiene fallback para formato antiguo
│
└─ Sistema híbrido de animación [COMPATIBLE]
   ├─ calculateInterpolatedPosition()
   ├─ DynamicMarkers con update loop
   └─ Interpolación temporal basada en departureUtc/arrivalUtc
```

---

## ✅ Conclusión

El sistema híbrido de animación ahora está **completamente adaptado** a la nueva estructura JSON del backend. Los cambios principales son:

1. ✅ **Nueva función** `procesarSegmentsSnapshot()` para extraer segments
2. ✅ **Detección automática** de formato nuevo vs antiguo
3. ✅ **Compatibilidad total** con sistema de interpolación temporal
4. ✅ **Identificación de retrasados** usando `slackMinutes`
5. ✅ **Asociación pedido-vuelo** usando `orderId`

**Estado actual:** ✅ **LISTO PARA TESTING CON BACKEND REAL**

El sistema funcionará automáticamente cuando el backend envíe la nueva estructura con `type: "PROGRESS"` y `snapshot.orderPlans`.
