# 🎯 PROMPT: Visualizador de Algoritmo Genético en Tiempo Real

## 📋 Contexto
Tienes un backend Spring Boot que ejecuta un **Algoritmo Genético REAL** y envía progreso en tiempo real vía WebSocket/STOMP. Necesitas crear un **visualizador frontend** que muestre:

1. ✅ Progreso por generación (0-100%)
2. ✅ Fitness actual y promedio
3. ✅ Número de pedidos procesados
4. ✅ Rutas asignadas (lista de vuelos)
5. ✅ Tiempo transcurrido
6. ✅ Indicador visual (barra de progreso, gráfico)

---

## 🔌 Conexión WebSocket

### Endpoint de conexión
```javascript
const socket = new SockJS('http://localhost:8000/ws');
const stompClient = Stomp.over(socket);
```

### Suscripción al canal
```javascript
// Después de iniciar la simulación, obtienes un sessionId
const sessionId = "20116d22-7694-4664-ac03-06523c92063c"; // Ejemplo

stompClient.subscribe('/topic/simulations/' + sessionId, function(mensaje) {
    const data = JSON.parse(mensaje.body);
    procesarMensaje(data);
});
```

---

## 📦 Estructura del Mensaje `ProgresoAGDTO`

El backend envía este JSON cada vez que completa una generación:

```json
{
  "tipo": "PROGRESO_AG",
  "sessionId": "20116d22-7694-4664-ac03-06523c92063c",
  "generacion": 3,
  "maxGeneraciones": 10,
  "progreso": 30.0,
  "mejorFitness": 2450.5,
  "fitnessPromedio": 2100.3,
  "pedidosProcesados": 19,
  "pedidosTotales": 19,
  "solucion": {
    "rutas": [
      {
        "pedidoId": 123,
        "origen": "SPIM",
        "destino": "SCIE",
        "vueloId": "SPIM-SCIE-002",
        "salida": "2025-01-02T05:30",
        "llegada": "2025-01-02T07:15",
        "duracionHoras": 1.75,
        "distanciaKm": 850.5
      },
      // ... más rutas
    ],
    "objetivo": 2450.5,
    "pedidosAsignados": 18,
    "pedidosNoAsignados": 1
  },
  "timestamp": "2025-11-26T07:32:51.395"
}
```

---

## 🎨 Implementación del Visualizador

### HTML Base
```html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Visualizador AG - SkyRoute</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.5.1/sockjs.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
    <style>
        body { 
            font-family: 'Segoe UI', sans-serif; 
            padding: 20px; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: rgba(255,255,255,0.1);
            padding: 30px;
            border-radius: 15px;
            backdrop-filter: blur(10px);
        }
        .header {
            text-align: center;
            margin-bottom: 30px;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: rgba(255,255,255,0.2);
            padding: 20px;
            border-radius: 10px;
            text-align: center;
        }
        .stat-value {
            font-size: 2em;
            font-weight: bold;
            margin: 10px 0;
        }
        .stat-label {
            font-size: 0.9em;
            opacity: 0.8;
        }
        .progress-container {
            background: rgba(0,0,0,0.3);
            border-radius: 10px;
            padding: 20px;
            margin-bottom: 20px;
        }
        .progress-bar {
            width: 100%;
            height: 30px;
            background: rgba(255,255,255,0.2);
            border-radius: 15px;
            overflow: hidden;
            position: relative;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #4CAF50, #8BC34A);
            transition: width 0.5s ease;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
        }
        .rutas-container {
            background: rgba(0,0,0,0.3);
            border-radius: 10px;
            padding: 20px;
            max-height: 400px;
            overflow-y: auto;
        }
        .ruta-item {
            background: rgba(255,255,255,0.1);
            padding: 10px;
            margin-bottom: 10px;
            border-radius: 5px;
            border-left: 4px solid #4CAF50;
        }
        .controls {
            text-align: center;
            margin-bottom: 20px;
        }
        button {
            padding: 12px 25px;
            font-size: 1em;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            margin: 0 5px;
            font-weight: bold;
            transition: transform 0.2s;
        }
        button:hover { transform: scale(1.05); }
        .btn-connect { background: #2196F3; color: white; }
        .btn-start { background: #4CAF50; color: white; }
        .btn-stop { background: #F44336; color: white; }
        button:disabled { background: #ccc; cursor: not-allowed; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🧬 Visualizador de Algoritmo Genético</h1>
            <p>Monitoreo en tiempo real del proceso de optimización</p>
        </div>

        <div class="controls">
            <button onclick="conectar()" class="btn-connect" id="btnConnect">1. Conectar</button>
            <button onclick="iniciarSimulacion()" class="btn-start" id="btnStart" disabled>2. Iniciar Simulación</button>
            <button onclick="detenerSimulacion()" class="btn-stop" id="btnStop" disabled>3. Detener</button>
        </div>

        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-label">Generación Actual</div>
                <div class="stat-value" id="statGeneracion">0 / 0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Mejor Fitness</div>
                <div class="stat-value" id="statFitness">0.0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Pedidos Procesados</div>
                <div class="stat-value" id="statPedidos">0 / 0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Tiempo Transcurrido</div>
                <div class="stat-value" id="statTiempo">0s</div>
            </div>
        </div>

        <div class="progress-container">
            <h3>📊 Progreso del Algoritmo</h3>
            <div class="progress-bar">
                <div class="progress-fill" id="progressFill" style="width: 0%">0%</div>
            </div>
        </div>

        <div class="rutas-container">
            <h3>✈️ Rutas Asignadas (<span id="rutasCount">0</span>)</h3>
            <div id="rutasList"></div>
        </div>
    </div>

    <script>
        const BASE_URL = 'http://localhost:8000';
        let stompClient = null;
        let currentSimId = null;
        let startTime = null;
        let timerInterval = null;

        // ========== CONEXIÓN ==========
        function conectar() {
            console.log("🔌 Conectando...");
            const socket = new SockJS(BASE_URL + '/ws');
            stompClient = Stomp.over(socket);
            stompClient.debug = null;

            stompClient.connect({}, function(frame) {
                console.log("✅ Conectado");
                document.getElementById('btnStart').disabled = false;
                document.getElementById('btnConnect').disabled = true;
                alert("✅ Conectado al servidor");
            }, function(error) {
                console.error("❌ Error:", error);
                alert("❌ Error de conexión");
            });
        }

        // ========== INICIAR SIMULACIÓN ==========
        function iniciarSimulacion() {
            console.log("🚀 Iniciando...");
            
            fetch(BASE_URL + '/api/simulations/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    fecha: "2025-01-02",
                    factorK: 5,
                    tamanioPoblacion: 10,
                    maxGeneraciones: 10,
                    limiteGeneracionesSinMejora: 5
                })
            })
            .then(res => res.json())
            .then(data => {
                currentSimId = data.sessionId;
                console.log("✅ Simulación iniciada:", currentSimId);
                
                // Iniciar cronómetro
                startTime = Date.now();
                timerInterval = setInterval(actualizarTiempo, 100);
                
                // Suscribirse
                suscribirse(currentSimId);
                
                document.getElementById('btnStart').disabled = true;
                document.getElementById('btnStop').disabled = false;
            })
            .catch(err => {
                console.error("❌ Error:", err);
                alert("❌ Error al iniciar: " + err.message);
            });
        }

        // ========== SUSCRIPCIÓN ==========
        function suscribirse(sessionId) {
            const topic = '/topic/simulations/' + sessionId;
            console.log("📡 Suscribiéndose a:", topic);

            stompClient.subscribe(topic, function(mensaje) {
                const data = JSON.parse(mensaje.body);
                console.log("📥 Mensaje recibido:", data);
                
                if (data.tipo === "PROGRESO_AG") {
                    actualizarVisualizador(data);
                }
            });
        }

        // ========== ACTUALIZAR VISUALIZADOR ==========
        function actualizarVisualizador(progreso) {
            // Generación
            document.getElementById('statGeneracion').textContent = 
                `${progreso.generacion} / ${progreso.maxGeneraciones}`;
            
            // Fitness
            document.getElementById('statFitness').textContent = 
                progreso.mejorFitness.toFixed(2);
            
            // Pedidos
            document.getElementById('statPedidos').textContent = 
                `${progreso.pedidosProcesados} / ${progreso.pedidosTotales}`;
            
            // Barra de progreso
            const progresoPct = progreso.progreso.toFixed(1);
            document.getElementById('progressFill').style.width = progresoPct + '%';
            document.getElementById('progressFill').textContent = progresoPct + '%';
            
            // Rutas
            if (progreso.solucion && progreso.solucion.rutas) {
                mostrarRutas(progreso.solucion.rutas);
            }
        }

        // ========== MOSTRAR RUTAS ==========
        function mostrarRutas(rutas) {
            document.getElementById('rutasCount').textContent = rutas.length;
            
            const lista = document.getElementById('rutasList');
            lista.innerHTML = rutas.map((ruta, idx) => `
                <div class="ruta-item">
                    <strong>#${idx + 1}</strong> 
                    Pedido ${ruta.pedidoId}: 
                    ${ruta.origen} → ${ruta.destino} 
                    (Vuelo: ${ruta.vueloId})
                    <br>
                    <small>
                        🕐 ${ruta.salida} → ${ruta.llegada} 
                        | ⏱️ ${ruta.duracionHoras.toFixed(2)}h 
                        | 📏 ${ruta.distanciaKm.toFixed(0)}km
                    </small>
                </div>
            `).join('');
        }

        // ========== ACTUALIZAR TIEMPO ==========
        function actualizarTiempo() {
            if (startTime) {
                const elapsed = (Date.now() - startTime) / 1000;
                document.getElementById('statTiempo').textContent = 
                    elapsed.toFixed(1) + 's';
            }
        }

        // ========== DETENER ==========
        function detenerSimulacion() {
            if (!currentSimId) return;
            
            fetch(BASE_URL + `/api/simulations/${currentSimId}/cancel`, { 
                method: 'POST' 
            }).then(() => {
                console.log("🛑 Simulación detenida");
                clearInterval(timerInterval);
                document.getElementById('btnStart').disabled = false;
                document.getElementById('btnStop').disabled = true;
            });
        }
    </script>
</body>
</html>
```

---

## 🚀 Instrucciones de Uso

1. **Guarda el HTML** como `visualizador-ag.html` en tu carpeta `/backend/`
2. **Abre el archivo** en tu navegador (Chrome/Firefox)
3. **Click en "1. Conectar"** → Espera confirmación
4. **Click en "2. Iniciar Simulación"** → Verás el progreso en tiempo real

---

## 📊 Lo que verás:

### Generación 0 (Inicial)
```
Generación: 0 / 10
Mejor Fitness: 2100.0
Pedidos: 19 / 19
Progreso: 0% ███░░░░░░░
```

### Generación 5 (Medio)
```
Generación: 5 / 10
Mejor Fitness: 2450.5
Pedidos: 19 / 19
Progreso: 50% ████████░░
```

### Generación 10 (Final)
```
Generación: 10 / 10
Mejor Fitness: 2500.0
Pedidos: 19 / 19
Progreso: 100% ██████████
```

---

## 🔧 Personalización

### Cambiar parámetros del AG
```javascript
body: JSON.stringify({
    fecha: "2025-01-02",
    factorK: 5,
    tamanioPoblacion: 30,      // ← Cambia aquí
    maxGeneraciones: 50,        // ← Cambia aquí
    limiteGeneracionesSinMejora: 10  // ← Cambia aquí
})
```

### Cambiar colores
```css
.progress-fill {
    background: linear-gradient(90deg, #FF6B6B, #FFD93D); /* ← Cambia aquí */
}
```

---

## ✅ Checklist de Verificación

- [ ] Backend corriendo en `localhost:8000`
- [ ] WebSocket endpoint activo: `/ws`
- [ ] HTML abierto en navegador
- [ ] Consola del navegador (F12) abierta para ver logs
- [ ] Botón "Conectar" clickeado
- [ ] Botón "Iniciar Simulación" clickeado

---

## 🐛 Troubleshooting

### No veo progreso
1. Abre consola (F12) → Tab "Console"
2. Busca mensajes `📥 Mensaje recibido:`
3. Si no hay mensajes, verifica que el `sessionId` sea correcto

### Progreso se queda en 0%
1. Verifica que el backend esté enviando `ProgresoAGDTO`
2. Revisa logs del backend: `tail -f info/backend.log | grep PROGRESO_AG`

### WebSocket se desconecta
1. Verifica que `spring.devtools.restart.enabled=false` en `application.properties`
2. No guardes archivos `.java` mientras corre la simulación

---

¡Listo! Ahora tendrás un **visualizador profesional** que muestra el progreso del Algoritmo Genético en tiempo real 🎉
