# 🎯 Guía de Uso - Sistema de Simulación SSE

## 📋 Descripción

El sistema de simulación SSE permite ejecutar planificaciones incrementales en tiempo real, donde el algoritmo genético procesa ventanas de tiempo cada vez más grandes y envía los resultados progresivamente al cliente.

---

## 🔄 Diferencias entre los Sistemas

### **Sistema Antiguo (Síncrono)**
```
POST /api/planificacion/semanal
```
- ❌ **Síncrono**: Espera a que termine toda la planificación
- ❌ **Una sola respuesta**: Devuelve solo el resultado final
- ❌ **Sin progreso**: No puedes ver el avance
- ✅ **Simple**: Útil para pruebas rápidas

### **Sistema Nuevo (SSE - Incremental)**
```
POST /api/simulacion/iniciar  +  GET /api/simulacion/stream
```
- ✅ **Asíncrono**: Inicia y devuelve inmediatamente
- ✅ **Múltiples eventos**: Envía resultados cada segundo
- ✅ **Progreso en tiempo real**: Ves la evolución del algoritmo
- ✅ **Interactivo**: Puedes detener/pausar la simulación

---

## 🚀 Endpoints del Sistema SSE

### **1. Iniciar Simulación**

```http
POST http://localhost:8000/api/simulacion/iniciar
Content-Type: application/json

{
  "fecha": "2025-01-15",
  "saltoMinutos": 5,
  "tamanioPoblacion": 50,
  "maxGeneraciones": 200
}
```

**Respuesta exitosa:**
```json
{
  "success": true,
  "mensaje": "Simulación iniciada exitosamente",
  "fecha": "2025-01-15",
  "saltoMinutos": 5,
  "instrucciones": "Conecta a GET /api/simulacion/stream para recibir eventos en tiempo real"
}
```

---

### **2. Stream SSE (Tiempo Real)**

```http
GET http://localhost:8000/api/simulacion/stream
Accept: text/event-stream
```

**Eventos que recibirás:**

#### **Evento: `estado`** (Inicial)
```json
{
  "activa": true,
  "tickActual": 0,
  "minutoActual": 0,
  "progreso": 0.0,
  "clientesConectados": 1
}
```

#### **Evento: `tick`** (Cada segundo)
```json
{
  "tick": 1,
  "minutoActual": 5,
  "saltoMinutos": 5,
  "fechaInicio": "2025-01-15",
  "tiempoEjecucionMs": 1234,
  "progreso": 0.0035,
  "totalTicks": 288,
  "planificacion": {
    "metadata": {
      "pedidosProcesados": 5,
      "pedidosATiempo": 4,
      "pedidosTarde": 1,
      "objetivo": 92.5
    },
    "vuelos": [...],
    "rutas": [...]
  }
}
```

#### **Evento: `finalizado`**
```json
{
  "mensaje": "Simulación completada",
  "ticksEjecutados": 288,
  "tiempoTotalMs": 288000
}
```

---

### **3. Detener Simulación**

```http
POST http://localhost:8000/api/simulacion/detener
```

**Respuesta:**
```json
{
  "success": true,
  "mensaje": "Simulación detenida exitosamente"
}
```

---

### **4. Estado Actual**

```http
GET http://localhost:8000/api/simulacion/estado
```

**Respuesta:**
```json
{
  "activa": true,
  "tickActual": 42,
  "minutoActual": 210,
  "progreso": 0.146,
  "fechaInicio": "2025-01-15T00:00:00",
  "saltoMinutos": 5,
  "clientesConectados": 2
}
```

---

### **5. Health Check**

```http
GET http://localhost:8000/api/simulacion/health
```

**Respuesta:**
```json
{
  "status": "UP",
  "servicio": "Simulación Incremental SSE",
  "simulacionActiva": true,
  "tickActual": 42,
  "progreso": "14.6%",
  "clientesConectados": 2
}
```

---

## 💻 Código Cliente JavaScript

### **Ejemplo Completo**

```javascript
// 1. Iniciar simulación
async function iniciarSimulacion() {
    const response = await fetch('http://localhost:8000/api/simulacion/iniciar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            fecha: '2025-01-15',
            saltoMinutos: 5,
            tamanioPoblacion: 50,
            maxGeneraciones: 200
        })
    });
    
    const data = await response.json();
    
    if (data.success) {
        console.log('✅ Simulación iniciada');
        conectarSSE();
    } else {
        console.error('❌ Error:', data.error);
    }
}

// 2. Conectar a SSE
function conectarSSE() {
    const eventSource = new EventSource('http://localhost:8000/api/simulacion/stream');
    
    // Evento inicial
    eventSource.addEventListener('estado', (event) => {
        const estado = JSON.parse(event.data);
        console.log('📊 Estado inicial:', estado);
    });
    
    // Cada tick (cada segundo)
    eventSource.addEventListener('tick', (event) => {
        const tick = JSON.parse(event.data);
        console.log(`⏱️ Tick ${tick.tick}: ${tick.minutoActual} minutos`);
        
        // Actualizar UI
        actualizarMapa(tick.planificacion.vuelos);
        actualizarProgreso(tick.progreso);
        actualizarEstadisticas(tick.planificacion.metadata);
    });
    
    // Simulación completada
    eventSource.addEventListener('finalizado', (event) => {
        console.log('✅ Simulación completada');
        eventSource.close();
    });
    
    // Manejo de errores
    eventSource.onerror = (error) => {
        console.error('❌ Error SSE:', error);
        eventSource.close();
    };
}

// 3. Detener simulación
async function detenerSimulacion() {
    const response = await fetch('http://localhost:8000/api/simulacion/detener', {
        method: 'POST'
    });
    
    const data = await response.json();
    console.log(data.mensaje);
}

// Funciones auxiliares
function actualizarMapa(vuelos) {
    // Dibujar vuelos en el mapa
    vuelos.forEach(vuelo => {
        // Tu lógica de visualización
    });
}

function actualizarProgreso(progreso) {
    const porcentaje = Math.round(progreso * 100);
    document.getElementById('progreso').textContent = `${porcentaje}%`;
}

function actualizarEstadisticas(metadata) {
    document.getElementById('pedidos').textContent = metadata.pedidosProcesados;
    document.getElementById('objetivo').textContent = metadata.objetivo.toFixed(2);
}
```

---

## 🔧 Configuración de Parámetros

### **saltoMinutos (Sa)**
- **Descripción**: Incremento de minutos por cada tick
- **Valores típicos**: 5, 10, 15
- **Ejemplo**: `saltoMinutos=5` → Tick 1: [0-5 min], Tick 2: [0-10 min], Tick 3: [0-15 min]

### **tamanioPoblacion**
- **Descripción**: Número de individuos en la población del AG
- **Rango recomendado**: 30-100
- **Default**: 50

### **maxGeneraciones**
- **Descripción**: Generaciones máximas del algoritmo genético
- **Rango recomendado**: 100-300
- **Default**: 200

---

## 📊 Fórmula del Horizonte Temporal

```
Total de ticks = LIMITE_MINUTOS / saltoMinutos
                = 1440 / saltoMinutos
                = 24 horas / saltoMinutos

Ejemplos:
- saltoMinutos = 5  → 288 ticks (24 horas)
- saltoMinutos = 10 → 144 ticks (24 horas)
- saltoMinutos = 15 → 96 ticks (24 horas)
```

---

## 🎯 Casos de Uso

### **Frontend con Mapa Interactivo**
```javascript
eventSource.addEventListener('tick', (event) => {
    const tick = JSON.parse(event.data);
    
    // Limpiar rutas anteriores
    mapa.clearLayers();
    
    // Dibujar nuevas rutas
    tick.planificacion.vuelos.forEach(vuelo => {
        const polyline = L.polyline([
            [vuelo.ruta.origin.lat, vuelo.ruta.origin.lng],
            [vuelo.ruta.destination.lat, vuelo.ruta.destination.lng]
        ], { color: 'blue' });
        
        polyline.addTo(mapa);
    });
});
```

### **Dashboard de Métricas**
```javascript
eventSource.addEventListener('tick', (event) => {
    const tick = JSON.parse(event.data);
    const meta = tick.planificacion.metadata;
    
    // Actualizar gráficos
    chartPedidos.update([meta.pedidosATiempo, meta.pedidosTarde]);
    chartObjetivo.addPoint(meta.objetivo);
    chartProgreso.setValue(tick.progreso * 100);
});
```

---

## 🛠️ Pruebas con cURL

### **1. Iniciar**
```bash
curl -X POST http://localhost:8000/api/simulacion/iniciar \
  -H "Content-Type: application/json" \
  -d '{
    "fecha": "2025-01-15",
    "saltoMinutos": 5,
    "tamanioPoblacion": 50,
    "maxGeneraciones": 200
  }'
```

### **2. Stream SSE**
```bash
curl -N http://localhost:8000/api/simulacion/stream
```

### **3. Estado**
```bash
curl http://localhost:8000/api/simulacion/estado
```

### **4. Detener**
```bash
curl -X POST http://localhost:8000/api/simulacion/detener
```

---

## 🌐 Probar con el HTML

1. Abre el archivo `test-sse-simulacion.html` en tu navegador:
   ```bash
   firefox test-sse-simulacion.html
   # o
   google-chrome test-sse-simulacion.html
   ```

2. Asegúrate de que el backend esté corriendo:
   ```bash
   mvn spring-boot:run
   ```

3. Configura los parámetros en la interfaz web

4. Click en "▶️ Iniciar Simulación"

5. Observa los eventos en tiempo real

---

## ⚠️ Consideraciones

### **CORS**
Si el frontend está en un dominio diferente, necesitas configurar CORS en el backend.

### **Timeout**
SSE mantiene la conexión abierta. Asegúrate de que tu proxy/nginx tenga timeouts largos.

### **Múltiples Clientes**
Puedes conectar múltiples navegadores/clientes al mismo stream.

### **Reconexión**
EventSource reconecta automáticamente si se pierde la conexión.

---

## 📚 Referencias

- **Documentación SSE**: [MDN Web Docs](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- **Spring SSE**: [Spring SseEmitter](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html)

---

## 🎉 ¡Listo!

Ahora tienes un sistema completo de simulación incremental con SSE. El frontend verá cómo evoluciona la planificación en tiempo real, tick por tick. 🚀
