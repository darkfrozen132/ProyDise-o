# 🔍 DIAGNÓSTICO RÁPIDO DEL PROBLEMA

## Estado Actual

### ✅ Backend
- **Puerto:** 8000
- **Estado:** CORRIENDO ✅
- **Aeropuertos:** 30 aeropuertos cargados
- **Planes de vuelo:** 2866 cargados
- **Pedidos PENDIENTE:** 4440
- **WebSocket:** Configurado en `/ws/planificacion`

### ✅ Frontend
- **Puerto:** 3001 (NO 3000!) ⚠️
- **Estado:** CORRIENDO ✅
- **URL:** http://localhost:3001

### 🔴 PROBLEMA DETECTADO

**En tu captura de pantalla veo:**
1. ✅ El mapa SE MUESTRA correctamente
2. ✅ Los aeropuertos (puntos rojos) SE MUESTRAN
3. ❌ NO hay aviones volando (vuelos)
4. ❌ El estado dice "Detenida"
5. ⚠️ La fecha está en "02/01/2025" pero el tiempo está en "22/01/2025, 05:00:00"

## 🔍 CAUSAS POSIBLES

### 1. WebSocket no conectado
**Síntoma:** No se inicia automáticamente
**Verificar en consola del navegador (F12):**
```
¿Aparece?: 🔌 Auto-conectando WebSocket de planificación...
¿Aparece?: 🔌 WebSocket Planificación conectado
```

### 2. Auto-inicio no funcionando  
**Síntoma:** Fecha seleccionada pero planificación no inicia
**Verificar en consola:**
```
¿Aparece?: 🚀 Auto-iniciando planificación...
```

### 3. No hay pedidos para esa fecha
**Síntoma:** Backend responde pero sin vuelos
**Verificar en consola:**
```
¿Aparece?: ⚠️ Sin vuelos en iteración X
```

## 🛠️ SOLUCIÓN PASO A PASO

### Paso 1: Abrir Consola del Navegador
1. Presiona **F12** en el navegador
2. Ve a la pestaña **Console**
3. Busca mensajes con 🔌 o ❌

### Paso 2: Verificar WebSocket
En la consola deberías ver:
```javascript
✅ 🔌 Auto-conectando WebSocket de planificación...
✅ 🔌 WebSocket Planificación conectado: ws://localhost:8000/ws/planificacion
✅ ✅ Conexión establecida
```

Si NO ves estos mensajes, hay un problema de conexión.

### Paso 3: Iniciar Manualmente
Si el auto-inicio falla:
1. Selecciona fecha: **2025-01-15** (no 02/01/2025)
2. Espera 2 segundos
3. La planificación debería iniciarse automáticamente

### Paso 4: Verificar Logs de Planificación
Deberías ver:
```javascript
📤 Solicitud de planificación enviada para fecha: 2025-01-15
📩 Mensaje de planificación: {...}
✈️ Procesando X vuelos de la iteración #1
```

Si ves "Ya hay una planificación en curso", significa que hay un proceso bloqueado.

## 🔧 SOLUCIÓN RÁPIDA

### Opción A: Recargar página
1. Presiona **Ctrl+Shift+R** (recarga forzada)
2. Espera que cargue
3. Selecciona fecha: **2025-01-15**
4. Observa la consola

### Opción B: Limpiar y reiniciar
1. Click en "Limpiar Mapa" (botón azul)
2. Selecciona nueva fecha
3. Espera auto-inicio

### Opción C: Reiniciar backend
Si nada funciona:
```bash
# Detener backend (Ctrl+C en el terminal)
# Reiniciar
cd backend
mvn spring-boot:run
```

## 📊 LOGS A BUSCAR

### ✅ LOGS CORRECTOS (todo funciona):
```
🔌 Auto-conectando WebSocket de planificación...
🔌 WebSocket Planificación conectado: ws://localhost:8000/ws/planificacion
✅ Conexión establecida
🚀 Auto-iniciando planificación...
📤 Solicitud de planificación enviada para fecha: 2025-01-15
📩 Mensaje recibido: completado
✈️ Procesando 5 vuelos de la iteración #1
🔄 Convertidos 5 de 5 vuelos
📊 Total vuelos en mapa: 5
```

### ❌ LOGS DE ERROR (hay problema):
```
❌ Error en WebSocket: ...
⚠️ WebSocket no conectado
⚠️ Aeropuertos no encontrados: XXXX
❌ No se pudo enviar la siguiente solicitud
```

## 🎯 PRUEBA DEFINITIVA

**Abre en el navegador:**
```
file:///home/leoncio/Documentos/GitHub/ProyDise-o/test_websocket_planificacion.html
```

Este archivo de test te dirá EXACTAMENTE qué está fallando:
- Si el WebSocket conecta ✅/❌
- Si el backend responde ✅/❌
- Cuántos vuelos llegan ✅/❌

## 🔴 ERROR COMÚN: "Ya hay una planificación en curso"

**Causa:** El backend tiene una sesión bloqueada
**Solución:**
1. Cierra TODAS las pestañas del navegador que tengan la app
2. Reinicia el backend:
   ```bash
   # Ctrl+C en el terminal del backend
   cd backend && mvn spring-boot:run
   ```
3. Abre de nuevo http://localhost:3001

## 📝 SIGUIENTE ACCIÓN

**Por favor, haz esto:**
1. Abre la consola del navegador (F12)
2. Recarga la página (Ctrl+Shift+R)
3. Copia TODOS los logs que aparezcan
4. Busca específicamente:
   - Mensajes con 🔌
   - Mensajes con ❌
   - Mensajes con ⚠️
5. Pégame esos logs para ver qué está fallando

---

**Última actualización:** 22 noviembre 2025, 23:55
