# 🔍 DIAGNÓSTICO: PROBLEMA DE VISUALIZACIÓN EN EL FRONTEND

## ❌ PROBLEMA IDENTIFICADO

El visualizador del frontend (`visualizador-ag.html`) **NO está mostrando correctamente las rutas** porque el **JSON que envía el backend NO coincide exactamente con lo que espera el frontend**.

---

## 📊 ANÁLISIS DEL FLUJO DE DATOS

### 1. ✅ BACKEND - Estructura JSON que ENVÍA

El backend envía a través de WebSocket un objeto `ProgresoAGDTO` con esta estructura:

```json
{
  "tipo": "PROGRESO_AG",
  "generacion": 5,
  "maxGeneraciones": 10,
  "progreso": 50.0,
  "mejorFitness": 1234.56,
  "fitnessPromedio": 1100.0,
  "pedidosProcesados": 10,
  "pedidosTotales": 13,
  "timestamp": "2025-11-19T15:30:00",
  "solucion": {
    "vuelos": [
      {
        "fechaInicial": "2025-01-02 08:30",
        "fechaFinal": "2025-01-02 14:45",
        "origenCodigoICAO": "SPIM",
        "destinoCodigoICAO": "KJFK",
        "flightId": "SPIM-KJFK-0830",
        "departureUtc": "2025-01-02T08:30:00Z",
        "arrivalUtc": "2025-01-02T14:45:00Z",
        "quantity": 150,
        "slackMinutes": 120,
        "pedidos": [
          {"idPedido": 123, "cantidad": 50},
          {"idPedido": 456, "cantidad": 100}
        ]
      }
    ]
  }
}
```

**📝 Campos clave del backend:**
- `solucion.vuelos[]` - Array de vuelos
- Cada vuelo tiene: `origenCodigoICAO`, `destinoCodigoICAO`, `flightId`, `departureUtc`, `arrivalUtc`, `quantity`, `slackMinutes`

---

### 2. ❌ FRONTEND - Estructura JSON que ESPERA

El visualizador HTML (línea 372-405) intenta acceder a campos que **NO EXISTEN** en el JSON del backend:

```javascript
// ❌ LÍNEA 372: Intenta acceder a 'rutas' pero el backend envía 'vuelos'
if (progreso.solucion && progreso.solucion.rutas) {
    mostrarRutas(progreso.solucion.rutas);
}

// ❌ LÍNEA 420-443: Espera campos que NO están en el JSON del backend
ruta.origen        // ❌ No existe, backend usa: origenCodigoICAO
ruta.destino       // ❌ No existe, backend usa: destinoCodigoICAO
ruta.vueloId       // ❌ No existe, backend usa: flightId
ruta.pedidoId      // ❌ No existe en el nivel de vuelo
ruta.salida        // ❌ No existe, backend usa: departureUtc
ruta.llegada       // ❌ No existe, backend usa: arrivalUtc
ruta.slackMinutes  // ✅ Existe correctamente
```

---

## 🔧 SOLUCIONES PROPUESTAS

### ✅ SOLUCIÓN 1: MODIFICAR EL FRONTEND (RECOMENDADO)

Adaptar el JavaScript del visualizador para usar los campos correctos del backend:

#### Cambios necesarios en `visualizador-ag.html`:

**1. Cambiar línea 372:**
```javascript
// ❌ ANTES:
if (progreso.solucion && progreso.solucion.rutas) {
    mostrarRutas(progreso.solucion.rutas);
}

// ✅ DESPUÉS:
if (progreso.solucion && progreso.solucion.vuelos) {
    mostrarRutas(progreso.solucion.vuelos);
}
```

**2. Actualizar función `mostrarRutas()` (línea 378-466):**
```javascript
function mostrarRutas(vuelos) {  // ✅ Cambiar nombre del parámetro
    document.getElementById('rutasCount').textContent = vuelos.length;
    
    const lista = document.getElementById('rutasList');
    
    if (vuelos.length === 0) {
        lista.innerHTML = '<div class="no-rutas">No hay rutas asignadas aún...</div>';
        return;
    }
    
    // ✅ Mostrar en la interfaz con los CAMPOS CORRECTOS
    lista.innerHTML = vuelos.map((vuelo, idx) => {
        // ✅ Usar los campos correctos del backend
        let slackColor = '#999';
        let slackEmoji = '⏱️';
        if (vuelo.slackMinutes !== undefined && vuelo.slackMinutes !== null) {
            if (vuelo.slackMinutes > 30) {
                slackColor = '#4CAF50';  // Verde
                slackEmoji = '✅';
            } else if (vuelo.slackMinutes > 0) {
                slackColor = '#FFC107';  // Amarillo
                slackEmoji = '⚠️';
            } else {
                slackColor = '#F44336';  // Rojo
                slackEmoji = '❌';
            }
        }
        
        return `
        <div class="ruta-item" style="border-left-color: ${slackColor}">
            <strong>#${idx + 1}</strong> 
            ${vuelo.flightId ? `<strong>${vuelo.flightId}</strong>` : `Vuelo ${idx + 1}`}
            <br>
            <strong>${vuelo.origenCodigoICAO}</strong> → 
            <strong>${vuelo.destinoCodigoICAO}</strong>
            <br>
            <small>
                🛫 ${vuelo.departureUtc || vuelo.fechaInicial || 'N/A'}<br>
                🛬 ${vuelo.arrivalUtc || vuelo.fechaFinal || 'N/A'}<br>
                📦 Paquetes: <strong>${vuelo.quantity || vuelo.getTotalPaquetes?.() || 'N/A'}</strong><br>
                ${slackEmoji} Holgura: <strong style="color: ${slackColor}">
                    ${vuelo.slackMinutes !== undefined && vuelo.slackMinutes !== null 
                        ? (vuelo.slackMinutes > 0 ? '+' : '') + vuelo.slackMinutes + ' min' 
                        : 'N/A'}
                </strong>
            </small>
        </div>
        `;
    }).join('');
}
```

**3. Actualizar la parte de "rutasConCoordenadas" (línea 392-419):**
```javascript
// ✅ PREPARAR DATOS PARA TU MAPA
const rutasConCoordenadas = vuelos.map(vuelo => {
    const origenData = aeropuertosMap[vuelo.origenCodigoICAO];
    const destinoData = aeropuertosMap[vuelo.destinoCodigoICAO];
    
    return {
        flightId: vuelo.flightId,
        origen: {
            codigo: vuelo.origenCodigoICAO,
            lat: origenData?.lat,
            lon: origenData?.lon,
            ciudad: origenData?.ciudad,
            pais: origenData?.pais
        },
        destino: {
            codigo: vuelo.destinoCodigoICAO,
            lat: destinoData?.lat,
            lon: destinoData?.lon,
            ciudad: destinoData?.ciudad,
            pais: destinoData?.pais
        },
        departureUtc: vuelo.departureUtc,
        arrivalUtc: vuelo.arrivalUtc,
        quantity: vuelo.quantity,
        slackMinutes: vuelo.slackMinutes,
        pedidos: vuelo.pedidos
    };
});
```

---

### ✅ SOLUCIÓN 2: MODIFICAR EL BACKEND (NO RECOMENDADO)

Cambiar el backend para usar `rutas` en lugar de `vuelos` y adaptar los nombres de campos. **NO RECOMENDADO** porque:
- Requiere cambios en múltiples archivos Java
- Rompe la consistencia con el resto de la API
- Los campos actuales del backend son correctos y siguen convenciones REST

---

## 📋 RESUMEN DE INCOMPATIBILIDADES

| Campo esperado (Frontend) | Campo real (Backend) | Estado |
|---------------------------|----------------------|--------|
| `solucion.rutas` | `solucion.vuelos` | ❌ NO COINCIDE |
| `ruta.origen` | `vuelo.origenCodigoICAO` | ❌ NO COINCIDE |
| `ruta.destino` | `vuelo.destinoCodigoICAO` | ❌ NO COINCIDE |
| `ruta.vueloId` | `vuelo.flightId` | ❌ NO COINCIDE |
| `ruta.pedidoId` | N/A (está en `vuelo.pedidos[]`) | ❌ NO EXISTE |
| `ruta.salida` | `vuelo.departureUtc` | ❌ NO COINCIDE |
| `ruta.llegada` | `vuelo.arrivalUtc` | ❌ NO COINCIDE |
| `ruta.slackMinutes` | `vuelo.slackMinutes` | ✅ COINCIDE |
| `ruta.quantity` | `vuelo.quantity` | ✅ COINCIDE |

---

## 🎯 ACCIÓN REQUERIDA

**MODIFICAR EL ARCHIVO:** `/home/leoncio/Documentos/GitHub/ProyDise-o/backend/visualizador-ag.html`

**LÍNEAS A CAMBIAR:**
1. Línea 372: Cambiar `progreso.solucion.rutas` → `progreso.solucion.vuelos`
2. Línea 378-466: Actualizar función `mostrarRutas()` para usar los campos correctos
3. Línea 392-419: Actualizar mapeo de coordenadas

---

## 🧪 CÓMO VERIFICAR LA CORRECCIÓN

1. **Abrir el visualizador en el navegador:**
   ```
   http://localhost:8000/visualizador-ag.html
   ```

2. **Hacer clic en:**
   - ✅ "1. Conectar WebSocket"
   - ✅ "2. Iniciar Simulación"

3. **Abrir la consola del navegador (F12)** y verificar:
   - ✅ Se reciben mensajes WebSocket con `tipo: "PROGRESO_AG"`
   - ✅ Aparecen logs: `📥 Mensaje recibido:`
   - ✅ Se imprimen rutas con coordenadas: `🗺️ DATOS PARA TU MAPA`

4. **Verificar en la interfaz:**
   - ✅ La sección "✈️ Rutas Asignadas" muestra vuelos
   - ✅ Cada vuelo muestra: Origen → Destino, fechas, paquetes, holgura

---

## 🔍 DEBUGGING ADICIONAL

Si aún no funciona después de los cambios:

1. **Verificar en consola del navegador:**
```javascript
// Inspeccionar el último mensaje recibido
console.log(ultimoMensaje);

// Verificar estructura de la solución
console.log(ultimoMensaje.solucion);

// Ver si tiene vuelos
console.log(ultimoMensaje.solucion.vuelos);
```

2. **Verificar WebSocket en Network tab:**
   - F12 → Network → WS → Seleccionar conexión
   - Ver mensajes en "Messages"
   - Verificar que llegan mensajes con `tipo: "PROGRESO_AG"`

3. **Logs del backend:**
```bash
tail -f backend/info/backend.log | grep "Progreso AG enviado"
```

---

## 📚 ARCHIVOS RELEVANTES

**Backend:**
- `/backend/src/main/java/com/proyecto/backend/simulation/dto/ProgresoAGDTO.java`
- `/backend/src/main/java/com/proyecto/backend/planificador/semanal/dto/response/PlanificacionResponseSimple.java`
- `/backend/src/main/java/com/proyecto/backend/planificador/semanal/dto/response/VueloSimplificadoDTO.java`
- `/backend/src/main/java/com/proyecto/backend/simulation/service/SimulationService.java`

**Frontend:**
- `/backend/visualizador-ag.html` (⚠️ ARCHIVO A MODIFICAR)

---

## 🎨 BONUS: Campos disponibles en VueloSimplificadoDTO

```typescript
interface VueloSimplificadoDTO {
  // Campos antiguos (compatibilidad)
  fechaInicial: string;        // "2025-01-02 08:30"
  fechaFinal: string;          // "2025-01-02 14:45"
  origenCodigoICAO: string;    // "SPIM"
  destinoCodigoICAO: string;   // "KJFK"
  pedidos: Array<{             // Array de pedidos asignados
    idPedido: number;
    cantidad: number;
  }>;
  
  // Campos nuevos para frontend
  flightId: string;            // "SPIM-KJFK-0830"
  departureUtc: string;        // "2025-01-02T08:30:00Z"
  arrivalUtc: string;          // "2025-01-02T14:45:00Z"
  quantity: number;            // 150 (total de paquetes)
  slackMinutes: number;        // 120 (minutos de holgura)
}
```
