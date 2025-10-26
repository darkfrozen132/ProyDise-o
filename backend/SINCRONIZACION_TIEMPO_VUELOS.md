# 📐 Sincronización de Tiempo: Simulación y Tracking de Vuelos

## 🎯 Objetivo

Sincronizar el movimiento visual del avión en el mapa con la duración real del vuelo en tiempo simulado.

## ⚙️ Parámetros Clave

### En `SimulacionOrchestrator.java`:

```java
// Cuántos minutos simulados equivalen a 1 segundo real
private static final double TIME_SCALE = 60.0;  // 1 seg real = 60 min simulados = 1 hora

// Cada cuánto se ejecuta un tick
private static final long INTERVALO_TICK_MS = 1000;  // 1 segundo
```

### En `VueloTrackingService.java`:

```java
// Deben coincidir con SimulacionOrchestrator
private static final double TIME_SCALE = 60.0;
private static final long INTERVALO_TICK_MS = 1000;
```

## 📊 Cálculos de Sincronización

### Fórmula Principal:

```
Duración en Segundos Reales = Duración del Vuelo (minutos) / TIME_SCALE
```

### Ejemplo 1: Vuelo Lima → Berlín

**Datos del vuelo:**
- Salida: 01:00 AM
- Llegada: 10:00 AM
- Duración: 9 horas = 540 minutos

**Con TIME_SCALE = 60.0:**
```
Duración Real = 540 minutos / 60.0 = 9 segundos reales
```

**Resultado:**
- ✈️ El avión se moverá visualmente de Lima a Berlín en **9 segundos reales**
- 📍 Cada segundo real el avión avanza 1/9 del camino (11.11%)
- ⏱️ La simulación avanza 60 minutos cada segundo

### Ejemplo 2: Con TIME_SCALE = 120.0 (más rápido)

```
Duración Real = 540 minutos / 120.0 = 4.5 segundos reales
```

**Resultado:**
- ✈️ El avión completará el viaje en **4.5 segundos**
- Simulación más rápida: 2 horas simuladas por segundo real

### Ejemplo 3: Con TIME_SCALE = 30.0 (más lento)

```
Duración Real = 540 minutos / 30.0 = 18 segundos reales
```

**Resultado:**
- ✈️ El avión tardará **18 segundos** en llegar
- Simulación más lenta: 30 minutos simulados por segundo real

## 🔄 Flujo de Sincronización

### 1. Inicio del Vuelo

```java
// VueloTrackingService.streamearCoordenadas()

// Obtener horarios del vuelo
LocalDateTime horaSalida = ruta.getSalida();     // 01:00
LocalDateTime horaLlegada = ruta.getLlegada();   // 10:00

// Calcular duración en minutos
long duracionVueloMinutos = Duration.between(horaSalida, horaLlegada).toMinutes();  // 540

// Calcular cuántos segundos reales durará
double duracionVueloSegundosReales = duracionVueloMinutos / TIME_SCALE;  // 9.0

// Número de pasos (ticks)
int totalPasos = (int) Math.ceil(duracionVueloSegundosReales);  // 9
```

### 2. Interpolación de Coordenadas

```java
for (int i = 0; i <= totalPasos; i++) {
    // Progreso: 0.0 → 1.0
    double progreso = (double) i / totalPasos;
    
    // Interpolar latitud y longitud
    double latActual = latInicio + (latFin - latInicio) * progreso;
    double lonActual = lonInicio + (lonFin - lonInicio) * progreso;
    
    // Calcular hora simulada del vuelo
    LocalDateTime horaSimuladaVuelo = horaSalida.plusMinutes(
        (long)(duracionVueloMinutos * progreso)
    );
    
    // Actualizar en BD
    actualizarRutaEnBD(rutaId, latActual, lonActual, progreso * 100, true);
    
    // Esperar 1 segundo (INTERVALO_TICK_MS)
    Thread.sleep(INTERVALO_TICK_MS);
}
```

## 📋 Tabla de Equivalencias

| TIME_SCALE | 1 segundo real = | Vuelo 9h (540 min) |
|------------|------------------|-------------------|
| 30.0       | 30 min simulados | 18 segundos reales |
| 60.0       | 1 hora simulada  | 9 segundos reales |
| 120.0      | 2 horas simuladas | 4.5 segundos reales |
| 180.0      | 3 horas simuladas | 3 segundos reales |
| 360.0      | 6 horas simuladas | 1.5 segundos reales |

## 🎮 Configuración de Horarios

En `DatosIniciales.java`:

```java
// Horarios de vuelo sincronizados con la simulación
LocalDateTime salida = LocalDateTime.of(2025, 1, 1, 1, 0);   // 01:00 AM
LocalDateTime llegada = LocalDateTime.of(2025, 1, 1, 10, 0); // 10:00 AM

rutaLimaBerlin.setSalida(salida);
rutaLimaBerlin.setLlegada(llegada);
```

**Duración:** 9 horas = 540 minutos

## 📡 Eventos SSE Enviados

Cada segundo (tick) se envía un evento con:

```json
{
  "id": 2,
  "originCode": "SPIM",
  "destinationCode": "EDDI",
  "currentLatitude": -8.7972,      // ← Actualizado cada segundo
  "currentLongitude": -72.5884,    // ← Actualizado cada segundo
  "progress": 5.0,                 // ← Porcentaje completado
  "horaSimuladaVuelo": "2025-01-01T01:27",  // ← Hora actual del vuelo
  "horaSalida": "2025-01-01T01:00",
  "horaLlegada": "2025-01-01T10:00",
  "speed": 900,
  "altitude": 35000
}
```

## ✅ Ventajas de este Sistema

1. **Sincronización perfecta**: El avión llega exactamente cuando debe según el horario
2. **Escalabilidad temporal**: Cambiar TIME_SCALE ajusta automáticamente la velocidad
3. **Coherencia visual**: El movimiento coincide con el tiempo de simulación
4. **Flexibilidad**: Funciona con cualquier duración de vuelo

## 🔧 Cómo Modificar la Velocidad

### Para hacer la simulación más rápida:
```java
private static final double TIME_SCALE = 120.0;  // 2 horas por segundo
```

### Para hacer la simulación más lenta:
```java
private static final double TIME_SCALE = 30.0;   // 30 minutos por segundo
```

**⚠️ IMPORTANTE:** Modificar `TIME_SCALE` en ambos archivos:
- `SimulacionOrchestrator.java`
- `VueloTrackingService.java`

## 🎯 Verificación

Para verificar que funciona correctamente:

```bash
# Consultar coordenadas actuales cada segundo
mysql -h ... -u admin -p'...' dp1 -e "
  SELECT current_latitud, current_longitud, progreso 
  FROM rutas_solucion WHERE id=2;"
```

Deberías ver las coordenadas cambiar desde Lima (-12.02, -77.11) hacia Berlín (52.47, 13.40).
