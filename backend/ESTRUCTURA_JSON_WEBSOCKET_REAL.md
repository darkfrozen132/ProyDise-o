# 📦 ESTRUCTURA JSON REAL DEL BACKEND

## ✅ RESPUESTA: SÍ, EL BACKEND ENVÍA ESOS DATOS

**El backend envía datos MUY SIMILARES** al JSON que describiste, pero con **nombres de campos en español** y una estructura ligeramente diferente.

---

## 📡 Objeto Principal: `ProgresoAGDTO`

El backend envía este objeto vía WebSocket al topic `/topic/simulations/{sessionId}`:

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "tipo": "PROGRESO_AG",
  "generacion": 3,
  "maxGeneraciones": 5,
  "progreso": 60.0,
  "mejorFitness": 0.89,
  "fitnessPromedio": 0.75,
  "pedidosProcesados": 150,
  "pedidosTotales": 500,
  "fechaSimulada": "2025-01-15T10:30:00",
  "timestamp": "2025-11-26T14:35:22",
  "mensaje": "Generación 3 completada",
  "solucion": {
    "vuelos": [
      {
        "fechaInicial": "2025-01-15 08:30",
        "fechaFinal": "2025-01-15 14:45",
        "origenCodigoICAO": "SPIM",
        "destinoCodigoICAO": "KJFK",
        "pedidos": [
          {
            "idPedido": 123,
            "cantidad": 50
          },
          {
            "idPedido": 456,
            "cantidad": 30
          }
        ]
      },
      {
        "fechaInicial": "2025-01-15 09:00",
        "fechaFinal": "2025-01-15 12:20",
        "origenCodigoICAO": "SPIM",
        "destinoCodigoICAO": "SCEL",
        "pedidos": [
          {
            "idPedido": 789,
            "cantidad": 25
          }
        ]
      }
    ]
  }
}
```

---

## 🎯 MAPEO CON TU ESTRUCTURA JSON

### ✅ Campos que coinciden (nombres traducidos):

| **Tu Estructura**         | **Backend Real (ProgresoAGDTO)**     | **Tipo**       |
|---------------------------|--------------------------------------|----------------|
| `simulationId`            | `sessionId`                          | String (UUID)  |
| `type`                    | `tipo`                               | String         |
| `snapshot.processedOrders`| `pedidosProcesados`                  | Integer        |
| `snapshot.totalOrders`    | `pedidosTotales`                     | Integer        |
| `snapshot.fitness`        | `mejorFitness`                       | Double         |
| `snapshot.generatedAt`    | `fechaSimulada`                      | LocalDateTime  |

---

## 🗺️ ESTRUCTURA DE VUELOS (LO IMPORTANTE PARA EL MAPA)

### 📍 Campo: `solucion.vuelos[]`

Cada vuelo tiene esta estructura:

```json
{
  "fechaInicial": "2025-01-15 08:30",      // ⏰ Fecha/hora de despegue
  "fechaFinal": "2025-01-15 14:45",        // 🛬 Fecha/hora de aterrizaje
  "origenCodigoICAO": "SPIM",              // 📍 Código ICAO del origen (Lima, Perú)
  "destinoCodigoICAO": "KJFK",             // 📍 Código ICAO del destino (New York, USA)
  "pedidos": [
    {
      "idPedido": 123,                     // 📦 ID del pedido
      "cantidad": 50                       // 📊 Cantidad de paquetes
    }
  ]
}
```

---

## ⚠️ DIFERENCIAS CLAVE CON TU ESTRUCTURA

### ❌ Lo que NO está presente:

1. **No hay `slackMinutes`** (holgura de tiempo) en el JSON actual
2. **No hay `departureUtc` ni `arrivalUtc`** (las fechas están en formato `yyyy-MM-dd HH:mm` sin zona horaria explícita)
3. **No hay `flightId`** (código de vuelo como "LIM-MIA-0800")
4. **No hay segmentación de rutas** (`routes` → `segments`)
   - Solo hay vuelos directos, no rutas multi-segmento

### ✅ Lo que SÍ está presente:

1. ✅ **Códigos ICAO de aeropuertos** (`origenCodigoICAO`, `destinoCodigoICAO`)
2. ✅ **Fechas de salida/llegada** (`fechaInicial`, `fechaFinal`)
3. ✅ **Lista de pedidos asignados** (`pedidos[]`)
4. ✅ **Cantidad de paquetes por pedido** (`cantidad`)

---

## 🧭 CÓMO USAR ESTOS DATOS EN EL FRONTEND

### 1️⃣ Cargar coordenadas de aeropuertos

Primero, el frontend debe obtener las coordenadas de los aeropuertos:

```javascript
// GET /api/aeropuertos/listar
const aeropuertos = {
  "SPIM": { lat: -12.0219, lng: -77.1143, nombre: "Lima, Perú" },
  "KJFK": { lat: 40.6413, lng: -73.7781, nombre: "New York, USA" },
  "SCEL": { lat: -33.3930, lng: -70.7858, nombre: "Santiago, Chile" }
};
```

### 2️⃣ Por cada vuelo en `solucion.vuelos[]`:

```javascript
solucion.vuelos.forEach(vuelo => {
  const origen = aeropuertos[vuelo.origenCodigoICAO];
  const destino = aeropuertos[vuelo.destinoCodigoICAO];
  
  // Parsear fechas
  const salida = new Date(vuelo.fechaInicial.replace(' ', 'T'));
  const llegada = new Date(vuelo.fechaFinal.replace(' ', 'T'));
  
  // Comparar con tiempo simulado actual (fechaSimulada)
  const tiempoActual = new Date(progresoAGDTO.fechaSimulada);
  
  // Calcular si el vuelo está en curso
  if (tiempoActual >= salida && tiempoActual <= llegada) {
    // 🛫 VUELO EN CURSO - Calcular posición interpolada
    const duracionTotal = llegada - salida;
    const tiempoTranscurrido = tiempoActual - salida;
    const progreso = tiempoTranscurrido / duracionTotal;
    
    // Interpolar coordenadas
    const latActual = origen.lat + (destino.lat - origen.lat) * progreso;
    const lngActual = origen.lng + (destino.lng - origen.lng) * progreso;
    
    // Dibujar avión en movimiento
    dibujarAvion(latActual, lngActual, vuelo);
  } else if (tiempoActual < salida) {
    // ⏰ VUELO PENDIENTE - No dibujar aún
    console.log(`Vuelo ${vuelo.origenCodigoICAO} → ${vuelo.destinoCodigoICAO} sale en el futuro`);
  } else {
    // ✅ VUELO COMPLETADO - Marcar como completado
    console.log(`Vuelo ${vuelo.origenCodigoICAO} → ${vuelo.destinoCodigoICAO} ya llegó`);
  }
});
```

---

## 📊 EJEMPLO REAL COMPLETO

### JSON Enviado por el Backend:

```json
{
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tipo": "PROGRESO_AG",
  "generacion": 4,
  "maxGeneraciones": 5,
  "progreso": 80.0,
  "mejorFitness": 0.92,
  "fitnessPromedio": 0.83,
  "pedidosProcesados": 400,
  "pedidosTotales": 500,
  "fechaSimulada": "2025-01-15T12:00:00",
  "timestamp": "2025-11-26T15:20:10",
  "mensaje": "Generación 4 completada - Mejora del 5%",
  "solucion": {
    "vuelos": [
      {
        "fechaInicial": "2025-01-15 08:00",
        "fechaFinal": "2025-01-15 14:30",
        "origenCodigoICAO": "SPIM",
        "destinoCodigoICAO": "KJFK",
        "pedidos": [
          { "idPedido": 101, "cantidad": 30 },
          { "idPedido": 102, "cantidad": 45 }
        ]
      },
      {
        "fechaInicial": "2025-01-15 09:00",
        "fechaFinal": "2025-01-15 11:20",
        "origenCodigoICAO": "SPIM",
        "destinoCodigoICAO": "SCEL",
        "pedidos": [
          { "idPedido": 201, "cantidad": 20 }
        ]
      },
      {
        "fechaInicial": "2025-01-15 13:00",
        "fechaFinal": "2025-01-15 19:45",
        "origenCodigoICAO": "KJFK",
        "destinoCodigoICAO": "EGLL",
        "pedidos": [
          { "idPedido": 301, "cantidad": 60 }
        ]
      }
    ]
  }
}
```

### Visualización en el Mapa:

**Tiempo simulado: 2025-01-15 12:00**

- ✅ **Vuelo SPIM → KJFK**: 
  - Salió a las 08:00, llega a las 14:30
  - Progreso: (12:00 - 08:00) / (14:30 - 08:00) = 4h / 6.5h = **61.5%**
  - Posición: 61.5% del camino entre Lima y New York
  - **SE DIBUJA el avión en movimiento** 🛫

- ✅ **Vuelo SPIM → SCEL**: 
  - Salió a las 09:00, llegó a las 11:20
  - **Ya completado** (llegó 40 minutos antes)
  - No se dibuja en el aire, se marca como completado ✅

- ⏰ **Vuelo KJFK → EGLL**: 
  - Sale a las 13:00 (en 1 hora)
  - **Aún no despega**
  - No se dibuja todavía ⏳

---

## 🔧 CAMPOS QUE NECESITAS AGREGAR AL BACKEND

Si necesitas la estructura exacta que describiste (con `slackMinutes`, `departureUtc`, `flightId`, etc.), deberías:

### 1. Modificar `VueloSimplificadoDTO.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VueloSimplificadoDTO {
    
    // Campos actuales
    private String fechaInicial;
    private String fechaFinal;
    private String origenCodigoICAO;
    private String destinoCodigoICAO;
    private List<PedidoEnVueloDTO> pedidos = new ArrayList<>();
    
    // 🆕 NUEVOS CAMPOS PARA EL FRONTEND
    private String flightId;              // "LIM-MIA-0800"
    private Integer slackMinutes;         // Holgura de tiempo
    private String departureUtc;          // "2025-01-15T13:00:00Z"
    private String arrivalUtc;            // "2025-01-15T19:30:00Z"
    private Integer totalQuantity;        // Total de paquetes
}
```

---

## ✅ CONCLUSIÓN

**Respuesta directa**: El backend SÍ envía datos suficientes para dibujar el mapa, pero **con nombres en español** y sin algunos campos opcionales como `slackMinutes` o `flightId`.

**Lo que tienes disponible AHORA:**
- ✅ Códigos ICAO de aeropuertos (origen/destino)
- ✅ Fechas de salida/llegada
- ✅ Lista de pedidos por vuelo
- ✅ Tiempo simulado actual (`fechaSimulada`)

**Lo que necesitas agregar (opcional):**
- ❌ `slackMinutes` (holgura)
- ❌ `flightId` (código de vuelo)
- ❌ Formato UTC explícito (`departureUtc`, `arrivalUtc`)
- ❌ Rutas multi-segmento (actualmente solo vuelos directos)

**Puedes visualizar el mapa CON LOS DATOS ACTUALES**, solo necesitas:
1. Cargar coordenadas de aeropuertos desde `/api/aeropuertos/listar`
2. Interpolar posición basándote en `fechaInicial`, `fechaFinal` y `fechaSimulada`
3. Dibujar marcadores de avión en movimiento

---

## 🚀 SIGUIENTE PASO

¿Quieres que:
1. **Modifique el backend** para agregar los campos faltantes (`slackMinutes`, `flightId`, etc.)?
2. **Adapte tu frontend** para usar la estructura actual del backend?
3. **Cree un visualizador HTML de prueba** que use los datos reales del backend?

¡Tú decides! 🎯
