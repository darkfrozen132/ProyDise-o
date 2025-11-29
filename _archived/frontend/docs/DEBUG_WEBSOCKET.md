# 🐛 Debug WebSocket - Sin Aviones en el Mapa

## 🔍 Diagnóstico del Problema

El mapa muestra **solo aeropuertos** pero **no aviones** a pesar de que el sistema híbrido está implementado.

---

## ✅ Lo que YA está implementado:

1. ✅ **Update loop** continuo (60 FPS) en `DynamicMarkers`
2. ✅ **calculateInterpolatedPosition()** para interpolación temporal
3. ✅ **procesarSegmentsSnapshot()** para procesar la nueva estructura JSON
4. ✅ **Detección automática** de estructura en `procesarMensajeSimulacion()`
5. ✅ **Timestamps** (`fechaInicial`, `fechaFinal`) en objetos de vuelo

---

## 🚨 Posibles causas del problema:

### 1. **Backend no envía mensajes WebSocket**
El backend debe enviar mensajes con esta estructura:

```json
{
  "type": "PROGRESS",
  "snapshot": {
    "simulationId": "uuid",
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
```

**Verificar:** ¿El backend está enviando este formato?

---

### 2. **WebSocket no está conectado**
**Verificar:**
- ¿La aplicación muestra "✅ WebSocket conectado"?
- ¿El backend está corriendo en `http://localhost:8000`?
- ¿La simulación se inició correctamente?

---

### 3. **Mensajes llegan pero en formato diferente**
**Verificar en consola del navegador (F12):**

Buscar estos logs:
```
📨 Mensaje recibido: {objeto completo}
```

**Casos posibles:**

#### Caso A: Formato antiguo (vuelos directos)
```javascript
{
  "tipo": "PROGRESO_AG",
  "solucion": {
    "vuelos": [...]
  }
}
```
→ Se procesa con `procesarVuelosDirectos()`

#### Caso B: Formato antiguo (rutas con subRutas)
```javascript
{
  "tipo": "PROGRESO_AG",
  "solucion": {
    "rutas": [...]
  }
}
```
→ Se procesa con `procesarRutasSimulacion()`

#### Caso C: Nueva estructura ✅ (esperada)
```javascript
{
  "type": "PROGRESS",
  "snapshot": {
    "orderPlans": [...]
  }
}
```
→ Se procesa con `procesarSegmentsSnapshot()`

---

### 4. **Los datos llegan pero los aeropuertos no se encuentran**

**Logs esperados en consola:**
```
✈️ Segment 1: LIM-MIA-0800
   LIM → MIA
   ✅ Origen: LIM [lat, lng]
   ✅ Destino: MIA [lat, lng]
   ✅ Vuelo creado con interpolación temporal
   🎬 Sistema híbrido ACTIVADO
```

**Si aparece:**
```
❌ Aeropuerto ORIGEN no encontrado: "LIM"
```
→ Problema: Códigos de aeropuerto no coinciden con la base de datos

---

## 🔧 Pasos para diagnosticar:

### Paso 1: Abrir consola del navegador
```
F12 → Pestaña "Console"
```

### Paso 2: Buscar mensajes clave

#### A. Verificar conexión WebSocket:
```
✅ WebSocket conectado
📡 Suscrito a: /topic/simulations/{sessionId}
```

#### B. Verificar mensajes recibidos:
```
📨 Mensaje recibido: {...}
```

**¿Qué debe aparecer?**
- Si dice `"type": "PROGRESS"` → ✅ Formato correcto
- Si dice `"tipo": "PROGRESO_AG"` → ⚠️ Formato antiguo

#### C. Verificar procesamiento:
```
🎮 Simulación corriendo - Snapshot recibido
📊 Snapshot: 150/500 pedidos procesados
✈️ Procesando segments del snapshot...
```

#### D. Verificar vuelos creados:
```
📦 Pedido 1/10: ORD-001
   ✈️ Segment 1: LIM-MIA-0800
      ✅ Vuelo creado con interpolación temporal
      🎬 Sistema híbrido ACTIVADO

📊 Total de segments procesados: 25
📊 Total de vuelos creados: 25
🎬 Vuelos con interpolación temporal: 25/25
```

---

## 📋 Checklist de verificación:

- [ ] **Backend corriendo**: ¿`http://localhost:8000` responde?
- [ ] **WebSocket conectado**: ¿Aparece "✅ WebSocket conectado"?
- [ ] **Simulación iniciada**: ¿Se hizo click en "Iniciar Simulación"?
- [ ] **Mensajes recibidos**: ¿Aparece "📨 Mensaje recibido"?
- [ ] **Estructura correcta**: ¿El mensaje tiene `"type": "PROGRESS"` y `"snapshot"`?
- [ ] **Segments procesados**: ¿Aparece "✈️ Procesando segments del snapshot"?
- [ ] **Aeropuertos encontrados**: ¿Aparece "✅ Origen: XXX" y "✅ Destino: YYY"?
- [ ] **Vuelos creados**: ¿Aparece "📊 Total de vuelos creados: X"?
- [ ] **Sistema híbrido activo**: ¿Aparece "🎬 Sistema híbrido ACTIVADO"?

---

## 🛠️ Soluciones según el diagnóstico:

### Problema: No aparece "📨 Mensaje recibido"
**Causa:** WebSocket no está recibiendo mensajes del backend

**Solución:**
1. Verificar que el backend esté enviando mensajes
2. Verificar que el `sessionId` sea correcto
3. Verificar que el topic sea `/topic/simulations/{sessionId}`

---

### Problema: Aparece "⚠️ No hay orderPlans en el snapshot"
**Causa:** El mensaje llega pero no tiene la estructura correcta

**Solución:**
1. Verificar en consola el objeto completo del mensaje
2. Revisar si el backend está enviando `snapshot.orderPlans`
3. Si el backend usa formato antiguo, los vuelos se procesarán con `procesarVuelosDirectos()` o `procesarRutasSimulacion()`

---

### Problema: Aparece "❌ Aeropuerto no encontrado"
**Causa:** Códigos de aeropuerto no coinciden

**Solución:**
1. Verificar que los códigos sean **ICAO** (4 letras, ej: "SPIM") o **IATA** (3 letras, ej: "LIM")
2. Verificar que la base de datos de aeropuertos contenga esos códigos
3. Revisar en consola: "📍 Aeropuertos disponibles: 30"

---

### Problema: Vuelos se crean pero no aparecen en el mapa
**Causa:** Posible problema en `DynamicMarkers` o vista activa

**Solución:**
1. Verificar que `activeView` sea `'flights'` o `'routes'`
2. Verificar logs: "🗺️ DynamicMarkers - Recibidos X vuelos"
3. Verificar que los timestamps sean válidos (formato ISO 8601)

---

## 🧪 Test manual:

### Copiar y pegar en consola del navegador:

```javascript
// Ver estado actual
console.log('Vuelos actuales:', window.flights);
console.log('Aeropuertos:', window.airports);
console.log('Vista activa:', window.activeView);

// Ver si hay marcadores en el mapa
const markers = document.querySelectorAll('.leaflet-marker-pane .leaflet-marker-icon');
console.log('Marcadores en el mapa:', markers.length);

// Ver si hay aviones (debe tener isFlight)
const airplanes = Array.from(markers).filter(m => 
  m.innerHTML.includes('✈') || m.innerHTML.includes('airplane')
);
console.log('Aviones:', airplanes.length);
```

---

## 📞 Información para reportar:

Si el problema persiste, recopilar esta información:

1. **Logs de consola** (copiar todo el output desde el inicio)
2. **Estructura del mensaje** recibido (objeto JSON completo)
3. **Cantidad de aeropuertos** cargados: "📍 Aeropuertos disponibles: X"
4. **Estado del WebSocket**: ¿Conectado?
5. **Vista activa**: ¿flights, routes, airports?
6. **Mensajes de error**: ¿Algún ❌ en consola?

---

## 🎯 Resumen rápido:

| Síntoma | Causa probable | Solución |
|---------|----------------|----------|
| Solo aeropuertos visibles | Backend no envía mensajes | Verificar backend y WebSocket |
| "No hay orderPlans" | Estructura incorrecta | Verificar formato JSON del backend |
| "Aeropuerto no encontrado" | Códigos no coinciden | Verificar códigos ICAO/IATA |
| Vuelos creados pero invisibles | Vista incorrecta | Cambiar a vista "flights" |

---

**Próximo paso:** Abrir consola (F12) y buscar los logs mencionados arriba.
