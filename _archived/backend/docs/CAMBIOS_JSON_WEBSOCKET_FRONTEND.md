# 🔧 CAMBIOS EN EL BACKEND PARA VISUALIZACIÓN EN MAPA

## ✅ MODIFICACIONES REALIZADAS

El backend ahora envía **datos completos en formato UTC** necesarios para visualizar vuelos en tiempo real en tu mapa frontend.

---

## 📦 1. DTO Actualizado: `VueloSimplificadoDTO.java`

### 🆕 Nuevos Campos Agregados:

```java
@JsonProperty("flightId")
private String flightId;              // Ej: "LIM-MIA-0800"

@JsonProperty("departureUtc")
private String departureUtc;          // Ej: "2025-01-15T13:00:00Z"

@JsonProperty("arrivalUtc")
private String arrivalUtc;            // Ej: "2025-01-15T19:30:00Z"

@JsonProperty("quantity")
private Integer quantity;             // Total de paquetes en el vuelo

@JsonProperty("slackMinutes")
private Integer slackMinutes;         // Holgura de tiempo (+ a tiempo, - retrasado)
```

### 📝 Campos Existentes (sin cambios):

```java
private String fechaInicial;          // "2025-01-15 08:30"
private String fechaFinal;            // "2025-01-15 14:45"
private String origenCodigoICAO;      // "SPIM" (Lima)
private String destinoCodigoICAO;     // "KJFK" (New York)
private List<PedidoEnVueloDTO> pedidos;
```

---

## 🛠️ 2. Servicio Actualizado: `AlgoritmoGeneticoService.java`

### ✅ Método `convertirAResponseSimple()`:

#### **Generación de `flightId`:**
```java
String hora = String.format("%02d%02d", salidaUTC.getHour(), salidaUTC.getMinute());
dto.setFlightId(origen + "-" + destino + "-" + hora);
// Resultado: "SPIM-KJFK-0830"
```

#### **Formato UTC ISO-8601:**
```java
dto.setDepartureUtc(formatearFechaUTC(salidaUTC));
dto.setArrivalUtc(formatearFechaUTC(llegadaUTC));
// Resultado: "2025-01-15T13:00:00Z"
```

#### **Cálculo de `slackMinutes`:**
```java
// Holgura = deadline_pedido - llegada_vuelo
long minutosHolgura = Duration.between(llegadaFinal, deadlinePedido).toMinutes();
dto.setSlackMinutes((int) minutosHolgura);
```

- **Positivo** → Vuelo llega a tiempo (tiene margen)
- **Negativo o 0** → Vuelo retrasado

#### **Suma de `quantity`:**
```java
// Al agregar pedidos, suma las cantidades
Integer cantidadActual = vueloDTO.getQuantity();
vueloDTO.setQuantity(cantidadActual + vueloUso.getCantidadAsignada());
```

---

## 📡 3. Estructura JSON COMPLETA Enviada por WebSocket

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
        ],
        "flightId": "SPIM-KJFK-0800",
        "departureUtc": "2025-01-15T13:00:00Z",
        "arrivalUtc": "2025-01-15T19:30:00Z",
        "quantity": 75,
        "slackMinutes": 45
      },
      {
        "fechaInicial": "2025-01-15 09:00",
        "fechaFinal": "2025-01-15 11:20",
        "origenCodigoICAO": "SPIM",
        "destinoCodigoICAO": "SCEL",
        "pedidos": [
          { "idPedido": 201, "cantidad": 20 }
        ],
        "flightId": "SPIM-SCEL-0900",
        "departureUtc": "2025-01-15T14:00:00Z",
        "arrivalUtc": "2025-01-15T16:20:00Z",
        "quantity": 20,
        "slackMinutes": -15
      }
    ]
  }
}
```

---

## 🗺️ 4. CÓMO USAR ESTOS DATOS EN TU FRONTEND

### Paso 1: Parsear Fechas UTC

```javascript
const vuelo = progreso.solucion.vuelos[0];

const departureTime = new Date(vuelo.departureUtc);  // "2025-01-15T13:00:00Z"
const arrivalTime = new Date(vuelo.arrivalUtc);      // "2025-01-15T19:30:00Z"
const currentTime = new Date(progreso.fechaSimulada); // "2025-01-15T12:00:00"
```

### Paso 2: Calcular Progreso del Vuelo

```javascript
if (currentTime >= departureTime && currentTime <= arrivalTime) {
  // 🛫 VUELO EN CURSO
  const totalDuration = arrivalTime - departureTime;
  const elapsed = currentTime - departureTime;
  const progress = elapsed / totalDuration; // 0.0 a 1.0
  
  console.log(`Vuelo ${vuelo.flightId} está al ${(progress * 100).toFixed(1)}%`);
  
} else if (currentTime < departureTime) {
  // ⏰ VUELO PENDIENTE
  console.log(`Vuelo ${vuelo.flightId} sale en ${Math.floor((departureTime - currentTime) / 60000)} minutos`);
  
} else {
  // ✅ VUELO COMPLETADO
  console.log(`Vuelo ${vuelo.flightId} ya llegó`);
}
```

### Paso 3: Interpolar Posición del Avión

```javascript
// Obtener coordenadas de aeropuertos
const origen = aeropuertos[vuelo.origenCodigoICAO];    // {lat, lng}
const destino = aeropuertos[vuelo.destinoCodigoICAO];  // {lat, lng}

// Calcular posición actual
const latActual = origen.lat + (destino.lat - origen.lat) * progress;
const lngActual = origen.lng + (destino.lng - origen.lng) * progress;

// Dibujar en el mapa
const marker = L.marker([latActual, lngActual], {
  icon: airplaneIcon,
  rotationAngle: calcularAngulo(origen, destino)
});

marker.bindPopup(`
  <b>${vuelo.flightId}</b><br>
  ${vuelo.origenCodigoICAO} → ${vuelo.destinoCodigoICAO}<br>
  Progreso: ${(progress * 100).toFixed(1)}%<br>
  Paquetes: ${vuelo.quantity}<br>
  Holgura: ${vuelo.slackMinutes > 0 ? '+' + vuelo.slackMinutes : vuelo.slackMinutes} min
`);
```

### Paso 4: Colorear según Holgura (slackMinutes)

```javascript
let color;
if (vuelo.slackMinutes > 30) {
  color = '#4CAF50';  // 🟢 Verde: A tiempo con margen
} else if (vuelo.slackMinutes > 0) {
  color = '#FFC107';  // 🟡 Amarillo: A tiempo justo
} else {
  color = '#F44336';  // 🔴 Rojo: Retrasado
}

// Aplicar color al marcador o línea
marker.setStyle({ color: color });
```

---

## 📊 5. COMPARACIÓN: ANTES vs DESPUÉS

| **Campo**          | **ANTES**                    | **DESPUÉS**                              |
|--------------------|------------------------------|------------------------------------------|
| ID del vuelo       | ❌ No existe                  | ✅ `flightId: "SPIM-KJFK-0800"`          |
| Salida UTC         | ❌ Solo local: `"08:00"`      | ✅ `departureUtc: "2025-01-15T13:00:00Z"` |
| Llegada UTC        | ❌ Solo local: `"14:30"`      | ✅ `arrivalUtc: "2025-01-15T19:30:00Z"`   |
| Cantidad total     | ❌ No calculada               | ✅ `quantity: 75` (suma automática)       |
| Holgura            | ❌ No incluida                | ✅ `slackMinutes: 45` (calculada vs deadline) |

---

## 🧪 6. PROBAR LOS CAMBIOS

### 1. Reiniciar el backend:
```bash
cd /home/leoncio/Documentos/GitHub/ProyDise-o/backend
mvn spring-boot:run
```

### 2. Abrir el visualizador HTML:
```bash
# Abre en el navegador:
file:///home/leoncio/Documentos/GitHub/ProyDise-o/backend/visualizador-ag.html
```

### 3. Conectar y ejecutar:
- Click en **"1. Conectar WebSocket"**
- Click en **"2. Iniciar Simulación"**
- Abrir consola del navegador (F12)
- Ver el JSON impreso con los nuevos campos

### 4. Verificar en consola:
```javascript
// En la consola del navegador:
window.ultimasRutas[0]

// Debería mostrar:
{
  flightId: "SPIM-KJFK-0800",
  departureUtc: "2025-01-15T13:00:00Z",
  arrivalUtc: "2025-01-15T19:30:00Z",
  quantity: 75,
  slackMinutes: 45,
  // ... otros campos
}
```

---

## ✅ RESUMEN

### Archivos Modificados:
1. ✅ `VueloSimplificadoDTO.java` - Agregados 5 campos nuevos
2. ✅ `AlgoritmoGeneticoService.java` - Lógica de cálculo y formato UTC

### Funcionalidades Agregadas:
1. ✅ **FlightId único** por vuelo
2. ✅ **Fechas UTC ISO-8601** para cálculos precisos
3. ✅ **Cantidad total** de paquetes por vuelo
4. ✅ **Holgura de tiempo** (slack) calculada vs deadline
5. ✅ **Formato automático** de todas las fechas

### Compatibilidad:
- ✅ Los campos antiguos siguen funcionando (sin breaking changes)
- ✅ Los nuevos campos son opcionales (tu frontend puede ignorarlos)
- ✅ El visualizador HTML ya está preparado para mostrar los datos

---

## 🎯 PRÓXIMOS PASOS

1. **Actualizar tu frontend React** para usar los nuevos campos UTC
2. **Implementar interpolación** de posiciones de aviones
3. **Agregar colores dinámicos** según `slackMinutes`
4. **Animar vuelos** en tiempo real usando `fechaSimulada`

¡Ahora tu backend envía **TODOS** los datos necesarios para una visualización completa en el mapa! 🚀✈️🗺️
