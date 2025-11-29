# ✅ Correcciones Realizadas - 26/11/2025

## 1. Error: React Duplicate Keys (`1764198868629`)

### Problema
React mostraba warnings de claves duplicadas porque:
1. Los mensajes de simulación usaban `id: Date.now()` que puede repetirse en el mismo milisegundo
2. Los IDs de vuelos en snapshot usaban `${segment.flightId}-${orderId}-${segment.departureUtc}` que podía repetirse

### Solución Aplicada

#### A) Mensajes de simulación (línea ~1477)
```javascript
// ANTES
const nuevoMensaje = {
    id: Date.now(),  // ❌ Puede duplicarse
    ...
};

// DESPUÉS
contadorMensajesRef.current += 1;
const nuevoMensaje = {
    id: `msg-${Date.now()}-${contadorMensajesRef.current}-${Math.random().toString(36).substr(2, 9)}`,  // ✅ Único
    ...
};
```

#### B) Vuelos de snapshot (línea ~1772)
```javascript
// ANTES
id: `${segment.flightId}-${orderId}-${segment.departureUtc}`,  // ❌ Puede duplicarse

// DESPUÉS  
id: `SNAP-${segment.flightId}-${orderId}-${routeIndex}-${segIndex}-${segmentIndex}-${Math.random().toString(36).substr(2, 6)}`,  // ✅ Único
```

---

## 2. Error: Failed to load tiles (400 Bad Request)

### Problema
OpenStreetMap devolvía error 400 para tiles con coordenadas inválidas:
- `b.tile.openstreetmap.org/3/-4/5.png` (X negativa)
- `c.tile.openstreetmap.org/3/-1/6.png` (X negativa)

Esto ocurre cuando el usuario arrastra el mapa fuera de los límites del mundo.

### Solución Aplicada

Se agregaron restricciones al MapContainer:
```javascript
<MapContainer 
    ...
    worldCopyJump={true}           // ✅ Salta automáticamente al centro
    maxBoundsViscosity={1.0}       // ✅ Fuerza límites estrictos
    maxBounds={[[-90, -180], [90, 180]]}  // ✅ Límites del mundo
>
    <TileLayer 
        ...
        bounds={[[-90, -180], [90, 180]]}  // ✅ Límites para tiles
    />
</MapContainer>
```

---

## ✅ Archivos Modificados

- `front/src/pages/simulacion/Simulador/SimuladorSemanal.js`
  - Línea ~1477: ID único para mensajes
  - Línea ~1772: ID único para vuelos de snapshot
  - Línea ~3016: Límites del mapa

---

## 🧪 Verificación

1. Recargar la página de simulación
2. Iniciar una simulación
3. Verificar en la consola que:
   - ❌ No aparecen warnings de "duplicate key"
   - ❌ No aparecen errores 400 de tiles
