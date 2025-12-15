# 🎯 INSTRUCCIONES: Planificación Simple REST

## ⚡ Inicio Rápido (3 pasos)

### 1. Iniciar Backend
```bash
cd /home/leoncio/Documentos/GitHub/ProyDise-o/backend
mvn spring-boot:run
```

### 2. Iniciar Frontend
```bash
cd /home/leoncio/Documentos/GitHub/ProyDise-o/front
npm start
```

### 3. Abrir en Navegador
```
http://localhost:3001/operaciones/simulador-simple
```

---

## 🧪 Probar que Funciona

### Opción A: Script Automático
```bash
cd /home/leoncio/Documentos/GitHub/ProyDise-o
./test_planificacion_simple.sh
```

### Opción B: Curl Manual
```bash
curl -X POST "http://localhost:8000/api/planificacion/ejecutar-simple?fecha=2025-01-15&factorK=5" | jq
```

---

## 💡 Cómo Usar la Aplicación

1. **Seleccionar fecha** → Ejemplo: `2025-01-15`
2. **Click en "🚀 Iniciar Simulación"**
3. **Esperar** → Verás "⏳ Cargando..."
4. **Ver resultados** → Vuelos aparecen en el mapa
5. **Limpiar** → Click en "🧹 Limpiar" para reiniciar

---

## 📋 ¿Qué Cambió?

### ❌ Antes (WebSocket Complejo)
- Conexión WebSocket persistente
- Múltiples mensajes: `inicio`, `progreso`, `completado`
- Estado complejo con `EstadoPlanificacion`
- Iteraciones automáticas
- Difícil de depurar

### ✅ Ahora (REST Simple)
- Una sola llamada HTTP POST
- Respuesta con TODOS los vuelos
- Sin estado (stateless)
- Una sola ejecución
- Fácil de probar y depurar

---

## 📂 Archivos Nuevos

```
backend/src/main/java/.../controller/
  └── PlanificacionSimpleController.java   ← REST controller

front/src/components/
  ├── SimuladorSimple.js                   ← Componente React
  └── SimuladorSimple.css                  ← Estilos

front/src/App.js                           ← Ruta agregada

test_planificacion_simple.sh               ← Script de prueba
SOLUCION_SIMPLE_REST.md                    ← Documentación completa
INSTRUCCIONES_RAPIDAS.md                   ← Este archivo
```

---

## 🐛 Solución de Problemas

### Backend no arranca
```bash
# Verificar puerto 8000
sudo lsof -i :8000

# Si está ocupado, matar proceso
kill -9 <PID>
```

### Frontend no arranca
```bash
# Verificar puerto 3001
sudo lsof -i :3001

# Reinstalar dependencias
cd front
rm -rf node_modules
npm install
```

### No se muestran vuelos
1. Abrir DevTools → Console
2. Buscar mensajes con emojis (📍, ✅, ❌)
3. Verificar que aeropuertos cargaron
4. Verificar respuesta del backend

---

## 📊 Logs Esperados

### Backend (Terminal)
```
═══════════════════════════════════════════════════════════
🚀 PLANIFICACIÓN SIMPLE INICIADA
📅 Fecha: 2025-01-15
⚙️ Factor K: 5
═══════════════════════════════════════════════════════════
✅ PLANIFICACIÓN COMPLETADA
⏱️ Duración: 3547 ms (3 segundos)
✈️ Vuelos generados: 150
📦 Pedidos planificados: 450
═══════════════════════════════════════════════════════════
```

### Frontend (Console)
```
═══════════════════════════════════════════════════════════
🚀 INICIANDO SIMULACIÓN SIMPLE
📅 Fecha: 2025-01-15
═══════════════════════════════════════════════════════════
📍 Cargando aeropuertos...
✅ 30 aeropuertos cargados
✅ Respuesta recibida en 3547 ms
🔄 Procesando 150 vuelos...
✅ 150 vuelos con coordenadas válidas
═══════════════════════════════════════════════════════════
✅ SIMULACIÓN COMPLETADA
✈️ Vuelos: 150
⏱️ Duración: 3547 ms
═══════════════════════════════════════════════════════════
```

---

## 🎨 Interfaz Visual

```
╔══════════════════════════════════════════════════════════╗
║          ✈️ Simulador Semanal Simple                    ║
╠══════════════════════════════════════════════════════════╣
║                                                          ║
║  📅 Fecha: [2025-01-15]  [🚀 Iniciar]  [🧹 Limpiar]    ║
║                                                          ║
║  ┌────────────────────────────────────────────────┐    ║
║  │ ✅ 150 vuelos planificados                      │    ║
║  └────────────────────────────────────────────────┘    ║
║                                                          ║
║  ┌────────────────────────────────────────────────┐    ║
║  │ ✈️ 150  📦 450  ⏱️ 3.5s                        │    ║
║  └────────────────────────────────────────────────┘    ║
║                                                          ║
║  ╔════════════════════════════════════════════════╗    ║
║  ║                                                ║    ║
║  ║             🗺️ MAPA INTERACTIVO               ║    ║
║  ║                                                ║    ║
║  ║   🔴 Aeropuertos                               ║    ║
║  ║   ━━━ Rutas de vuelos                         ║    ║
║  ║   ✈️  Vuelos activos                          ║    ║
║  ║                                                ║    ║
║  ╚════════════════════════════════════════════════╝    ║
║                                                          ║
╚══════════════════════════════════════════════════════════╝
```

---

## ⚙️ Configuración Opcional

### Cambiar parámetros del algoritmo

En `SimuladorSimple.js`, línea 145:
```javascript
params: {
  fecha: fechaSeleccionada,
  factorK: 5,              // ← Ajustar aquí
  tamanioPoblacion: 20,    // ← Ajustar aquí
  maxGeneraciones: 20,     // ← Ajustar aquí
  limiteGeneracionesSinMejora: 10  // ← Ajustar aquí
}
```

### Cambiar puerto del backend

En `SimuladorSimple.js`, línea 142:
```javascript
const response = await axios.post(
  'http://localhost:8000/api/planificacion/ejecutar-simple',  // ← Cambiar puerto
```

---

## 📞 Contacto

Si algo no funciona:
1. Revisar logs del backend (terminal donde corre Spring Boot)
2. Revisar logs del frontend (DevTools → Console)
3. Ejecutar script de prueba: `./test_planificacion_simple.sh`
4. Leer documentación completa: `SOLUCION_SIMPLE_REST.md`

---

## ✅ Checklist de Verificación

- [ ] Backend corriendo en puerto 8000
- [ ] Frontend corriendo en puerto 3001
- [ ] Navegador abierto en `/operaciones/simulador-simple`
- [ ] Script de prueba ejecutado exitosamente
- [ ] Aeropuertos visibles en el mapa (marcadores rojos)
- [ ] Al hacer click en "Iniciar", aparecen vuelos (líneas de colores)

---

**¡Listo para usar!** 🚀
