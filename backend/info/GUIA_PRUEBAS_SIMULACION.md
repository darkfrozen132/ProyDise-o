# 🧪 Guía de Pruebas - Simulación Semanal Incremental

## 📋 Requisitos Previos

1. **Backend ejecutándose**: `mvn spring-boot:run`
2. **Puerto**: 8000 (por defecto)
3. **Base de datos**: MySQL con datos de aeropuertos y pedidos

---

## 🚀 Método 1: Pruebas con PowerShell (Recomendado)

### 1️⃣ Iniciar Simulación

```powershell
# Guardar en una variable para ver mejor el resultado
$response = Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/iniciar" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{
    "fecha": "2025-01-17",
    "saltoMinutos": 5
  }'

# Mostrar respuesta
$response | ConvertTo-Json -Depth 5
```

**Respuesta esperada**:
```json
{
  "success": true,
  "mensaje": "Simulación iniciada exitosamente",
  "fecha": "2025-01-17",
  "saltoMinutos": 5,
  "instrucciones": "Conecta a GET /api/simulacion/stream para recibir eventos en tiempo real"
}
```

---

### 2️⃣ Verificar Estado de la Simulación

```powershell
$estado = Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/estado" -Method GET
$estado | ConvertTo-Json -Depth 5
```

**Respuesta esperada**:
```json
{
  "activa": true,
  "fechaInicio": "2025-01-17",
  "inicioSimulacion": "2025-01-17T00:00:00",
  "minutoActual": 150,
  "saltoMinutos": 5,
  "tickActual": 0,
  "clientesConectados": 0,
  "progreso": 0.1041667,
  "limiteMinutos": 1440
}
```

---

### 3️⃣ Detener Simulación

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/detener" -Method POST
$response | ConvertTo-Json
```

**Respuesta esperada**:
```json
{
  "success": true,
  "mensaje": "Simulación detenida exitosamente"
}
```

---

### 4️⃣ Health Check

```powershell
$health = Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/health" -Method GET
$health | ConvertTo-Json -Depth 5
```

---

## 🌐 Método 2: Pruebas con curl (Git Bash o WSL)

### 1️⃣ Iniciar Simulación

```bash
curl -X POST http://localhost:8000/api/simulacion/iniciar \
  -H "Content-Type: application/json" \
  -d '{
    "fecha": "2025-01-17",
    "saltoMinutos": 5
  }' | jq
```

### 2️⃣ Ver Estado

```bash
curl http://localhost:8000/api/simulacion/estado | jq
```

### 3️⃣ Detener Simulación

```bash
curl -X POST http://localhost:8000/api/simulacion/detener | jq
```

---

## 📡 Método 3: Probar SSE (Server-Sent Events)

### Opción A: Con HTML (Ver archivo `test-simulacion-sse.html`)

Abre el archivo `test-simulacion-sse.html` en tu navegador (ver abajo).

### Opción B: Con PowerShell (Lectura básica)

```powershell
# Esto mostrará los eventos en tiempo real
$request = [System.Net.WebRequest]::Create("http://localhost:8000/api/simulacion/stream")
$request.Method = "GET"
$request.Accept = "text/event-stream"

$response = $request.GetResponse()
$stream = $response.GetResponseStream()
$reader = New-Object System.IO.StreamReader($stream)

while ($true) {
    $line = $reader.ReadLine()
    if ($line) {
        Write-Host $line -ForegroundColor Cyan
    }
}
```

---

## 🧪 Método 4: Postman o Insomnia

### Configuración en Postman:

1. **Iniciar Simulación**
   - Método: `POST`
   - URL: `http://localhost:8000/api/simulacion/iniciar`
   - Headers: `Content-Type: application/json`
   - Body (raw JSON):
     ```json
     {
       "fecha": "2025-01-17",
       "saltoMinutos": 5
     }
     ```

2. **Ver Estado**
   - Método: `GET`
   - URL: `http://localhost:8000/api/simulacion/estado`

3. **SSE Stream**
   - Método: `GET`
   - URL: `http://localhost:8000/api/simulacion/stream`
   - ⚠️ **Nota**: Postman tiene soporte limitado para SSE

---

## 📊 Verificación de Logs

Mientras la simulación corre, verifica los logs del backend:

```
🚀 Iniciando simulacion: fecha=2025-01-17, K=5, Sa=5min, Ta=60s
✅ Simulacion iniciada - Pedidos cargados: 50
🧬 AG Tick 1: Iniciando planificacion hasta 2025-01-17T00:05 (ventana: 5min)
✅ AG Tick 1 completado en 2500ms (limite: 60000ms)
📡 State Tick 1: Actualizando estado - Tiempo: 2025-01-17T00:00:05
```

---

## 🔍 Casos de Prueba Completos

### Test 1: Flujo Completo Básico

```powershell
# 1. Iniciar simulación
Write-Host "1. Iniciando simulación..." -ForegroundColor Green
$inicio = Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/iniciar" `
  -Method POST -ContentType "application/json" `
  -Body '{"fecha": "2025-01-17", "saltoMinutos": 5}'
$inicio

Start-Sleep -Seconds 2

# 2. Verificar estado 5 veces (cada 3 segundos)
Write-Host "`n2. Monitoreando estado..." -ForegroundColor Green
for ($i = 1; $i -le 5; $i++) {
    Write-Host "  Check $i/5" -ForegroundColor Yellow
    $estado = Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/estado"
    Write-Host "    Minuto: $($estado.minutoActual), Progreso: $([math]::Round($estado.progreso * 100, 2))%"
    Start-Sleep -Seconds 3
}

# 3. Detener simulación
Write-Host "`n3. Deteniendo simulación..." -ForegroundColor Green
$fin = Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/detener" -Method POST
$fin
```

### Test 2: Manejo de Errores

```powershell
# Intentar iniciar dos simulaciones (debe fallar la segunda)
Write-Host "Test: Iniciar simulación duplicada" -ForegroundColor Yellow

Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/iniciar" `
  -Method POST -ContentType "application/json" `
  -Body '{"fecha": "2025-01-17", "saltoMinutos": 5}'

Start-Sleep -Seconds 1

# Esta debe fallar
try {
    Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/iniciar" `
      -Method POST -ContentType "application/json" `
      -Body '{"fecha": "2025-01-18", "saltoMinutos": 5}'
} catch {
    Write-Host "ERROR ESPERADO: $_" -ForegroundColor Red
}

# Limpiar
Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/detener" -Method POST
```

### Test 3: Parámetros Inválidos

```powershell
# Fecha nula (debe fallar)
try {
    Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/iniciar" `
      -Method POST -ContentType "application/json" `
      -Body '{"saltoMinutos": 5}'
} catch {
    Write-Host "ERROR ESPERADO (fecha nula): $_" -ForegroundColor Red
}

# Salto negativo (debe fallar)
try {
    Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/iniciar" `
      -Method POST -ContentType "application/json" `
      -Body '{"fecha": "2025-01-17", "saltoMinutos": -5}'
} catch {
    Write-Host "ERROR ESPERADO (salto negativo): $_" -ForegroundColor Red
}
```

---

## 📈 Monitoreo en Tiempo Real

### Script de Monitoreo Continuo

Guarda como `monitor-simulacion.ps1`:

```powershell
Write-Host "=== Monitor de Simulación ===" -ForegroundColor Cyan
Write-Host "Presiona Ctrl+C para salir`n"

while ($true) {
    Clear-Host
    Write-Host "=== Monitor de Simulación ===" -ForegroundColor Cyan
    Write-Host "Actualizado: $(Get-Date -Format 'HH:mm:ss')`n" -ForegroundColor Gray
    
    try {
        $estado = Invoke-RestMethod -Uri "http://localhost:8000/api/simulacion/estado"
        
        if ($estado.activa) {
            Write-Host "Estado: ACTIVA" -ForegroundColor Green
            Write-Host "Fecha: $($estado.fechaInicio)"
            Write-Host "Minuto actual: $($estado.minutoActual) / $($estado.limiteMinutos)"
            Write-Host "Progreso: $([math]::Round($estado.progreso * 100, 2))%"
            Write-Host "Clientes conectados: $($estado.clientesConectados)"
            
            # Barra de progreso
            $barWidth = 50
            $filled = [math]::Floor($estado.progreso * $barWidth)
            $bar = ("#" * $filled).PadRight($barWidth, "-")
            Write-Host "`n[$bar]`n" -ForegroundColor Yellow
        } else {
            Write-Host "Estado: INACTIVA" -ForegroundColor Red
            Write-Host "`nNo hay simulación en ejecución"
        }
        
    } catch {
        Write-Host "ERROR: No se pudo conectar al backend" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 2
}
```

Ejecutar:
```powershell
.\monitor-simulacion.ps1
```

---

## 🎯 Resultados Esperados

### Simulación Exitosa

- ✅ La simulación se inicia sin errores
- ✅ El progreso aumenta gradualmente (0% → 100%)
- ✅ Los logs muestran ticks del AG y del StateUpdater
- ✅ El estado se puede consultar en cualquier momento
- ✅ La simulación se puede detener manualmente
- ✅ Al completarse, se detiene automáticamente

### Métricas de Rendimiento

- **Tiempo por tick AG**: < 60 segundos (idealmente < 10s)
- **Frecuencia de actualización**: 1 segundo (StateUpdater)
- **Salto temporal**: 5 minutos de simulación cada tick

---

## ❓ Troubleshooting

### Problema: "Connection refused"
- ✅ Verificar que el backend esté corriendo: `mvn spring-boot:run`
- ✅ Verificar el puerto: `http://localhost:8000`

### Problema: "Ya hay una simulación activa"
- ✅ Detener la simulación actual: `POST /api/simulacion/detener`
- ✅ Reiniciar el backend

### Problema: "No hay pedidos para procesar"
- ✅ Verificar que haya pedidos en la base de datos para la fecha especificada
- ✅ Ejecutar: `SELECT * FROM pedidos WHERE anio=2025 AND mes=1 AND dia=17;`

### Problema: El progreso no avanza
- ✅ Verificar logs del backend
- ✅ Buscar errores en el algoritmo genético
- ✅ Verificar timeout del AG (debe ser < 60s)

---

## 📝 Notas Adicionales

- **Fecha recomendada**: 2025-01-17 (tiene varios pedidos de ejemplo)
- **Salto recomendado**: 5 minutos (balance entre detalle y velocidad)
- **Duración total**: ~24 minutos (288 ticks × 5 segundos/tick)
- **Límite de tiempo**: 1440 minutos = 24 horas de simulación

---

## 🔗 Endpoints Disponibles

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/simulacion/iniciar` | Inicia simulación |
| POST | `/api/simulacion/detener` | Detiene simulación |
| GET | `/api/simulacion/estado` | Obtiene estado actual |
| GET | `/api/simulacion/stream` | SSE para eventos en tiempo real |
| GET | `/api/simulacion/health` | Health check |

---

¡Ahora estás listo para probar la simulación! 🚀
