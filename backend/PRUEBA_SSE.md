# 🚀 Guía para probar SSE (Server-Sent Events)

## ✅ Backend implementado exitosamente

El planificador con SSE está funcionando en: `http://localhost:8080`

---

## 📡 Endpoints disponibles:

### 1. **POST** `/api/simulacion/iniciar`
Inicia la simulación (cada 1 segundo = 10 horas simuladas)

```bash
curl -X POST http://localhost:8080/api/simulacion/iniciar
```

**Respuesta:**
```json
{
  "mensaje": "Simulación iniciada exitosamente",
  "estado": {
    "horaSimulada": "2025-01-01T00:00:00",
    "tiempoRealTranscurridoMs": 0,
    "activa": true,
    "tickActual": 0,
    "estadoDescripcion": "ACTIVA",
    "timeScale": 10.0
  }
}
```

---

### 2. **GET** `/api/simulacion/stream` ⭐ **SSE EN TIEMPO REAL**
Stream que devuelve JSON cada segundo con el tiempo simulado y tiempo real

```bash
# Ver stream en terminal:
curl -N http://localhost:8080/api/simulacion/stream
```

**Respuesta (cada 1 segundo):**
```json
{
  "horaSimulada": "2025-01-01T10:00:00",
  "tiempoRealTranscurridoMs": 1000,
  "activa": true,
  "tickActual": 1,
  "estadoDescripcion": "ACTIVA",
  "timeScale": 10.0
}
```

---

### 3. **POST** `/api/simulacion/pausar`
Pausa la simulación

```bash
curl -X POST http://localhost:8080/api/simulacion/pausar
```

---

### 4. **POST** `/api/simulacion/reanudar`
Reanuda la simulación pausada

```bash
curl -X POST http://localhost:8080/api/simulacion/reanudar
```

---

### 5. **POST** `/api/simulacion/detener`
Detiene y resetea completamente la simulación

```bash
curl -X POST http://localhost:8080/api/simulacion/detener
```

---

### 6. **GET** `/api/simulacion/estado`
Obtiene el estado actual (una sola vez, sin stream)

```bash
curl http://localhost:8080/api/simulacion/estado
```

---

## 🌐 Prueba con HTML (Copia y pega en un archivo .html):

```html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Simulación en Tiempo Real - SSE</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }

        .container {
            background: white;
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            padding: 40px;
            max-width: 600px;
            width: 100%;
        }

        h1 {
            text-align: center;
            color: #667eea;
            margin-bottom: 10px;
            font-size: 2em;
        }

        .subtitle {
            text-align: center;
            color: #888;
            margin-bottom: 30px;
            font-size: 0.9em;
        }

        .time-display {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 15px;
            margin-bottom: 20px;
            text-align: center;
        }

        .time-label {
            font-size: 0.8em;
            opacity: 0.9;
            margin-bottom: 5px;
        }

        .time-value {
            font-size: 2em;
            font-weight: bold;
            font-family: 'Courier New', monospace;
        }

        .controls {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 10px;
            margin-bottom: 20px;
        }

        button {
            padding: 15px;
            border: none;
            border-radius: 10px;
            font-size: 1em;
            font-weight: bold;
            cursor: pointer;
            transition: all 0.3s ease;
            color: white;
        }

        button:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        }

        .btn-iniciar {
            background: #10b981;
            grid-column: span 2;
        }

        .btn-pausar {
            background: #f59e0b;
        }

        .btn-reanudar {
            background: #3b82f6;
        }

        .btn-detener {
            background: #ef4444;
            grid-column: span 2;
        }

        .status {
            text-align: center;
            padding: 15px;
            border-radius: 10px;
            margin-bottom: 20px;
            font-weight: bold;
        }

        .status.activa {
            background: #d1fae5;
            color: #065f46;
        }

        .status.pausada {
            background: #fed7aa;
            color: #92400e;
        }

        .status.detenida {
            background: #fee2e2;
            color: #991b1b;
        }

        .info {
            background: #f3f4f6;
            padding: 15px;
            border-radius: 10px;
            font-size: 0.9em;
            color: #666;
        }

        .info-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
        }

        .info-item:last-child {
            margin-bottom: 0;
        }

        .info-label {
            font-weight: bold;
            color: #444;
        }

        @keyframes pulse {
            0%, 100% {
                opacity: 1;
            }
            50% {
                opacity: 0.5;
            }
        }

        .status.activa {
            animation: pulse 2s infinite;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>⏰ Simulación en Tiempo Real</h1>
        <p class="subtitle">1 segundo real = 10 horas simuladas</p>

        <!-- Estado -->
        <div id="status" class="status detenida">
            🔴 SIMULACIÓN DETENIDA
        </div>

        <!-- Tiempos -->
        <div class="time-display">
            <div class="time-label">⏰ Hora Simulada</div>
            <div class="time-value" id="horaSimulada">--:--:--</div>
        </div>

        <div class="time-display">
            <div class="time-label">⏱️ Tiempo Real Transcurrido</div>
            <div class="time-value" id="tiempoReal">0s</div>
        </div>

        <!-- Controles -->
        <div class="controls">
            <button class="btn-iniciar" onclick="iniciar()">🚀 Iniciar</button>
            <button class="btn-pausar" onclick="pausar()">⏸️ Pausar</button>
            <button class="btn-reanudar" onclick="reanudar()">▶️ Reanudar</button>
            <button class="btn-detener" onclick="detener()">⏹️ Detener</button>
        </div>

        <!-- Info adicional -->
        <div class="info">
            <div class="info-item">
                <span class="info-label">Tick Actual:</span>
                <span id="tickActual">0</span>
            </div>
            <div class="info-item">
                <span class="info-label">Time Scale:</span>
                <span id="timeScale">10.0 h/s</span>
            </div>
            <div class="info-item">
                <span class="info-label">Conexión SSE:</span>
                <span id="conexionSSE">❌ Desconectado</span>
            </div>
        </div>
    </div>

    <script>
        const API_URL = 'http://localhost:8080/api/simulacion';
        let eventSource = null;

        // Conectar al stream SSE
        function conectarSSE() {
            if (eventSource) {
                eventSource.close();
            }

            eventSource = new EventSource(`${API_URL}/stream`);

            eventSource.onopen = () => {
                document.getElementById('conexionSSE').textContent = '✅ Conectado';
                console.log('✅ Conectado al stream SSE');
            };

            eventSource.onmessage = (event) => {
                const data = JSON.parse(event.data);
                console.log('📊 Datos recibidos:', data);

                // Actualizar hora simulada
                if (data.horaSimulada) {
                    const fecha = new Date(data.horaSimulada);
                    document.getElementById('horaSimulada').textContent = 
                        `${fecha.getDate()}/${fecha.getMonth() + 1} ${String(fecha.getHours()).padStart(2, '0')}:${String(fecha.getMinutes()).padStart(2, '0')}`;
                }

                // Actualizar tiempo real
                const segundos = Math.floor(data.tiempoRealTranscurridoMs / 1000);
                document.getElementById('tiempoReal').textContent = `${segundos}s`;

                // Actualizar estado
                const statusElement = document.getElementById('status');
                if (data.activa) {
                    statusElement.textContent = '🟢 SIMULACIÓN ACTIVA';
                    statusElement.className = 'status activa';
                } else {
                    statusElement.textContent = '🟡 SIMULACIÓN PAUSADA';
                    statusElement.className = 'status pausada';
                }

                // Actualizar info
                document.getElementById('tickActual').textContent = data.tickActual;
                document.getElementById('timeScale').textContent = `${data.timeScale} h/s`;
            };

            eventSource.onerror = (error) => {
                console.error('❌ Error en SSE:', error);
                document.getElementById('conexionSSE').textContent = '❌ Error';
                eventSource.close();
            };
        }

        // Control de simulación
        async function iniciar() {
            try {
                const response = await fetch(`${API_URL}/iniciar`, { method: 'POST' });
                const data = await response.json();
                console.log('🚀 Simulación iniciada:', data);
                conectarSSE();
            } catch (error) {
                console.error('❌ Error al iniciar:', error);
            }
        }

        async function pausar() {
            try {
                const response = await fetch(`${API_URL}/pausar`, { method: 'POST' });
                const data = await response.json();
                console.log('⏸️ Simulación pausada:', data);
            } catch (error) {
                console.error('❌ Error al pausar:', error);
            }
        }

        async function reanudar() {
            try {
                const response = await fetch(`${API_URL}/reanudar`, { method: 'POST' });
                const data = await response.json();
                console.log('▶️ Simulación reanudada:', data);
            } catch (error) {
                console.error('❌ Error al reanudar:', error);
            }
        }

        async function detener() {
            try {
                const response = await fetch(`${API_URL}/detener`, { method: 'POST' });
                const data = await response.json();
                console.log('⏹️ Simulación detenida:', data);
                
                // Actualizar UI
                document.getElementById('status').textContent = '🔴 SIMULACIÓN DETENIDA';
                document.getElementById('status').className = 'status detenida';
                document.getElementById('horaSimulada').textContent = '--:--:--';
                document.getElementById('tiempoReal').textContent = '0s';
                document.getElementById('tickActual').textContent = '0';
                
                if (eventSource) {
                    eventSource.close();
                    document.getElementById('conexionSSE').textContent = '❌ Desconectado';
                }
            } catch (error) {
                console.error('❌ Error al detener:', error);
            }
        }

        // Conectar automáticamente al cargar
        window.onload = () => {
            console.log('🌐 Página cargada, conectando al SSE...');
            conectarSSE();
        };

        // Cerrar conexión al salir
        window.onbeforeunload = () => {
            if (eventSource) {
                eventSource.close();
            }
        };
    </script>
</body>
</html>
```

---

## 🎯 Cómo probar:

### **Opción 1: Con curl (Terminal)**
```bash
# 1. Iniciar simulación
curl -X POST http://localhost:8080/api/simulacion/iniciar

# 2. Ver stream en tiempo real (Ctrl+C para detener)
curl -N http://localhost:8080/api/simulacion/stream
```

### **Opción 2: Con HTML**
1. Copia el código HTML de arriba
2. Guárdalo como `simulacion.html`
3. Ábrelo en tu navegador
4. Haz clic en "🚀 Iniciar"
5. Verás el tiempo actualizándose cada segundo automáticamente

---

## 📊 Lo que verás:

Cada segundo recibirás un JSON como este:
```json
{
  "horaSimulada": "2025-01-07T12:00:00",
  "tiempoRealTranscurridoMs": 75000,
  "activa": true,
  "tickActual": 75,
  "estadoDescripcion": "ACTIVA",
  "timeScale": 10.0
}
```

**Explicación:**
- `horaSimulada`: Hora en la simulación (avanza 10 horas cada segundo)
- `tiempoRealTranscurridoMs`: Milisegundos reales desde que inició
- `activa`: Si la simulación está corriendo
- `tickActual`: Número de ticks (segundos) transcurridos
- `timeScale`: 10.0 = 1 segundo real = 10 horas simuladas

---

## ✅ Estado actual:

- ✅ Backend corriendo en `http://localhost:8080`
- ✅ SSE implementado en `/api/simulacion/stream`
- ✅ JSON devuelto cada 1 segundo
- ✅ Tiempo simulado (10 horas/segundo)
- ✅ Tiempo real (milisegundos)
- ✅ Controles: iniciar, pausar, reanudar, detener

🎉 **¡El SSE está funcionando perfectamente!**
