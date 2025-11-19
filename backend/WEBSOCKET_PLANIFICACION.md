# 🔌 WebSocket de Planificación - Guía de Uso

## 📡 Endpoint

```
ws://localhost:8080/ws/planificacion
```

---

## 📤 Mensajes del Cliente → Servidor

### 1. Iniciar Planificación

```json
{
  "accion": "iniciar",
  "fecha": "2025-01-15",
  "factorK": 14
}
```

### 2. Pausar

```json
{
  "accion": "pausar"
}
```

### 3. Reanudar

```json
{
  "accion": "reanudar"
}
```

### 4. Cancelar

```json
{
  "accion": "cancelar"
}
```

---

## 📥 Mensajes del Servidor → Cliente

### 1. Conectado

```json
{
  "tipo": "conectado",
  "mensaje": "Conexión WebSocket establecida",
  "datos": {
    "sessionId": "abc123"
  }
}
```

### 2. Progreso (cada 0.5 segundos)

```json
{
  "tipo": "progreso",
  "mensaje": "Generación 5/20 - Fitness: 38000.00",
  "datos": {
    "generacion": 5,
    "totalGeneraciones": 20,
    "fitness": 38000.0,
    "porcentaje": 25.0,
    "tiempoMs": 2500,
    "etaMs": 7500,
    "pedidosProcesados": 1000,
    "totalPedidos": 1000
  }
}
```

### 3. Completado

```json
{
  "tipo": "completado",
  "mensaje": "Planificación completada exitosamente"
}
```

### 4. Error

```json
{
  "tipo": "error",
  "mensaje": "Error en planificación: mensaje de error"
}
```

### 5. Pausado

```json
{
  "tipo": "pausado",
  "mensaje": "Planificación pausada"
}
```

### 6. Reanudado

```json
{
  "tipo": "reanudado",
  "mensaje": "Planificación reanudada"
}
```

### 7. Cancelado

```json
{
  "tipo": "cancelado",
  "mensaje": "Planificación cancelada"
}
```

---

## 🌐 Ejemplo JavaScript (Frontend)

```javascript
class PlanificacionWebSocket {
    constructor() {
        this.ws = null;
        this.onProgreso = null;
        this.onCompletado = null;
        this.onError = null;
    }
    
    conectar() {
        this.ws = new WebSocket('ws://localhost:8080/ws/planificacion');
        
        this.ws.onopen = () => {
            console.log('✅ WebSocket conectado');
        };
        
        this.ws.onmessage = (event) => {
            const response = JSON.parse(event.data);
            
            switch (response.tipo) {
                case 'conectado':
                    console.log('🔌 Conectado:', response.datos.sessionId);
                    break;
                    
                case 'progreso':
                    if (this.onProgreso) {
                        this.onProgreso(response.datos);
                    }
                    console.log(`📊 Gen ${response.datos.generacion}/${response.datos.totalGeneraciones} - ${response.datos.porcentaje.toFixed(1)}%`);
                    break;
                    
                case 'completado':
                    if (this.onCompletado) {
                        this.onCompletado();
                    }
                    console.log('✅ Completado!');
                    break;
                    
                case 'error':
                    if (this.onError) {
                        this.onError(response.mensaje);
                    }
                    console.error('❌ Error:', response.mensaje);
                    break;
                    
                case 'pausado':
                    console.log('⏸️ Pausado');
                    break;
                    
                case 'reanudado':
                    console.log('▶️ Reanudado');
                    break;
                    
                case 'cancelado':
                    console.log('❌ Cancelado');
                    break;
            }
        };
        
        this.ws.onerror = (error) => {
            console.error('❌ WebSocket error:', error);
        };
        
        this.ws.onclose = () => {
            console.log('🔌 WebSocket desconectado');
        };
    }
    
    iniciar(fecha, factorK) {
        this.ws.send(JSON.stringify({
            accion: 'iniciar',
            fecha: fecha,
            factorK: factorK
        }));
    }
    
    pausar() {
        this.ws.send(JSON.stringify({ accion: 'pausar' }));
    }
    
    reanudar() {
        this.ws.send(JSON.stringify({ accion: 'reanudar' }));
    }
    
    cancelar() {
        this.ws.send(JSON.stringify({ accion: 'cancelar' }));
    }
    
    desconectar() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

// ============ USO ============

const planificador = new PlanificacionWebSocket();

// Callbacks
planificador.onProgreso = (datos) => {
    const porcentaje = datos.porcentaje.toFixed(1);
    const etaSeg = (datos.etaMs / 1000).toFixed(0);
    
    console.log(`📊 Progreso: ${porcentaje}% - ETA: ${etaSeg}s`);
    
    // Actualizar UI
    document.getElementById('progreso-bar').style.width = porcentaje + '%';
    document.getElementById('progreso-texto').textContent = 
        `Generación ${datos.generacion}/${datos.totalGeneraciones}`;
    document.getElementById('eta').textContent = `ETA: ${etaSeg}s`;
};

planificador.onCompletado = () => {
    console.log('✅ Planificación finalizada!');
    alert('Planificación completada con éxito');
};

planificador.onError = (mensaje) => {
    console.error('❌ Error:', mensaje);
    alert('Error: ' + mensaje);
};

// Conectar
planificador.conectar();

// Iniciar planificación (cuando el usuario lo pida)
document.getElementById('btn-iniciar').onclick = () => {
    planificador.iniciar('2025-01-15', 14);
};

// Pausar
document.getElementById('btn-pausar').onclick = () => {
    planificador.pausar();
};

// Reanudar
document.getElementById('btn-reanudar').onclick = () => {
    planificador.reanudar();
};

// Cancelar
document.getElementById('btn-cancelar').onclick = () => {
    planificador.cancelar();
};
```

---

## 🎨 Ejemplo HTML + CSS

```html
<!DOCTYPE html>
<html>
<head>
    <title>Planificación WebSocket</title>
    <style>
        .progreso-container {
            width: 100%;
            background-color: #f0f0f0;
            border-radius: 5px;
            margin: 20px 0;
        }
        .progreso-bar {
            height: 30px;
            background-color: #4CAF50;
            border-radius: 5px;
            transition: width 0.3s ease;
            width: 0%;
        }
        .buttons {
            margin: 20px 0;
        }
        button {
            padding: 10px 20px;
            margin: 5px;
            font-size: 16px;
            cursor: pointer;
        }
    </style>
</head>
<body>
    <h1>🚀 Planificación de Rutas</h1>
    
    <div class="progreso-container">
        <div id="progreso-bar" class="progreso-bar"></div>
    </div>
    
    <div id="progreso-texto">Esperando...</div>
    <div id="eta"></div>
    
    <div class="buttons">
        <button id="btn-iniciar">▶️ Iniciar</button>
        <button id="btn-pausar">⏸️ Pausar</button>
        <button id="btn-reanudar">⏯️ Reanudar</button>
        <button id="btn-cancelar">❌ Cancelar</button>
    </div>
    
    <script src="planificacion-ws.js"></script>
</body>
</html>
```

---

## 💾 Velocidad y Memoria

### **Ventajas del WebSocket vs SSE:**

| Característica | WebSocket | SSE |
|---|---|---|
| **Latencia** | ~10-20ms | ~50-100ms |
| **Overhead** | Mínimo (frames binarios) | HTTP headers cada mensaje |
| **Bidireccional** | ✅ Sí | ❌ No |
| **Control** | Pausar/Cancelar | Solo recibir |
| **Uso RAM** | ~1-2 KB/conexión | ~2-5 KB/conexión |

### **Optimizaciones aplicadas:**

✅ **Callback en RAM**: No persiste en BD, solo notifica  
✅ **Envío periódico**: Cada 0.5s (no sobrecarga)  
✅ **Estado en memoria**: `ConcurrentHashMap` thread-safe  
✅ **CompletableFuture**: Ejecución asíncrona sin bloquear  

---

## 🧪 Prueba con curl (WebSocket CLI)

```bash
# Instalar wscat
npm install -g wscat

# Conectar
wscat -c ws://localhost:8080/ws/planificacion

# Enviar mensaje (copiar y pegar)
{"accion":"iniciar","fecha":"2025-01-15","factorK":14}
```

---

## ✅ Checklist de Implementación

- [✅] WebSocketConfig configurado
- [✅] PlanificacionWebSocketHandler creado
- [✅] DTOs (Request/Response/Progreso) definidos
- [✅] AlgoritmoGeneticoService con métodos WS
- [✅] Control de estado (pausar/reanudar/cancelar)
- [✅] Compilación exitosa
- [⏳] Pruebas con frontend real

---

¡Listo para probar! 🚀
