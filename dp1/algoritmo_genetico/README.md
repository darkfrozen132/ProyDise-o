# Framework de Algoritmo Genético

Framework genérico y reutilizable de algoritmos genéticos en Java.

## 🧬 Estructura del Framework

```
algoritmo_genetico/
├── src/
│   └── genetico/
│       ├── Individuo.java          # Clase base abstracta para individuos
│       ├── Poblacion.java          # Manejo de poblaciones
│       ├── algoritmo/
│       │   └── AlgoritmoGenetico.java  # Motor principal del AG
│       └── operators/
│           ├── cruce/              # Operadores de cruzamiento
│           │   ├── OperadorCruce.java
│           │   ├── CruceUnPunto.java
│           │   ├── CruceDosPuntos.java
│           │   ├── CruceUniforme.java
│           │   ├── CruceAritmetico.java
│           │   └── CruceBLX.java
│           ├── mutacion/           # Operadores de mutación
│           │   ├── OperadorMutacion.java
│           │   ├── MutacionBitFlip.java
│           │   ├── MutacionGaussiana.java
│           │   ├── MutacionIntercambio.java
│           │   ├── MutacionInsercion.java
│           │   ├── MutacionInversion.java
│           │   └── MutacionPuntoAPunto.java
│           └── seleccion/          # Operadores de selección
│               ├── OperadorSeleccion.java
│               ├── SeleccionRuleta.java
│               ├── SeleccionTorneo.java
│               └── SeleccionElitista.java
└── README.md
```

## 🚀 Características

### Framework Genérico
- **Tipado genérico** (`<T>`) para cualquier tipo de gen
- **Arquitectura modular** con operadores intercambiables
- **Elitismo configurable** para preservar mejores soluciones
- **Estadísticas automáticas** de evolución por generación

### Operadores Incluidos

#### Cruzamiento
- **Un punto** - Cruce clásico en un punto de corte
- **Dos puntos** - Cruce en dos puntos de corte  
- **Uniforme** - Intercambio gen por gen con probabilidad
- **Aritmético** - Para números reales (promedio ponderado)
- **BLX-α** - Cruce difuso para optimización continua

#### Mutación
- **Bit flip** - Inversión de bits para binario
- **Gaussiana** - Ruido gaussiano para reales
- **Intercambio** - Swap de posiciones para permutaciones
- **Inserción** - Mover elemento a nueva posición
- **Inversión** - Invertir subsecuencia
- **Punto a punto** - Mutación de caracteres individuales

#### Selección
- **Ruleta** - Selección proporcional al fitness
- **Torneo** - Competencia entre k individuos
- **Elitista** - Selección de los mejores directamente

## 💻 Uso del Framework

### 1. Implementar Individuo Personalizado
```java
public class MiIndividuo extends Individuo<TipoGen> {
    @Override
    protected TipoGen[] crearGenotipo(int tamaño) {
        return new TipoGen[tamaño];
    }
    
    @Override
    public double calcularFitness() {
        // Implementar función objetivo específica
    }
    
    @Override
    public void inicializarAleatorio() {
        // Inicialización específica del problema
    }
    
    @Override
    public Individuo<TipoGen> clonar() {
        // Clonación específica
    }
}
```

### 2. Configurar y Ejecutar Algoritmo
```java
AlgoritmoGenetico<TipoGen> ag = new AlgoritmoGenetico<>(100, 500);
ag.setProbabilidadCruce(0.8);
ag.setProbabilidadMutacion(0.1);
ag.setOperadorSeleccion(new SeleccionTorneo<>(3));
ag.setOperadorCruce(new CruceUniforme<>(0.5));
ag.setOperadorMutacion(new MutacionIntercambio<>());

Poblacion<TipoGen> poblacion = /* crear población inicial */;
Individuo<TipoGen> mejor = ag.ejecutar(poblacion);
```

## 🔧 Compilación

```bash
cd algoritmo_genetico/
javac -d bin src/genetico/*.java src/genetico/*/*.java src/genetico/*/*/*.java
```

## ⚙️ Parámetros Configurables

- **Tamaño de población** - Número de individuos por generación
- **Número de generaciones** - Criterio de parada principal  
- **Probabilidad de cruce** - Fracción de individuos que se cruzan
- **Probabilidad de mutación** - Fracción de genes que mutan
- **Elitismo** - Preservar mejores individuos entre generaciones
- **Tamaño elite** - Número de individuos elite a preservar

## 📊 Estadísticas Automáticas

El framework recolecta automáticamente:
- Fitness promedio por generación
- Fitness máximo por generación  
- Progreso de evolución
- Mejor individuo encontrado

## 🎯 Casos de Uso

Este framework es adecuado para problemas de:
- **Optimización combinatoria** (TSP, asignación, scheduling)
- **Optimización continua** (ajuste de parámetros) 
- **Diseño evolutivo** (redes neuronales, algoritmos)
- **Búsqueda de patrones** (reglas, secuencias)

## 📋 Requisitos

- Java 8+
- Sin dependencias externas
- Framework completamente autónomo

---

## 📑 Pseudocódigo Simple de las Clases Java Principales

### Individuo.java
```
Clase abstracta Individuo:
    - genotipo: arreglo de genes
    - fitness: valor de aptitud
    Métodos:
        crearGenotipo(tamaño)
        calcularFitness()
        clonar()
        inicializarAleatorio()
        get/set genotipo y genes
        getFitness() // calcula si no está calculado
```

### Poblacion.java
```
Clase Poblacion:
    - individuos: lista de Individuo
    - tamaño: máximo
    Métodos:
        añadirIndividuo()
        getIndividuo(indice)
        ordenar() // por fitness
        getMejorIndividuo()
        getFitnessPromedio/Maximo/Minimo()
        limpiar()
```

### AlgoritmoGenetico.java
```
Clase AlgoritmoGenetico:
    - operadores: seleccion, cruce, mutacion
    - parámetros: tamañoPoblacion, generaciones, probabilidadCruce, probabilidadMutacion, elitismo
    Métodos:
        ejecutar(poblacionInicial):
            por cada generación:
                evaluar fitness
                registrar estadísticas
                crear nueva generación (selección, cruce, mutación, elitismo)
            devolver mejor individuo
```

### Operadores (Cruce, Mutación, Selección)
```
Interfaz OperadorCruce:
    cruzar(padre1, padre2) -> hijos

Interfaz OperadorMutacion:
    mutar(individuo, probabilidad)

Interfaz OperadorSeleccion:
    seleccionar(poblacion) -> individuo
```

Ejemplo de implementación:
```
CruceUnPunto:
    - Elegir punto de corte
    - Intercambiar genes después del punto

MutacionBitFlip:
    - Para cada gen, con probabilidad, invertir el bit

SeleccionRuleta:
    - Seleccionar individuo proporcional a su fitness
```

---

## 📑 Pseudocódigo de AlgoritmoGenético_WRoute (Ejemplo de aplicación)

```
Función AlgoritmoGenético_WRoute():
    • Inicializar parámetros
        - Definir poblaciónTamaño, tasaMutación, generaciones
        - Cargar datos de entrada: pedidos, camiones, tanques, y la grilla de rutas
    • Inicializar población:
        - población ← InicializarPoblación(poblaciónTamaño, pedidos, camiones)
    • Bucle evolutivo:
        Para i desde 1 hasta generaciones:
            ○ Para cada cromosoma en población:
                – calcularFitness ← EvaluarFitness(cromosoma)
            ○ Seleccionar el mejor cromosoma de la población actual
            ○ Si el mejor cromosoma tiene mayor calidad que la solución global:
                – actualizar mejorSolución
            ○ Generar nuevos individuos:
                – padres ← SeleccionarPadres(población)
                – hijo ← Cruzar(padres)
                – hijo ← Mutar(hijo, tasaMutación)
            ○ Reemplazar uno (o algunos) de los cromosomas de la población con el hijo
    • Retornar:
        - mejorSolución con el menor costo (mejor aptitud) encontrado

Función InicializarPoblación(tamaño, pedidos, camiones):
    • Crear una lista vacía población
    • Para i desde 1 hasta tamaño:
        ○ Inicializar un cromosoma vacío
        ○ Para cada camión en camiones:
            – Generar una ruta aleatoria (mezclando el orden de los pedidos)
            – Agregar la configuración (asociada al camión) al cromosoma
        ○ Agregar el cromosoma a población
    • Retornar población

Función EvaluarFitness(cromosoma):
    • Inicializar totalCosto = 0
    • Para cada par (camión, ruta) en el cromosoma:
        ○ Simular la ruta en la grilla para obtener métricas (distancia recorrida, tiempo de entrega, consumo de combustible, cumplimiento de plazos)
        ○ Calcular el costo de la ruta utilizando estos factores
        ○ Acumular el costo en totalCosto
    • Retornar totalCosto

Función Cruzar(padres):
    • Recibir dos padres (por ejemplo, padre1 y padre2)
    • Para cada posición en la secuencia de la ruta:
        ○ Si la posición es par, tomar el gen (índice del pedido) de padre1; de lo contrario, de padre2
    • Formar el cromosoma hijo con la combinación resultante
    • Retornar el cromosoma hijo
```

---
