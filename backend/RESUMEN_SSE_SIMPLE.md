# 🎯 ¿Qué Devuelve el SSE? - Resumen Rápido

## 📡 En Pocas Palabras

El sistema SSE envía **UN EVENTO JSON CADA SEGUNDO** mientras la simulación está activa.

---

## 🔄 Flujo Simple

```
1. Inicias simulación
   POST /api/simulacion/iniciar
   Body: { fecha: "2025-01-15", saltoMinutos: 5 }

2. Te conectas al stream
   GET /api/simulacion/stream

3. Cada segundo recibes:
   
   SEGUNDO 1:  Ventana [0-5 min]   → 3 pedidos   → 2 vuelos   → Progreso 0.34%
   SEGUNDO 2:  Ventana [0-10 min]  → 7 pedidos   → 5 vuelos   → Progreso 0.69%
   SEGUNDO 3:  Ventana [0-15 min]  → 12 pedidos  → 8 vuelos   → Progreso 1.04%
   ...
   SEGUNDO 288: Ventana [0-1440 min] → 1234 pedidos → 345 vuelos → Progreso 100%
   
4. Finaliza
   Event: finalizado
```

---

## 📦 Estructura de Cada Evento (Simplificado)

```json
{
  "tick": 1,                    // Número del tick
  "minutoActual": 5,            // Hasta dónde planificó
  "progreso": 0.0034,           // % completado (0.34%)
  "completada": false,          // ¿Es el último tick?
  "tiempoEjecucionMs": 856,     // Tiempo que tardó
  
  "planificacion": {
    "metadata": {
      "pedidosProcesados": 3,   // Pedidos en esta ventana
      "pedidosATiempo": 2,
      "pedidosTarde": 1,
      "objetivo": 234.5         // Fitness del algoritmo
    },
    
    "aeropuertos": [...],       // Todos los aeropuertos
    
    "vuelos": [                 // Vuelos con pedidos
      {
        "id": "UA123_D0_...",
        "originCode": "KLAX",
        "destinationCode": "KJFK",
        "salida": "2025-01-15T00:30:00",
        "llegada": "2025-01-15T08:45:00",
        "orders": [             // ⭐ PEDIDOS EN ESTE VUELO
          {
            "orderId": "Ped123",
            "cantidad": 50
          }
        ],
        "ruta": {               // ⭐ COORDENADAS PARA MAPA
          "origin": { "lat": 33.9425, "lng": -118.408 },
          "destination": { "lat": 40.6398, "lng": -73.7789 }
        }
      }
    ],
    
    "rutas": [                  // Rutas completas de pedidos
      {
        "pedidoId": 123,
        "destino": "KJFK",
        "subrutas": [
          {
            "vuelos": ["UA123_D0_..."],
            "escalas": []
          }
        ]
      }
    ]
  }
}
```

---

## 🎬 Ejemplo de Uso

```javascript
// 1. Iniciar
fetch('http://localhost:8080/api/simulacion/iniciar', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    fecha: '2025-01-15',
    saltoMinutos: 5
  })
});

// 2. Escuchar eventos
const eventSource = new EventSource('http://localhost:8080/api/simulacion/stream');

eventSource.addEventListener('tick', (event) => {
  const data = JSON.parse(event.data);
  
  console.log(`Tick ${data.tick}: ${data.minutoActual} minutos`);
  console.log(`Progreso: ${(data.progreso * 100).toFixed(2)}%`);
  console.log(`Pedidos: ${data.planificacion.metadata.pedidosProcesados}`);
  console.log(`Vuelos: ${data.planificacion.vuelos.length}`);
  
  // Actualizar tu mapa/UI
  actualizarMapa(data.planificacion.vuelos);
  actualizarProgreso(data.progreso);
});
```

---

## ✅ Puntos Clave

1. **Frecuencia**: 1 evento/segundo
2. **Duración**: 288 segundos (4.8 minutos) para 24 horas con salto de 5min
3. **Datos**: COMPLETOS en cada tick (no incrementales)
4. **Progreso**: Lineal desde 0% hasta 100%
5. **Contenido**: Planificación completa + metadata

---

## 🎯 Lo Más Importante

### Cada segundo recibes:
- ✅ Cuántos minutos procesó
- ✅ Cuántos pedidos hay
- ✅ Qué vuelos se están usando
- ✅ Qué rutas se crearon
- ✅ % de progreso

### Para el frontend:
- 🗺️ Usa `vuelos[].ruta` para dibujar en el mapa
- 📊 Usa `metadata` para estadísticas
- ⏱️ Usa `progreso` para barra de progreso
- ✈️ Usa `vuelos[].orders` para ver pedidos por vuelo

---

## 🚀 Pruébalo

Abre en tu navegador:
```
file:///ruta/a/ejemplo_sse_consola.html
```

Y verás en tiempo real todos los datos que llegan! 🎉
