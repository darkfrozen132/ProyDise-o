# 🎯 Sistema de Simulación Incremental SSE - Implementado

## ✅ Componentes Implementados

### 1. **DTOs para SSE** (`planificador/semanal/dto/sse/`)

- ✅ `EventoTickDTO.java` - Representa cada tick de la simulación
- ✅ `SimulacionEstadoDTO.java` - Estado actual de la simulación
- ✅ `IniciarSimulacionRequest.java` - Request para iniciar simulación

### 2. **Orquestador** (`planificador/service/`)

- ✅ `SimulacionOrchestrator.java` - Motor del loop incremental con SSE

### 3. **Controlador REST + SSE** (`planificador/semanal/controller/`)

- ✅ `SimulacionController.java` - API REST y streaming SSE

---

## 🚀 Endpoints Disponibles

### **1. Iniciar Simulación**

```http
POST /api/simulacion/iniciar
Content-Type: application/json

{
  "fecha": "2025-01-15",
  "saltoMinutos": 5,
  "tamanioPoblacion": 50,
  "maxGeneraciones": 200
}
```

**Respuesta:**
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
GET /api/simulacion/stream
Accept: text/event-stream
```

**Eventos recibidos:**

#### a) Evento `estado` (inicial)
```json
{
  "activa": true,
  "fechaInicio": "2025-01-15",
  "inicioSimulacion": "2025-11-01T01:20:00",
  "minutoActual": 0,
  "saltoMinutos": 5,
  "tickActual": 0,
  "clientesConectados": 1,
  "progreso": 0.0,
  "limiteMinutos": 1440
}
```

#### b) Evento `tick` (cada segundo)
```json
{
  "tick": 1,
  "minutoActual": 5,
  "saltoMinutos": 5,
  "fechaInicio": "2025-01-15",
  "planificacion": {
    "metadata": { ... },
    "aeropuertos": [ ... ],
    "vuelos": [ ... ],
    "rutas": [ ... ]
  },
  "tiempoEjecucionMs": 1234,
  "completada": false,
  "progreso": 0.0034722
}
```

#### c) Evento `finalizado`
```json
"Simulación detenida"
```

---

### **3. Obtener Estado**

```http
GET /api/simulacion/estado
```

**Respuesta:**
```json
{
  "activa": true,
  "fechaInicio": "2025-01-15",
  "inicioSimulacion": "2025-11-01T01:20:00",
  "minutoActual": 120,
  "saltoMinutos": 5,
  "tickActual": 24,
  "clientesConectados": 2,
  "progreso": 0.0833,
  "limiteMinutos": 1440
}
```

---

### **4. Detener Simulación**

```http
POST /api/simulacion/detener
```

**Respuesta:**
```json
{
  "success": true,
  "mensaje": "Simulación detenida exitosamente"
}
```

---

### **5. Health Check**

```http
GET /api/simulacion/health
```

**Respuesta (simulación activa):**
```json
{
  "status": "UP",
  "servicio": "Simulación Incremental SSE",
  "simulacionActiva": true,
  "tickActual": 50,
  "progreso": "17.4%",
  "clientesConectados": 3
}
```

---

## 🌐 Ejemplo de Uso desde Frontend (JavaScript)

### **Opción 1: EventSource API (Recomendado)**

```javascript
// 1. Iniciar simulación
async function iniciarSimulacion() {
  const response = await fetch('/api/simulacion/iniciar', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fecha: '2025-01-15',
      saltoMinutos: 5,
      tamanioPoblacion: 50,
      maxGeneraciones: 200
    })
  });
  
  const result = await response.json();
  console.log(result.mensaje);
  
  if (result.success) {
    conectarSSE();
  }
}

// 2. Conectar al stream SSE
function conectarSSE() {
  const eventSource = new EventSource('/api/simulacion/stream');
  
  // Evento: Estado inicial
  eventSource.addEventListener('estado', (event) => {
    const estado = JSON.parse(event.data);
    console.log('📊 Estado inicial:', estado);
    actualizarUI(estado);
  });
  
  // Evento: Cada tick (cada segundo)
  eventSource.addEventListener('tick', (event) => {
    const tick = JSON.parse(event.data);
    console.log(`⏱️ Tick ${tick.tick} - Progreso: ${(tick.progreso * 100).toFixed(1)}%`);
    
    // Actualizar mapa con nuevos vuelos
    actualizarMapa(tick.planificacion.vuelos);
    
    // Actualizar rutas
    actualizarRutas(tick.planificacion.rutas);
    
    // Actualizar barra de progreso
    actualizarProgreso(tick.progreso);
  });
  
  // Evento: Simulación finalizada
  eventSource.addEventListener('finalizado', (event) => {
    console.log('✅ Simulación completada');
    eventSource.close();
    mostrarMensaje('Simulación completada');
  });
  
  // Manejo de errores
  eventSource.onerror = (error) => {
    console.error('❌ Error en SSE:', error);
    eventSource.close();
  };
  
  return eventSource;
}

// 3. Detener simulación
async function detenerSimulacion(eventSource) {
  const response = await fetch('/api/simulacion/detener', {
    method: 'POST'
  });
  
  const result = await response.json();
  console.log(result.mensaje);
  
  if (eventSource) {
    eventSource.close();
  }
}

// Funciones auxiliares (implementar según tu UI)
function actualizarMapa(vuelos) {
  // Actualizar marcadores y rutas en el mapa
  vuelos.forEach(vuelo => {
    // Agregar vuelo al mapa con animación
    console.log(`Vuelo: ${vuelo.originCode} → ${vuelo.destinationCode}`);
  });
}

function actualizarRutas(rutas) {
  // Actualizar lista de rutas planificadas
  console.log(`Total rutas: ${rutas.length}`);
}

function actualizarProgreso(progreso) {
  // Actualizar barra de progreso
  const porcentaje = (progreso * 100).toFixed(1);
  console.log(`Progreso: ${porcentaje}%`);
}

function actualizarUI(estado) {
  console.log('Estado:', estado);
}

function mostrarMensaje(mensaje) {
  console.log(mensaje);
}
```

### **Opción 2: Fetch API con Streams**

```javascript
async function conectarSSEConFetch() {
  const response = await fetch('/api/simulacion/stream');
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');
    
    for (const line of lines) {
      if (line.startsWith('data:')) {
        const data = JSON.parse(line.substring(5));
        console.log('Evento recibido:', data);
      }
    }
  }
}
```

---

## 📊 Flujo de Ejecución

```
1. Frontend envía POST /api/simulacion/iniciar
   ↓
2. Backend crea SimulacionOrchestrator y thread de simulación
   ↓
3. Frontend conecta a GET /api/simulacion/stream (SSE)
   ↓
4. Backend envía evento "estado" inicial
   ↓
5. LOOP cada 1 segundo:
   ├─ minutoActual += saltoMinutos
   ├─ Ejecutar algoritmo genético [0-minutoActual]
   ├─ Broadcast evento "tick" a todos los clientes
   └─ Esperar 1 segundo
   ↓
6. Al llegar a 1440 minutos (24h) o POST /detener:
   ├─ Enviar evento "finalizado"
   └─ Cerrar conexiones SSE
```

---

## 🔧 Configuración

### **Constantes en SimulacionOrchestrator**

```java
private static final long INTERVALO_TICK_MS = 1000;  // 1 segundo entre ticks
private static final int LIMITE_MINUTOS = 1440;      // 24 horas
private static final long SSE_TIMEOUT = 30 * 60 * 1000; // 30 minutos timeout
```

### **Parámetros por Defecto**

- `saltoMinutos`: 5 (incremento de 5 minutos por tick)
- `tamanioPoblacion`: 50 (algoritmo genético)
- `maxGeneraciones`: 200 (algoritmo genético)

---

## 🎨 Ejemplo de UI React

```jsx
import React, { useState, useEffect } from 'react';

function SimulacionPanel() {
  const [estado, setEstado] = useState(null);
  const [progreso, setProgreso] = useState(0);
  const [eventSource, setEventSource] = useState(null);
  
  const iniciar = async () => {
    const response = await fetch('/api/simulacion/iniciar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fecha: '2025-01-15',
        saltoMinutos: 5,
        tamanioPoblacion: 50,
        maxGeneraciones: 200
      })
    });
    
    if (response.ok) {
      conectarSSE();
    }
  };
  
  const conectarSSE = () => {
    const es = new EventSource('/api/simulacion/stream');
    
    es.addEventListener('estado', (event) => {
      const data = JSON.parse(event.data);
      setEstado(data);
    });
    
    es.addEventListener('tick', (event) => {
      const tick = JSON.parse(event.data);
      setProgreso(tick.progreso);
      // Actualizar mapa aquí
    });
    
    es.addEventListener('finalizado', () => {
      es.close();
      alert('Simulación completada');
    });
    
    setEventSource(es);
  };
  
  const detener = async () => {
    await fetch('/api/simulacion/detener', { method: 'POST' });
    if (eventSource) eventSource.close();
  };
  
  return (
    <div>
      <h2>Simulación Incremental</h2>
      
      <button onClick={iniciar}>Iniciar</button>
      <button onClick={detener}>Detener</button>
      
      {estado && (
        <div>
          <p>Tick: {estado.tickActual}</p>
          <p>Minutos: {estado.minutoActual} / {estado.limiteMinutos}</p>
          <p>Progreso: {(progreso * 100).toFixed(1)}%</p>
          <progress value={progreso} max="1"></progress>
        </div>
      )}
    </div>
  );
}
```

---

## 🐛 Solución de Problemas

### **Error: "Ya hay una simulación activa"**
```bash
# Detener simulación actual
curl -X POST http://localhost:8080/api/simulacion/detener
```

### **Verificar estado de la simulación**
```bash
curl http://localhost:8080/api/simulacion/estado
```

### **Probar SSE con curl**
```bash
curl -N http://localhost:8080/api/simulacion/stream
```

---

## 📝 Logs Relevantes

```
🚀 Simulación iniciada: fecha=2025-01-15, saltoMinutos=5, población=50, generaciones=200
⏱️ Tick 1: Procesando ventana [0-5 min]
📤 Tick 1 completado en 1234ms - 2 clientes notificados - Progreso: 0.3%
⏱️ Tick 2: Procesando ventana [0-10 min]
📤 Tick 2 completado en 1456ms - 2 clientes notificados - Progreso: 0.7%
...
✅ Simulación completada: 1440 minutos procesados (24.0 horas)
🛑 Simulación detenida
```

---

## ✨ Ventajas del Sistema

✅ **Tiempo Real**: Frontend ve la planificación evolucionando segundo a segundo  
✅ **Progresivo**: Ventana temporal crece incrementalmente  
✅ **Escalable**: Múltiples clientes pueden conectarse simultáneamente  
✅ **Control**: Iniciar/detener/monitorear desde la UI  
✅ **Visualización**: Perfecto para mostrar en mapas animados  

---

## 🎯 Próximos Pasos Sugeridos

1. **Frontend**: Implementar UI con mapa interactivo
2. **Persistencia**: Guardar resultados de cada tick en BD
3. **Pausar/Reanudar**: Agregar funcionalidad de pausa
4. **Exportar**: Exportar resultados a CSV/Excel
5. **Notificaciones**: WebSockets para alertas en tiempo real
