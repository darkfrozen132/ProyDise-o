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
