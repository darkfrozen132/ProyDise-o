# 🔌 Integración WebSocket de Planificación - Implementado

## ✅ Lo que se ha implementado

### 1. Archivo: `src/config/websocket.js`

Se agregaron dos nuevas funciones para WebSocket de planificación:

#### `conectarWebSocketPlanificacion(onMessage, onError, onOpen, onClose)`
Conecta al WebSocket en `ws://localhost:8080/ws/planificacion` según especificación.

**Métodos disponibles:**
```javascript
ws.iniciarPlanificacion(fecha, factorK, opciones)
ws.enviar(mensaje)
ws.estaConectado()
ws.cerrar()
```

#### `useWebSocketPlanificacion(onMessage, onError)` 
Hook de React que retorna `{ ws, isConnected, iteraciones }`

---

### 2. Archivo: `src/pages/simulacion/Simulador/SimuladorSemanal.js`

Se agregó un panel completo de WebSocket de planificación con:

#### Estados agregados:
```javascript
const [wsPlanificacion, setWsPlanificacion] = useState(null);
const [wsConectado, setWsConectado] = useState(false);
const [iteracionesPlanificacion, setIteracionesPlanificacion] = useState([]);
const [estadoPlanificacion, setEstadoPlanificacion] = useState('idle');
const wsPlanificacionRef = useRef(null);
```

#### Funciones agregadas:
- `handleConectarWsPlanificacion()` - Conectar al WebSocket
- `handleDesconectarWsPlanificacion()` - Desconectar
- `handleIniciarPlanificacion()` - Iniciar planificación con fecha y factorK
- `handleLimpiarIteraciones()` - Limpiar historial

#### Panel UI agregado:
- **Indicadores de estado**: Conexión, estado de planificación, contador de iteraciones
- **4 botones de control**: Conectar, Desconectar, Iniciar Planificación, Limpiar
- **Lista de últimas 5 iteraciones** con:
  - Número de iteración
  - Duración en milisegundos
  - Cantidad de vuelos y pedidos
  - Tiempo simulado actual
  - Fitness (si está disponible)

---

## 🚀 Cómo usar

### Paso 1: Asegúrate de que el backend esté corriendo

```bash
# El backend debe estar en:
ws://localhost:8080/ws/planificacion
```

### Paso 2: Ir a la página de simulación

Navega a: `/operaciones/simulador-semanal`

### Paso 3: Usar el panel de WebSocket

1. **Selecciona una fecha de inicio** en el formulario de simulación
2. **Haz clic en "🔌 Conectar"** - El indicador cambiará a verde
3. **Haz clic en "▶️ Iniciar Planificación"**
4. **Observa las iteraciones** aparecer en tiempo real en el panel inferior

---

## 📊 Formato de mensajes

### Mensajes que envía el cliente:

```javascript
{
  "accion": "iniciar",
  "fecha": "2025-01-02",
  "factorK": 14,
  "tamanioPoblacion": 20,
  "maxGeneraciones": 20,
  "limiteGeneracionesSinMejora": 10
}
```

### Mensajes que recibe del servidor:

#### 1. Conexión
```json
{
  "tipo": "conexion",
  "mensaje": "Conexión establecida",
  "timestamp": "2025-01-02T10:30:00"
}
```

#### 2. Progreso
```json
{
  "tipo": "progreso",
  "datos": {
    "ejecucionNumero": 1,
    "tiempoSimulacionActual": "2025-01-02T00:00",
    "duracionRealMs": 2345,
    "avanceSimuladoMin": 25
  },
  "solucion": {
    "vuelos": [...],
    "totalVuelos": 2,
    "totalPedidos": 5
  }
}
```

#### 3. Completado
```json
{
  "tipo": "completado",
  "mensaje": "Planificación #1 completada",
  "solucion": {
    "vuelos": [...],
    "totalVuelos": 2,
    "totalPedidos": 2
  }
}
```

#### 4. Error
```json
{
  "tipo": "error",
  "mensaje": "Descripción del error",
  "timestamp": "2025-01-02T10:30:00"
}
```

---

## 🎨 Captura del panel

El panel se ve así:

```
┌────────────────────────────────────────────────────────┐
│ 🔌 WebSocket Planificación en Tiempo Real             │
├────────────────────────────────────────────────────────┤
│ 🟢 Conectado   ⚙️ Ejecutando   📊 5 iteraciones       │
├────────────────────────────────────────────────────────┤
│ [🔌 Conectar] [🔌 Desconectar] [▶️ Iniciar] [🗑️ Limpiar] │
├────────────────────────────────────────────────────────┤
│ 📜 Últimas Iteraciones:                                │
│ ┌────────────────────────────────────────────────────┐ │
│ │ ⚙️ Iteración #5                        1234ms      │ │
│ │ ✈️ 3 vuelos • 📦 7 pedidos                         │ │
│ │ ⏰ 2025-01-02T02:05                                │ │
│ └────────────────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────────────────┐ │
│ │ ⚙️ Iteración #4                        1156ms      │ │
│ │ ✈️ 2 vuelos • 📦 5 pedidos                         │ │
│ │ ⏰ 2025-01-02T01:40                                │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 🔧 Configuración de velocidad

El código usa estos parámetros predeterminados (puedes cambiarlos en `handleIniciarPlanificacion`):

```javascript
{
  factorK: 14,                           // Amplificación temporal
  tamanioPoblacion: 20,                  // Población del AG
  maxGeneraciones: 20,                   // Máximo de generaciones
  limiteGeneracionesSinMejora: 10        // Límite sin mejora
}
```

### Presets recomendados:

**⚡ Ultra Rápido** (para testing):
```javascript
{ factorK: 14, tamanioPoblacion: 10, maxGeneraciones: 10, limiteGeneracionesSinMejora: 5 }
```

**⚖️ Balance** (recomendado):
```javascript
{ factorK: 14, tamanioPoblacion: 20, maxGeneraciones: 20, limiteGeneracionesSinMejora: 10 }
```

**🐢 Calidad Máxima** (más lento):
```javascript
{ factorK: 14, tamanioPoblacion: 50, maxGeneraciones: 50, limiteGeneracionesSinMejora: 20 }
```

---

## 🛠️ Troubleshooting

### Problema: "WebSocket no conecta"
**Causa**: Backend no está corriendo o puerto incorrecto  
**Solución**: 
```bash
# Verifica que el backend esté corriendo
curl http://localhost:8080/actuator/health
```

### Problema: "Primero debes conectar el WebSocket"
**Causa**: Intentas iniciar planificación sin conectar  
**Solución**: Haz clic en "🔌 Conectar" primero

### Problema: "Por favor, selecciona una fecha de inicio"
**Causa**: No has seleccionado fecha en el formulario  
**Solución**: Selecciona una fecha en el campo "Fecha Inicio Simulación"

### Problema: No recibo iteraciones
**Causa**: Formato de mensaje incorrecto del backend  
**Solución**: Verifica en la consola del navegador (F12) los mensajes que llegan. Deben tener campo `tipo`.

---

## 📝 Próximos pasos (opcional)

1. **Integrar con el mapa**: Actualizar vuelos en tiempo real con las soluciones recibidas
2. **Agregar reconexión automática**: Si se cae la conexión
3. **Mostrar gráficos**: Fitness vs iteración
4. **Exportar resultados**: Descargar planificación en JSON/CSV
5. **Pausar/Reanudar**: Implementar control de la planificación

---

## 📚 Archivos modificados

1. ✅ `/src/config/websocket.js` - Agregadas funciones de planificación
2. ✅ `/src/pages/simulacion/Simulador/SimuladorSemanal.js` - Panel UI completo

---

## 🎯 Estado actual

✅ **WebSocket implementado**  
✅ **Panel UI funcional**  
✅ **Indicadores de estado**  
✅ **Botones de control**  
✅ **Lista de iteraciones en tiempo real**  
✅ **Manejo de errores**  
✅ **Cleanup automático al desmontar**  

**Listo para probar con el backend! 🚀**

---

**Fecha de implementación**: 19 de noviembre de 2025  
**Basado en**: `websocket.md` - Especificación de WebSocket de Planificación
