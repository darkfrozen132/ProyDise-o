# 🧬 Algoritmo Genético para Asignación de Pedidos a Vuelos

## 📋 Descripción General

Implementación de un **Algoritmo Genético (AG)** para resolver el problema de asignación óptima de pedidos a planes de vuelo en el sistema MoraPack.

### 🎯 Objetivo

Minimizar:
- ⏱️ Tiempo de espera de los pedidos
- ✈️ Cantidad de vuelos utilizados  
- 📦 Desperdicpercusión de capacidad

Maximizar:
- 📊 Utilización de capacidad de vuelos
- 🎯 Satisfacción de restricciones

---

## 🧬 Conceptos del Algoritmo Genético

### 1. Cromosoma (Individuo)

Representa una **solución completa** al problema de asignación.

```
Estructura: Array de enteros
Tamaño: Número de pedidos

Ejemplo con 5 pedidos y 3 vuelos:
Cromosoma: [2, 1, 2, 0, 1]

Interpretación:
- Pedido 0 → Vuelo 2
- Pedido 1 → Vuelo 1  
- Pedido 2 → Vuelo 2
- Pedido 3 → Vuelo 0
- Pedido 4 → Vuelo 1
```

### 2. Gen

Un **gen** es un elemento del cromosoma que representa:
- **Índice del vuelo** asignado a un pedido específico

### 3. Población

Conjunto de **cromosomas** (soluciones candidatas).

```
Población = [Cromosoma1, Cromosoma2, ..., CromosomaB]
Tamaño por defecto: 100 individuos
```

### 4. Fitness (Aptitud)

Función que **evalúa la calidad** de una solución.

**Menor fitness = Mejor solución**

```java
Fitness = CostoTiempoEspera + 
          CostoSobrecapacidad + 
          CostoVuelosUsados - 
          BonusUtilización
```

---

## 🔧 Parámetros del Algoritmo

| Parámetro | Valor | Descripción |
|-----------|-------|-------------|
| **TAMANIO_POBLACION** | 100 | Número de individuos por generación |
| **GENERACIONES_MAX** | 500 | Máximo de iteraciones |
| **TASA_MUTACION** | 0.1 (10%) | Probabilidad de mutación por individuo |
| **TASA_CRUCE** | 0.8 (80%) | Probabilidad de cruce entre padres |
| **ELITISMO** | 5 | Mejores individuos que pasan sin cambios |
| **PENALIZACION_SOBRECAPACIDAD** | 1000.0 | Penalización por exceder capacidad |

---

## 🔄 Flujo del Algoritmo

```
┌─────────────────────────────────────┐
│ 1. INICIALIZACIÓN                   │
│    - Cargar pedidos y vuelos        │
│    - Crear población aleatoria      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ 2. EVALUACIÓN                       │
│    - Calcular fitness de c/individuo│
│    - Identificar mejor solución     │
└──────────────┬──────────────────────┘
               │
               │ ┌─────────────────────┐
               ├─┤ Para cada generación│
               │ └─────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ 3. SELECCIÓN                        │
│    - Torneo: elegir padres          │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ 4. CRUCE (Crossover)                │
│    - Combinar padres → hijos        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ 5. MUTACIÓN                         │
│    - Alterar aleatoriamente genes   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ 6. REEMPLAZO                        │
│    - Elitismo + nueva generación    │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│ 7. CONVERGENCIA?                    │
│    ¿Solución óptima o max gens?     │
└─────────┬─────────────┬─────────────┘
          │ NO          │ SI
          └─────┐       │
                │       │
            ┌───▼───────▼──┐
            │  RESULTADO   │
            │ Mejor solución│
            └──────────────┘
```

---

## 🧬 Operadores Genéticos

### 1️⃣ Selección por Torneo

Elige los **mejores padres** para reproducción.

```java
Torneo de tamaño k=5:
1. Elegir 5 individuos aleatorios
2. Seleccionar el de MENOR fitness
3. Repetir para elegir segundo padre
```

**Ventajas:**
- ✅ Mantiene diversidad
- ✅ Presión selectiva controlable
- ✅ Computacionalmente eficiente

---

### 2️⃣ Cruce de Un Punto

Combina **genes de dos padres** para crear un hijo.

```
Padre 1: [2, 1, 0, 3, 2]
Padre 2: [1, 3, 2, 0, 1]
                ↑
         Punto de corte

Hijo:    [2, 1, 0, 0, 1]
         └─Padre1─┘ └Padre2┘
```

**Implementación:**
```java
int puntoCorte = random.nextInt(longitud);
hijo[0..puntoCorte] = padre1[0..puntoCorte]
hijo[puntoCorte..fin] = padre2[puntoCorte..fin]
```

---

### 3️⃣ Mutación Aleatoria

Cambia **genes aleatorios** para explorar nuevas soluciones.

```
Original: [2, 1, 0, 3, 2]
              ↓
Mutado:   [2, 0, 0, 3, 2]
             (gen mutado a vuelo 0)
```

**Implementación:**
```java
Para cada gen:
    Si random() < 0.1:
        gen = vueloAleatorio()
```

**Propósito:**
- 🔍 Exploración del espacio de soluciones
- 🚫 Evitar convergencia prematura
- 🎲 Introducir diversidad

---

### 4️⃣ Elitismo

Los **5 mejores individuos** pasan automáticamente a la siguiente generación.

```
Generación N:
  Individuo 1: fitness=10.5  ← Elite
  Individuo 2: fitness=12.3  ← Elite
  Individuo 3: fitness=15.7  ← Elite
  Individuo 4: fitness=18.2  ← Elite
  Individuo 5: fitness=19.1  ← Elite
  ... resto ...

Generación N+1:
  ✓ Se copian directamente los 5 mejores
  + 95 nuevos por cruce/mutación
```

**Ventajas:**
- ✅ Garantiza no perder las mejores soluciones
- ✅ Acelera convergencia
- ✅ Estabiliza el algoritmo

---

## 📊 Función de Fitness

### Componentes del Fitness

```java
fitness = 0.0

// 1. Penalización por sobrecapacidad
Para cada vuelo:
    Si cargaTotal > capacidadMaxima:
        exceso = cargaTotal - capacidadMaxima
        fitness += exceso * 1000.0  // Muy penalizado

// 2. Costo por usar vuelos
Para cada vuelo utilizado:
    fitness += 1.0

// 3. Bonus por utilización eficiente
Para cada vuelo:
    utilizacion = cargaTotal / capacidadMaxima
    fitness -= utilizacion * 0.5  // Recompensa
```

### Ejemplo de Cálculo

```
Escenario:
- Vuelo 1: Capacidad=300, Carga=280 → Utilización=93%
- Vuelo 2: Capacidad=200, Carga=150 → Utilización=75%
- Vuelo 3: Capacidad=250, Carga=270 → ¡SOBRECAPACIDAD! +20

Cálculo:
fitness = 0
fitness += 1.0          // Vuelo 1 usado
fitness -= 0.93*0.5     // Bonus=-0.465
fitness += 1.0          // Vuelo 2 usado
fitness -= 0.75*0.5     // Bonus=-0.375
fitness += 1.0          // Vuelo 3 usado
fitness += 20*1000.0    // Penalización=20000
fitness -= 1.08*0.5     // Bonus=-0.540

TOTAL: 20000.62 (solución INVÁLIDA por sobrecapacidad)
```

---

## 🚀 Uso del Algoritmo

### 1. Crear Instancia

```java
genetico ag = new genetico();
```

### 2. Cargar Datos

```java
List<Pedido> pedidos = pedidoService.obtenerTodos();
List<PlanDeVuelo> vuelos = planDeVueloService.obtenerTodos();

ag.inicializar(pedidos, vuelos);
```

### 3. Ejecutar

```java
genetico.Cromosoma mejorSolucion = ag.ejecutar();
```

### 4. Obtener Resultados

```java
// Convertir a formato legible
Map<String, List<Pedido>> asignaciones = ag.convertirASolucion(mejorSolucion);

// Imprimir estadísticas
ag.imprimirEstadisticas(mejorSolucion);
```

### Ejemplo de Salida

```
=== ALGORITMO GENÉTICO INICIALIZADO ===
Pedidos a asignar: 150
Vuelos disponibles: 25
Población: 100
Generaciones máximas: 500
=======================================

Generación 0 - Mejor fitness: 245.67
Generación 23 - ¡NUEVO MEJOR! Fitness: 187.23
Generación 45 - ¡NUEVO MEJOR! Fitness: 142.89
Generación 78 - ¡NUEVO MEJOR! Fitness: 98.45
Generación 100 - Mejor fitness actual: 98.45
...
Convergencia alcanzada en generación 234

=== ALGORITMO GENÉTICO COMPLETADO ===
Tiempo total: 3542 ms
Mejor fitness: 65.32
Solución válida: true
=====================================

=== ESTADÍSTICAS DE LA SOLUCIÓN ===
Vuelos utilizados: 12
Fitness total: 65.32
Solución válida: true
Ruta SKBO-SEQM: 15 pedidos, carga total: 1250
Ruta SEQM-SEGU: 22 pedidos, carga total: 1890
...
===================================
```

---

## 🎯 Convergencia

El algoritmo se detiene cuando:

### 1. Máximo de Generaciones
- Alcanza **500 generaciones**

### 2. Convergencia Detectada
- Baja variabilidad en fitness de la población
- Desviación estándar < 0.01

```java
desviacion = sqrt(Σ(fitness_i - media)² / n)

Si desviacion < 0.01:
    → CONVERGED
```

---

## 🔬 Ventajas del Algoritmo Genético

| Ventaja | Descripción |
|---------|-------------|
| 🌐 **Global** | Explora múltiples regiones del espacio |
| 🔀 **Robusto** | No se queda en óptimos locales |
| 🎲 **Estocástico** | Usa aleatoriedad para exploración |
| 📈 **Escalable** | Funciona con problemas grandes |
| 🔧 **Flexible** | Fácil adaptar a nuevas restricciones |

---

## ⚙️ Personalización

### Ajustar Parámetros

```java
// En la clase genetico.java

// Población más grande (más exploración, más lento)
private static final int TAMANIO_POBLACION = 200;

// Más generaciones (mejor solución, más tiempo)
private static final int GENERACIONES_MAX = 1000;

// Mayor mutación (más exploración)
private static final double TASA_MUTACION = 0.2;

// Más elitismo (convergencia más rápida)
private static final int ELITISMO = 10;
```

### Modificar Fitness

```java
// Agregar penalización por tiempo de espera
double tiempoEspera = calcularTiempoEspera(pedido, vuelo);
fitness += tiempoEspera * 10.0;

// Penalizar cambios de aeropuerto
if (requiereCambioAeropuerto(pedido, vuelo)) {
    fitness += 50.0;
}
```

---

## 📚 Próximas Mejoras

- [ ] Implementar cálculo de tiempo de espera
- [ ] Agregar restricciones de horarios
- [ ] Cruce de dos puntos
- [ ] Mutación adaptativa
- [ ] Visualización de evolución
- [ ] Paralelización de evaluación de fitness
- [ ] Algoritmo híbrido (AG + búsqueda local)

---

## 🧪 Testing

### Casos de Prueba Sugeridos

```java
// 1. Caso simple
pedidos = 10
vuelos = 3
→ Debería asignar fácilmente

// 2. Caso complejo
pedidos = 1000
vuelos = 50
→ Probar escalabilidad

// 3. Caso con sobrecapacidad
pedidos con carga > capacidad total
→ Verificar que detecta inviabilidad

// 4. Caso óptimo conocido
pedidos que encajan perfecto
→ Verificar que encuentra óptimo
```

---

## 📖 Referencias

- **Goldberg, D. E.** (1989). *Genetic Algorithms in Search, Optimization, and Machine Learning*
- **Mitchell, M.** (1998). *An Introduction to Genetic Algorithms*
- **Holland, J. H.** (1992). *Adaptation in Natural and Artificial Systems*

---

**Autor**: Sistema MoraPack  
**Fecha**: Octubre 2025  
**Versión**: 1.0  
**Estado**: ✅ Base implementada - Lista para extensión
