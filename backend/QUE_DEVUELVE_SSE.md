# 🔄 ¿Qué Devuelve el Sistema SSE? - Explicación Detallada

## 📡 Resumen Ejecutivo

El sistema SSE (Server-Sent Events) **devuelve constantemente eventos JSON** cada **1 segundo** mientras la simulación está activa. Cada evento contiene la planificación completa de rutas para una ventana temporal que **crece incrementalmente**.

---

## 🎯 Flujo de Eventos

### **1. Al Conectarse** 
```
GET /api/simulacion/stream
```

**Primer evento (inmediato):**
```json
event: estado
data: {
  "activa": true,
  "fechaInicio": "2025-01-15",
  "inicioSimulacion": "2025-11-01T10:30:00",
  "minutoActual": 0,
  "saltoMinutos": 5,
  "tickActual": 0,
  "clientesConectados": 1,// Eliminar
  "progreso": 0.0,//ELiminar
  "limiteMinutos": 1440//ELiminar
}
```

✅ **Te dice:** La simulación está activa, aún no ha procesado ningún minuto, y procesará hasta 1440 minutos (24 horas).

---

### **2. Cada Segundo (Tick 1, 2, 3...)**

**Después de 1 segundo:**
```json
event: tick
data: {
  "tick": 1,
  "minutoActual": 5,
  "saltoMinutos": 5,
  "fechaInicio": "2025-01-15",
  "completada": false,
  "progreso": 0.0034,  // 0.34%
  "tiempoEjecucionMs": 856,
  "planificacion": {
    "metadata": {
      "fechaInicio": "2025-01-15T00:00:00",
      "fechaFin": "2025-01-15T00:05:00",
      "rangoDescripcion": "Pedidos entre 2025-01-15T00:00:00 y 2025-01-15T00:05:00",
      "factorK": 1,
      "saltoConsumoMinutos": 5,
      "saltoAlgoritmoMinutos": 5,
      "pedidosProcesados": 3,
      "pedidosATiempo": 2,
      "pedidosTarde": 1,
      "pedidosNoEntregados": 0,
      "objetivo": 234.5,
      "tiempoEjecucionMs": 856,
      "generacionesEjecutadas": 200
    },
    "aeropuertos": [ //Mdificar, devuelve el tiempo real la cantidad de pedidos que tiene ese aeropuerto
      {
        "code": "KLAX",
        "lat": 33.9425,
        "lng": -118.408,
        "name": "Los Angeles - Estados Unidos",
        "region": "AMERICA",
        "country": "Estados Unidos",
        "sede": true,
        "capacity": "ILIMITADO",
        "packages": 0
      },
      // ... más aeropuertos
    ],
    "vuelos": [
      {
        "id": "UA123_D0_2025-01-15T00:00",
        "originCode": "KLAX",
        "destinationCode": "KJFK",
        "salida": "2025-01-15T00:30:00",
        "llegada": "2025-01-15T08:45:00",
        "capacidad": 1000,
        "altitude": 35000,
        "speed": 500,
        "regionOrigin": "AMERICA",
        "regionDestination": "AMERICA",
        "orders": [
          {
            "orderId": "Ped123",
            "cantidad": 50
          },
          {
            "orderId": "Ped124",
            "cantidad": 30
          }
        ],
        "ruta": {
          "origin": {
            "lat": 33.9425,
            "lng": -118.408
          },
          "destination": {
            "lat": 40.6398,
            "lng": -73.7789
          }
        }
      },
      // ... más vuelos
    ],
    "rutas": [
      {
        "pedidoId": 123,
        "clienteId": "CLI001",
        "destino": "KJFK",
        "cantidad": 50,
        "estado": "EN_PROCESO",
        "fechaPedido": "2025-01-15T00:03:00",
        "fechaLimite": "2025-01-17T00:03:00",
        "subrutas": [
          {
            "hub": "KLAX",
            "cantidad": 50,
            "llegada": "2025-01-15T08:45:00",
            "vuelos": ["UA123_D0_2025-01-15T00:00"],
            "escalas": []
          }
        ]
      },
      // ... más rutas
    ],
    "pedidosProcesados": [
      {
        "id": 123,
        "fecha": "2025-01-15 00:03",
        "destino": "KJFK",
        "cantidad": 50,
        "clienteId": "CLI001",
        "estado": "PENDIENTE"
      },
      // ... más pedidos
    ]
  }
}
```

✅ **Te dice:** 
- Acabo de planificar la ventana de **0 a 5 minutos**
- Encontré **3 pedidos** en esa ventana
- He creado **rutas** para ellos
- El algoritmo tardó **856ms** en ejecutar
- Voy al **0.34%** de completitud

**Después de 2 segundos (Tick 2):**
```json
event: tick
data: {
  "tick": 2,
  "minutoActual": 10,
  "saltoMinutos": 5,
  "fechaInicio": "2025-01-15",
  "completada": false,
  "progreso": 0.0069,  // 0.69%
  "tiempoEjecucionMs": 923,
  "planificacion": {
    "metadata": {
      "fechaInicio": "2025-01-15T00:00:00",
      "fechaFin": "2025-01-15T00:10:00",  // ⚠️ AHORA ES 10 MINUTOS
      "rangoDescripcion": "Pedidos entre 2025-01-15T00:00:00 y 2025-01-15T00:10:00",
      "factorK": 2,  // ⚠️ FACTOR K AUMENTÓ
      "saltoConsumoMinutos": 10,
      "pedidosProcesados": 7,  // ⚠️ MÁS PEDIDOS
      // ... mismo formato que antes, pero con MÁS DATOS
    },
    // ... aeropuertos, vuelos, rutas (MÁS COMPLETOS)
  }
}
```

✅ **Te dice:** 
- Ahora planifiqué la ventana de **0 a 10 minutos** (creció)
- Encontré **7 pedidos** (los 3 anteriores + 4 nuevos)
- Las **rutas se recalcularon** completamente
- Progreso: **0.69%**

---

### **3. Cada Segundo Subsiguiente**

El patrón continúa:

| Tick | Minuto Actual | Ventana Temporal | Progreso | Pedidos Acumulados |
|------|---------------|------------------|----------|-------------------|
| 1    | 5             | [0-5 min]        | 0.34%    | 3                 |
| 2    | 10            | [0-10 min]       | 0.69%    | 7                 |
| 3    | 15            | [0-15 min]       | 1.04%    | 12                |
| 4    | 20            | [0-20 min]       | 1.39%    | 18                |
| ...  | ...           | ...              | ...      | ...               |
| 288  | 1440          | [0-1440 min]     | 100%     | 1,234             |

---

### **4. Al Completarse (Último Evento)**

**Tick 288 (24 horas = 1440 minutos):**
```json
event: tick
data: {
  "tick": 288,
  "minutoActual": 1440,
  "saltoMinutos": 5,
  "fechaInicio": "2025-01-15",
  "completada": true,  // ⚠️ COMPLETADA = TRUE
  "progreso": 1.0,     // ⚠️ 100%
  "tiempoEjecucionMs": 1256,
  "planificacion": {
    "metadata": {
      "fechaInicio": "2025-01-15T00:00:00",
      "fechaFin": "2025-01-16T00:00:00",  // ⚠️ 24 HORAS COMPLETAS
      "pedidosProcesados": 1234,
      "pedidosATiempo": 980,
      "pedidosTarde": 234,
      "pedidosNoEntregados": 20,
      "objetivo": 45678.9
    },
    // ... DATOS COMPLETOS DE TODO EL DÍA
  }
}
```

**Seguido de:**
```json
event: finalizado
data: "Simulación completada"
```

✅ La conexión SSE se cierra automáticamente.

---

## 📊 Estructura de Datos Devueltos

### **EventoTickDTO (Cada Segundo)**

```typescript
interface EventoTickDTO {
  // ==================== METADATOS DEL TICK ====================
  tick: number;              // Número del tick: 1, 2, 3...
  minutoActual: number;      // Minuto hasta el cual se planificó: 5, 10, 15...
  saltoMinutos: number;      // Incremento por tick: 5 (configurable)
  fechaInicio: string;       // Fecha base: "2025-01-15"
  completada: boolean;       // ¿Es el último tick?
  progreso: number;          // 0.0 a 1.0 (0% a 100%)
  tiempoEjecucionMs: number; // Tiempo que tardó el AG en ejecutar
  
  // ==================== PLANIFICACIÓN COMPLETA ====================
  planificacion: {
    metadata: {
      fechaInicio: string;           // "2025-01-15T00:00:00"
      fechaFin: string;              // "2025-01-15T00:XX:00" (crece cada tick)
      rangoDescripcion: string;      // Descripción legible
      factorK: number;               // Factor K del algoritmo (crece)
      saltoConsumoMinutos: number;   // Rango temporal actual
      saltoAlgoritmoMinutos: number; // Paso del algoritmo
      pedidosProcesados: number;     // Pedidos en esta ventana
      pedidosATiempo: number;        // Entregados a tiempo
      pedidosTarde: number;          // Entregados tarde
      pedidosNoEntregados: number;   // No se pudieron entregar
      objetivo: number;              // Función objetivo (fitness)
      tiempoEjecucionMs: number;     // Duración del algoritmo
      generacionesEjecutadas: number;// Generaciones del AG
    },
    
    // ==================== AEROPUERTOS ====================
    aeropuertos: [
      {
        code: string;        // "KLAX"
        lat: number;         // 33.9425
        lng: number;         // -118.408
        name: string;        // "Los Angeles - Estados Unidos"
        region: string;      // "AMERICA"
        country: string;     // "Estados Unidos"
        sede: boolean;       // true si es hub principal
        capacity: string;    // "ILIMITADO" o número
        packages: number;    // Paquetes actuales (siempre 0 en esta versión)
      }
      // ... todos los aeropuertos
    ],
    
    // ==================== VUELOS CON PEDIDOS ====================
    vuelos: [
      {
        id: string;              // "UA123_D0_2025-01-15T00:00"
        originCode: string;      // "KLAX"
        destinationCode: string; // "KJFK"
        salida: string;          // "2025-01-15T00:30:00"
        llegada: string;         // "2025-01-15T08:45:00"
        capacidad: number;       // 1000 kg
        altitude: number;        // 35000 pies (para animación)
        speed: number;           // 500 km/h (para animación)
        regionOrigin: string;    // "AMERICA"
        regionDestination: string; // "AMERICA"
        
        // ⭐ PEDIDOS ASIGNADOS A ESTE VUELO
        orders: [
          {
            orderId: string;   // "Ped123"
            cantidad: number;  // 50 kg
          }
          // ... todos los pedidos en este vuelo
        ],
        
        // ⭐ COORDENADAS PARA EL MAPA
        ruta: {
          origin: {
            lat: number;     // 33.9425
            lng: number;     // -118.408
          },
          destination: {
            lat: number;     // 40.6398
            lng: number;     // -73.7789
          }
        }
      }
      // ... todos los vuelos usados
    ],
    
    // ==================== RUTAS DE PEDIDOS ====================
    rutas: [
      {
        pedidoId: number;        // 123
        clienteId: string;       // "CLI001"
        destino: string;         // "KJFK"
        cantidad: number;        // 50 kg
        estado: string;          // "EN_PROCESO"
        fechaPedido: string;     // "2025-01-15T00:03:00"
        fechaLimite: string;     // "2025-01-17T00:03:00"
        
        // ⭐ SUBRUTAS (puede haber varias si se divide)
        subrutas: [
          {
            hub: string;         // "KLAX" (hub origen)
            cantidad: number;    // 50 kg (porción de esta subruta)
            llegada: string;     // "2025-01-15T08:45:00"
            vuelos: string[];    // ["UA123_D0_2025-01-15T00:00"]
            escalas: string[];   // [] (vacío si es directo)
          }
          // ... más subrutas si el pedido se dividió
        ]
      }
      // ... todas las rutas planificadas
    ],
    
    // ==================== PEDIDOS PROCESADOS ====================
    pedidosProcesados: [
      {
        id: number;          // 123
        fecha: string;       // "2025-01-15 00:03"
        destino: string;     // "KJFK"
        cantidad: number;    // 50
        clienteId: string;   // "CLI001"
        estado: string;      // "PENDIENTE"
      }
      // ... todos los pedidos en esta ventana
    ]
  }
}
```

---

## 🎬 Ejemplo Real de Uso en Frontend

### **JavaScript (Conexión SSE)**

```javascript
// 1. Iniciar la simulación
fetch('http://localhost:8080/api/simulacion/iniciar', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    fecha: '2025-01-15',
    saltoMinutos: 5,
    tamanioPoblacion: 50,
    maxGeneraciones: 200
  })
});

// 2. Conectar al stream SSE
const eventSource = new EventSource('http://localhost:8080/api/simulacion/stream');

// 3. Escuchar eventos
eventSource.addEventListener('estado', (event) => {
  const estado = JSON.parse(event.data);
  console.log('Estado inicial:', estado);
  // { activa: true, minutoActual: 0, progreso: 0, ... }
});

eventSource.addEventListener('tick', (event) => {
  const tick = JSON.parse(event.data);
  
  console.log(`Tick ${tick.tick}:`);
  console.log(`  - Ventana: [0-${tick.minutoActual} min]`);
  console.log(`  - Progreso: ${(tick.progreso * 100).toFixed(2)}%`);
  console.log(`  - Pedidos: ${tick.planificacion.metadata.pedidosProcesados}`);
  console.log(`  - Vuelos: ${tick.planificacion.vuelos.length}`);
  console.log(`  - Rutas: ${tick.planificacion.rutas.length}`);
  
  // ⭐ ACTUALIZAR MAPA CON NUEVOS VUELOS
  actualizarMapa(tick.planificacion.vuelos);
  
  // ⭐ ACTUALIZAR PROGRESO
  actualizarBarraProgreso(tick.progreso);
  
  // ⭐ ACTUALIZAR ESTADÍSTICAS
  actualizarEstadisticas(tick.planificacion.metadata);
  
  if (tick.completada) {
    console.log('✅ Simulación completada!');
  }
});

eventSource.addEventListener('finalizado', (event) => {
  console.log('🏁 Simulación finalizada:', event.data);
  eventSource.close();
});

eventSource.onerror = (error) => {
  console.error('❌ Error en SSE:', error);
};
```

---

## 🔑 Puntos Clave

### **1. Datos Acumulativos**
Cada tick **NO envía solo lo nuevo**, sino la **planificación completa** desde el minuto 0 hasta el minuto actual.

**Ejemplo:**
- Tick 1: Planifica [0-5 min] → Devuelve pedidos 1, 2, 3
- Tick 2: Planifica [0-10 min] → Devuelve pedidos 1, 2, 3, 4, 5, 6, 7 **(RE-PLANIFICA TODOS)**

✅ Esto permite que el algoritmo genético **re-optimice** con más información.

### **2. Frecuencia Constante**
- ⏱️ **1 evento por segundo** (configurable en `INTERVALO_TICK_MS`)
- 📊 **288 eventos** para 24 horas con salto de 5 minutos
- ⚡ Cada evento tarda ~800-1200ms en procesarse

### **3. Progreso Lineal**
```
Progreso = minutoActual / 1440
```

| Tick | Minutos | Progreso |
|------|---------|----------|
| 1    | 5       | 0.35%    |
| 10   | 50      | 3.47%    |
| 100  | 500     | 34.7%    |
| 288  | 1440    | 100%     |

### **4. Datos Completos en Cada Tick**
Cada tick incluye:
- ✅ **Aeropuertos** (siempre los mismos, base de datos estática)
- ✅ **Vuelos** (creciente, según pedidos procesados)
- ✅ **Rutas** (creciente, una por pedido)
- ✅ **Pedidos** (creciente, según ventana temporal)
- ✅ **Metadata** (estadísticas actualizadas)

---

## 🎯 Uso Recomendado en Frontend

### **Para Animación en Mapa**
```javascript
eventSource.addEventListener('tick', (event) => {
  const { planificacion } = JSON.parse(event.data);
  
  // Limpiar mapa
  clearMap();
  
  // Dibujar aeropuertos
  planificacion.aeropuertos.forEach(airport => {
    drawAirport(airport.lat, airport.lng, airport.code);
  });
  
  // Dibujar rutas de vuelos
  planificacion.vuelos.forEach(vuelo => {
    drawFlightPath(
      vuelo.ruta.origin,
      vuelo.ruta.destination,
      vuelo.orders.length // Grosor según pedidos
    );
  });
});
```

### **Para Dashboard de Estadísticas**
```javascript
eventSource.addEventListener('tick', (event) => {
  const { planificacion, progreso, tick } = JSON.parse(event.data);
  
  updateStats({
    tick: tick,
    progreso: (progreso * 100).toFixed(2) + '%',
    pedidosTotales: planificacion.metadata.pedidosProcesados,
    aTiempo: planificacion.metadata.pedidosATiempo,
    tarde: planificacion.metadata.pedidosTarde,
    noEntregados: planificacion.metadata.pedidosNoEntregados,
    objetivo: planificacion.metadata.objetivo.toFixed(2),
    vuelosUsados: planificacion.vuelos.length
  });
});
```

---

## 📝 Resumen Final

**¿Qué devuelve el SSE cada segundo?**

```
Un objeto JSON con:
├─ tick: número del tick (1, 2, 3...)
├─ minutoActual: hasta dónde planificó (5, 10, 15...)
├─ progreso: % de completitud (0.0 a 1.0)
├─ completada: si terminó (true/false)
└─ planificacion: {
    ├─ metadata: estadísticas generales
    ├─ aeropuertos: todos los aeropuertos
    ├─ vuelos: vuelos con pedidos asignados
    ├─ rutas: rutas completas de cada pedido
    └─ pedidosProcesados: lista de pedidos
}
```

**Cada segundo recibes la planificación COMPLETA de 0 hasta `minutoActual`**

🎉 **Ideal para visualizar en tiempo real cómo evoluciona la planificación!**
