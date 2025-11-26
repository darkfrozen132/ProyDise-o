# 🧬 Algoritmo Genético con Decodificador - Implementación en Español

## 📋 Resumen Ejecutivo

Este archivo documenta la implementación completa del **Algoritmo Genético con Decodificador Greedy** para optimización de rutas logísticas de MoraPack, ahora con **nombres de variables y métodos en español**.

---

## 🏗️ Arquitectura del Algoritmo

### **Cromosoma**
```java
static class Chromosome {
    double[] keys;  // 1 valor aleatorio (0.0-1.0) por cada VUELO plantilla
}
```
- **NO** asigna vuelos a pedidos directamente
- Cada gen es un **peso de prioridad** para ese vuelo
- El decodificador usa estos pesos en la heurística de selección

---

## 🎯 Función Objetivo (Fitness)

```java
objetivo = 1.0 × aTiempo 
         - 1.5 × tarde 
         - 4.0 × violaciones 
         + 0.001 × holguraPromedio
```

### **Métricas:**
- **aTiempo**: Pedidos entregados antes de `fechaLimite`
- **tarde**: Pedidos entregados después o no entregados
- **violaciones**: Excesos de capacidad de almacén detectados
- **holguraPromedio**: Tiempo promedio de holgura (para desempatar)

---

## 🔄 Flujo del Algoritmo Genético

### **1. Inicialización**
```java
public void inicializar(List<Pedido> pedidos, List<PlanDeVuelo> vuelos, List<Aeropuerto> aeropuertos)
```
- Convierte modelos del dominio a modelos internos (`World`, `Airport`, `Flight`, `Order`)
- Identifica hubs exportadores: `SPIM`, `EBCI`, `UBBB`
- Crea índices de vuelos por aeropuerto de salida

### **2. Ejecución Principal**
```java
public Solution ejecutar()
```

**Bucle de generaciones (MAX_GEN = 200):**
```
Para cada generación:
  1. Evaluar fitness de todos los cromosomas
     └─> fitness() → decodeSorted() → construirSubrutaDesdeHub()
  
  2. Ordenar población por fitness (mayor es mejor)
  
  3. Selección y Reproducción:
     ├─ Preservar ELITE_K=4 mejores (elitismo)
     ├─ Seleccionar padres aleatoriamente
     ├─ Cruce (PCROSS=0.8): intercambiar genes
     └─ Mutación (PMUT=0.05): perturbar genes ±10%
  
  4. Criterio de parada:
     └─ Si NO_IMPROV_LIMIT=40 generaciones sin mejora → PARAR
```

---

## 🚀 Decodificador Greedy (El Corazón del Algoritmo)

### **Método Principal: `decodeSorted()`**

```java
static Solution decodeSorted(World W, List<Order> ordenesSorted, 
                             Chromosome cromosoma, int diasHorizonte)
```

**Para cada pedido (ordenados por tiempo de liberación):**

```
MIENTRAS restante > 0:
  
  1. Para cada HUB exportador (SPIM, EBCI, UBBB):
     └─> construirSubrutaDesdeHub(hub, destino, cantidad)
         └─> Expansión greedy paso a paso
  
  2. Elegir hub con mejor subruta (menor arrivalUTC)
  
  3. Reservar capacidad:
     ├─ Vuelos usados en cada tramo
     ├─ Almacenes intermedios (esperas fuera de hubs)
     └─ Almacén destino (ventana de 2 horas)
  
  4. Extender reservas de destino:
     └─> TODAS las subrutas deben mantener almacén 
         hasta último_arribo + 2 horas
```

---

## 🛤️ Construcción de Subruta: `construirSubrutaDesdeHub()`

### **Expansión Greedy Iterativa:**

```
actual = hub
tiempoActual = tiempoLiberación
visitados = {hub}

MIENTRAS actual ≠ destino Y expansiones < 2000:
  
  1. Enumerar vuelos candidatos desde 'actual':
     └─> enumerarCandidatos()
         ├─ Filtrar por tiempo: depUTC ≥ tiempoActual + 30min
         ├─ Filtrar por fecha límite: arrUTC ≤ fechaLimite
         ├─ Filtrar por capacidad: available > 0
         └─> Recortar a TOP 2000 por arrivalUTC
  
  2. Filtrar revisitas:
     └─> Eliminar vuelos que van a aeropuertos ya visitados
  
  3. Seleccionar mejor vuelo:
     └─> seleccionarPorPrioridad()
         
         Puntuación = wClave × clave           (1.0 × gen del cromosoma)
                    + wTemprano × gananciaHoras (0.1 × bonus por llegar antes)
                    + wGeo × progreso           (0.05 × reducción distancia Haversine)
                    + wDirecto                  (50.0 si vuelo va directo al destino)
                    + wDosSaltos                (5.0 si siguiente tiene directo a destino)
  
  4. Validar capacidad de almacén (si hay espera):
     SI actual NO es hub Y hay tiempo de espera:
       └─> calcularMaximoAjusteAlmacenamiento()
           └─> Binary search: ¿cuánta cantidad cabe en [inicioSlot, finSlot]?
  
  5. Agregar tramo al plan:
     ├─ Actualizar: actual = destino_vuelo
     ├─ Actualizar: tiempoActual = arrivalUTC
     └─ Marcar: visitados += actual

FIN MIENTRAS

6. Validar llegada al destino
7. Validar capacidad de almacén en destino (ventana 2h)
8. Reservar capacidades y retornar subruta
```

---

## 📊 Control de Capacidad

### **Vuelos: Array 2D**
```java
int[][] capUsed;  // capUsed[índiceVuelo][día] = cantidad usada
```

### **Almacenes: Difference Arrays**
```java
class StockTracker {
    class Store {
        int[] delta;  // delta[slot] = cambio en ese slot
        int[] pref;   // pref[slot] = ocupación acumulada
    }
}
```

**Operaciones:**
```java
// Reservar intervalo [inicioSlot, finSlot)
addInterval(aeropuerto, inicioSlot, finSlot, cantidad):
    delta[inicioSlot] += cantidad
    delta[finSlot] -= cantidad
    dirty = true

// Verificar si cabe
canFit(aeropuerto, inicioSlot, finSlot, cantidad):
    SI dirty: reconstruir pref[] (prefix sum)
    RETORNAR: ∀t ∈ [inicioSlot, finSlot): pref[t] + cantidad ≤ capacidad
```

**Complejidad:**
- `addInterval()`: **O(1)**
- `canFit()`: **O(slots)** cuando dirty, sino **O(slots a verificar)**

---

## 🎲 Heurística de Selección de Vuelos

### **Pesos Configurados:**

| **Factor** | **Peso** | **Descripción** |
|------------|----------|-----------------|
| `wClave` | 1.0 | Valor del gen del cromosoma (aleatorio) |
| `wTemprano` | 0.1 | Bonus por llegar más temprano que otros candidatos |
| `wGeo` | 0.05 | Progreso geográfico (Haversine) hacia destino |
| **`wDirecto`** | **50.0** | 🔥 **MEGA BONUS** si vuelo va directo al destino |
| `wDosSaltos` | 5.0 | Bonus si siguiente aeropuerto tiene directo a destino |

### **¿Por qué wDirecto = 50.0?**
- Domina sobre todos los demás factores
- Fuerza al algoritmo a **priorizar rutas directas**
- Evita dar muchas vueltas innecesarias

---

## 🔧 Parámetros de Negocio

```java
MIN_TURN_MIN = 30           // Conexión mínima entre vuelos
DUE_SAME_MIN = 2 × 24 × 60  // 2 días (mismo continente)
DUE_CROSS_MIN = 3 × 24 × 60 // 3 días (cruce continentes)
PICKUP_WINDOW_MIN = 120     // 2 horas de recojo en destino
SLOT_MIN = 60               // Granularidad de slots de almacén
```

---

## 📝 Traducción de Nombres Clave

| **Inglés (prueba.java)** | **Español (genetico.java)** |
|--------------------------|----------------------------|
| `buildSubrouteFromHub()` | `construirSubrutaDesdeHub()` |
| `enumerateCandidates()` | `enumerarCandidatos()` |
| `selectByPriority()` | `seleccionarPorPrioridad()` |
| `maxStorageFit()` | `calcularMaximoAjusteAlmacenamiento()` |
| `distancesToDest()` | `obtenerDistanciasADestino()` |
| `directOriginsToDest()` | `obtenerOrigenesDirectosADestino()` |
| `computeDueForHub()` | `calcularFechaLimiteParaHub()` |
| `DecodeContext` | `contextoDecodificacion` |
| `SelectContext` | `contextoSeleccion` |
| `FlightCandidate` | `candidatoVuelo` |
| `destinationReservations` | `reservasDestino` |
| `onTime` | `aTiempo` |
| `late` | `tarde` |
| `capViol` | `violaciones` |
| `avgSlack` | `holguraPromedio` |

---

## ✅ Ventajas de Esta Implementación

### **1. Factibilidad Garantizada**
- Valida capacidades en **cada paso** (vuelos y almacenes)
- No genera soluciones inválidas

### **2. Heurística Inteligente**
- Prioriza vuelos directos (`wDirecto=50.0`)
- Considera progreso geográfico (Haversine)
- Evita ciclos (Set de visitados)

### **3. Eficiencia**
- Difference arrays: O(1) inserciones
- Precomputación de horarios UTC
- Recorte a Top-K candidatos

### **4. Escalabilidad**
- Horizonte configurable (31 días por defecto)
- Hubs configurables desde afuera
- Paralelizable (evaluación de fitness)

---

## 🚨 Casos Especiales Manejados

### **1. Extensión de Reservas en Destino**
Si un pedido tiene múltiples subrutas:
```
Subruta1: llega D5 14:00 (30 unidades)
Subruta2: llega D5 18:00 (20 unidades)

→ Las 30 unidades de subruta1 deben ocupar almacén hasta 18:00+2h
  (no solo hasta 14:00+2h)
```

### **2. Ajuste de Cantidad por Capacidad**
Si un vuelo tiene disponible=80 pero almacén solo cabe 50:
```
→ pathCapacity = min(80, 50) = 50
→ Se transportan solo 50 unidades
```

### **3. Binary Search de Capacidad**
```java
calcularMaximoAjusteAlmacenamiento(stock, aeropuerto, inicioSlot, finSlot, 100):
  // ¿Cuántas de las 100 unidades solicitadas realmente caben?
  → Retorna: 73  (por ejemplo)
```

---

## 🎯 Ejemplo de Ejecución

### **Entrada:**
```
Pedido: destino=SCEL, cantidad=100, liberación=D1 06:00
Hubs: {SPIM, EBCI, UBBB}
```

### **Proceso:**

**Generación 1:**
- Cromosoma aleatorio: `keys = [0.23, 0.87, 0.45, ...]`
- Fitness: `17.5` (mediocre)

**Generación 50:**
- Mejor cromosoma: `keys = [0.91, 0.12, 0.78, ...]`
- Fitness: `89.3`
- Subruta elegida:
  ```
  SPIM → SCEL (directo)
  - Vuelo 1: SPIM→SCEL D1 08:00-10:00 qty=100
  - Llegada: D1 10:00 (dentro de plazo)
  ```

**Generación 150:**
- Convergencia alcanzada (stall=40)
- Fitness final: `95.7`
- OnTime: 95%, Late: 5%

---

## 📚 Referencias

- **Archivo original**: `prueba.java` (1000 líneas, lógica completa)
- **Nuevo archivo**: `genetico.java` (nombres en español)
- **Resumen técnico**: Este documento

---

## 🛠️ Uso Básico

```java
// 1. Crear instancia
genetico algoritmo = new genetico();

// 2. Inicializar con datos
algoritmo.inicializar(pedidos, vuelos, aeropuertos);

// 3. Ejecutar
Solution solucion = algoritmo.ejecutar();

// 4. Revisar resultados
log.info("Fitness: {}", solucion.objective);
log.info("A tiempo: {}", solucion.servedOnTime);
log.info("Tarde: {}", solucion.servedLate);
```

---

## ✨ Conclusión

Esta implementación combina:
- ✅ Algoritmo Genético (exploración global)
- ✅ Decoder Greedy (construcción de soluciones factibles)
- ✅ Heurística multi-criterio (balance calidad/tiempo)
- ✅ Control de capacidad (difference arrays)
- ✅ **Nombres en español** (legibilidad para el equipo)

**Resultado:** Sistema robusto, escalable y mantenible para optimización de rutas logísticas. 🚀
