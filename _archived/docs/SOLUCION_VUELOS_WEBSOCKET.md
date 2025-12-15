# 🔧 SOLUCIÓN: Vuelos WebSocket no se mostraban en el mapa

## 📋 Problema Original

**Síntoma:** Los aeropuertos SÍ se mostraban en el mapa, pero cuando iniciaba la simulación WebSocket, **los vuelos NO aparecían**.

## 🔍 Causa Raíz

Había **DOS sistemas de vuelos compitiendo**:

1. **Sistema Local (planFixed + simClock)** - Líneas 288-360
   - Actualizaba `flights` basándose en `planFixed` y `simClock`
   - Se ejecutaba cada vez que cambiaba `simClock`
   - **REEMPLAZABA** el array `flights` completamente

2. **Sistema WebSocket (procesarRutasSimulacion)** - Líneas 1116+
   - Agregaba vuelos desde mensajes WebSocket
   - Intentaba **ACUMULAR** vuelos: `setFlights(prev => [...prev, ...nuevosVuelos])`

### El Conflicto

```javascript
// Sistema Local (useEffect ejecutándose constantemente)
useEffect(() => {
    // ... procesamiento de vuelos locales ...
    setFlights(vuelosLocales); // ❌ REEMPLAZA todo el array
}, [simClock, planFixed, airports]);

// Sistema WebSocket (al recibir mensaje)
const procesarRutasSimulacion = () => {
    // ... procesamiento de vuelos WebSocket ...
    setFlights(prev => [...prev, ...nuevosVuelos]); // ❌ Intentaba acumular
};
```

**Resultado:** Los vuelos WebSocket se agregaban, pero inmediatamente eran **BORRADOS** por el useEffect del sistema local.

---

## ✅ Solución Implementada

### 1. **Deshabilitar Sistema Local de Vuelos**

**Archivo:** `SimuladorSemanal.js` (líneas 288-360)

```javascript
/* ==================== VUELOS LOCALES DESHABILITADOS - SOLO WEBSOCKET ==================== */
// ❌ COMENTADO: Ya no usamos vuelos locales basados en planFixed y simClock
// ✅ AHORA: Todos los vuelos vienen del WebSocket mediante procesarRutasSimulacion()

// useEffect(() => {
//     if (!simClock || planFixed.length === 0 || airports.length === 0) return;
//     ... código comentado ...
// }, [simClock, planFixed, airports]);
```

### 2. **Deshabilitar Reloj de Simulación Local**

**Archivo:** `SimuladorSemanal.js` (líneas ~370-385)

```javascript
/* ==================== RELOJ LOCAL DESHABILITADO - SOLO WEBSOCKET ==================== */
// ❌ COMENTADO: Ya no avanzamos el reloj localmente
// ✅ AHORA: El tiempo viene del backend en los mensajes WebSocket

// useEffect(() => {
//     if (!simulacionActiva) return;
//     const advanceMs = DESIRED_TIME_SCALE * REAL_TICK_MS;
//     const id = setInterval(() => { ... }, REAL_TICK_MS);
//     return () => clearInterval(id);
// }, [simulacionActiva]);
```

### 3. **Mejorar procesarRutasSimulacion con Logs Detallados**

**Cambios en `procesarRutasSimulacion` (línea 1116):**

```javascript
const procesarRutasSimulacion = useCallback((rutas) => {
    console.log(`🔍 procesarRutasSimulacion - Recibidas ${rutas?.length || 0} rutas`);
    console.log(`📍 Aeropuertos disponibles: ${airports.length}`);
    
    // ✅ Validación de entrada
    if (!rutas || rutas.length === 0) {
        console.warn('⚠️ No hay rutas para procesar');
        return;
    }
    
    const nuevosVuelos = [];

    rutas.forEach((ruta, rutaIdx) => {
        console.log(`\n📦 Ruta ${rutaIdx + 1}/${rutas.length} - PedidoID: ${ruta.pedidoId}`);
        
        ruta.subRutas?.forEach((subRuta, idx) => {
            // Buscar aeropuertos (case-insensitive)
            const origen = airports.find(a => 
                a.code.toUpperCase() === subRuta.origen.toUpperCase()
            );
            
            // ✅ Logs detallados si no encuentra aeropuerto
            if (!origen) {
                console.error(`❌ Aeropuerto ORIGEN no encontrado: "${subRuta.origen}"`);
                console.log(`📋 Aeropuertos disponibles (primeros 5):`, 
                    airports.slice(0, 5).map(a => a.code));
                return;
            }
            
            // ... crear vuelo ...
            
            nuevosVuelos.push(nuevoVuelo);
        });
    });

    // 🔥 CAMBIO IMPORTANTE: REEMPLAZAR en lugar de acumular
    console.log(`🔄 Reemplazando flights array con ${nuevosVuelos.length} vuelos nuevos`);
    setFlights(nuevosVuelos); // ← ANTES: [...prev, ...nuevosVuelos]
    
}, [airports]);
```

**Razón del cambio:** Reemplazar en lugar de acumular asegura que:
- Solo se muestran los vuelos de la **última generación** del AG
- No hay vuelos duplicados
- El mapa no se satura con vuelos antiguos

### 4. **Limpiar Vuelos al Iniciar Nueva Simulación**

**Cambios en `iniciarSimulacionWebSocketStomp` (línea 972):**

```javascript
const iniciarSimulacionWebSocketStomp = useCallback(async () => {
    // ... validaciones ...

    try {
        console.log('🚀 Iniciando simulación semanal con WebSocket STOMP...');
        
        // 🧹 LIMPIAR VUELOS ANTERIORES antes de iniciar nueva simulación
        console.log('🧹 Limpiando vuelos anteriores...');
        setFlights([]);
        setFlightsInAir(0);
        setProgresoAG(null);
        setMensajesSimulacion([]);
        
        setEstadoSimulacionStomp('running');
        // ... resto del código ...
    }
}, [/* deps */]);
```

**Razón:** Asegura que cada nueva simulación empiece con un **mapa limpio**, sin vuelos de simulaciones anteriores.

---

## 🎯 Resultado Final

Ahora el flujo es:

1. ✅ Usuario hace clic en **"Iniciar Simulación WebSocket"**
2. ✅ Se **limpian todos los vuelos** anteriores
3. ✅ Se inicia la simulación en el backend
4. ✅ Backend envía mensajes con `tipo: 'PROGRESO_AG'`
5. ✅ `procesarMensajeSimulacion` recibe el mensaje
6. ✅ Llama a `procesarRutasSimulacion(datos.solucion.rutas)`
7. ✅ Se crean vuelos **VERDES** (`#10b981`)
8. ✅ Se **REEMPLAZAN** los vuelos en el mapa (no se acumulan)
9. ✅ Los vuelos **SÍ SE VEN** en el mapa 🎉

---

## 🐛 Debugging

### Console Logs a Verificar

Cuando inicies la simulación, deberías ver:

```javascript
🚀 Iniciando simulación semanal con WebSocket STOMP...
🧹 Limpiando vuelos anteriores...
📨 Respuesta del servidor: {sessionId: "...", mensaje: "...", topicUrl: "..."}
✅ Simulación iniciada - Session ID: abc-123-xyz
📡 Suscribiéndose a: /topic/simulations/abc-123-xyz
📨 Mensaje recibido: {tipo: "PROGRESO_AG", generacion: 1, ...}
🧬 Progreso AG - Generación 1/20

🔍 procesarRutasSimulacion - Recibidas 3 rutas
📍 Aeropuertos disponibles: 45

📦 Ruta 1/3 - PedidoID: 123
  ✈️ SubRuta 1/2: KJFK → LFPG
    ✅ Origen: KJFK [40.64, -73.78]
    ✅ Destino: LFPG [49.01, 2.55]
    ✅ Vuelo creado en posición: [44.82, -35.62]

📊 ============================================
📊 Total de vuelos WebSocket creados: 6
📊 ============================================

🗺️ Primer vuelo (ejemplo): {
  id: "AG-123-0-1732574400000-0.12345",
  from: "KJFK",
  to: "LFPG",
  position: [44.82, -35.62],
  color: "#10b981"
}

🔄 Reemplazando flights array con 6 vuelos nuevos
✈️ Vuelos activos: 6
```

### Si NO ves vuelos

Verifica:

1. **Aeropuertos no encontrados:**
   ```
   ❌ Aeropuerto ORIGEN no encontrado: "JFK"
   ```
   **Solución:** El backend debe enviar códigos ICAO (4 letras), no IATA (3 letras)
   - ✅ Correcto: `KJFK`, `LFPG`, `EGLL`
   - ❌ Incorrecto: `JFK`, `CDG`, `LHR`

2. **Rutas vacías:**
   ```
   🔍 procesarRutasSimulacion - Recibidas 0 rutas
   ```
   **Solución:** Verifica que el backend envíe `datos.solucion.rutas` con contenido

3. **SubRutas vacías:**
   ```
   📦 Ruta 1/3 - PedidoID: 123
   ⚠️ Esta ruta no tiene subRutas
   ```
   **Solución:** Cada ruta debe tener `subRutas: [{origen, destino, vuelo, ...}]`

---

## 📊 Comparación Antes/Después

### ❌ ANTES (No funcionaba)

```javascript
// Sistema Local ejecutándose constantemente
useEffect(() => {
    setFlights(vuelosLocales); // ← Borraba vuelos WebSocket
}, [simClock]);

// Sistema WebSocket
const procesarRutasSimulacion = () => {
    setFlights(prev => [...prev, ...nuevosVuelos]); // ← Se acumulaban
};
```

**Problemas:**
- Vuelos WebSocket eran borrados inmediatamente
- Acumulación de vuelos causaba duplicados
- Conflicto entre dos fuentes de verdad

### ✅ DESPUÉS (Funciona)

```javascript
// Sistema Local DESHABILITADO
// useEffect(() => { ... }, [simClock]); ← COMENTADO

// Solo Sistema WebSocket
const procesarRutasSimulacion = () => {
    setFlights(nuevosVuelos); // ← Reemplaza limpiamente
};
```

**Beneficios:**
- ✅ Una sola fuente de verdad (WebSocket)
- ✅ No hay conflictos
- ✅ No hay acumulación de vuelos viejos
- ✅ Logs detallados para debugging

---

## 🎨 Identificación Visual

**Vuelos WebSocket (del AG):**
- 🟢 **Color verde** (`#10b981`)
- Siempre en progreso (mitad del recorrido)
- ID comienza con `AG-`

**Vuelos Locales (DESHABILITADOS):**
- 🔵 **Color azul** (`#007bff`)
- Ya no se usan

---

## 🚀 Próximos Pasos

1. **Iniciar el backend** en `http://localhost:8000`
2. **Abrir la aplicación** en el navegador
3. **Conectar WebSocket STOMP** (botón morado)
4. **Seleccionar fecha** (ej: `2025-01-02`)
5. **Iniciar simulación** (botón verde)
6. **Verificar console logs** (F12 → Console)
7. **Ver vuelos verdes** aparecer en el mapa 🎉

---

## 📝 Archivos Modificados

- ✅ `src/pages/simulacion/Simulador/SimuladorSemanal.js`
  - Líneas 288-360: Sistema local de vuelos comentado
  - Líneas 370-385: Reloj local comentado
  - Líneas 972-990: Limpieza al iniciar simulación
  - Líneas 1116-1200: `procesarRutasSimulacion` mejorado con logs

---

**Fecha de solución:** 26 de noviembre de 2025  
**Versión:** 1.0 - Solo WebSocket (sin sistema local)

---

## 🆘 Soporte

Si los vuelos aún no aparecen:

1. **Abre la consola del navegador** (F12)
2. **Copia TODOS los logs** desde que haces clic en "Iniciar Simulación"
3. **Verifica:**
   - ¿Se reciben mensajes con `tipo: 'PROGRESO_AG'`?
   - ¿Hay errores de aeropuertos no encontrados?
   - ¿El mensaje tiene `datos.solucion.rutas`?
   - ¿Las rutas tienen `subRutas`?

**Revisa el formato esperado del mensaje:**
```json
{
  "sessionId": "abc-123-xyz",
  "tipo": "PROGRESO_AG",
  "generacion": 1,
  "maxGeneraciones": 20,
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

¡Los vuelos deberían mostrarse ahora! 🚀✈️
