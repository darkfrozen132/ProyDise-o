# 🎬 Sistema Híbrido - Diagrama de Flujo

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                        SISTEMA HÍBRIDO DE ANIMACIÓN                            ║
╚═══════════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. BACKEND → WEBSOCKET                                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │  Mensaje PROGRESO_AG          │
                    │  {                            │
                    │    vuelos: [                  │
                    │      {                        │
                    │        origenCodigoICAO       │
                    │        destinoCodigoICAO      │
                    │        fechaInicial ⏰        │
                    │        fechaFinal   ⏰        │
                    │        totalPaquetes          │
                    │        pedidos: [...]         │
                    │      }                        │
                    │    ]                          │
                    │  }                            │
                    └───────────────┬───────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. PROCESAMIENTO → procesarVuelosDirectos()                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
          ┌──────────────────┐          ┌──────────────────┐
          │ Buscar aeropuertos│          │ Validar timestamps│
          │ en airportsRef     │          │ ¿Tiene fechas?    │
          └─────────┬──────────┘          └─────────┬─────────┘
                    │                               │
                    │                      ┌────────┴────────┐
                    │                      │ SÍ              │ NO
                    │                      │ ⏰ Fechas válidas│ ⚠️ Sin fechas
                    │                      └────────┬────────┘
                    │                               │
                    └───────────────┬───────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │  Crear objeto vuelo:          │
                    │  {                            │
                    │    id                         │
                    │    origin: {lat, lng}         │
                    │    destination: {lat, lng}    │
                    │    fechaInicial ⏰            │
                    │    fechaFinal   ⏰            │
                    │    rotation                   │
                    │    ...                        │
                    │  }                            │
                    └───────────────┬───────────────┘
                                    │
                                    ▼
                            setFlights([...])
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. RENDERIZADO → DynamicMarkers Component                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
        ┌──────────────────────┐        ┌──────────────────────┐
        │ Update Loop (60 FPS)  │        │ useEffect(() => {    │
        │ requestAnimationFrame │        │   render markers     │
        └─────────┬──────────────┘        └─────────┬────────────┘
                  │                                 │
                  │  tiempoSimuladoRef.current      │
                  │  = Date.now()                   │
                  │                                 │
                  └────────────┬────────────────────┘
                               │
                               ▼
                    flights.forEach(flight => {
                               │
                               ▼
              ┌────────────────────────────────┐
              │ ¿Tiene fechaInicial y          │
              │    fechaFinal?                 │
              └─────┬──────────────────┬───────┘
                    │                  │
           ┌────────┴────────┐    ┌───┴────────┐
           │ SÍ              │    │ NO         │
           │ ⏰ Interpolación │    │ 📍 Snapshot│
           └────────┬────────┘    └───┬────────┘
                    │                 │
                    ▼                 ▼
    ┌────────────────────────┐  ┌──────────────────────┐
    │calculateInterpolated   │  │ position = {         │
    │Position(               │  │   lat: currentLat,   │
    │  flight,               │  │   lng: currentLng    │
    │  tiempoSimuladoRef     │  │ }                    │
    │)                       │  └──────────┬───────────┘
    │                        │             │
    │ Calcula:               │             │
    │ • ratio = (now - t0)   │             │
    │   / (t1 - t0)          │             │
    │ • lat = lat0 + Δlat    │             │
    │   × ratio              │             │
    │ • lng = lng0 + Δlng    │             │
    │   × ratio              │             │
    │ • progress, status     │             │
    └────────────┬───────────┘             │
                 │                         │
                 └────────┬────────────────┘
                          │
                          ▼
            position = { lat, lng, progress, status }
                          │
                          ▼
              ┌───────────────────────────┐
              │ ¿Marcador ya existe?       │
              └─────┬─────────────────┬───┘
                    │                 │
           ┌────────┴────────┐   ┌───┴──────────┐
           │ SÍ              │   │ NO           │
           │ UPDATE          │   │ CREATE       │
           └────────┬────────┘   └───┬──────────┘
                    │                │
                    ▼                ▼
        ┌──────────────────┐  ┌──────────────────┐
        │ Calcular distancia│  │ L.marker(        │
        │ desde última pos  │  │   [lat, lng],    │
        └────────┬───────────┘  │   { icon }       │
                 │              │ )                │
                 │              │ .bindPopup()     │
         ┌───────┴────────┐    │ .addTo(map)      │
         │ distance > 100m?│    └──────────────────┘
         └───┬─────────┬──┘
             │ SÍ      │ NO
             ▼         ▼
    ┌────────────┐  ┌───────────┐
    │animateMarker│  │setLatLng()│
    │(           │  │(instant)  │
    │  marker,   │  └───────────┘
    │  oldPos,   │
    │  newPos,   │
    │  1000ms    │
    │)           │
    │            │
    │ RAF loop:  │
    │ • Easing   │
    │ • Smooth   │
    └────────────┘
                    │
                    ▼
           ┌─────────────────────┐
           │ Actualizar ícono    │
           │ • Color dinámico    │
           │ • Rotación          │
           └──────────┬──────────┘
                      │
                      ▼
           ┌─────────────────────┐
           │ Actualizar popup    │
           │ • Info actualizada  │
           │ • Progreso %        │
           │ • Estado            │
           └─────────────────────┘
                    })
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. RESULTADO VISUAL                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
┌───────────┐  ┌───────────┐  ┌───────────┐
│ ✈️ Avión 1 │  │ ✈️ Avión 2 │  │ ✈️ Avión 3 │
│ 🟢 Verde   │  │ 🔵 Azul    │  │ 🔴 Rojo    │
│ Progreso:  │  │ Progreso:  │  │ Progreso:  │
│ 85%        │  │ 45%        │  │ 10%        │
│ Llegando   │  │ En ruta    │  │ Retrasado  │
│            │  │            │  │            │
│ Movimiento │  │ Movimiento │  │ Movimiento │
│ continuo   │  │ continuo   │  │ continuo   │
│ píxel a    │  │ píxel a    │  │ píxel a    │
│ píxel      │  │ píxel      │  │ píxel      │
└───────────┘  └───────────┘  └───────────┘
       │              │              │
       └──────────────┼──────────────┘
                      │
                      ▼
            🎯 EXPERIENCIA FLUIDA
            ✅ Movimiento continuo
            ✅ Colores dinámicos
            ✅ Sin saltos visuales
            ✅ Sincronización precisa


╔═══════════════════════════════════════════════════════════════════════════════╗
║                            CARACTERÍSTICAS CLAVE                               ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                                ║
║  🎬 INTERPOLACIÓN TEMPORAL (CONTINUA)                                         ║
║     • Update loop: 60 FPS (requestAnimationFrame)                             ║
║     • Cálculo: position = origin + (destination - origin) × ratio             ║
║     • Independiente de frecuencia de backend                                  ║
║     • Movimiento predecible y matemático                                      ║
║                                                                                ║
║  🎨 SUAVIZADO CON RAF (TRANSICIONES)                                          ║
║     • Activación: Solo si distance > 100m                                     ║
║     • Duración: 1000ms                                                        ║
║     • Easing: Cubic ease-out                                                  ║
║     • Previene: Saltos visuales durante correcciones                          ║
║                                                                                ║
║  🔄 FALLBACK ROBUSTO                                                          ║
║     • Con timestamps → Interpolación temporal                                 ║
║     • Sin timestamps → Posición directa del backend                           ║
║     • Sistema siempre funcional                                               ║
║                                                                                ║
║  🎯 COLORES DINÁMICOS                                                         ║
║     • 🟢 Verde: Completado / Llegando (progress > 75%)                        ║
║     • 🔴 Rojo: Retrasado                                                      ║
║     • 🔵 Azul: En progreso (0-75%)                                            ║
║                                                                                ║
╚═══════════════════════════════════════════════════════════════════════════════╝


╔═══════════════════════════════════════════════════════════════════════════════╗
║                          COMPARACIÓN DE SISTEMAS                               ║
╠════════════════════╦══════════════════════╦═══════════════════════════════════╣
║ Aspecto            ║ Sistema Anterior     ║ Sistema Híbrido (NUEVO)           ║
╠════════════════════╬══════════════════════╬═══════════════════════════════════╣
║ Movimiento         ║ Saltos cada snapshot ║ Continuo píxel a píxel            ║
║ Frecuencia update  ║ Depende de backend   ║ 60 FPS independiente              ║
║ Posición           ║ Backend directo      ║ Calculada localmente              ║
║ Transiciones       ║ Instantáneas         ║ Suavizadas con easing             ║
║ Colores            ║ Estáticos            ║ Dinámicos por progreso            ║
║ Sincronización     ║ Aproximada           ║ Matemática exacta                 ║
║ Robustez           ║ Depende de datos     ║ Fallback automático               ║
║ Carga de red       ║ Alta (snapshots)     ║ Baja (solo timestamps)            ║
║ Experiencia visual ║ ⭐⭐⭐             ║ ⭐⭐⭐⭐⭐                         ║
╚════════════════════╩══════════════════════╩═══════════════════════════════════╝
```

**Estado:** ✅ **IMPLEMENTADO Y LISTO PARA TESTING**

**Documentación completa:**
- `SISTEMA_HIBRIDO_ANIMACION.md` - Documentación técnica detallada
- `SISTEMA_HIBRIDO_RESUMEN.md` - Resumen ejecutivo
- Este archivo - Diagrama de flujo visual
