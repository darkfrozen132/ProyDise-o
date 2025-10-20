# ⏱️ Parámetros de Tiempo y Restricciones - Algoritmo Genético

## 📋 Nuevos Parámetros Agregados

### 1. Tiempo de Anticipación de Vuelo

```java
private static final int TIEMPO_ANTICIPACION_VUELO = 30; // minutos
```

**Descripción**: Tiempo mínimo que un pedido debe llegar **antes** de la salida del vuelo.

**Ejemplo**:
```
Vuelo sale a las: 14:30
Pedido debe llegar a más tardar: 14:00 (30 minutos antes)
```

**Restricción**: Si un pedido llega después de este tiempo, debe tomar el vuelo del día siguiente.

---

### 2. Tiempo de Procesamiento Continental

```java
private static final int TIEMPO_PROCESAMIENTO_CONTINENTAL = 2; // días
```

**Descripción**: Tiempo total de procesamiento y entrega para vuelos **continentales** (mismo continente).

**Ejemplo**:
```
Vuelo: SKBO (Bogotá, América del Sur) → SEQM (Quito, América del Sur)
Tipo: Continental
Tiempo procesamiento: 2 días
```

---

### 3. Tiempo de Procesamiento Intercontinental

```java
private static final int TIEMPO_PROCESAMIENTO_INTERCONTINENTAL = 5; // días
```

**Descripción**: Tiempo total de procesamiento y entrega para vuelos **intercontinentales** (diferentes continentes).

**Ejemplo**:
```
Vuelo: SKBO (Bogotá, América del Sur) → LEMD (Madrid, Europa)
Tipo: Intercontinental
Tiempo procesamiento: 5 días
```

---

## 🌍 Clasificación Continental vs Intercontinental

### Criterio

Un vuelo es **continental** si:
- Aeropuerto origen y destino están en el **mismo continente**

Un vuelo es **intercontinental** si:
- Aeropuerto origen y destino están en **continentes diferentes**

### Ejemplo de Clasificación

| Origen | Destino | Tipo | Tiempo |
|--------|---------|------|--------|
| SKBO (América del Sur) | SEQM (América del Sur) | ✅ Continental | 2 días |
| SKBO (América del Sur) | SEGU (América del Sur) | ✅ Continental | 2 días |
| SKBO (América del Sur) | LEMD (Europa) | ❌ Intercontinental | 5 días |
| LEMD (Europa) | RJTT (Asia) | ❌ Intercontinental | 5 días |

---

## ⏰ Cálculo del Tiempo de Espera

### Fórmula

```
TiempoEspera = TiempoHastaVuelo + TiempoProcesamiento

Donde:
- TiempoHastaVuelo = (TiempoSalidaVuelo - TiempoLlegadaPedido)
- TiempoProcesamiento = 2 días (continental) o 5 días (intercontinental)
```

### Ejemplo Detallado

#### Caso 1: Pedido Continental

```
📦 Pedido:
  - Día: 15
  - Hora: 10:00
  - Destino: SEQM (América del Sur)

✈️ Vuelo SKBO→SEQM:
  - Hora salida: 14:30 (todos los días)
  - Tipo: Continental
  - Tiempo procesamiento: 2 días

⏱️ Cálculo:
1. Pedido llega: Día 15, 10:00
2. Vuelo más cercano: Día 15, 14:30
3. Tiempo hasta vuelo: 14:30 - 10:00 = 4.5 horas (270 minutos)
4. ¿Cumple anticipación? 270 min > 30 min ✅ SÍ
5. Tiempo procesamiento: 2 días = 2880 minutos
6. Tiempo espera TOTAL: 270 + 2880 = 3150 minutos = 2.19 días

✅ RESULTADO: Pedido espera ~2.2 días para llegar
```

#### Caso 2: Pedido que Pierde el Vuelo

```
📦 Pedido:
  - Día: 15
  - Hora: 14:15
  - Destino: SEQM

✈️ Vuelo SKBO→SEQM:
  - Hora salida: 14:30 (todos los días)

⏱️ Cálculo:
1. Pedido llega: Día 15, 14:15
2. Vuelo del día: Día 15, 14:30
3. Tiempo hasta vuelo: 14:30 - 14:15 = 15 minutos
4. ¿Cumple anticipación? 15 min < 30 min ❌ NO
5. Debe tomar vuelo del DÍA SIGUIENTE
6. Vuelo disponible: Día 16, 14:30
7. Tiempo hasta vuelo: 24h 15min = 1455 minutos
8. Tiempo procesamiento: 2 días = 2880 minutos
9. Tiempo espera TOTAL: 1455 + 2880 = 4335 minutos = 3.01 días

❌ RESULTADO: Pedido espera ~3 días (perdió el vuelo por 15 minutos)
```

#### Caso 3: Pedido Intercontinental

```
📦 Pedido:
  - Día: 20
  - Hora: 08:00
  - Destino: LEMD (Europa)

✈️ Vuelo SKBO→LEMD:
  - Hora salida: 22:00 (todos los días)
  - Tipo: Intercontinental
  - Tiempo procesamiento: 5 días

⏱️ Cálculo:
1. Pedido llega: Día 20, 08:00
2. Vuelo más cercano: Día 20, 22:00
3. Tiempo hasta vuelo: 22:00 - 08:00 = 14 horas (840 minutos)
4. ¿Cumple anticipación? 840 min > 30 min ✅ SÍ
5. Tiempo procesamiento: 5 días = 7200 minutos
6. Tiempo espera TOTAL: 840 + 7200 = 8040 minutos = 5.58 días

✅ RESULTADO: Pedido espera ~5.6 días para llegar
```

---

## 🚀 Uso del Algoritmo con los Nuevos Parámetros

### 1. Preparar los Datos

```java
// Cargar datos desde los servicios
List<Pedido> pedidos = pedidoService.obtenerTodos();
List<PlanDeVuelo> vuelos = planDeVueloService.obtenerTodos();
List<Aeropuerto> aeropuertos = aeropuertoService.obtenerTodos();
```

### 2. Inicializar el Algoritmo

```java
genetico ag = new genetico();

// IMPORTANTE: Ahora necesita 3 parámetros (pedidos, vuelos, aeropuertos)
ag.inicializar(pedidos, vuelos, aeropuertos);
```

### 3. Ejecutar

```java
genetico.Cromosoma mejorSolucion = ag.ejecutar();
```

### 4. Ver Resultados

```java
ag.imprimirEstadisticas(mejorSolucion);
```

### Ejemplo Completo

```java
@Service
@RequiredArgsConstructor
public class OptimizacionService {
    
    private final PedidoService pedidoService;
    private final PlanDeVueloService planDeVueloService;
    private final AeropuertoService aeropuertoService;
    
    public Map<String, List<Pedido>> optimizarAsignaciones() {
        // 1. Cargar datos
        List<Pedido> pedidos = pedidoService.obtenerTodos();
        List<PlanDeVuelo> vuelos = planDeVueloService.obtenerTodos();
        List<Aeropuerto> aeropuertos = aeropuertoService.obtenerTodos();
        
        // 2. Inicializar algoritmo genético
        genetico ag = new genetico();
        ag.inicializar(pedidos, vuelos, aeropuertos);
        
        // 3. Ejecutar optimización
        genetico.Cromosoma solucion = ag.ejecutar();
        
        // 4. Imprimir estadísticas
        ag.imprimirEstadisticas(solucion);
        
        // 5. Convertir a formato legible
        Map<String, List<Pedido>> asignaciones = ag.convertirASolucion(solucion);
        
        return asignaciones;
    }
}
```

---

## 📊 Impacto en el Fitness

### Función de Fitness Actualizada

```java
fitness = CostoTiempoEspera + 
          CostoSobrecapacidad + 
          CostoVuelosUsados - 
          BonusUtilización

Donde:
- CostoTiempoEspera = Σ(tiempoEsperaDias * 10.0) por cada pedido
- CostoSobrecapacidad = exceso * 1000.0 (si se excede capacidad)
- CostoVuelosUsados = 1.0 por cada vuelo usado
- BonusUtilización = -0.5 * (carga/capacidad) por cada vuelo
```

### Ejemplo de Fitness

```
Escenario:
- Vuelo 1 (SKBO→SEQM, Continental):
  * Pedido A: espera 2.2 días
  * Pedido B: espera 2.5 días
  * Pedido C: espera 3.0 días
  * Carga total: 250/300 (83% utilización)

- Vuelo 2 (SKBO→LEMD, Intercontinental):
  * Pedido D: espera 5.6 días
  * Pedido E: espera 6.0 días
  * Carga total: 180/200 (90% utilización)

Cálculo Fitness:
1. Tiempo espera:
   (2.2 + 2.5 + 3.0 + 5.6 + 6.0) * 10.0 = 193.0

2. Vuelos usados:
   2 * 1.0 = 2.0

3. Bonus utilización:
   -(0.83 * 0.5) - (0.90 * 0.5) = -0.865

TOTAL FITNESS: 193.0 + 2.0 - 0.865 = 194.135
```

---

## 🎯 Optimización del Tiempo de Espera

### Estrategias del Algoritmo Genético

1. **Asignar pedidos tempranos a vuelos tempranos**
   - Minimiza tiempo de espera hasta el vuelo

2. **Preferir vuelos continentales cuando sea posible**
   - 2 días vs 5 días de procesamiento

3. **Maximizar utilización de vuelos**
   - Reduce cantidad de vuelos necesarios

4. **Evitar pérdida de vuelos**
   - Respetar los 30 minutos de anticipación

### Ejemplo de Mejora

#### ❌ Solución Mala

```
Pedido llegó: Día 10, 10:00
Vuelo asignado: Día 15, 14:00 (Continental)
Tiempo espera: 5 días + 2 días = 7 días
```

#### ✅ Solución Óptima

```
Pedido llegó: Día 10, 10:00
Vuelo asignado: Día 10, 14:00 (Continental)
Tiempo espera: 4 horas + 2 días = 2.17 días
Mejora: 4.83 días menos!
```

---

## 📝 Logs del Sistema

### Ejemplo de Salida

```
=== ALGORITMO GENÉTICO INICIALIZADO ===
Pedidos a asignar: 150
Vuelos disponibles: 25
Aeropuertos cargados: 500
Población: 100
Generaciones máximas: 500
Tiempo anticipación vuelo: 30 minutos
Tiempo procesamiento continental: 2 días
Tiempo procesamiento intercontinental: 5 días
=======================================

Generación 0 - Mejor fitness: 2845.67
Generación 15 - ¡NUEVO MEJOR! Fitness: 1987.23
Generación 38 - ¡NUEVO MEJOR! Fitness: 1542.89
Generación 72 - ¡NUEVO MEJOR! Fitness: 998.45
...
Convergencia alcanzada en generación 234

=== ALGORITMO GENÉTICO COMPLETADO ===
Tiempo total: 4521 ms
Mejor fitness: 865.32
Solución válida: true
=====================================

=== ESTADÍSTICAS DE LA SOLUCIÓN ===
Vuelos utilizados: 18
Fitness total: 865.32
Solución válida: true
Ruta SKBO-SEQM (Continental): 22 pedidos, carga total: 1890
  - Tiempo espera promedio: 2.3 días
Ruta SKBO-LEMD (Intercontinental): 15 pedidos, carga total: 1250
  - Tiempo espera promedio: 5.8 días
...
===================================
```

---

## ⚙️ Ajuste de Parámetros

### Si quieres cambiar los tiempos:

```java
// Reducir tiempo de anticipación (más arriesgado)
private static final int TIEMPO_ANTICIPACION_VUELO = 15; // minutos

// Aumentar tiempo continental (más conservador)
private static final int TIEMPO_PROCESAMIENTO_CONTINENTAL = 3; // días

// Reducir tiempo intercontinental (más agresivo)
private static final int TIEMPO_PROCESAMIENTO_INTERCONTINENTAL = 4; // días
```

### Si quieres cambiar el peso del tiempo en el fitness:

```java
// Penalizar más el tiempo de espera
private static final double PENALIZACION_TIEMPO = 20.0;

// Penalizar menos el tiempo de espera
private static final double PENALIZACION_TIEMPO = 5.0;
```

---

## 🧪 Casos de Prueba

### Caso 1: Pedido Justo a Tiempo

```java
Pedido pedido = new Pedido(15, 14, 0, "SEQM", 100, "0001");
// Llega a las 14:00, vuelo sale 14:30
// Tiempo hasta vuelo: 30 minutos ✅ JUSTO
```

### Caso 2: Pedido Tarde

```java
Pedido pedido = new Pedido(15, 14, 5, "SEQM", 100, "0001");
// Llega a las 14:05, vuelo sale 14:30
// Tiempo hasta vuelo: 25 minutos ❌ TARDE
// Debe tomar vuelo del día siguiente
```

### Caso 3: Mix Continental e Intercontinental

```java
// Continental
Pedido p1 = new Pedido(10, 10, 0, "SEQM", 100, "0001");
// Intercontinental
Pedido p2 = new Pedido(10, 10, 0, "LEMD", 100, "0002");

// El algoritmo debe preferir asignar p1 a vuelos continentales
// para minimizar tiempo de espera total
```

---

**Fecha**: Octubre 2025  
**Sistema**: MoraPack - Optimización con Restricciones Temporales  
**Estado**: ✅ Implementado y documentado
