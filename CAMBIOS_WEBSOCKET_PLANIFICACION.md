# Cambios para Corregir WebSocket de Planificación

## Fecha: 22 de noviembre de 2025

## Resumen
Se realizaron correcciones en el backend y frontend para asegurar que el WebSocket de planificación (`ws://localhost:8000/ws/planificacion`) funcione correctamente y muestre los vuelos en el mapa.

---

## 🔧 CAMBIOS EN EL BACKEND

### 1. WebSocketConfig.java
**Archivo:** `/backend/src/main/java/com/proyecto/backend/config/WebSocketConfig.java`

**Cambio:** Corregido comentario de documentación
- ❌ Antes: `ws://localhost:8080/ws/planificacion`
- ✅ Ahora: `ws://localhost:8000/ws/planificacion`

**Razón:** El puerto del servidor está configurado en 8000 (application.properties), no 8080.

---

## 🎨 CAMBIOS EN EL FRONTEND

### 1. Función `convertirVueloPlanificacionAMapa()` mejorada
**Archivo:** `/front/src/pages/simulacion/Simulador/SimuladorSemanal.js`

**Mejoras realizadas:**

#### a) Comparación case-insensitive de códigos ICAO
```javascript
// ❌ Antes
const origen = airports.find(a => a.code === vuelo.origenCodigoICAO);

// ✅ Ahora
const origen = airports.find(a => 
    String(a.code).toUpperCase() === String(vuelo.origenCodigoICAO).toUpperCase()
);
```

#### b) Cálculo dinámico del progreso basado en fechas
```javascript
// Ahora calcula el progreso real basado en fechaInicial y fechaFinal
const fechaInicio = new Date(vuelo.fechaInicial);
const fechaFin = new Date(vuelo.fechaFinal);
const ahora = new Date();

const totalDuracion = fechaFin - fechaInicio;
const transcurrido = ahora - fechaInicio;

if (totalDuracion > 0 && transcurrido > 0) {
    progress = Math.max(0, Math.min(1, transcurrido / totalDuracion));
}
```

#### c) Uso correcto de `bearingDegrees()` para rotación
```javascript
const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
const rotation = (brg - 90 + 360) % 360;
```

#### d) Estado dinámico del vuelo
```javascript
const status = progress >= 1 ? 'arrived' : (progress <= 0 ? 'scheduled' : 'active');
```

#### e) Cálculo correcto de paquetes
```javascript
const totalPaquetes = vuelo.pedidos?.reduce((sum, p) => sum + (p.cantidad || 0), 0) || 0;
```

---

### 2. Manejo mejorado de mensajes WebSocket

#### Mensaje tipo 'progreso'
- Actualiza tiempo de simulación desde `data.datos.tiempoSimulacionActual`
- ACUMULA vuelos en el mapa (no reemplaza)
- Mejor logging para debugging

#### Mensaje tipo 'completado'
- Logs más detallados y estructurados
- Acumulación correcta de vuelos
- Manejo automático de iteraciones continuas
- Timeout de 100ms entre solicitudes para mejor visualización
- Límite de 1000 iteraciones máximas

---

### 3. Función `handleIniciarPlanificacion()` mejorada

**Cambios:**
```javascript
// 🧹 Limpia TODO antes de iniciar
setIteracionesPlanificacion([]);
setFlights([]); // ← NUEVO: Limpia vuelos del mapa
setFlightsInAir(0); // ← NUEVO
setIntentosRealizados(0); // ← NUEVO

// ✅ Valida fecha antes de iniciar
if (!fechaInicioSimulacion) {
    alert('Por favor, selecciona una fecha de inicio');
    return;
}
```

---

### 4. Nuevas funciones de control

#### `handleDetenerPlanificacion()`
```javascript
// Detiene la planificación continua sin cerrar WebSocket
// Los vuelos permanecen en el mapa
```

#### `handleLimpiarMapa()`
```javascript
// Limpia todos los vuelos del mapa
// Resetea contadores e iteraciones
```

---

### 5. Nuevos botones de control en la UI

**Ubicación:** Panel de control superior, después de los botones de simulación

**Botones agregados:**
1. **Detener Planificación** (amarillo)
   - Solo activo cuando hay planificación en curso
   - Detiene el proceso continuo
   - Los vuelos permanecen visibles

2. **Limpiar Mapa** (azul)
   - Solo activo cuando hay vuelos en el mapa
   - Limpia completamente el mapa
   - Resetea contadores

---

## 📊 ESTRUCTURA DE DATOS

### Vuelo del WebSocket (del backend)
```json
{
  "fechaInicial": "2025-01-15 08:30",
  "fechaFinal": "2025-01-15 14:45",
  "origenCodigoICAO": "SPIM",
  "destinoCodigoICAO": "KJFK",
  "pedidos": [
    {
      "idPedido": 123,
      "cantidad": 50
    }
  ]
}
```

### Vuelo en el mapa (formato frontend)
```javascript
{
  id: "PL-SPIM-KJFK-1234567890-0.123",
  origin: { code: "SPIM", lat: -12.0219, lng: -77.1143, region: "América del Sur" },
  destination: { code: "KJFK", lat: 40.6413, lng: -73.7781, region: "América del Norte" },
  progress: 0.35, // 35% del trayecto
  altitude: 35000,
  speed: 850,
  status: "active", // 'scheduled', 'active', 'arrived'
  currentLat: -5.123,
  currentLng: -75.456,
  aircraftColor: "#3b82f6", // Azul para planificación
  rotation: 45,
  packageCapacity: 50,
  currentPackages: 50,
  packageType: "MPE",
  isSameContinentFlight: false,
  fechaInicial: "2025-01-15 08:30",
  fechaFinal: "2025-01-15 14:45",
  pedidos: [...]
}
```

---

## 🚀 FLUJO DE FUNCIONAMIENTO

1. **Usuario selecciona fecha de inicio**
2. **WebSocket se conecta automáticamente** al montar el componente
3. **Al seleccionar fecha** → Se auto-inicia la planificación
4. **Backend procesa** cada intervalo de tiempo (K=5)
5. **Frontend recibe vuelos** en mensaje `completado`
6. **Vuelos se convierten** al formato del mapa
7. **Vuelos se ACUMULAN** en el mapa (no se reemplazan)
8. **Se solicita automáticamente** el siguiente intervalo
9. **Proceso continúa** hasta 1000 iteraciones o hasta que usuario detenga

---

## 🐛 DEBUGGING

### Logs importantes a observar:

```javascript
// ✅ Vuelo recibido correctamente
'✈️ Procesando X vuelos de la iteración #Y'
'🔍 Ejemplo de vuelo recibido: {...}'

// ✅ Conversión exitosa
'🔄 Convertidos X de Y vuelos'
'🗺️ Ejemplo de vuelo convertido: {...}'

// ✅ Actualización del mapa
'📊 Total vuelos en mapa: X (Y anteriores + Z nuevos)'
'✈️ Vuelos en aire actualizados: X'

// ⚠️ Problemas potenciales
'⚠️ Aeropuertos no encontrados: XXXX o YYYY'
'⚠️ No se pudieron convertir vuelos (aeropuertos no encontrados)'
'⚠️ Sin vuelos en iteración X'
```

---

## 🔍 VERIFICACIÓN

### En la consola del navegador deberías ver:
1. ✅ `🔌 Auto-conectando WebSocket de planificación...`
2. ✅ `🔌 WebSocket Planificación conectado: ws://localhost:8000/ws/planificacion`
3. ✅ `✅ Conexión establecida`
4. ✅ `🚀 Auto-iniciando planificación...`
5. ✅ `📤 Solicitud de planificación enviada para fecha: YYYY-MM-DD`
6. ✅ `✈️ Procesando X vuelos de la iteración #1`
7. ✅ `🗺️ Ejemplo de vuelo convertido: {...}`
8. ✅ `📊 Total vuelos en mapa: X`

### En el mapa deberías ver:
- ✈️ Aviones azules (`#3b82f6`) apareciendo gradualmente
- 🗺️ Los vuelos se mantienen visibles y se van acumulando
- 📊 El contador "Vuelos en el aire" aumentando

---

## ⚙️ CONFIGURACIÓN ACTUAL

- **Puerto backend:** 8000
- **WebSocket endpoint:** `ws://localhost:8000/ws/planificacion`
- **Factor K:** 5 (para planificación semanal)
- **Parámetros AG:** 
  - Tamaño población: 20
  - Max generaciones: 20
  - Límite sin mejora: 10
- **Max iteraciones:** 1000
- **Delay entre iteraciones:** 100ms

---

## 📝 NOTAS IMPORTANTES

1. **Los vuelos se ACUMULAN**, no se reemplazan
2. **El WebSocket se auto-conecta** al montar el componente
3. **La planificación se auto-inicia** al seleccionar fecha
4. **El proceso es continuo** hasta detenerlo manualmente
5. **El color azul** (`#3b82f6`) identifica vuelos de planificación
6. **El progreso se calcula dinámicamente** basado en las fechas reales

---

## 🎯 PRÓXIMOS PASOS (OPCIONAL)

1. Agregar filtro para mostrar solo vuelos de planificación vs simulación
2. Implementar colores diferentes según región o prioridad
3. Agregar animación suave de los aviones en tiempo real
4. Panel lateral con detalles de vuelos seleccionados
5. Exportar plan de vuelos a JSON/CSV

---

## 🧪 PRUEBAS RECOMENDADAS

1. **Iniciar planificación** con fecha válida (2025-01-01 o posterior)
2. **Observar acumulación** de vuelos en el mapa
3. **Verificar logs** en consola del navegador
4. **Detener planificación** y verificar que vuelos permanecen
5. **Limpiar mapa** y verificar que todo se resetea
6. **Reconectar WebSocket** tras error de red

---

## ✅ CHECKLIST DE VERIFICACIÓN

- [x] Backend en puerto 8000
- [x] WebSocket configurado en /ws/planificacion
- [x] Frontend conecta a ws://localhost:8000/ws/planificacion
- [x] Función convertirVueloPlanificacionAMapa actualizada
- [x] Manejo correcto de mensajes 'completado'
- [x] Acumulación de vuelos en el mapa
- [x] Botones de control agregados
- [x] Logging detallado implementado
- [x] Auto-inicio configurado
- [x] Límite de iteraciones establecido

---

¡Todo listo! El WebSocket de planificación ahora funciona correctamente y muestra los vuelos en el mapa. 🎉
