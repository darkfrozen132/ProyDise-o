# ✅ CHECKLIST DE VERIFICACIÓN - Movimiento de Aviones

## 🔍 PASOS PARA VERIFICAR

### 1️⃣ **Recargar Navegador**

```bash
# En el navegador:
Ctrl + Shift + R  (Windows/Linux)
Cmd + Shift + R   (Mac)
```

**Por qué:** Los cambios en JavaScript necesitan limpieza de caché

---

### 2️⃣ **Abrir Consola del Navegador**

```bash
F12 o Ctrl + Shift + I
```

**Ir a la pestaña "Console"**

---

### 3️⃣ **Iniciar Backend (si no está corriendo)**

```bash
cd backend
mvn spring-boot:run
```

**Esperar:** `Started BackendApplication in X seconds`

---

### 4️⃣ **Iniciar Frontend (si no está corriendo)**

```bash
cd front
npm start
```

**Esperar:** Navegador abre en `http://localhost:3000`

---

### 5️⃣ **Ir al Simulador**

1. Click en "Simulación" o "Simulador Semanal"
2. Seleccionar fecha de inicio
3. Click en "Iniciar Simulación"

---

### 6️⃣ **VERIFICAR LOGS EN CONSOLA**

#### ✅ Logs Esperados (Orden):

```
🕐 Iniciando intervalo de tiempo simulado
🚀 Iniciando simulación para 2025-XX-XX
📨 Respuesta del servidor: {sessionId: "..."}
✅ Simulación iniciada - Session ID: ...
📡 Suscrito a: /topic/simulations/...
🧬 Progreso AG - Generación 1/10
⏰ Backend - Tiempo simulado actualizado: 2025-XX-XXT00:00:00Z
⏰ Tiempo simulado: 2025-XX-XXT00:05:23Z
   Base: 2025-XX-XXT00:00:00Z
   Δ Real: 10.2s
   Velocidad: 500x
✈️ Re-calculando posiciones de 45 vuelos
   Tiempo simulado: 2025-XX-XXT00:05:23Z
🛫 Aviones en el aire: 12/45
```

#### ❌ Logs de Error (NO deberían aparecer):

```
❌ Error: Cannot read property 'current' of undefined
❌ WebSocket error
❌ TypeError: vuelosEnMovimiento is not defined
```

---

### 7️⃣ **VERIFICAR MAPA**

#### ✅ Comportamiento Esperado:

- [ ] **Aviones aparecen** en el mapa (iconos azules/rojos/verdes)
- [ ] **Aviones SE MUEVEN** suavemente entre aeropuertos
- [ ] **Rotación correcta** (aviones apuntan hacia su destino)
- [ ] **Popups funcionan** (click en avión muestra info)
- [ ] **Contador actualiza** ("Aviones en el aire: X/Y")

#### ❌ Problemas (NO deberían ocurrir):

- [ ] Aviones congelados (no se mueven)
- [ ] Aviones "saltan" bruscamente
- [ ] Aviones desaparecen
- [ ] Mapa vacío (sin aviones)
- [ ] Consola llena de errores rojos

---

### 8️⃣ **VERIFICAR REACT DEVTOOLS** (Opcional)

1. Abrir React Developer Tools (extensión de navegador)
2. Buscar componente `SimuladorSemanal`
3. Ver "Hooks" en la sección derecha

#### Estados a Verificar:

```
State:
  tiempoSimulado: 1747267523000  ← Debe cambiar cada 50ms
  tiempoMovimiento: 12450        ← Debe incrementarse
  tiempoSimuladoBackend: 1747267200000  ← Base fija
  ultimaActualizacionReal: 1699200000000  ← Base fija
  speedMultiplier: 500
  flights: Array(45)             ← Base de vuelos
  vuelosEnMovimiento: [...]      ← NO EXISTE (calculado con useMemo)
```

---

### 9️⃣ **PRUEBAS ADICIONALES**

#### Prueba 1: Limpiar Mapa
```
1. Click en "Limpiar Mapa" o "Reset"
2. Verificar:
   - Aviones desaparecen
   - Contador vuelve a 0
   - Consola muestra: "✅ Mapa limpiado y simulación reseteada"
```

#### Prueba 2: Nueva Simulación
```
1. Cambiar fecha
2. Click en "Iniciar Simulación" nuevamente
3. Verificar:
   - Nuevos aviones aparecen
   - Tiempo simulado se resetea
   - Animación inicia correctamente
```

#### Prueba 3: Pausar/Reanudar (si hay botones)
```
1. Click en "Pausar"
2. Verificar: Aviones se detienen
3. Click en "Reanudar"
4. Verificar: Aviones continúan moviéndose
```

---

## 🐛 TROUBLESHOOTING

### Problema 1: "Aviones no se mueven"

**Verificar:**
1. ¿El backend está corriendo? (puerto 8000)
2. ¿WebSocket conectado? (busca "✅ WebSocket conectado" en consola)
3. ¿Hay vuelos en el array? (`console.log(flights.length)`)
4. ¿`tiempoSimulado` cambia? (React DevTools)

**Solución:**
```bash
# Recargar navegador
Ctrl + Shift + R

# Reiniciar backend
cd backend
mvn spring-boot:run

# Limpiar caché de npm (último recurso)
cd front
rm -rf node_modules package-lock.json
npm install
npm start
```

---

### Problema 2: "Error: tiempoSimuladoRef is not defined"

**Causa:** Caché viejo del navegador

**Solución:**
```bash
# 1. Limpiar caché del navegador
Ctrl + Shift + Delete

# 2. Seleccionar:
   - Imágenes y archivos en caché
   - Últimos 7 días

# 3. Limpiar

# 4. Recargar página
Ctrl + Shift + R
```

---

### Problema 3: "Consola muestra errores rojos"

**Verificar:**
1. Copiar el error completo
2. Buscar en el código la línea mencionada
3. Verificar que no queden referencias a:
   - `tiempoSimuladoRef.current`
   - `tiempoSimuladoBackendRef.current`
   - `ultimaActualizacionRealRef.current`

**Solución:**
```bash
# Buscar refs viejas en el código
grep -n "tiempoSimuladoRef.current" front/src/pages/simulacion/Simulador/SimuladorSemanal.js
grep -n "tiempoSimuladoBackendRef.current" front/src/pages/simulacion/Simulador/SimuladorSemanal.js

# Si encuentra algo, reemplazar manualmente con estado
```

---

### Problema 4: "Backend no responde"

**Verificar:**
```bash
# 1. Backend corriendo
curl http://localhost:8000/api/aeropuertos/listar

# 2. WebSocket disponible
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" http://localhost:8000/ws

# 3. Ver logs del backend
# (En la terminal donde corre mvn spring-boot:run)
```

---

## ✅ RESULTADO FINAL ESPERADO

Cuando todo funciona correctamente:

```
✅ Backend corriendo en puerto 8000
✅ Frontend corriendo en puerto 3000
✅ WebSocket conectado
✅ Simulación iniciada
✅ Aviones aparecen en el mapa
✈️ AVIONES SE MUEVEN SUAVEMENTE
✅ Tiempo simulado avanza
✅ Contador actualiza dinámicamente
✅ Logs claros sin errores
```

---

## 📊 MÉTRICAS DE ÉXITO

| Métrica | Valor Esperado | ¿Cumple? |
|---------|---------------|----------|
| **Movimiento** | Aviones se mueven suave | [ ] |
| **Performance** | 20 FPS (50ms/frame) | [ ] |
| **Logs** | Sin errores rojos | [ ] |
| **Tiempo** | Avanza automáticamente | [ ] |
| **Contador** | Actualiza dinámicamente | [ ] |
| **Rotación** | Aviones apuntan correcto | [ ] |

---

## 🎉 SI TODO FUNCIONA

**¡FELICITACIONES!** 🎊

Los aviones ahora se mueven usando **estado reactivo de React** en lugar de refs.

### Beneficios Logrados:

1. ✅ **Movimiento suave** de aviones
2. ✅ **Re-render automático** (sin hacks)
3. ✅ **Performance óptima** (20 FPS)
4. ✅ **Código mantenible** (más simple)
5. ✅ **Debugging fácil** (estado visible en DevTools)

---

## 📝 NOTA FINAL

Si encuentras algún problema:

1. **Lee los logs de la consola** (F12)
2. **Verifica React DevTools** (estados)
3. **Revisa `CAMBIOS_IMPLEMENTADOS_MOVIMIENTO_AVIONES.md`** (detalles)
4. **Compara con `CODIGO_APARTE`** (referencia)

---

**¿Todo funciona?** ¡Hora de celebrar! 🚀✈️🎉
