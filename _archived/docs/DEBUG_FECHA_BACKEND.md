# 🔍 Debugging: Verificar Fecha Enviada al Backend

## 🎯 Objetivo
Determinar si la fecha seleccionada por el usuario (`02/01/2025`) llega correctamente al backend o si el backend la está ignorando.

## 📊 Evidencia Actual

### Frontend (UI):
```
📅 Fecha seleccionada: 02/01/2025
```

### Backend (Logs):
```
🚀 Iniciando planificación para fecha: 2025-01-26
📨 Iniciando simulación para 2025-11-26
```

**Diferencia detectada**: 10 meses (≈468,000 minutos)

## 🔎 Paso 1: Verificar en el Navegador

### Abrir DevTools
1. Presiona `F12` en el navegador
2. Ve a la pestaña **Network**
3. Filtra por **WS** (WebSocket)
4. Borra todo (🚫 Clear)

### Iniciar Simulación
1. Selecciona fecha: `02/01/2025`
2. Click en "Iniciar Planificación"
3. En la pestaña Network → WS verás la conexión WebSocket

### Inspeccionar Mensajes
1. Click en la conexión WebSocket
2. Ve a la pestaña **Messages**
3. Busca el mensaje que dice `SEND` con destino `/app/iniciarPlanificacion`
4. Click en ese mensaje para ver el **payload**

### ✅ Qué Buscar
El mensaje debe verse así:
```json
{
  "fechaInicio": "2025-01-02",
  "factorK": 5,
  "parametrosAG": {
    "tamanioPoblacion": 20,
    "maxGeneraciones": 20,
    "limiteGeneracionesSinMejora": 10
  }
}
```

**Verifica**:
- ✅ ¿El campo `fechaInicio` contiene `2025-01-02`?
- ✅ ¿O contiene `2025-11-26` (fecha actual)?

## 🔎 Paso 2: Verificar en Console (Frontend)

### Abrir Console
1. Presiona `F12` → Pestaña **Console**
2. Busca el log: `📤 Solicitud de planificación enviada para fecha:`

### ✅ Qué Buscar
```
📤 Solicitud de planificación enviada para fecha: 2025-01-02
```

**Verifica**:
- ✅ ¿La fecha coincide con lo que seleccionaste?
- ⚠️ Si dice `2025-11-26`, el problema está en el FRONTEND
- ✅ Si dice `2025-01-02`, el problema está en el BACKEND

## 🔎 Paso 3: Revisar Backend (Java)

### Buscar en el código Java del backend
```bash
cd backend/
grep -r "iniciarPlanificacion" src/
```

### Archivo probable:
```
backend/src/main/java/.../controller/WebSocketPlanificacionController.java
```

### ✅ Qué Buscar

#### Método que recibe el mensaje:
```java
@MessageMapping("/iniciarPlanificacion")
public void iniciarPlanificacion(
    @Payload Map<String, Object> payload,
    SimpMessageHeaderAccessor headerAccessor
) {
    String fechaInicio = (String) payload.get("fechaInicio");
    System.out.println("📅 FECHA RECIBIDA: " + fechaInicio);
    
    // ⚠️ Verifica si hay algo como esto:
    // LocalDate fecha = LocalDate.now(); ← PROBLEMA!
    
    // ✅ Debería ser:
    // LocalDate fecha = LocalDate.parse(fechaInicio);
}
```

### ⚠️ Posibles Problemas en Backend

#### Problema 1: Ignorando el parámetro
```java
// ❌ MAL
LocalDate fecha = LocalDate.now(); // Siempre usa fecha actual
```

#### Problema 2: Parseando mal
```java
// ❌ MAL
LocalDate fecha = LocalDate.parse(fechaInicio, DateTimeFormatter.ISO_DATE_TIME);
// El formato es ISO_DATE, no ISO_DATE_TIME
```

#### Problema 3: No recibiendo el campo
```java
// ❌ MAL
String fechaInicio = (String) payload.get("fecha"); // Campo incorrecto
// Debería ser: payload.get("fechaInicio")
```

## 📝 Agregar Logs de Debug

### En Frontend (SimuladorSemanal.js)

#### Antes de enviar (línea ~1224):
```javascript
const enviarSolicitudPlanificacion = () => {
    // ...validaciones...
    
    // 🔍 DEBUG: Verificar fecha antes de enviar
    console.log('🔍 DEBUG - Fecha a enviar:');
    console.log('  📅 Valor:', fechaInicioSimulacion);
    console.log('  📅 Tipo:', typeof fechaInicioSimulacion);
    console.log('  📅 JSON:', JSON.stringify(fechaInicioSimulacion));
    
    // Enviar solicitud...
    wsPlanificacionRef.current.iniciarPlanificacion(
        fechaInicioSimulacion,
        5,
        { /* ... */ }
    );
};
```

### En Backend (WebSocketPlanificacionController.java)

#### Al recibir:
```java
@MessageMapping("/iniciarPlanificacion")
public void iniciarPlanificacion(
    @Payload Map<String, Object> payload,
    SimpMessageHeaderAccessor headerAccessor
) {
    // 🔍 DEBUG: Verificar fecha recibida
    System.out.println("🔍 DEBUG - Payload completo: " + payload);
    
    String fechaInicio = (String) payload.get("fechaInicio");
    System.out.println("🔍 DEBUG - fechaInicio RAW: " + fechaInicio);
    
    LocalDate fecha = LocalDate.parse(fechaInicio);
    System.out.println("🔍 DEBUG - fechaInicio PARSED: " + fecha);
    
    LocalDate hoy = LocalDate.now();
    System.out.println("🔍 DEBUG - Fecha actual sistema: " + hoy);
    
    if (!fecha.equals(hoy)) {
        System.out.println("⚠️ ALERTA: Fecha recibida difiere de hoy por " + 
            ChronoUnit.DAYS.between(fecha, hoy) + " días");
    }
    
    // ...resto del código...
}
```

## 🎯 Diagnóstico según Resultados

### Caso 1: Frontend envía fecha incorrecta
```
Console: 📤 Solicitud enviada para fecha: 2025-11-26
Network → WS: "fechaInicio": "2025-11-26"
```
**Problema**: Variable `fechaInicioSimulacion` no tiene el valor del input
**Solución**: Revisar binding del `<input type="date">`

### Caso 2: Backend recibe pero ignora
```
Console: 📤 Solicitud enviada para fecha: 2025-01-02
Network → WS: "fechaInicio": "2025-01-02"
Backend: 📅 FECHA RECIBIDA: 2025-01-02
Backend: 🚀 Usando fecha: 2025-11-26
```
**Problema**: Backend sobrescribe con `LocalDate.now()`
**Solución**: Usar la fecha recibida en lugar de `now()`

### Caso 3: Backend no recibe el campo
```
Console: 📤 Solicitud enviada para fecha: 2025-01-02
Network → WS: "fechaInicio": "2025-01-02"
Backend: 📅 FECHA RECIBIDA: null
```
**Problema**: Nombre del campo no coincide
**Solución**: Verificar `payload.get("fechaInicio")` en Java

### Caso 4: Todo correcto pero vuelos con fecha antigua
```
Frontend: ✅ Envía 2025-01-02
Backend: ✅ Recibe 2025-01-02
Vuelos: ⚠️ Tienen fechas de enero 2025
Sistema: 📅 Está en noviembre 2025
```
**Problema**: ¡Este es el CORRECTO! La simulación es de enero
**Solución**: Ninguna - los vuelos DEBEN estar "ya aterrizados" si la simulación es del pasado

## 💡 Solución Recomendada

Si el usuario quiere simular fechas pasadas (enero), el sistema debe:

### Opción A: Modo "Replay" (Recomendado)
```javascript
// En frontend, al procesar vuelos:
if (tieneFechas && salida < ahora && llegada < ahora) {
    // Vuelo del pasado - Modo replay
    console.log('📼 MODO REPLAY: Vuelo histórico');
    // Usar tiempo simulado en lugar de tiempo real
    const tiempoSimulado = simulationTimeRef.current;
    const progreso = calcularProgresoSimulado(salida, llegada, tiempoSimulado);
}
```

### Opción B: Validación de Fecha
```javascript
// En handleIniciarPlanificacion:
const fechaSeleccionada = new Date(fechaInicioSimulacion);
const hoy = new Date();

if (fechaSeleccionada < hoy) {
    const confirmar = window.confirm(
        `⚠️ La fecha seleccionada (${fechaInicioSimulacion}) es del pasado.\n\n` +
        `Todos los vuelos aparecerán como "ya aterrizados".\n\n` +
        `¿Deseas continuar con esta fecha o usar la fecha actual (${hoy.toISOString().split('T')[0]})?`
    );
    
    if (!confirmar) {
        setFechaInicioSimulacion(hoy.toISOString().split('T')[0]);
        return;
    }
}
```

### Opción C: Auto-ajuste al Presente
```javascript
// En el backend al generar vuelos:
LocalDate fechaBase = LocalDate.parse(fechaInicio);
LocalDate hoy = LocalDate.now();

if (fechaBase.isBefore(hoy)) {
    // Ajustar todos los vuelos para que sean de la semana actual
    long diasDiferencia = ChronoUnit.DAYS.between(fechaBase, hoy);
    // Sumar diasDiferencia a todas las fechas de vuelo
}
```

## ✅ Checklist de Verificación

- [ ] Abrir DevTools → Network → WS
- [ ] Iniciar simulación con fecha `02/01/2025`
- [ ] Capturar mensaje SEND de `/app/iniciarPlanificacion`
- [ ] Verificar campo `fechaInicio` en payload
- [ ] Revisar Console para logs de fecha enviada
- [ ] Revisar logs del backend para fecha recibida
- [ ] Comparar: ¿Coinciden frontend y backend?
- [ ] Si no coinciden, identificar dónde se pierde/cambia
- [ ] Aplicar solución según caso diagnosticado

---

**Próximos Pasos**: Ejecutar este debugging y reportar los resultados.
