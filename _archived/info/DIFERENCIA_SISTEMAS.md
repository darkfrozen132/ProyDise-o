# 🎯 RESUMEN RÁPIDO - Sistema SSE vs Sistema Antiguo

## ❌ Lo que NO debes hacer (Sistema Antiguo)

```http
POST http://localhost:8000/api/planificacion/semanal

{
  "fecha": "2025-01-15",
  "factorK": 1,
  "parametrosGenetico": {
    "saltoAlgoritmoMinutos": 5,
    "tamanioPoblacion": 50,
    "maxGeneraciones": 200
  }
}
```

**Problema:** 
- 📨 **UNA SOLA respuesta** al final
- ⏳ **Espera bloqueante** hasta que termine
- 🚫 **Sin progreso** en tiempo real
- 🚫 **Sin SSE**

---

## ✅ Lo que SÍ debes hacer (Sistema SSE Nuevo)

### **Paso 1: Iniciar la simulación**

```http
POST http://localhost:8000/api/simulacion/iniciar

{
  "fecha": "2025-01-15",
  "saltoMinutos": 5,
  "tamanioPoblacion": 50,
  "maxGeneraciones": 200
}
```

**Respuesta inmediata:**
```json
{
  "success": true,
  "mensaje": "Simulación iniciada exitosamente"
}
```

---

### **Paso 2: Conectarte al stream SSE**

```http
GET http://localhost:8000/api/simulacion/stream
```

**Recibirás eventos cada segundo:**

```
event: tick
data: {"tick":1,"minutoActual":5,"planificacion":{...}}

event: tick
data: {"tick":2,"minutoActual":10,"planificacion":{...}}

event: tick
data: {"tick":3,"minutoActual":15,"planificacion":{...}}

...

event: finalizado
data: {"mensaje":"Simulación completada"}
```

---

## 🔄 Flujo Visual

```
Sistema Antiguo (❌):
─────────────────────────────────────────────────────

Cliente                           Servidor
  │                                  │
  │ POST /planificacion/semanal     │
  ├─────────────────────────────────>│
  │                                  │ 🔄 Ejecutando...
  │          ESPERANDO...            │ 🔄 Ejecutando...
  │          ESPERANDO...            │ 🔄 Ejecutando...
  │          ESPERANDO...            │ 🔄 Ejecutando...
  │                                  │
  │<─────────────────────────────────┤
  │      ✅ Respuesta final          │
  │                                  │



Sistema Nuevo SSE (✅):
─────────────────────────────────────────────────────

Cliente                           Servidor
  │                                  │
  │ POST /simulacion/iniciar        │
  ├─────────────────────────────────>│
  │<─────────────────────────────────┤
  │      ✅ OK (inmediato)           │
  │                                  │
  │ GET /simulacion/stream          │
  ├─────────────────────────────────>│
  │                                  │ Thread en background
  │<═════════════════════════════════│ inicia loop
  │   evento: tick 1                │
  │<═════════════════════════════════│
  │   evento: tick 2                │ 🔄 Loop cada 1 seg
  │<═════════════════════════════════│
  │   evento: tick 3                │
  │<═════════════════════════════════│
  │   evento: tick 4                │
  │   ...                            │
  │<═════════════════════════════════│
  │   evento: finalizado            │
  │                                  │
```

---

## 📝 Código JavaScript del Cliente

```javascript
// ❌ FORMA ANTIGUA (Síncrona)
const response = await fetch('/api/planificacion/semanal', {
    method: 'POST',
    body: JSON.stringify({ fecha: '2025-01-15', factorK: 1, ... })
});
const resultado = await response.json();
// Solo recibes el resultado final después de esperar


// ✅ FORMA NUEVA (SSE - Asíncrona)

// 1. Iniciar
const init = await fetch('/api/simulacion/iniciar', {
    method: 'POST',
    body: JSON.stringify({ fecha: '2025-01-15', saltoMinutos: 5, ... })
});

// 2. Conectar SSE
const eventSource = new EventSource('/api/simulacion/stream');

// 3. Recibir eventos en tiempo real
eventSource.addEventListener('tick', (event) => {
    const tick = JSON.parse(event.data);
    console.log(`Tick ${tick.tick}: ${tick.minutoActual} minutos procesados`);
    
    // Actualizar UI en cada tick
    actualizarMapa(tick.planificacion);
    actualizarProgreso(tick.progreso);
});

eventSource.addEventListener('finalizado', (event) => {
    console.log('✅ Completado!');
    eventSource.close();
});
```

---

## 🎯 Endpoints Correctos a Usar

### ✅ **NUEVOS (Sistema SSE)**

| Método | Endpoint | Propósito |
|--------|----------|-----------|
| POST | `/api/simulacion/iniciar` | Inicia simulación incremental |
| GET | `/api/simulacion/stream` | **SSE Stream** - Eventos en tiempo real |
| POST | `/api/simulacion/detener` | Detiene simulación |
| GET | `/api/simulacion/estado` | Estado actual |
| GET | `/api/simulacion/health` | Health check |

### 📦 **ANTIGUOS (Mantener para compatibilidad)**

| Método | Endpoint | Propósito |
|--------|----------|-----------|
| POST | `/api/planificacion/semanal` | Planificación síncrona (respuesta única) |
| GET | `/api/planificacion/semanal/world/estado` | Estado del World |
| POST | `/api/planificacion/semanal/world/refrescar` | Refrescar World |

---

## 🚀 Guía de Migración Rápida

### **Si antes hacías esto:**
```javascript
fetch('/api/planificacion/semanal', {
    method: 'POST',
    body: JSON.stringify({
        fecha: '2025-01-15',
        factorK: 288,  // Para procesar 24 horas
        parametrosGenetico: { ... }
    })
})
.then(res => res.json())
.then(data => {
    // Recibir todo de una vez
    mostrarResultados(data);
});
```

### **Ahora debes hacer esto:**
```javascript
// 1. Iniciar
await fetch('/api/simulacion/iniciar', {
    method: 'POST',
    body: JSON.stringify({
        fecha: '2025-01-15',
        saltoMinutos: 5,  // Incremento por tick
        tamanioPoblacion: 50,
        maxGeneraciones: 200
    })
});

// 2. Escuchar eventos
const eventSource = new EventSource('/api/simulacion/stream');

eventSource.addEventListener('tick', (event) => {
    const tick = JSON.parse(event.data);
    // Recibir actualizaciones cada segundo
    actualizarResultadosProgresivamente(tick);
});
```

---

## 💡 Ventajas del Sistema Nuevo

✅ **Progreso visible**: Ves la evolución en tiempo real  
✅ **No bloquea**: El servidor devuelve respuesta inmediata  
✅ **Interactivo**: Puedes detener/pausar  
✅ **Múltiples clientes**: Varios usuarios pueden ver el mismo progreso  
✅ **Mejor UX**: El usuario ve que algo está pasando  

---

## 📊 Ejemplo de Output

### Sistema Antiguo (❌)
```
POST /api/planificacion/semanal
... esperando 5 minutos ...
{
  "metadata": { "pedidosProcesados": 100 },
  "vuelos": [...],
  "rutas": [...]
}
```

### Sistema Nuevo (✅)
```
POST /api/simulacion/iniciar
{ "success": true }

GET /api/simulacion/stream
event: tick
data: {"tick":1,"minutoActual":5,"planificacion":{...}}

event: tick
data: {"tick":2,"minutoActual":10,"planificacion":{...}}

event: tick
data: {"tick":3,"minutoActual":15,"planificacion":{...}}

... cada segundo ...

event: finalizado
data: {"mensaje":"Completado"}
```

---

## 🎉 ¡Importante!

🔴 **NO uses** `/api/planificacion/semanal` si quieres SSE  
🟢 **USA** `/api/simulacion/iniciar` + `/api/simulacion/stream`

---

## 🔧 Prueba Rápida

```bash
# Terminal 1: Iniciar simulación
curl -X POST http://localhost:8000/api/simulacion/iniciar \
  -H "Content-Type: application/json" \
  -d '{"fecha":"2025-01-15","saltoMinutos":5,"tamanioPoblacion":50,"maxGeneraciones":200}'

# Terminal 2: Ver stream SSE
curl -N http://localhost:8000/api/simulacion/stream
```

O simplemente abre en tu navegador:
```
file:///ruta/a/test-sse-simulacion.html
```
