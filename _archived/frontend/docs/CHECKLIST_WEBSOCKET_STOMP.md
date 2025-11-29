# ✅ CHECKLIST DE VERIFICACIÓN - WebSocket STOMP

## 📋 Antes de Probar

### Backend (Spring Boot)

- [ ] Backend corriendo en `http://localhost:8000`
- [ ] Endpoint `/api/simulations/start` responde correctamente
- [ ] Endpoint `/api/simulations/{sessionId}/cancel` funcional
- [ ] WebSocket endpoint `/ws` configurado con SockJS
- [ ] CORS habilitado para `http://localhost:3000`
- [ ] Topic `/topic/simulations/{sessionId}` configurado
- [ ] Algoritmo Genético envía mensajes de progreso

### Frontend (React)

- [ ] Dependencias instaladas:
  ```bash
  npm install sockjs-client @stomp/stompjs
  ```
- [ ] `SimuladorSemanal.js` modificado con las nuevas funciones
- [ ] `WebSocketStomp.css` creado e importado
- [ ] Frontend corriendo en `http://localhost:3000`
- [ ] Ruta a SimuladorSemanal accesible

---

## 🧪 Pruebas Funcionales

### 1. Prueba de Conexión

- [ ] Abrir SimuladorSemanal
- [ ] Verificar que aparece el panel morado "WebSocket STOMP"
- [ ] Click en "🔌 Conectar WebSocket"
- [ ] Esperar 1-2 segundos
- [ ] Verificar indicador cambia a: 🟢 **CONECTADO**
- [ ] Verificar en consola: `✅ WebSocket STOMP conectado`
- [ ] Verificar mensaje en log: `✅ Conexión WebSocket establecida`

**❌ Si falla:**
- Verificar que backend esté corriendo
- Verificar URL en código: `http://localhost:8000/ws`
- Verificar CORS en backend
- Ver errores en consola del navegador

---

### 2. Prueba de Inicio de Simulación

- [ ] Seleccionar fecha en el input (ej: 2025-01-02)
- [ ] Verificar que el botón "🚀 Iniciar Simulación Semanal" esté habilitado
- [ ] Click en "Iniciar Simulación Semanal"
- [ ] Verificar en consola: `🚀 Iniciando simulación semanal...`
- [ ] Verificar en consola: `📨 Respuesta del servidor: {...}`
- [ ] Verificar que se recibe un `sessionId`
- [ ] Verificar en consola: `📡 Suscribiéndose a: /topic/simulations/...`
- [ ] Verificar mensaje en log: `✅ Simulación iniciada - Session ID: ...`
- [ ] Verificar mensaje en log: `📡 Suscrito a: /topic/simulations/...`

**❌ Si falla:**
- Verificar que la fecha esté seleccionada
- Verificar endpoint REST: `POST /api/simulations/start`
- Verificar formato de respuesta del backend (debe tener `sessionId`)
- Ver errores en consola

---

### 3. Prueba de Recepción de Mensajes

- [ ] Esperar 1-5 segundos después de iniciar
- [ ] Verificar que llegan mensajes en la consola: `📨 Mensaje recibido: {...}`
- [ ] Verificar que el log muestra mensajes nuevos cada pocos segundos
- [ ] Verificar formato de mensaje: debe tener `tipo: 'PROGRESO_AG'` o `status: 'RUNNING'`

**Tipos de mensajes esperados:**

#### A) Progreso del AG
```json
{
  "tipo": "PROGRESO_AG",
  "generacion": 1,
  "maxGeneraciones": 20,
  "progreso": 5.0,
  "mejorFitness": 1050.23,
  ...
}
```

- [ ] Verificar mensaje en log: `🧬 Generación X/Y - Fitness: ... - Progreso: ...%`

#### B) Snapshot de Simulación
```json
{
  "status": "RUNNING",
  "iteration": 42,
  "processedOrders": 150,
  ...
}
```

- [ ] Verificar mensaje en log: `🎮 Iteración X - Y/Z pedidos`

**❌ Si no llegan mensajes:**
- Verificar que el backend esté enviando a `/topic/simulations/{sessionId}` correcto
- Verificar en consola si hay errores de suscripción
- Verificar formato JSON de los mensajes del backend

---

### 4. Prueba de Panel de Progreso

- [ ] Verificar que aparece el panel blanco "🧬 Progreso del Algoritmo Genético"
- [ ] Verificar que la barra de progreso se actualiza (0% → 100%)
- [ ] Verificar que las 4 tarjetas de métricas muestran datos:
  - **Generación**: debe mostrar "X / Y"
  - **Mejor Fitness**: debe mostrar número decimal
  - **Fitness Promedio**: debe mostrar número decimal
  - **Pedidos Procesados**: debe mostrar "X / Y"
- [ ] Verificar que los valores cambian con cada mensaje recibido

**❌ Si no se actualiza:**
- Verificar en consola: `setProgresoAG(...)`
- Verificar que los mensajes tienen los campos correctos
- Verificar que no hay errores de parsing JSON

---

### 5. Prueba de Graficación de Vuelos

- [ ] Esperar a que lleguen mensajes con `solucion.rutas`
- [ ] Verificar en consola: `✈️ Procesando N rutas...`
- [ ] Verificar en consola: `✅ Procesados N vuelos del AG`
- [ ] Verificar que aparecen **íconos de aviones verdes** en el mapa
- [ ] Verificar que los aviones se mueven gradualmente
- [ ] Verificar que la rotación de los aviones es correcta

**❌ Si no aparecen vuelos:**
- Verificar en consola: `⚠️ Aeropuertos no encontrados: ...`
- Verificar que los códigos ICAO del backend coinciden con la BD
- Verificar en consola: `📍 Aeropuertos disponibles: ...`
- Verificar que los aeropuertos están cargados (`airports.length > 0`)

---

### 6. Prueba de Log de Eventos

- [ ] Verificar que el panel "📝 Log de Eventos" muestra mensajes
- [ ] Verificar que cada mensaje tiene:
  - Timestamp (ej: "14:30:45")
  - Texto descriptivo
  - Color según tipo (verde, rojo, amarillo, azul)
- [ ] Verificar que los mensajes más recientes están arriba
- [ ] Verificar que el log es scrolleable
- [ ] Verificar que no hay más de 50 mensajes (límite)

**❌ Si el log está vacío:**
- Verificar que la función `agregarMensaje()` se está llamando
- Verificar en consola: logs de la función
- Verificar estado: `mensajesSimulacion`

---

### 7. Prueba de Cancelación

- [ ] Durante una simulación activa, click en "🛑 Cancelar Simulación"
- [ ] Verificar en consola: `🛑 Cancelando simulación...`
- [ ] Verificar que se hace POST a `/api/simulations/{sessionId}/cancel`
- [ ] Verificar mensaje en log: `🛑 Simulación cancelada`
- [ ] Verificar que dejan de llegar mensajes
- [ ] Verificar que el botón "Cancelar" se deshabilita

**❌ Si falla:**
- Verificar que hay un `sessionId` activo
- Verificar endpoint de cancelación en backend
- Ver errores en consola

---

### 8. Prueba de Limpieza

- [ ] Click en "🧹 Limpiar Todo"
- [ ] Verificar en consola: `🧹 Todo limpiado`
- [ ] Verificar que el log se vacía
- [ ] Verificar que el panel de progreso desaparece
- [ ] Verificar que los vuelos verdes desaparecen del mapa
- [ ] Verificar que el estado vuelve a: `connected`

**❌ Si no se limpia:**
- Verificar función `limpiarTodoStomp()`
- Verificar que todos los `setState()` se ejecutan

---

### 9. Prueba de Desconexión

- [ ] Click en "🔌 Desconectar"
- [ ] Verificar en consola: `🔌 WebSocket STOMP desconectado completamente`
- [ ] Verificar indicador cambia a: 🔴 **DESCONECTADO**
- [ ] Verificar que el botón "Conectar WebSocket" aparece de nuevo
- [ ] Verificar que los otros botones desaparecen

**❌ Si no se desconecta:**
- Verificar función `desconectarWebSocketStomp()`
- Verificar que `stompClient.deactivate()` se llama

---

### 10. Prueba de Simulación Completa

- [ ] Iniciar una simulación nueva
- [ ] Dejar que corra hasta que el backend envíe `status: 'COMPLETED'`
- [ ] Verificar mensaje en log: `🎉 Simulación completada exitosamente`
- [ ] Verificar que el estado cambia a: `completed`
- [ ] Verificar que la barra de progreso llega a 100%
- [ ] Verificar que todos los vuelos finales están graficados

**❌ Si no completa:**
- Verificar que el backend envía mensaje de completado
- Verificar formato: `{ status: 'COMPLETED', ... }`

---

## 🔍 Verificación de Logs

### Consola del Navegador (F12 → Console)

**Secuencia esperada:**

```
📡 Conectando WebSocket STOMP...
✅ WebSocket STOMP conectado
🚀 Iniciando simulación semanal con WebSocket STOMP...
📨 Respuesta del servidor: {sessionId: "...", ...}
📡 Suscribiéndose a: /topic/simulations/abc-123
📨 Mensaje recibido: {tipo: "PROGRESO_AG", ...}
🧬 Progreso AG - Generación 1/20
✈️ Procesando 15 rutas...
✅ Procesados 15 vuelos del AG
📨 Mensaje recibido: {tipo: "PROGRESO_AG", ...}
🧬 Progreso AG - Generación 2/20
✈️ Procesando 18 rutas...
✅ Procesados 18 vuelos del AG
...
📨 Mensaje recibido: {status: "COMPLETED", ...}
🎉 Simulación completada exitosamente
```

---

## 🎨 Verificación Visual

### Panel WebSocket STOMP

- [ ] Degradado morado visible
- [ ] Título: "🌐 WebSocket STOMP - Simulación en Tiempo Real"
- [ ] Indicador de estado circular pulsante
- [ ] Botones con íconos (🔌, 🚀, 🛑, 🧹)
- [ ] Efecto hover en botones (se elevan ligeramente)

### Panel de Progreso

- [ ] Fondo blanco con bordes redondeados
- [ ] Título: "🧬 Progreso del Algoritmo Genético"
- [ ] Barra de progreso con gradiente verde-azul
- [ ] Porcentaje dentro de la barra
- [ ] 4 tarjetas con fondo gris claro
- [ ] Números en negrita y grandes

### Log de Eventos

- [ ] Fondo blanco
- [ ] Scroll vertical funcionando
- [ ] Mensajes con bordes de colores (verde, rojo, amarillo, azul)
- [ ] Timestamps en gris pequeño
- [ ] Animación de entrada (slideIn)

---

## 🚨 Problemas Comunes

### Error: "WebSocket no conectado"

**Solución:**
1. Click en "Conectar WebSocket" primero
2. Esperar 1-2 segundos
3. Verificar indicador verde

---

### Error: "No se recibió sessionId"

**Solución:**
1. Verificar respuesta del backend:
   ```bash
   curl -X POST http://localhost:8000/api/simulations/start \
     -H "Content-Type: application/json" \
     -d '{"fecha":"2025-01-02","factorK":5}'
   ```
2. Debe retornar: `{"sessionId": "...", ...}`

---

### Error: No aparecen vuelos en el mapa

**Solución:**
1. Abrir consola (F12)
2. Buscar: `⚠️ Aeropuertos no encontrados: ...`
3. Verificar que los códigos ICAO coinciden
4. Verificar: `console.log('📍 Aeropuertos disponibles:', airports.map(a => a.code))`

---

### Error: "Error STOMP"

**Solución:**
1. Verificar CORS en backend
2. Verificar URL: `http://localhost:8000/ws`
3. Verificar que SockJS está habilitado en backend

---

## ✅ Checklist de Éxito Total

Al finalizar todas las pruebas, debes tener:

- [x] ✅ Conexión WebSocket establecida
- [x] ✅ Simulación iniciada correctamente
- [x] ✅ Mensajes recibidos en tiempo real
- [x] ✅ Barra de progreso actualizándose
- [x] ✅ Métricas del AG actualizándose
- [x] ✅ Vuelos verdes graficados en el mapa
- [x] ✅ Log con timestamps y colores
- [x] ✅ Cancelación funcional
- [x] ✅ Limpieza de estado funcional
- [x] ✅ Desconexión limpia
- [x] ✅ Sin errores en consola
- [x] ✅ Sin memory leaks

---

## 📊 Métricas de Rendimiento

- **Tiempo de conexión**: < 2 segundos
- **Latencia de mensajes**: < 500ms
- **FPS del mapa**: > 30fps
- **Memoria usada**: < 100MB adicionales
- **Sin errores**: durante 10 minutos de simulación

---

## 🎓 Conclusión

Si todos los checkboxes están marcados, la implementación es **100% funcional** ✅

**Fecha de verificación**: __________  
**Verificado por**: __________  
**Estado**: [ ] ✅ Aprobado  [ ] ❌ Requiere correcciones

---

**🚀 ¡Sistema listo para producción!**
