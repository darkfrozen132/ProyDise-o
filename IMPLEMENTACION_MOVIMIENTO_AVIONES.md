# 🚀 IMPLEMENTACIÓN: Movimiento de Aviones Reactivo

## 📋 Archivo a Modificar

`front/src/pages/simulacion/Simulador/SimuladorSemanal.js`

---

## 🔧 PASO 1: Reemplazar Refs por Estado (Línea ~215 y ~628)

### ❌ ELIMINAR (código actual):

```javascript
const tiempoSimuladoBackendRef = React.useRef(null);
const ultimaActualizacionRealRef = React.useRef(null);
const tiempoSimuladoRef = React.useRef(Date.now());

// ... y más abajo ...

const tiempoSimuladoBackendRef = useRef(null);
const ultimaActualizacionRealRef = useRef(null);
```

### ✅ AGREGAR (nuevo código):

```javascript
// ========== SISTEMA DE TIEMPO SIMULADO (REACTIVO) ==========
const [tiempoSimuladoBackend, setTiempoSimuladoBackend] = useState(null);
const [ultimaActualizacionReal, setUltimaActualizacionReal] = useState(null);
const [tiempoMovimiento, setTiempoMovimiento] = useState(0);
const [tiempoSimulado, setTiempoSimulado] = useState(Date.now());
const [speedMultiplier, setSpeedMultiplier] = useState(DESIRED_TIME_SCALE); // 500x
```

---

## 🔧 PASO 2: Agregar Intervalos de Tiempo (Después de los estados)

### ✅ AGREGAR (nuevo código):

```javascript
// ========== INTERVALO: Incrementar tiempoMovimiento ==========
useEffect(() => {
	if (!ultimaActualizacionReal || !tiempoSimuladoBackend) return;

	console.log('🕐 Iniciando intervalo de tiempo simulado');

	const interval = setInterval(() => {
		const tiempoReal = Date.now() - ultimaActualizacionReal;
		setTiempoMovimiento(tiempoReal);
	}, 50); // 20 FPS (cada 50ms)

	return () => {
		console.log('🛑 Deteniendo intervalo de tiempo simulado');
		clearInterval(interval);
	};
}, [ultimaActualizacionReal, tiempoSimuladoBackend]);

// ========== CALCULAR: Tiempo Simulado ==========
useEffect(() => {
	if (!tiempoSimuladoBackend) {
		setTiempoSimulado(Date.now());
		return;
	}

	// Calcular tiempo simulado: base + (tiempo_real × velocidad)
	const msSimuladosPasados = tiempoMovimiento * speedMultiplier;
	const nuevoTiempoSimulado = tiempoSimuladoBackend + msSimuladosPasados;
	
	setTiempoSimulado(nuevoTiempoSimulado);

	// Debug cada 2 segundos (solo algunos frames)
	if (Math.random() < 0.02) {
		const fechaSimulada = new Date(nuevoTiempoSimulado);
		console.log(`⏰ Tiempo simulado: ${fechaSimulada.toISOString()}`);
		console.log(`   Base: ${new Date(tiempoSimuladoBackend).toISOString()}`);
		console.log(`   Δ Real: ${(tiempoMovimiento / 1000).toFixed(1)}s`);
		console.log(`   Velocidad: ${speedMultiplier}x`);
	}
}, [tiempoSimuladoBackend, tiempoMovimiento, speedMultiplier]);
```

---

## 🔧 PASO 3: Calcular Vuelos en Movimiento con useMemo

### ✅ AGREGAR (antes del return del componente, después de todos los useEffect):

```javascript
// ========== CALCULAR: Vuelos en Movimiento (REACTIVO) ==========
const vuelosEnMovimiento = useMemo(() => {
	if (!tiempoSimulado || flights.length === 0) {
		return [];
	}

	// Debug cada 2 segundos
	if (Math.random() < 0.02) {
		console.log(`✈️ Re-calculando posiciones de ${flights.length} vuelos`);
		console.log(`   Tiempo simulado: ${new Date(tiempoSimulado).toISOString()}`);
	}

	return flights.map(flight => {
		// Calcular posición interpolada basada en tiempo simulado
		const interpolated = calculateInterpolatedPosition(flight, tiempoSimulado);
		
		// Calcular rotación del avión
		const bearing = bearingDegrees(
			interpolated.lat,
			interpolated.lng,
			flight.destination.lat,
			flight.destination.lng
		);
		const rotation = (bearing - 90 + 360) % 360;

		return {
			...flight,
			currentLat: interpolated.lat,
			currentLng: interpolated.lng,
			progress: interpolated.progress,
			status: interpolated.status,
			rotation: rotation
		};
	});
}, [flights, tiempoSimulado]); // 🎯 DEPENDENCIAS REACTIVAS

// Debug: Cantidad de vuelos en el aire
useEffect(() => {
	const enAire = vuelosEnMovimiento.filter(v => 
		v.status === 'active' && v.progress > 0 && v.progress < 100
	).length;
	
	if (enAire !== flightsInAir) {
		setFlightsInAir(enAire);
		console.log(`🛫 Aviones en el aire: ${enAire}/${vuelosEnMovimiento.length}`);
	}
}, [vuelosEnMovimiento, flightsInAir]);
```

---

## 🔧 PASO 4: Actualizar procesarMensajeSimulacion

### 📍 BUSCAR (línea ~1530):

```javascript
if (datos.fechaSimulada) {
	const timestampSimulado = new Date(datos.fechaSimulada).getTime();
	tiempoSimuladoBackendRef.current = timestampSimulado;
	ultimaActualizacionRealRef.current = Date.now();
	// ...
}
```

### ✅ REEMPLAZAR CON:

```javascript
if (datos.fechaSimulada) {
	const timestampSimulado = new Date(datos.fechaSimulada).getTime();
	
	// ✅ ACTUALIZAR ESTADO (no refs)
	setTiempoSimuladoBackend(timestampSimulado);
	setUltimaActualizacionReal(Date.now());
	setTiempoMovimiento(0); // Resetear movimiento
	
	console.log(`⏰ Backend - Tiempo simulado actualizado: ${datos.fechaSimulada}`);
}
```

### 📍 TAMBIÉN BUSCAR (línea ~988 y ~1036):

```javascript
tiempoSimuladoBackendRef.current = data.datos.tiempoSimulacionActual;
```

### ✅ REEMPLAZAR CON:

```javascript
setTiempoSimuladoBackend(data.datos.tiempoSimulacionActual);
setUltimaActualizacionReal(Date.now());
```

---

## 🔧 PASO 5: Actualizar Renderizado del Mapa

### 📍 BUSCAR (línea ~2200+ en el return, dentro del MapContainer):

```javascript
{flights.map((flight) => {
	// ... código actual que usa calculateInterpolatedPosition directamente ...
	const interpolated = calculateInterpolatedPosition(flight, tiempoSimuladoRef.current);
	
	return (
		<Marker
			key={flight.id}
			position={[interpolated.lat, interpolated.lng]}
			// ...
		/>
	);
})}
```

### ✅ REEMPLAZAR CON:

```javascript
{vuelosEnMovimiento.map((flight) => {
	// Verificar que las coordenadas sean válidas
	if (!flight.currentLat || !flight.currentLng || 
			isNaN(flight.currentLat) || isNaN(flight.currentLng)) {
		return null;
	}

	return (
		<Marker
			key={flight.id}
			position={[flight.currentLat, flight.currentLng]} // 🎯 POSICIÓN CALCULADA
			icon={createAirplaneIcon(flight, flight.rotation)}
		>
			<Popup>
				<strong>{flight.flightId}</strong><br/>
				{flight.origin.code} → {flight.destination.code}<br/>
				Progreso: {Math.round(flight.progress)}%<br/>
				Estado: {flight.status}<br/>
				{flight.fechaInicial && (
					<>
						Salida: {new Date(flight.fechaInicial).toLocaleTimeString('es-PE', {
							hour: '2-digit',
							minute: '2-digit',
							timeZone: 'UTC'
						})}<br/>
					</>
				)}
				{flight.fechaFinal && (
					<>
						Llegada: {new Date(flight.fechaFinal).toLocaleTimeString('es-PE', {
							hour: '2-digit',
							minute: '2-digit',
							timeZone: 'UTC'
						})}<br/>
					</>
				)}
				{flight.pedidoId && (
					<>Pedido: {flight.pedidoId}</>
				)}
			</Popup>
		</Marker>
	);
})}
```

---

## 🔧 PASO 6: Limpiar Estado al Resetear

### 📍 BUSCAR (función `handleLimpiarMapa`):

```javascript
const handleLimpiarMapa = () => {
	// ... código actual ...
	setFlights([]);
	// ...
};
```

### ✅ AGREGAR dentro de `handleLimpiarMapa`:

```javascript
const handleLimpiarMapa = () => {
	console.log('🧹 Limpiando mapa y reseteando simulación...');
	
	// Limpiar vuelos
	setFlights([]);
	setFlightsInAir(0);
	contadorVuelosRef.current = 0;
	
	// ✅ RESETEAR TIEMPO SIMULADO
	setTiempoSimuladoBackend(null);
	setUltimaActualizacionReal(null);
	setTiempoMovimiento(0);
	setTiempoSimulado(Date.now());
	
	// Limpiar progreso
	setProgresoAG(null);
	setMensajesSimulacion([]);
	
	// ... resto del código ...
};
```

---

## 🔧 PASO 7: Agregar Controles de Velocidad (OPCIONAL)

### ✅ AGREGAR en el panel de controles (dentro del return, cerca de los botones):

```javascript
{/* Control de Velocidad de Simulación */}
<div className="control-speed">
	<label htmlFor="speed-slider">
		Velocidad: {speedMultiplier}x
	</label>
	<input
		id="speed-slider"
		type="range"
		min="100"
		max="2000"
		step="100"
		value={speedMultiplier}
		onChange={(e) => setSpeedMultiplier(parseInt(e.target.value))}
		disabled={estadoSimulacionStomp !== 'running'}
	/>
	<div className="speed-presets">
		<button onClick={() => setSpeedMultiplier(100)}>100x</button>
		<button onClick={() => setSpeedMultiplier(500)}>500x</button>
		<button onClick={() => setSpeedMultiplier(1000)}>1000x</button>
		<button onClick={() => setSpeedMultiplier(2000)}>2000x</button>
	</div>
</div>
```

### ✅ AGREGAR CSS (en `SimuladorSemanal.css`):

```css
/* Control de Velocidad */
.control-speed {
	padding: 10px;
	background: #f5f5f5;
	border-radius: 8px;
	margin: 10px 0;
}

.control-speed label {
	display: block;
	font-weight: bold;
	margin-bottom: 8px;
}

.control-speed input[type="range"] {
	width: 100%;
	margin-bottom: 10px;
}

.speed-presets {
	display: flex;
	gap: 5px;
	justify-content: space-between;
}

.speed-presets button {
	flex: 1;
	padding: 5px;
	background: #007bff;
	color: white;
	border: none;
	border-radius: 4px;
	cursor: pointer;
	font-size: 12px;
}

.speed-presets button:hover {
	background: #0056b3;
}

.speed-presets button:disabled {
	background: #ccc;
	cursor: not-allowed;
}
```

---

## 🔧 PASO 8: Agregar Display de Tiempo Simulado

### ✅ AGREGAR en el panel de información (cerca del reloj):

```javascript
{/* Tiempo Simulado */}
<div className="tiempo-simulado-display">
	<div className="tiempo-label">Tiempo Simulado:</div>
	<div className="tiempo-valor">
		{tiempoSimulado ? 
			new Date(tiempoSimulado).toLocaleString('es-PE', {
				dateStyle: 'short',
				timeStyle: 'medium',
				timeZone: 'UTC'
			}) 
			: 'No iniciado'
		}
	</div>
	{tiempoSimuladoBackend && (
		<div className="tiempo-info">
			Velocidad: {speedMultiplier}x | 
			Δt: {(tiempoMovimiento / 1000).toFixed(1)}s real
		</div>
	)}
</div>
```

### ✅ AGREGAR CSS:

```css
/* Display de Tiempo Simulado */
.tiempo-simulado-display {
	padding: 12px;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	color: white;
	border-radius: 8px;
	margin: 10px 0;
	box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.tiempo-label {
	font-size: 12px;
	opacity: 0.9;
	margin-bottom: 4px;
}

.tiempo-valor {
	font-size: 18px;
	font-weight: bold;
	font-family: 'Courier New', monospace;
}

.tiempo-info {
	font-size: 11px;
	margin-top: 4px;
	opacity: 0.8;
}
```

---

## ✅ VERIFICACIÓN FINAL

Después de implementar todos los cambios:

### 1️⃣ **Verificar en Consola del Navegador**

Deberías ver logs como:

```
✅ WebSocket Conectado
🚀 Iniciando simulación para 2025-01-15
📨 Respuesta del servidor: {sessionId: "abc123"}
⏰ Backend - Tiempo simulado actualizado: 2025-01-15T00:00:00Z
🕐 Iniciando intervalo de tiempo simulado
⏰ Tiempo simulado: 2025-01-15T01:23:45Z
   Base: 2025-01-15T00:00:00Z
   Δ Real: 10.2s
   Velocidad: 500x
✈️ Re-calculando posiciones de 45 vuelos
🛫 Aviones en el aire: 12/45
```

### 2️⃣ **Verificar en el Mapa**

- ✅ Los aviones deben **moverse suavemente**
- ✅ La velocidad debe ser ajustable con el slider
- ✅ El tiempo simulado debe avanzar
- ✅ Los aviones deben **rotar** hacia su destino

### 3️⃣ **Verificar Estado de React**

Instala **React Developer Tools** y verifica:

- `tiempoSimulado` debe cambiar cada 50ms
- `vuelosEnMovimiento` debe re-calcularse automáticamente
- Los componentes `Marker` deben re-renderizarse

---

## 🐛 Troubleshooting

### Problema: Los aviones no se mueven

**Solución:**
1. Verifica que `tiempoSimulado` esté cambiando (React DevTools)
2. Verifica que `flights` tenga `fechaInicial` y `fechaFinal`
3. Revisa la consola para errores de parsing de fechas

### Problema: Los aviones se mueven muy rápido/lento

**Solución:**
1. Ajusta `speedMultiplier` (100x - 2000x)
2. Verifica que `DESIRED_TIME_SCALE` sea correcto (500)

### Problema: Los aviones "saltan" en lugar de moverse suave

**Solución:**
1. El intervalo de 50ms (20 FPS) es suficiente
2. Verifica que `calculateInterpolatedPosition` esté recibiendo el tiempo correcto
3. No cambies el intervalo a menos de 50ms (puede causar lag)

---

## 📊 Comparación Antes/Después

| Aspecto | ❌ Antes | ✅ Después |
|---------|---------|-----------|
| **Tiempo Simulado** | Ref (no reactivo) | Estado (reactivo) |
| **Cálculo de Posiciones** | En render | useMemo |
| **Re-render** | Manual/forzado | Automático |
| **Performance** | 60 FPS (overkill) | 20 FPS (óptimo) |
| **Debugging** | Difícil | Fácil |
| **Mantenibilidad** | Complejo | Simple |

---

## 🚀 Siguiente Paso

Una vez implementados estos cambios:

1. Guarda el archivo
2. Recarga el navegador (Ctrl + Shift + R)
3. Inicia una simulación
4. ¡Los aviones deberían moverse! ✈️

---

**¿Quieres que implemente estos cambios automáticamente o prefieres hacerlo manualmente paso a paso?** 🤔
