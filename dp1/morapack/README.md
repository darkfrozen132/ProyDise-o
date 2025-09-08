# Proyecto MoraPack - Optimización de Envíos

Sistema de optimización de asignación de pedidos a sedes usando algoritmos genéticos para la empresa de logística MoraPack.

## Estructura del Proyecto

```
morapack/
├── src/
│   └── morapack/
│       ├── genetico/          # Clases base para algoritmo genético
│       ├── modelo/            # Modelos de dominio (Aeropuerto, Vuelo, Pedido)
│       ├── datos/             # Cargadores de datos desde CSV
│       ├── optimizacion/      # Algoritmo de optimización específico
│       └── main/              # Programa principal
├── bin/                       # Archivos compilados (.class)
├── compilar.sh               # Script de compilación
├── ejecutar.sh               # Script de ejecución
└── README.md                 # Este archivo
```

## Descripción del Problema

MoraPack es una empresa de logística con 3 sedes principales:
- **Lima, Perú** (América del Sur)
- **Bruselas, Bélgica** (Europa)
- **Bakú, Azerbaiyán** (Asia)

### Objetivos
- Minimizar costos totales de envío
- Cumplir con plazos de entrega (2 días mismo continente, 3 días intercontinental)
- Balancear carga entre sedes
- Optimizar asignación de pedidos a sedes

### Restricciones
- Costos intercontinentales 50% más altos
- Tiempos de envío: 12h mismo continente, 24h intercontinental
- Prioridades de pedidos afectan costos
- Capacidad limitada de sedes

## Cómo Usar

### 1. Compilar
```bash
./compilar.sh
```

### 2. Ejecutar
```bash
./ejecutar.sh
```

### 3. Datos Requeridos
El programa busca archivos CSV en la carpeta `../../../datos/`:
- `aeropuertos_simple.csv` - Lista de aeropuertos con coordenadas
- `vuelos_simple.csv` - Vuelos disponibles entre aeropuertos

## Algoritmo Genético

### Parámetros por Defecto
- **Población**: 100 individuos
- **Generaciones**: 1000
- **Probabilidad de Cruce**: 0.8
- **Probabilidad de Mutación**: 0.1

### Representación
- Cada individuo es un vector de asignaciones `[sede_pedido1, sede_pedido2, ...]`
- Genes: índices de sedes (0, 1, 2)
- Función de fitness: `1 / (1 + costo_total + penalizaciones)`

### Operadores
- **Selección**: Torneo de tamaño 3
- **Cruce**: Un punto
- **Mutación**: Cambio aleatorio de sede
- **Elitismo**: Mejor individuo siempre se conserva

## Función de Fitness

```
fitness = 1 / (1 + costo_total + penalizaciones)

donde:
costo_total = Σ (costo_base + costo_distancia) * factor_continente * factor_prioridad
penalizaciones = violaciones_factibilidad * 2 * costo + desbalance_sedes * 1000
```

### Factores de Costo
- **Costo base**: $10 por producto
- **Costo distancia**: $0.1 por km
- **Factor continente**: 1.0 (mismo), 1.5 (diferente)
- **Factor prioridad**: 3.0 (alta), 2.0 (media), 1.0 (baja)

## Salida del Programa

El programa muestra:
1. **Estadísticas de datos cargados**
2. **Progreso de optimización** (cada 100 generaciones)
3. **Comparación con solución aleatoria**
4. **Resultados finales**:
   - Fitness y costo total
   - Distribución de pedidos por sede
   - Detalles de primeras asignaciones
   - Porcentaje de mejora obtenido

## Ejemplo de Salida

```
=== SISTEMA DE OPTIMIZACIÓN MORAPACK ===
Cargando datos...
Cargados 25 aeropuertos
Cargados 45 vuelos
Generados 50 pedidos de ejemplo
Identificadas 3 sedes MoraPack

Iniciando optimización genética...
Población: 100, Generaciones: 1000, Pc: 0.80, Pm: 0.10
Fitness inicial: 0.000123
Generación 100: Fitness=0.000456 (270.73% mejora)
...
Optimización completada. Mejora total: 206.66%

=== COMPARACIÓN CON SOLUCIÓN ALEATORIA ===
Costo solución aleatoria: $125,430.25
Costo solución optimizada: $82,156.78
Mejora obtenida: 34.52%
```

## Características Técnicas

### Clases Principales
- `Aeropuerto`: Modelo de aeropuerto con cálculo de distancias
- `Pedido`: Modelo de pedido con reglas de negocio MoraPack
- `IndividuoMoraPack`: Representación genética de soluciones
- `OptimizadorMoraPack`: Motor de optimización genética
- `CargadorDatosMoraPack`: Carga de datos desde CSV

### Algoritmos Utilizados
- **Haversine**: Cálculo de distancias geográficas
- **Torneo**: Selección de padres
- **Elitismo**: Preservación de mejores soluciones
- **Cruce un punto**: Intercambio genético
- **Mutación uniforme**: Exploración del espacio de soluciones

## Dependencias

- Java 8 o superior
- Archivos CSV con datos de aeropuertos y vuelos
- Sistema Unix/Linux para scripts bash

## Autor

Sistema desarrollado para optimización logística de MoraPack usando algoritmos genéticos.
