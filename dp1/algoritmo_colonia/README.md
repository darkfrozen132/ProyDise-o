# Algoritmo de Colonia de Hormigas (ACO) - Framework Java

Framework completo de **Ant Colony Optimization** implementado en Java, diseñado para ser reutilizable y aplicable a cualquier problema de optimización.

## 🐜 Características del Framework

### ✅ **Componentes Principales**
1. **Hormiga** - Clase base abstracta para construir soluciones
2. **Colonia** - Maneja la población de hormigas
3. **Feromona** - Sistema de memoria química para marcar buenos caminos
4. **Heurística** - Conocimiento específico del problema
5. **AlgoritmoColoniaHormigas** - Motor principal del ACO

### 🏗️ **Arquitectura Modular**
```
src/colonia/
├── Hormiga.java                    # Clase base para hormigas
├── Colonia.java                    # Manejo de población
├── Feromona.java                   # Sistema de feromonas
├── Heuristica.java                 # Información heurística
├── algoritmo/
│   └── AlgoritmoColoniaHormigas.java # Motor principal ACO
└── componentes/
    └── UtilACO.java                # Utilidades y herramientas
```

## 🧬 Algoritmo ACO

### Flujo Principal
1. **Inicialización**: Crear colonia, feromonas, heurística
2. **Construcción**: Cada hormiga construye una solución paso a paso
3. **Evaluación**: Calcular calidad de las soluciones
4. **Actualización**: Depositar feromonas en buenos caminos
5. **Evaporación**: Reducir feromonas para evitar convergencia prematura
6. **Iteración**: Repetir hasta criterio de parada

### Fórmula de Probabilidad ACO
```
P(i,j) = [τ(i,j)]^α × [η(i,j)]^β / Σ[τ(i,k)]^α × [η(i,k)]^β

donde:
- τ(i,j) = nivel de feromona entre i y j
- η(i,j) = información heurística entre i y j  
- α = factor de importancia de feromona
- β = factor de importancia de heurística
```

## 📊 Parámetros del Algoritmo

| Parámetro | Símbolo | Rango Típico | Descripción |
|-----------|---------|--------------|-------------|
| Número de hormigas | m | 10-100 | Tamaño de la colonia |
| Factor feromona | α | 0.5-2.0 | Importancia de rastros de feromona |
| Factor heurístico | β | 1.0-5.0 | Importancia de información heurística |
| Evaporación | ρ | 0.1-0.3 | Tasa de evaporación de feromonas |
| Iteraciones | - | 100-1000 | Número máximo de iteraciones |

## 🚀 Cómo Usar el Framework

### 1. Extender la Clase Hormiga
```java
public class HormigaProblema extends Hormiga {
    @Override
    public void construirSolucion(Feromona feromona, Heuristica heuristica) {
        // Implementar construcción paso a paso
    }
    
    @Override
    public void evaluarSolucion() {
        // Calcular calidad de la solución
    }
    
    // Implementar métodos abstractos restantes
}
```

### 2. Implementar Heurística Específica
```java
public class HeuristicaProblema extends Heuristica {
    @Override
    public double calcularValor(int origen, int destino) {
        // Calcular información heurística específica del problema
        return valorHeuristico;
    }
}
```

### 3. Configurar y Ejecutar
```java
AlgoritmoColoniaHormigas aco = new AlgoritmoColoniaHormigas();
aco.configurar(50, 1000, 1.0, 2.0, 0.1, 1.0);
aco.setDebug(true);

HeuristicaProblema heuristica = new HeuristicaProblema(2.0);
aco.inicializar(tamanoProblem, HormigaProblema.class, heuristica);

ResultadoACO resultado = aco.ejecutar();
```

## 🎯 Ventajas del ACO

### ✅ **Fortalezas**
- **Búsqueda distribuida**: Múltiples hormigas exploran el espacio
- **Memoria adaptativa**: Feromonas refuerzan buenas soluciones
- **Balance exploración/explotación**: α y β controlan el balance
- **Convergencia controlada**: Evaporación evita mínimos locales
- **Paralelizable**: Hormigas pueden trabajar independientemente

### 🎨 **Aplicaciones Típicas**
- **TSP** (Traveling Salesman Problem)
- **Ruteo de vehículos**
- **Scheduling y asignación**
- **Redes y telecomunicaciones**
- **Problemas de path-finding**

## 🔧 Configuración Avanzada

### Parámetros Recomendados por Problema
```java
// Para problemas pequeños (< 50 nodos)
aco.configurar(30, 500, 1.0, 2.0, 0.1, 1.0);

// Para problemas medianos (50-200 nodos)  
aco.configurar(50, 1000, 1.0, 3.0, 0.1, 1.0);

// Para problemas grandes (> 200 nodos)
aco.configurar(100, 2000, 0.5, 5.0, 0.05, 1.0);
```

### Control de Convergencia
```java
aco.setMaxIteracionesSinMejora(100); // Parada temprana
aco.getFeromona().setLimites(0.01, 10.0); // Límites de feromona
```

## 📈 Análisis de Resultados

### Métricas Disponibles
- **Calidad de solución**: Valor objetivo de la mejor solución
- **Convergencia**: Iteración donde se encontró la mejor solución
- **Diversidad**: Estadísticas de la colonia (promedio, desviación)
- **Distribución de feromonas**: Min, max, promedio de feromonas
- **Tiempo de ejecución**: Performance del algoritmo

### Estadísticas Detalladas
```java
EstadisticasACO stats = aco.obtenerEstadisticas();
System.out.println("Mejor calidad: " + stats.colonia.mejorCalidad);
System.out.println("Promedio colonia: " + stats.colonia.calidadPromedio);
System.out.println("Feromona promedio: " + stats.feromona.promedio);
```

## 🐜 Tipos de Variantes ACO

El framework soporta fácilmente diferentes variantes:

- **AS** (Ant System) - Versión original
- **ACS** (Ant Colony System) - Con selección pseudoaleatoria
- **MMAS** (Max-Min Ant System) - Con límites de feromona
- **Elitist AS** - Solo mejores hormigas depositan feromona

## 🔬 Investigación y Extensiones

### Posibles Mejoras
- **Búsqueda local**: Aplicar mejoramiento post-construcción
- **Hibridación**: Combinar con otros metaheurísticos
- **Paralelización**: Distribuir hormigas en múltiples threads
- **Aprendizaje adaptativo**: Ajustar parámetros dinámicamente

---

**Framework desarrollado para optimización con Algoritmos de Colonia de Hormigas**  
*Inspirado en el comportamiento inteligente de colonias de hormigas reales*
