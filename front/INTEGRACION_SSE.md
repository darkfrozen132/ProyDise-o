# 🚀 Integración SSE (Server-Sent Events) - Simulador Semanal

## ✅ Implementación Completada

Se ha integrado exitosamente el sistema de tiempo real usando **Server-Sent Events (SSE)** desde el backend Spring Boot hacia el frontend React.

---

## 📡 Funcionalidades Implementadas

### 1. **Conexión SSE en Tiempo Real**
- Conexión automática al stream `/api/simulacion/stream` cuando la simulación está activa
- Recepción de actualizaciones cada 1 segundo con el estado de la simulación
- Cierre automático de la conexión cuando la simulación se detiene o el componente se desmonta

### 2. **Panel de Control de Simulación**
El componente `SimuladorSemanal.js` ahora incluye un panel superior con:

#### **Información en Tiempo Real:**
- ⏰ **Tiempo Simulado**: Muestra la hora simulada en formato legible (DD/MM/YYYY HH:MM:SS)
- ⏱️ **Tiempo Real**: Muestra los segundos transcurridos y el número de ticks
- ⚡ **Escala de Tiempo**: Muestra la relación temporal (1s real = 10h simuladas)
- 📡 **Estado SSE**: Indicador visual de si la conexión está ACTIVA o INACTIVA

#### **Botones de Control:**
- 🚀 **Iniciar**: Inicia la simulación y abre la conexión SSE
- ⏸️ **Pausar**: Pausa la simulación (cierra el stream)
- ▶️ **Reanudar**: Reanuda la simulación pausada (reabre el stream)
- ⏹️ **Detener**: Detiene completamente y resetea la simulación

---

## 🔧 Archivos Modificados

### 1. `/src/config/api.js`
**Funciones agregadas:**
```javascript
// Funciones de control
- iniciarSimulacion()      // POST /api/simulacion/iniciar
- pausarSimulacion()        // POST /api/simulacion/pausar
- reanudarSimulacion()      // POST /api/simulacion/reanudar
- detenerSimulacion()       // POST /api/simulacion/detener
- obtenerEstadoSimulacion() // GET /api/simulacion/estado

// Conexión SSE
- conectarStreamSimulacion(onMessage, onError) // GET /api/simulacion/stream (EventSource)
```

### 2. `/src/pages/simulacion/Simulador/SimuladorSemanal.js`
**Estados agregados:**
```javascript
const [simulacionActiva, setSimulacionActiva] = useState(false);
const [horaSimulada, setHoraSimulada] = useState(null);
const [tiempoRealMs, setTiempoRealMs] = useState(0);
const [tickActual, setTickActual] = useState(0);
const [timeScale, setTimeScale] = useState(10.0);
const eventSourceRef = useRef(null);
```

**Hooks agregados:**
- `useEffect` para gestionar la conexión SSE basado en `simulacionActiva`
- Funciones handlers: `handleIniciarSimulacion`, `handlePausarSimulacion`, `handleReanudarSimulacion`, `handleDetenerSimulacion`

**UI agregada:**
- Panel de información en tiempo real con diseño moderno (degradado púrpura, glassmorphism)
- Grid responsivo con 4 tarjetas de información
- Botones de control con estados habilitados/deshabilitados según el estado de la simulación

---

## 🎯 Flujo de Funcionamiento

### **Inicio de Simulación:**
1. Usuario presiona "🚀 Iniciar"
2. Se ejecuta `POST /api/simulacion/iniciar`
3. Se establece `simulacionActiva = true`
4. El `useEffect` detecta el cambio y ejecuta `conectarStreamSimulacion()`
5. Se abre una conexión `EventSource` a `/api/simulacion/stream`
6. Cada 1 segundo se recibe un JSON con el estado actualizado
7. Los estados se actualizan en tiempo real en la UI

### **Pausa de Simulación:**
1. Usuario presiona "⏸️ Pausar"
2. Se ejecuta `POST /api/simulacion/pausar`
3. Se establece `simulacionActiva = false`
4. El `useEffect` cierra la conexión SSE (`eventSource.close()`)

### **Reanudación:**
1. Usuario presiona "▶️ Reanudar"
2. Se ejecuta `POST /api/simulacion/reanudar`
3. Se establece `simulacionActiva = true`
4. El `useEffect` reabre la conexión SSE

### **Detención:**
1. Usuario presiona "⏹️ Detener"
2. Se ejecuta `POST /api/simulacion/detener`
3. Se establece `simulacionActiva = false` y se resetean todos los estados
4. El `useEffect` cierra la conexión SSE

---

## 🔍 Estructura de Datos SSE

El stream devuelve JSON con este formato cada segundo:
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

## 🧪 Cómo Probar

### **Requisitos:**
1. Backend Spring Boot ejecutándose en `http://localhost:8080`
2. Frontend React ejecutándose en `http://localhost:3000`

### **Pasos:**
1. Accede a `http://localhost:3000/operaciones/simulador-semanal`
2. Observa el panel de control SSE en la parte superior
3. Presiona "🚀 Iniciar" para comenzar la simulación
4. Observa cómo se actualiza el tiempo simulado cada segundo
5. El indicador "● ACTIVA" se vuelve verde
6. Verás incrementarse:
   - Tiempo Simulado (en horas simuladas)
   - Tiempo Real (en segundos)
   - Ticks (número de iteraciones)

### **Verificación en Consola:**
Abre la consola del navegador (F12) y verás logs como:
```
📡 Conectando al stream SSE de simulación...
📡 SSE - Estado recibido: {horaSimulada: "2025-01-01T10:00:00", ...}
```

### **Prueba de CURL (opcional):**
```bash
# Iniciar simulación
curl -X POST http://localhost:8080/api/simulacion/iniciar

# Ver stream en terminal
curl -N http://localhost:8080/api/simulacion/stream

# Pausar
curl -X POST http://localhost:8080/api/simulacion/pausar

# Reanudar
curl -X POST http://localhost:8080/api/simulacion/reanudar

# Detener
curl -X POST http://localhost:8080/api/simulacion/detener
```

---

## 🎨 Características del Diseño

- **Gradiente moderno**: Púrpura (#667eea) a violeta (#764ba2)
- **Glassmorphism**: Efecto de vidrio esmerilado con `backdrop-filter: blur(10px)`
- **Responsive Grid**: Se adapta automáticamente a diferentes tamaños de pantalla
- **Estados visuales**: Botones deshabilitados cuando no aplican
- **Indicadores de color**: Verde para activo, rojo para inactivo
- **Fuente monospace**: Para los valores numéricos (mejor legibilidad)
- **Sombras suaves**: Para dar profundidad a las tarjetas

---

## ⚠️ Consideraciones

1. **CORS**: Asegúrate de que el backend permita peticiones desde `localhost:3000`
2. **EventSource**: Solo funciona con peticiones GET (limitación del navegador)
3. **Conexión persistente**: El SSE mantiene una conexión HTTP abierta (normal)
4. **Cleanup**: La conexión se cierra automáticamente al cambiar de página
5. **Error handling**: Si el backend no responde, verás errores en consola y el estado quedará inactivo

---

## 🚀 Próximos Pasos Sugeridos

- [ ] Sincronizar la actualización de vuelos con el tiempo simulado
- [ ] Agregar indicador de latencia de red
- [ ] Implementar reconexión automática en caso de pérdida de conexión
- [ ] Agregar gráfica de línea temporal visual
- [ ] Guardar historial de eventos de la simulación

---

## 📝 Notas Técnicas

- **Tiempo real vs Simulado**: 1 segundo real = 10 horas simuladas (configurable en backend)
- **Formato de fecha**: Se usa `toLocaleString('es-ES')` para formato español
- **Gestión de memoria**: La conexión SSE se cierra correctamente en el cleanup del useEffect
- **Performance**: El panel se actualiza eficientemente sin re-renders innecesarios

---

✅ **Integración completada exitosamente** - El simulador ahora está sincronizado en tiempo real con el backend usando SSE.
