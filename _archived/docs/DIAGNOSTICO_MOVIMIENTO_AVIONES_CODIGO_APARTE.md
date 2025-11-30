# 🔍 DIAGNÓSTICO: Movimiento de Aviones - CODIGO_APARTE vs Tu Frontend

## 📊 Comparación Crítica

### ✅ CODIGO_APARTE (FUNCIONA)

#### 1️⃣ **Sistema de Tiempo Simulado**
```typescript
// useSimulacion.ts - Línea 265
const [tiempoMovimiento, setTiempoMovimiento] = useState(0);
const [tiempoSimulado, setTiempoSimulado] = useState<Date | null>(null);
const [simBaseSimulado, setSimBaseSimulado] = useState<number | null>(null);
const [simBaseReal, setSimBaseReal] = useState<number | null>(null);
const [simSpeed, setSimSpeed] = useState(DEFAULT_SPEED); // 2000x

// ===== INCREMENTO DE TIEMPO SIMULADO (ANIMACIÓN) =====
useEffect(() => {
  if (!shouldAnimate || simBaseReal === null || simBaseSimulado === null) return;

  const interval = setInterval(() => {
    setTiempoMovimiento(Date.now() - simBaseReal);
  }, 50); // 🎯 CADA 50ms (20 FPS)

  return () => clearInterval(interval);
}, [shouldAnimate, simBaseReal, simBaseSimulado]);

// ===== CÁLCULO DE TIEMPO SIMULADO =====
useEffect(() => {
  if (simBaseSimulado === null) {
    setTiempoSimulado(null);
    return;
  }
  const msSimuladosPasados = tiempoMovimiento * simSpeed; // 🔥 MULTIPLICAR POR VELOCIDAD
  let targetMs = simBaseSimulado + msSimuladosPasados;
  if (timelineEndMs !== null) {
    targetMs = Math.min(targetMs, timelineEndMs);
  }
  setTiempoSimulado(new Date(targetMs)); // 🎯 ACTUALIZAR ESTADO
}, [simBaseSimulado, tiempoMovimiento, simSpeed, timelineEndMs]);
```

**📍 Clave:** 
- `tiempoMovimiento` se incrementa cada 50ms con el **tiempo real transcurrido**
- `tiempoSimulado` = `simBaseSimulado` + (`tiempoMovimiento` × `simSpeed`)
- El estado `tiempoSimulado` se actualiza **automáticamente** y causa re-render

---

#### 2️⃣ **Cálculo de Vuelos en Movimiento**
```typescript
// useSimulacion.ts - Línea 314
const vuelosEnMovimiento: VueloEnMovimiento[] = useMemo(() => {
  if (!tiempoSimulado || activeSegments.length === 0) {
    return [];
  }

  const tiempoActualMs = tiempoSimulado.getTime(); // 🎯 USA ESTADO REACTIVO

  const vuelosEnCurso: VueloEnMovimiento[] = [];

  activeSegments.forEach((segmento, index) => {
    const horaSalida = Date.parse(segmento.departureUtc);
    const horaLlegada = Date.parse(segmento.arrivalUtc);
    const duracionVuelo = horaLlegada - horaSalida;

    let progreso = 0;
    let estadoVisual: VueloEnMovimiento['estadoVisual'] = 'en curso';

    if (tiempoActualMs >= horaLlegada) {
      progreso = 100;
      estadoVisual = 'completado';
    } else {
      const tiempoTranscurrido = Math.max(0, tiempoActualMs - horaSalida);
      progreso = Math.min(100, (tiempoTranscurrido / duracionVuelo) * 100);
      estadoVisual = segmento.retrasado ? 'retrasado' : 'en curso';
    }

    const ratio = Math.min(progreso / 100, 1);
    const latActual = origen[0] + (destino[0] - origen[0]) * ratio;
    const lonActual = origen[1] + (destino[1] - origen[1]) * ratio;

    vuelosEnCurso.push({
      id: segmento.id,
      latActual,
      lonActual,
      progreso,
      // ...
    });
  });

  return vuelosEnCurso;
}, [activeSegments, tiempoSimulado, aeropuertos]); // 🎯 DEPENDENCIAS REACTIVAS
```

**📍 Clave:**
- `useMemo` con dependencia de `tiempoSimulado` → **re-calcula automáticamente**
- Calcula posición **directamente** desde `tiempoSimulado.getTime()`
- **NO usa refs**, usa **estado reactivo**

---

#### 3️⃣ **Renderizado del Mapa**
```tsx
// MapaVuelos.tsx - Línea 78
export function MapaVuelos({ vuelosEnMovimiento, ... }: MapaVuelosProps) {
  // ...
  
  {vuelosEnMovimiento?.map((vuelo) => {
    const bearing = calculateBearing(vuelo.latActual, vuelo.lonActual, destinoLat, destinoLon);

    return (
      <Marker
        key={vuelo.id} // 🎯 KEY ÚNICO
        position={[vuelo.latActual, vuelo.lonActual]} // 🎯 POSICIÓN CALCULADA
        icon={createRotatedPlaneIcon(vuelo.estadoVisual, bearing)}
        opacity={opacidadAvion}
      >
        {/* ... */}
      </Marker>
    );
  })}
}
```

**📍 Clave:**
- Recibe `vuelosEnMovimiento` como **prop reactiva**
- Cuando `vuelosEnMovimiento` cambia → **re-render automático**
- No necesita `useEffect` para actualizar posiciones

---

### ❌ TU FRONTEND (NO FUNCIONA)

#### 1️⃣ **Sistema de Tiempo Simulado**
```javascript
// SimuladorSemanal.js - Línea 215
const tiempoSimuladoBackendRef = React.useRef(null); // ❌ REF (NO REACTIVO)
const ultimaActualizacionRealRef = React.useRef(null); // ❌ REF (NO REACTIVO)
const tiempoSimuladoRef = React.useRef(Date.now()); // ❌ REF (NO REACTIVO)

useEffect(() => {
  const interpolationInterval = setInterval(() => {
    if (tiempoSimuladoBackendRef.current && ultimaActualizacionRealRef.current) {
      const ahora = Date.now();
      const deltaTiempoReal = ahora - ultimaActualizacionRealRef.current;

      // ❌ ACTUALIZA REF, NO ESTADO
      const tiempoSimuladoMs = tiempoSimuladoBackendRef.current + deltaTiempoReal;
      tiempoSimuladoRef.current = tiempoSimuladoMs; // ❌ NO CAUSA RE-RENDER
    }
  }, 1000 / 60); // Cada ~16ms (60 FPS)

  return () => clearInterval(interpolationInterval);
}, []);
```

**🐛 PROBLEMA:**
- Usa **refs** en lugar de **estado**
- Actualizar `tiempoSimuladoRef.current` **NO causa re-render**
- Los componentes no se enteran de que el tiempo cambió

---

#### 2️⃣ **Cálculo de Posiciones**
```javascript
// SimuladorSemanal.js - Línea 440
function calculateInterpolatedPosition(vuelo, tiempoActualMs) {
  // ... cálculos correctos ...
  
  const tiempoTranscurrido = tiempoActualMs - horaSalida;
  const progreso = (tiempoTranscurrido / duracionVuelo) * 100;
  const ratio = progreso / 100;

  const lat = vuelo.origin.lat + (vuelo.destination.lat - vuelo.origin.lat) * ratio;
  const lng = vuelo.origin.lng + (vuelo.destination.lng - vuelo.origin.lng) * ratio;

  return { lat, lng, progress: progreso, status: 'active' };
}
```

**✅ La función está bien**, pero...

**🐛 PROBLEMA:**
- Se llama con `tiempoSimuladoRef.current` (ref)
- Cuando la ref cambia → **la función NO se re-ejecuta**
- Los marcadores siguen mostrando las **posiciones antiguas**

---

#### 3️⃣ **Renderizado del Mapa**
```javascript
// SimuladorSemanal.js - Línea 2200+ (dentro de return)
{flights.map((flight) => {
  // ❌ USA REF (NO REACTIVO)
  const interpolated = calculateInterpolatedPosition(flight, tiempoSimuladoRef.current);
  
  return (
    <Marker
      key={flight.id}
      position={[interpolated.lat, interpolated.lng]}
      icon={createAirplaneIcon(flight, flight.rotation)}
    />
  );
})}
```

**🐛 PROBLEMA:**
- Lee `tiempoSimuladoRef.current` durante el render
- Cuando la ref cambia → **NO hay re-render**
- Los aviones quedan **congelados** en la primera posición

---

## 🎯 SOLUCIÓN: Migrar de Refs a Estado Reactivo

### 📝 Cambios Necesarios

#### 1️⃣ **Convertir Refs a Estado**

```javascript
// ❌ ANTES (refs no reactivos)
const tiempoSimuladoBackendRef = React.useRef(null);
const tiempoSimuladoRef = React.useRef(Date.now());
const ultimaActualizacionRealRef = React.useRef(null);

// ✅ DESPUÉS (estado reactivo)
const [tiempoSimuladoBackend, setTiempoSimuladoBackend] = useState(null);
const [tiempoSimulado, setTiempoSimulado] = useState(Date.now());
const [ultimaActualizacionReal, setUltimaActualizacionReal] = useState(null);
const [tiempoMovimiento, setTiempoMovimiento] = useState(0);
```

---

#### 2️⃣ **Actualizar Intervalo de Tiempo**

```javascript
// ✅ NUEVO: Incrementar tiempoMovimiento cada 50ms
useEffect(() => {
  if (!ultimaActualizacionReal || !tiempoSimuladoBackend) return;

  const interval = setInterval(() => {
    setTiempoMovimiento(Date.now() - ultimaActualizacionReal);
  }, 50); // 20 FPS (suficiente para animación suave)

  return () => clearInterval(interval);
}, [ultimaActualizacionReal, tiempoSimuladoBackend]);
```

---

#### 3️⃣ **Calcular Tiempo Simulado**

```javascript
// ✅ NUEVO: Calcular tiempo simulado basado en tiempoMovimiento
useEffect(() => {
  if (!tiempoSimuladoBackend) {
    setTiempoSimulado(Date.now());
    return;
  }

  const SPEED_MULTIPLIER = 500; // Tu K (ajustable)
  const msSimuladosPasados = tiempoMovimiento * SPEED_MULTIPLIER;
  const nuevoTiempoSimulado = tiempoSimuladoBackend + msSimuladosPasados;
  
  setTiempoSimulado(nuevoTiempoSimulado); // 🎯 ACTUALIZAR ESTADO
}, [tiempoSimuladoBackend, tiempoMovimiento]);
```

---

#### 4️⃣ **Calcular Vuelos con useMemo**

```javascript
// ✅ NUEVO: Calcular posiciones reactivamente
const vuelosEnMovimiento = useMemo(() => {
  if (!tiempoSimulado || flights.length === 0) {
    return [];
  }

  return flights.map(flight => {
    const interpolated = calculateInterpolatedPosition(flight, tiempoSimulado);
    
    return {
      ...flight,
      currentLat: interpolated.lat,
      currentLng: interpolated.lng,
      progress: interpolated.progress,
      status: interpolated.status
    };
  });
}, [flights, tiempoSimulado]); // 🎯 DEPENDENCIAS REACTIVAS
```

---

#### 5️⃣ **Renderizar con Datos Reactivos**

```javascript
// ✅ NUEVO: Renderizar usando estado calculado
{vuelosEnMovimiento.map((flight) => (
  <Marker
    key={flight.id}
    position={[flight.currentLat, flight.currentLng]} // 🎯 POSICIÓN CALCULADA
    icon={createAirplaneIcon(flight, flight.rotation)}
  >
    <Popup>
      Vuelo: {flight.flightId}<br/>
      Progreso: {Math.round(flight.progress)}%<br/>
      Estado: {flight.status}
    </Popup>
  </Marker>
))}
```

---

#### 6️⃣ **Actualizar desde WebSocket**

```javascript
// ✅ ACTUALIZAR ESTADO al recibir mensaje
const procesarMensajeSimulacion = useCallback((datos) => {
  if (datos.tipo === 'PROGRESO_AG' && datos.fechaSimulada) {
    const timestampSimulado = new Date(datos.fechaSimulada).getTime();
    
    // ✅ ACTUALIZAR ESTADO (no refs)
    setTiempoSimuladoBackend(timestampSimulado);
    setUltimaActualizacionReal(Date.now());
    
    console.log(`⏰ Tiempo simulado: ${datos.fechaSimulada}`);
  }
  
  // Procesar vuelos...
}, []);
```

---

## 📊 Resumen de Cambios

| Componente | ❌ Antes | ✅ Después |
|-----------|---------|-----------|
| **Tiempo Simulado** | `tiempoSimuladoRef` (ref) | `tiempoSimulado` (estado) |
| **Tiempo Backend** | `tiempoSimuladoBackendRef` (ref) | `tiempoSimuladoBackend` (estado) |
| **Última Actualización** | `ultimaActualizacionRealRef` (ref) | `ultimaActualizacionReal` (estado) |
| **Cálculo de Posiciones** | En `render` con ref | `useMemo` con dependencias |
| **Intervalo** | 60 FPS (overkill) | 20 FPS (suficiente) |
| **Re-render** | ❌ No se dispara | ✅ Automático |

---

## 🚀 Implementación

¿Quieres que implemente estos cambios en tu `SimuladorSemanal.js`?

Los pasos serían:

1. ✅ Convertir refs a estado
2. ✅ Crear `useMemo` para `vuelosEnMovimiento`
3. ✅ Actualizar intervalo de tiempo
4. ✅ Modificar `procesarMensajeSimulacion`
5. ✅ Actualizar renderizado del mapa
6. ✅ Probar y ajustar velocidad

---

## 🔍 Por Qué CODIGO_APARTE Funciona

1. **Estado Reactivo**: Usa `useState` para tiempo simulado
2. **useMemo**: Re-calcula vuelos automáticamente cuando cambia el tiempo
3. **Dependencias**: React sabe cuándo re-renderizar
4. **Simplicidad**: No intenta "optimizar" con refs innecesarios

---

## 🐛 Por Qué Tu Código NO Funciona

1. **Refs No Reactivos**: Cambiar una ref NO causa re-render
2. **Cálculos en Render**: Lee refs durante render (valor viejo)
3. **Sin useMemo**: No hay re-cálculo automático
4. **Optimización Prematura**: Refs para "performance" rompen reactividad

---

## 💡 Lección Clave

> **En React, si algo debe causar re-render, DEBE ser `useState`**
> 
> **Si algo depende de otro valor, usa `useMemo` o `useEffect`**
> 
> **Las refs son para valores que NO afectan la UI directamente**

---

**¿Procedemos con la implementación?** 🚀
